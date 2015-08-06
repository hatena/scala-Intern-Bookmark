package internbookmark
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import slick.util.AsyncExecutor
import slick.jdbc.JdbcDataSource
import slick.dbio.{NoStream, DBIOAction}

package object repository {
  trait MyJdbcBackend extends slick.jdbc.JdbcBackend {

    override val Database = new DatabaseFactoryDef {}

    trait DatabaseFactoryDef extends super.DatabaseFactoryDef {
      override def forSource(source: JdbcDataSource, executor: AsyncExecutor = AsyncExecutor.default()) =
        new DatabaseDef(source, executor)
    }

    class DatabaseDef(override val source: JdbcDataSource, override val executor: AsyncExecutor) extends super.DatabaseDef(source, executor) {
      def runWithCtx[R](a: DBIOAction[R, NoStream, Nothing])(ctx: Context): Future[R] = {
        runInternalWithContext(a)(ctx)
      }

      def createContext(useSameThread: Boolean): Context = {
        val ctx = createDatabaseActionContext(useSameThread)
        acquireSession(ctx)
        ctx.pin
        ctx
      }

      def releaseContext(ctx: Context): Unit = {
        ctx.unpin
        releaseSession(ctx, discardErrors = true)
      }

      private final def runInternalWithContext[R](a: DBIOAction[R, NoStream, Nothing])(ctx: Context): Future[R] =
        try runInContext(a, ctx, false, true)
        catch { case NonFatal(ex) => Future.failed(ex) }
    }
  }

  object MyJdbcBackend extends MyJdbcBackend

  object Context {
    private var db: MyJdbcBackend.DatabaseDef = null

    def setup(configName: String) {
      db = MyJdbcBackend.Database.forConfig(configName).asInstanceOf[MyJdbcBackend.DatabaseDef]
    }

    def createContext(): Context = Context(db, db.createContext(useSameThread = true))

    def releaseContext(ctx: Context) = db.releaseContext(ctx.ctx)

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
      Await.result(ctx.db.runWithCtx(a)(ctx.ctx), Duration.Inf)
  }

  case class Context(db: MyJdbcBackend.DatabaseDef, ctx: MyJdbcBackend.Context) {
    def withTransaction[T](f: => T): T = ctx.session.withTransaction(f)
  }

  private[repository] def run[R](a: DBIOAction[R, NoStream, Nothing])(implicit ctx: Context): R = Context.runDBIO(a)
}
