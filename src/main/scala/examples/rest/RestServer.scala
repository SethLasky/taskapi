package examples.rest

import org.http4s.{HttpRoutes, Response}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import cats.effect.{ContextShift, IO, Timer}
import io.circe.generic.auto._
import examples.config.Config
import examples.{Task, TaskStreamServerKafkaImpl, TaskWriteServerPostgreKafkaImpl}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._

import scala.concurrent.duration._
import scala.language.postfixOps

trait RestServer extends Http4sDsl[IO] with TaskStreamServerKafkaImpl with TaskWriteServerPostgreKafkaImpl {

  implicit protected val cs: ContextShift[IO]
  implicit protected val t: Timer[IO]

  def server(config: Config) = {
    val transactor = config.postgre.map(_.transactor)
    val service = HttpRoutes.of[IO] {
      case GET -> Root / "ping" => Ok("pong")
      case GET -> Root / "tasks" => Ok(streamChangesFromIntermediary(config.kafka.consumer))
      case GET -> Root / "tasks" / ownerId => Ok(streamChangesFromIntermediary(config.kafka.consumer).filter(_.newTask.ownerId.toString == ownerId))
      case GET -> Root / "tasks" / ownerId / id => Ok(streamChangesFromIntermediary(config.kafka.consumer).filter(change => change.newTask.ownerId.toString == ownerId && change.newTask.id.toString == id))
      case request@POST -> Root / "tasks" => request.decode[Task] {
        writeTaskAndStreamChange(transactor, _, config.kafka.producer).attempt.evalMap(handleResponse).compile.toList.map(_.head)
      }
    }

    val routes = CORS(service)
    val httpApp = Router("/" -> routes).orNotFound
    BlazeServerBuilder[IO].bindHttp(config.http.port, config.http.host).withHttpApp(httpApp).withIdleTimeout(5.minutes).serve
  }

  private def handleResponse(either: Either[Throwable, String]): IO[Response[IO]] = either match {
    case Right(str) => Ok(str)
    case Left(err) => BadRequest(s"${err.getMessage}")
  }
}
