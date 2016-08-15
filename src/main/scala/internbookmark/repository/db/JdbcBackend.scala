package internbookmark.repository.db

import scala.concurrent.Future
import scala.util.control.NonFatal
import slick.dbio.{NoStream, DBIOAction}
import slick.jdbc.{JdbcDataSource, TransactionalJdbcBackend}
import slick.util.AsyncExecutor

trait JdbcBackend extends TransactionalJdbcBackend {

  override val Database = new DatabaseFactoryDef {}

  trait DatabaseFactoryDef extends super.DatabaseFactoryDef {
    override def forSource(
      source: JdbcDataSource,
      executor: AsyncExecutor = AsyncExecutor.default()
    ): DatabaseDef = new DatabaseDef(source, executor)
  }

  class DatabaseDef(override val source: JdbcDataSource, override val executor: AsyncExecutor) extends super.TransactionalDatabaseDef(source, executor) {
    def withContext[R](a: DBIOAction[R, NoStream, Nothing])(ctx: Context): Future[R] =
      runInContext(a)(ctx)

    def startSession(useSameThread: Boolean): Context = {
      val ctx = createDatabaseActionContext(useSameThread)
      acquireSession(ctx)
      ctx.pin
      ctx
    }

    def endSession(ctx: Context): Unit = {
      ctx.unpin
      releaseSession(ctx, discardErrors = true)
    }

    private final def runInContext[R](a: DBIOAction[R, NoStream, Nothing])(ctx: Context): Future[R] =
      try runInContext(a, ctx, false, true)
      catch { case NonFatal(ex) => Future.failed(ex) }
  }
}

object JdbcBackend extends JdbcBackend
