package examples

import cats.effect.{ContextShift, IO, Sync, Timer}
import examples.config.KafkaConsumerConfig
import examples.kafka.KafkaClient
import fs2.Stream
import io.circe.Decoder
import io.circe.parser.decode
import taskapis.TaskStreamServer

trait TaskStreamServerKafkaImpl extends TaskStreamServer[IO, KafkaConsumerConfig, Change] with KafkaClient {
  implicit protected val cs: ContextShift[IO]
  implicit protected val t: Timer[IO]

  def streamChangesFromIntermediary(configOption: Option[KafkaConsumerConfig])(implicit F: Sync[IO], d: Decoder[Change]): Stream[IO, Change] = for {
    config <- Stream.eval(IO.fromOption(configOption)(new Throwable("Missing kafka config")))
    settings = defaultConsumerSettings(config.servers, config.groupId)
    stream <- consumeFromTopic(settings, config.topic).map(_.record.value).evalMap(value => IO.fromEither(decode[Change](value)))
  } yield stream
}

case class Task(id: Int, ownerId: Int, detail: String, done: Boolean)

case class Change(originalTask: Option[Task], newTask: Task)