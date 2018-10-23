package ws.gmax.cors

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives.{complete, options, respondWithHeaders, _}
import akka.http.scaladsl.server.Route

trait CorsRoutes {

  private val corsResponseHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With"),
    `Access-Control-Allow-Methods`(GET, POST, PUT, DELETE, OPTIONS)
  )

  /** CORS preflight request */
  private val preFlight: Route = options {
    respondWithHeaders(corsResponseHeaders) {
      complete(NoContent)
    }
  }

  protected def withCorsHandler(enabled: Boolean)(inner: Route): Route =
    if (enabled) {
      respondWithHeaders(`Access-Control-Allow-Origin`.*) {
        preFlight ~ inner
      }
    } else {
      inner
    }
}