package ws

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor

package object gmax {
  implicit val system: ActorSystem = ActorSystem("oauth2")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(30, TimeUnit.SECONDS)

  implicit val config: Config = system.settings.config
  val port: Int = config.getInt("app.http-port")
  val withCorsFilter: Boolean = config.getBoolean("app.withCorsFilter")
}
