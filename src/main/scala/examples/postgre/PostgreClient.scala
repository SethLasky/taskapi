package examples.postgre

import doobie._

trait PostgreClient{
  def findOne[X: Read](sql: Fragment): doobie.ConnectionIO[Option[X]] = sql.query[X].option

  def replace(sql: Fragment) = sql.update
}
