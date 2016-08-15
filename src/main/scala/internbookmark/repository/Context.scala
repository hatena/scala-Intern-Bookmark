package internbookmark.repository

import db.JdbcBackend
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.dbio.{NoStream, DBIOAction}

object Context {
  private var db: JdbcBackend.DatabaseDef = null

  def setup(configName: String) {
    db = JdbcBackend.Database.forConfig(configName).asInstanceOf[JdbcBackend.DatabaseDef]
  }

  def createContext(): Context = Context(db, db.startSession(useSameThread = true))

  def releaseContext(ctx: Context) = db.endSession(ctx.ctx)

  def withContext[T](f: Context => T): T = {
    val ctx = createContext()
    try {
      f(ctx)
    } finally releaseContext(ctx)
  }

  def destroy() {
    if(db != null) Await.result(db.shutdown, Duration.Inf)
  }

  def runDBIO[R](a: DBIOAction[R, NoStream, Nothing])(implicit ctx: Context): R =
    Await.result(ctx.db.withContext(a)(ctx.ctx), Duration.Inf)

}

case class Context(db: JdbcBackend.DatabaseDef, ctx: JdbcBackend.Context) {
  def withTransaction[T](f: => T): T =
    ctx.session.asInstanceOf[db.TransactionalSession].withTransaction(f)
}
