package taskapis

import cats.effect.Sync
import fs2.Stream
import io.circe.Decoder

trait TaskStreamServer[F[_], Config, Change]  {

  def streamChangesFromIntermediary(config: Option[Config] = None)(implicit F: Sync[F], d: Decoder[Change]): Stream[F, Change]
}
