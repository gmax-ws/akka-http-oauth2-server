package ws.gmax.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import ws.gmax.model._
import ws.gmax.service.OAuth2Service

import scala.util.{Failure, Success}

object OAuth2Protocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val pemKeysFormat: RootJsonFormat[PemKeys] = jsonFormat2(PemKeys)
  implicit val accessTokenFormat: RootJsonFormat[AccessToken] = jsonFormat4(AccessToken)
  implicit val errorFormat: RootJsonFormat[OAuth2Error] = jsonFormat1(OAuth2Error)
  implicit val authInfoFormat: RootJsonFormat[AuthInfo] = jsonFormat3(AuthInfo)
}

trait OAuth2Routes {
  self: OAuth2Service =>

  import ws.gmax.routes.OAuth2Protocol._

  private def redirectUser(request: AuthorizationMessage, code: AccessCode) = {
    val uri = s"${request.redirectUri}?code=${code.code}&state=${request.state}"
    redirect(uri, PermanentRedirect)
  }

  private def processRequest(request: OAuth2Message): Route =
    onComplete(oauth2Request(request).mapTo[Either[AccessTokenError, AccessResult]]) {
      case Success(response) => response match {
        case Right(result) =>
          result match {
            case token: AccessToken => complete(token)
            case code: AccessCode =>
              request match {
                case req: AuthorizationMessage => redirectUser(req, code)
                case _ => reject // should not happen
              }
          }
        case Left(error) => complete((error.code, error.message))
      }
      case Failure(th) => failWith(th)
    }

  val authorizationCode: Route = formFields('grant_type, 'client_id, 'client_secret, 'scope, 'redirect_uri) {
    (grantType, clientId, clientSecret, scope, redirectUri) =>
      processRequest(AuthorizationCodeMessage(grantType, clientId, clientSecret, scope, redirectUri))
  }

  val userCredentials: Route = formFields('grant_type, 'client_id, 'client_secret, 'scope, 'username, 'password) {
    (grantType, clientId, clientSecret, scope, username, password) =>
      processRequest(UserCredentialsMessage(grantType, clientId, clientSecret, scope, username, password))
  }

  val clientCredentials: Route = formFields('grant_type, 'client_id, 'client_secret, 'scope) {
    (grantType, clientId, clientSecret, scope) =>
      processRequest(ClientCredentialsMessage(grantType, clientId, clientSecret, scope))
  }

  val authorize: Route = path("authorize") {
    get {
      parameters('response_type, 'client_id, 'redirect_uri, 'scope, 'state) {
        (responseType, clientId, redirectUri, scope, state) =>
          processRequest(AuthorizationMessage(responseType, clientId, redirectUri, scope, state))
      }
    }
  }

  val accessToken: Route = path("access_token") {
    post {
      authorizationCode ~ userCredentials ~ clientCredentials
    }
  }

  val validation: Route = path("validation") {
    get {
      parameters('token) { token =>
        onComplete(validateToken(token).mapTo[Either[Throwable, AuthInfo]]) {
          case Success(info) => complete(info)
          case Failure(th) => failWith(th)
        }
      }
    }
  }

  val publicKey: Route = path("publicKey") {
    get {
      onComplete(getPublicKey.mapTo[String]) {
        case Success(p) => complete(p)
        case Failure(th) => failWith(th)
      }
    }
  }

  val pemKeys: Route = path("generateKeyPair") {
    get {
      onComplete(generateKeys.mapTo[PemKeys]) {
        case Success(p) => complete(p)
        case Failure(th) => failWith(th)
      }
    }
  }

  val oauthRoutes: Route = authorize ~ accessToken ~ validation ~ publicKey ~ pemKeys
}
