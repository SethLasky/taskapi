import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, Timer}
import examples.{Change, Task}
import io.circe.fs2._
import examples.engine.Engine
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import io.circe.generic.auto._
import fs2.Stream
import org.http4s.{Method, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.Random

class EngineRegressionTest extends AnyWordSpecLike with Matchers with Http4sClientDsl[IO]{

  implicit lazy val ec = ExecutionContext.global
  implicit val ctxShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)

  "The example engine" must {

    "scale to an arbitrary number and behave in a distributed manner" in {
      val instances = 9
      val tasks = 20

      val test = for{
        client <- BlazeClientBuilder[IO](ec).stream concurrently runEngines(instances)
        _ <- checkEngines(instances, client).reduce(_ + _)
        stream <- runStreamers(instances, client, tasks) concurrently Stream.eval(IO.sleep(500.milliseconds)).flatMap(_ => runTasks(tasks, instances, client))
      } yield stream

      test.take(instances * tasks).compile.toList.unsafeRunSync().size shouldBe instances * tasks
    }
  }

  def runTasks(tasks: Int, instances: Int, client: Client[IO], id: Int = 1234, ownerId: Int = 5678) = {
    intStream(tasks).evalMap { int =>
      updateTask(Task(id, ownerId, int.toString, false), client, port(Random.between(1, instances)).toString)
    }
  }

  def intStream(number: Int) = Stream.emits(1 to number).covary[IO]

  def runStreamers(number: Int, client: Client[IO], tasks: Int) = intStream(number).map{ int =>
    streamChanges(client, port(int).toString)
  }.parJoinUnbounded

  def runEngines(number: Int) = intStream(number).mapAsync[IO, ExitCode](number){ int =>
    Engine.run(List(port(int).toString))
  }

  def port(add: Int, default: Int = 6000) = default + add

  def checkEngines(number: Int, client: Client[IO]) = intStream(number).evalMap(int => ping(client, port(int).toString)).take(number)

  def updateTask(task: Task, client: Client[IO], port: String) = {
    val request = Method.POST(task.asJson.noSpaces, Uri.fromString(s"http://localhost:$port/tasks").right.get)
    request.flatMap(client.expect[String])
  }

  def streamChanges(client: Client[IO], port: String) = {
    val request = Method.GET(Uri.fromString(s"http://localhost:$port/tasks").right.get)
    Stream.eval(request).flatMap(client.stream).flatMap(_.body.through(byteStreamParser).through(decoder[IO, Change]))
  }

  def ping(client: Client[IO], port: String): IO[String] = {
    val request = Method.GET(Uri.fromString(s"http://localhost:$port/ping").right.get)
    request.flatMap(client.expect[String])
  }.flatMap(s => if(s == "pong") IO(s) else ping(client, port)).handleErrorWith(_ => ping(client, port))
}
