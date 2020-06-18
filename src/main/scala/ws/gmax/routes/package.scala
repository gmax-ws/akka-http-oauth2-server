package ws.gmax

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import ws.gmax.model.{AccessToken, AuthInfo, OAuth2Error, PemKeys}

package object routes extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val simpleResponseFormat: RootJsonFormat[SimpleResponse] = jsonFormat2(SimpleResponse)
  implicit val pemKeysFormat: RootJsonFormat[PemKeys] = jsonFormat2(PemKeys)
  implicit val accessTokenFormat: RootJsonFormat[AccessToken] = jsonFormat4(AccessToken)
  implicit val errorFormat: RootJsonFormat[OAuth2Error] = jsonFormat1(OAuth2Error)
  implicit val authInfoFormat: RootJsonFormat[AuthInfo] = jsonFormat3(AuthInfo)
}
