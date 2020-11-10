package examples

import cats.effect.{ContextShift, IO, Sync, Timer}
import doobie.util.transactor.Transactor.Aux
import doobie.implicits._
import examples.config.KafkaProducerConfig
import examples.postgre.PostgreClient
import examples.kafka.KafkaClient
import fs2.Stream
import fs2.kafka.ProducerResult
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import taskapis.TaskWriteServer

trait TaskWriteServerPostgreKafkaImpl extends TaskWriteServer[IO, Aux[IO, Unit], KafkaProducerConfig, Task, Change, ProducerResult[String, String, Unit]] with KafkaClient with PostgreClient {
  implicit protected val cs: ContextShift[IO]
  implicit protected val t: Timer[IO]

  def findTask(task: Task) = findOne[Task](sql"SELECT id, ownerId, detail, done FROM tasks WHERE id = ${task.id} AND ownerId = ${task.ownerId}")

  def updateOrWriteTask(oldTask: Option[Task], updatedTask: Task) = replace(oldTask match {
    case Some(task) => sql"UPDATE tasks SET detail = ${updatedTask.detail}, done = ${updatedTask.done} WHERE id = ${task.id} AND ownerId = ${task.ownerId}"
    case None => sql"INSERT INTO tasks (id, ownerId, detail, done) values (${updatedTask.id}, ${updatedTask.ownerId}, ${updatedTask.detail}, ${updatedTask.done})"
  }).run

  def writeToDatabase(transactor: Option[Aux[IO, Unit]], task: Task)(implicit F: Sync[IO], e: Encoder[Task], d: Decoder[Task]) = {
    for {
      originalTask <- findTask(task)
      _ <- updateOrWriteTask(originalTask, task)
      updatedTask <- findTask(task).map(_.getOrElse(throw new Throwable("Couldn't update task")))
    } yield Change(originalTask, updatedTask)
  }.transact(transactor.getOrElse(throw new Throwable("Missing postgre config")))

  def sendChangeToIntermediary(configOption: Option[KafkaProducerConfig], changes: Stream[IO, Change])(implicit F: Sync[IO], e: Encoder[Change]) =
    for {
      config <- Stream.eval(IO.fromOption(configOption)(new Throwable("Missing kafka config")))
      settings = defaultProducerSettings(config.servers)
      stream = changes.map(_.asJson.noSpaces).map(change => createProducerRecord(config.topic, change, change))
      produce <- produceToTopic(settings, stream)
    } yield produce

  def writeTaskAndStreamChange(transactor: Option[Aux[IO, Unit]], task: Task, configOption: Option[KafkaProducerConfig])(implicit et: Encoder[Task], d: Decoder[Task], ec: Encoder[Change]) =
    sendChangeToIntermediary(configOption, Stream.eval(writeToDatabase(transactor, task))).take(1).map { _ =>
      s"Task ${task.id} has been updated."
    }
}

