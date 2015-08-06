package internbookmark.repository

import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.MySQLDriver.api._

object Identifier {
  def generate(implicit ctx: Context): Long =
    run(sql"SELECT UUID_SHORT() as ID".as[Long].map(_.head))
}
