package examples

import cats.effect.Sync
import fs2.Stream
import io.circe.Decoder
import taskapis.TaskStreamServer

import scala.concurrent.{ExecutionContext, Future}

class ImplWithFutures extends TaskStreamServer[Future, Int, Int] {
  implicit val ec = ExecutionContext.global
  def streamChangesFromIntermediary(config: Option[Int])(implicit F: Sync[Future], d: Decoder[Int]): fs2.Stream[Future, Int] = Stream.eval(Future(1))
}
