package taskapis

import cats.effect.Sync
import fs2.Stream
import io.circe.{Decoder, Encoder}

trait TaskWriteServer[F[_], DBConfig, IntermediaryConfig, Task, Change, IntermediaryResult]  {

  def writeToDatabase(config: Option[DBConfig], task: Task)(implicit F: Sync[F], e: Encoder[Task], d: Decoder[Task]): F[Change]

  def sendChangeToIntermediary(config: Option[IntermediaryConfig], changes: Stream[F, Change])(implicit F: Sync[F], e: Encoder[Change]): Stream[F, IntermediaryResult]
}