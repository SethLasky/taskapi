package examples.kafka

import cats.effect.{ContextShift, IO, Timer}
import fs2.Stream
import fs2.kafka._

trait KafkaClient {

  def defaultConsumerSettings(servers: String, groupId: String) =
    ConsumerSettings[IO, String, String]
      .withAutoOffsetReset(AutoOffsetReset.Latest)
      .withBootstrapServers(servers)
      .withGroupId(groupId)

  def defaultProducerSettings(servers: String) =
    ProducerSettings[IO, String, String]
      .withBootstrapServers(servers)

  def createProducerRecord(topic: String, key: String, value: String) = ProducerRecords.one(ProducerRecord(topic, key, value))

  def consumeFromTopic(settings: ConsumerSettings[IO, String, String], topic: String)(implicit cs: ContextShift[IO], t: Timer[IO]) =
    consumerStream[IO].using(settings).evalTap(_.subscribeTo(topic)).flatMap(_.stream)

  def produceToTopic[X](settings: ProducerSettings[IO, String, String], stream: Stream[IO, ProducerRecords[String, String, X]])(implicit cs: ContextShift[IO], t: Timer[IO]) =
    stream.through(produce(settings))
}
