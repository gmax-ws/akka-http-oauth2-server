package ws.gmax.oauth

import akka.http.scaladsl.model.StatusCodes._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.lang3.RandomStringUtils
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import spray.json._
import ws.gmax.model._

class JwtToken(config: Config) extends LazyLogging {

  import ws.gmax.routes.OAuth2Protocol._

  private val sessions = scala.collection.mutable.Map[String, AuthorizationMessage]()

  private val privateKey = config.getString("oauth2.privateKey")
  val publicKey = config.getString("oauth2.publicKey")
  private val iss = config.getString("oauth2.iss")

  private val applicationId = config.getString("oauth2.applicationId")
  private val expireIn = config.getLong("oauth2.expireIn")

  private val tokenType = config.getString("oauth2.tokenType")
  private val realm = config.getString("oauth2.realm")
  private val scopes = config.getStringList("oauth2.scopes")

  private def authenticateClient(clientId: String, clientSecret: String) =
    clientId == "system" && clientSecret == "manager"

  private def authenticateUser(username: String, password: String) =
    username == "client" && password == "secret"

  private def checkScopes(reqScopes: Set[String]) =
    reqScopes forall (scopes.contains(_))

  private def clientRoles(reqScopes: Set[String]) = reqScopes map {
    case "read" => "ROLE_READ"
    case "write" => "ROLE_WRITE"
    case "admin" => "ROLE_ADMIN"
  }

  private def jwt(about: String, to: String, roles: Set[String]) = {
    val claim = JwtClaim()
      .by(iss)
      .about(about)
      .to(to)
      .issuedNow
      .expiresIn(expireIn)
      .+("authorization", roles)
    Jwt.encode(claim, privateKey, JwtAlgorithm.RS256)
  }

  private def makeJwtToken(grantType: String, clientId: String, scope: String): Either[AccessTokenError, AccessToken] = {
    val reqScopes = scope.split(" ").toSet
    if (checkScopes(reqScopes)) {
      val token = jwt(grantType, clientId, clientRoles(reqScopes))
      Right(AccessToken(token, expireIn, tokenType, scope))
    } else {
      Left(AccessTokenError(BadRequest, s"Invalid scope: $scope"))
    }
  }

  private def makeToken(request: AuthorizationCodeMessage) = {
    sessions.get(request.code) match {
      case Some(data) =>
        sessions.remove(request.code)
        if (request.redirectUri == data.redirectUri) {
          makeJwtToken(request.grantType, request.clientId, data.scope)
        } else {
          Left(AccessTokenError(BadRequest, s"Invalid redirect URI: ${request.redirectUri}"))
        }
      case None =>
        Left(AccessTokenError(BadRequest, s"Invalid code: ${request.code}"))
    }
  }

  private def clientCredentialsGrant(request: ClientCredentialsMessage) = {
    if (authenticateClient(request.clientId, request.clientSecret)) {
      request.grantType match {
        case "client_credentials" => makeJwtToken(request.grantType, request.clientId, request.scope)
        case _ => Left(AccessTokenError(BadRequest, s"Invalid grant type: ${request.grantType}"))
      }
    } else {
      Left(AccessTokenError(Unauthorized, s"realm = $realm client = ${request.clientId}"))
    }
  }

  private def userCredentialsGrant(request: UserCredentialsMessage) = {
    if (authenticateClient(request.clientId, request.clientSecret)) {
      request.grantType match {
        case "password" =>
          if (authenticateUser(request.username, request.password)) {
            makeJwtToken(request.grantType, request.clientId, request.scope)
          } else {
            Left(AccessTokenError(Unauthorized, s"realm = $realm username = ${request.username}"))
          }
        case _ => Left(AccessTokenError(BadRequest, s"Invalid grant type: ${request.grantType}"))
      }
    } else {
      Left(AccessTokenError(Unauthorized, s"realm = $realm client = ${request.clientId}"))
    }
  }

  private def authorizationCodeGrant1(request: AuthorizationMessage) = {
    if (request.responseType == "code") {
      val reqScopes = request.scope.split(" ").toSet
      if (checkScopes(reqScopes)) {
        val code = RandomStringUtils.randomAlphanumeric(16)
        sessions += code -> request
        Right(AccessCode(code))
      } else {
        Left(AccessTokenError(BadRequest, s"Invalid scope: ${request.scope}"))
      }
    } else if (request.responseType == "token") {
      makeJwtToken(request.responseType, request.clientId, request.scope)
    } else {
      Left(AccessTokenError(BadRequest, s"Unknown response type: ${request.responseType}"))
    }
  }

  private def authorizationCodeGrant(request: AuthorizationCodeMessage) = {
    if (authenticateClient(request.clientId, request.clientSecret)) {
      request.grantType match {
        case "authorization_code" => makeToken(request)
        case _ => Left(AccessTokenError(BadRequest, s"Invalid grant type: ${request.grantType}"))
      }
    } else {
      Left(AccessTokenError(Unauthorized, s"realm = $realm client = ${request.clientId}"))
    }
  }

  def issue(message: OAuth2Message): Either[AccessTokenError, AccessResult] = message match {
    case request: ClientCredentialsMessage => clientCredentialsGrant(request)
    case request: UserCredentialsMessage => userCredentialsGrant(request)
    case request: AuthorizationCodeMessage => authorizationCodeGrant(request)
    case request: AuthorizationMessage => authorizationCodeGrant1(request)
  }

  def verifyToken(token: String, secret: String): Either[Throwable, AuthInfo] =
    Jwt.decode(token, secret, Seq(JwtAlgorithm.RS256)).map { decodedJson =>
      logger.info(s"Decoded json token $decodedJson")
      decodedJson.parseJson.convertTo[AuthInfo]
    }.toEither

  def validate(token: String): Either[Throwable, AuthInfo] =
    verifyToken(token, publicKey)

  def pemKeys(): PemKeys =
    PemKeys(Pem.getPem(), Pem.getPem(false))
}

object JwtToken {
  def apply(config: Config): JwtToken = new JwtToken(config)
}
