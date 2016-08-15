package internbookmark.repository

import internbookmark.model.User
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.LocalDateTime
import slick.driver.MySQLDriver.api._
import slick.jdbc.GetResult
import com.github.tototoshi.slick.MySQLJodaSupport._

object Users {
  private type UserRow = User
  private val UserRow = User
  private implicit val getUserRowResult = GetResult(r => UserRow(r.<<, r.<<, r.<<, r.<<))

  def find(userId: Long)(implicit ctx: Context): Option[User] = run(
    sql"""SELECT * FROM user WHERE id = $userId LIMIT 1""".as[UserRow].map(_.headOption)
  )

  private def findByName(name: String)(implicit ctx: Context): Option[User] = run(
    sql"""SELECT * FROM user WHERE name = $name LIMIT 1""".as[UserRow].map(_.headOption)
  )

  def findOrCreateByName(name: String)(implicit ctx: Context): User =
    findByName(name).getOrElse(createByName(name))

  def createByName(name: String)(implicit ctx: Context): User = {
    val id = Identifier.generate
    val user = User(id, name, new LocalDateTime(), new LocalDateTime())
    run(sqlu"""
      INSERT INTO user
        (id, name, created_at, updated_at)
        VALUES
        (
          ${user.id},
          ${user.name},
          ${user.createdAt},
          ${user.updatedAt}
        )
    """)
    user
  }
}
