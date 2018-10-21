package ws.gmax.model

import akka.http.scaladsl.model.StatusCode

sealed trait OAuth2Message

case class ClientCredentialsMessage(grantType: String, clientId: String, clientSecret: String, scope: String) extends OAuth2Message

case class UserCredentialsMessage(grantType: String, clientId: String, clientSecret: String, scope: String, username: String, password: String) extends OAuth2Message

case class AuthorizationCodeMessage(grantType: String, clientId: String, clientSecret: String, redirectUri: String, code: String) extends OAuth2Message

case class AuthorizationMessage(responseType: String, clientId: String, redirectUri: String, scope: String, state: String) extends OAuth2Message

sealed trait JwtMessage

case class VerifyTokenMessage(token: String) extends JwtMessage

case object GenerateKeyPair extends JwtMessage

case object PublicKeyMessage extends JwtMessage

sealed trait AccessResult

case class AccessToken(access_token: String, expires_in: Long, token_type: String, scope: String) extends AccessResult

case class AccessCode(code: String) extends AccessResult

case class AccessTokenError(code: StatusCode, message: String)

case class OAuth2Error(error: String)

case class AuthInfo(iss: String, sub: String, authorization: Option[Set[String]])

case class PemKeys(publicKey: String, privateKey: String)