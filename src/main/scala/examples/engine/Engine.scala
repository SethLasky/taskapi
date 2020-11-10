package examples.engine

import cats.effect.{ContextShift, ExitCode, IO, IOApp, Timer}
import examples.config.Config
import examples.rest.RestServer
import fs2.Stream
import io.circe.config.parser
import io.circe.generic.auto._

object Engine extends IOApp with RestServer {

  override implicit protected val cs: ContextShift[IO] = contextShift
  override implicit protected val t: Timer[IO] = timer

  def getConfig(args: List[String]) = Stream.eval(parser.decodeF[IO, Config]()).map{ config =>
    if(args.nonEmpty) config.copy(http = config.http.copy(port = args.head.toInt)) else config
  }
  override def run(args: List[String]): IO[ExitCode] = {
    getConfig(args).flatMap(server)
  }.compile.drain.as(ExitCode.Success)
}
