package examples.config

import cats.effect.{ContextShift, IO}
import doobie.util.transactor.Transactor

import scala.util.Random

case class Config(kafka: KafkaConfig, postgre: PostgreConfig, http: HttpConfig)

case class KafkaConfig(consumer: Option[KafkaConsumerConfig], producer: Option[KafkaProducerConfig])

case class KafkaConsumerConfig(topic: String, servers: String){
  def groupId = Random.alphanumeric.take(10).mkString
}

case class KafkaProducerConfig(topic: String, servers: String)

case class PostgreConfig(url: String, user: String, pass: String){
  def transactor(implicit cs: ContextShift[IO]) = Transactor.fromDriverManager[IO]("org.postgresql.Driver", url, user, pass)
}

case class HttpConfig(host: String, port: Int)
