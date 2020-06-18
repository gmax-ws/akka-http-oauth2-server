package ws.gmax

import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import ws.gmax.actor.OAuth2SupervisorActor
import ws.gmax.routes.{CorsRoutes, _}
import ws.gmax.service.OAuth2Service

import scala.concurrent.Future
import scala.io.StdIn

final case class SimpleResponse(message: String, timestamp: String =
  ISODateTimeFormat.dateTime().print(new DateTime()))

/** Global exception handler */
sealed trait ExceptionHandling {
  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case th: Throwable => complete((InternalServerError, SimpleResponse(th.getMessage)))
  }
}

/** Global rejection handler */
sealed trait RejectionHandling {

  implicit def rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle { case MissingCookieRejection(cookieName) =>
      complete((BadRequest, SimpleResponse(s"No cookies, $cookieName no service!!!")))
    }
    .handle { case AuthorizationFailedRejection =>
      complete((Forbidden, SimpleResponse("You're out of your depth!")))
    }
    .handle { case ValidationRejection(message, _) =>
      complete((InternalServerError, SimpleResponse(s"That wasn't valid! $message")))
    }
    .handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete((MethodNotAllowed, SimpleResponse(s"Can't do that! Supported: ${names mkString " or "}!")))
    }
    .handleNotFound {
      complete((NotFound, SimpleResponse("Not here!")))
    }
    .result()
}

sealed trait AppTrait extends ExceptionHandling with RejectionHandling with CorsRoutes with LazyLogging {

  def shutdownAndExit(code: Int): Unit = {
    system.terminate()
    System.exit(code)
  }

  def startHttpServer(routes: Route): Future[Http.ServerBinding] = {
    val httpServer = Http().bindAndHandle(routes, "0.0.0.0", port)
    println(s"HTTP server is ready http://localhost:$port/${system.name}/")
    print("Press RETURN to stop...")
    httpServer
  }

  def stopHttpServer(httpServer: Future[Http.ServerBinding]): Unit = {
    println("Shutting down HTTP server...")
    httpServer
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { _ ⇒
        println("HTTP server is down")
        shutdownAndExit(0)
      }
  }

  def startupApp(): Unit = {
    /** Create other actors */
    val oauth2SupervisorActor: ActorRef = system.actorOf(OAuth2SupervisorActor(),
      "oauth2SupervisorActor")

    val services = OAuth2Service(oauth2SupervisorActor)

    /** Routes */
    val routes: Route = handleExceptions(exceptionHandler) {
      withCorsHandler(withCorsFilter) {
        pathPrefix(system.name) {
          services.oauthRoutes
        }
      }
    }

    val httpServer: Future[Http.ServerBinding] = startHttpServer(routes)
    StdIn.readLine() // let it run until user presses return
    stopHttpServer(httpServer)
  }
}

object OAuth2ServiceBoot extends App with AppTrait {
  startupApp()
}
