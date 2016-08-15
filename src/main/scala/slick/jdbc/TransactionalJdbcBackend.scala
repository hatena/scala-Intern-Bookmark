package slick
package jdbc

import slick.util.AsyncExecutor

trait TransactionalJdbcBackend extends JdbcBackend {
  class TransactionalDatabaseDef(
    source: JdbcDataSource,
    executor: AsyncExecutor = AsyncExecutor.default()
  ) extends super.DatabaseDef(source, executor) {
    override def createSession(): Session = new TransactionalSession(this)

    class TransactionalSession(database: Database)
        extends BaseSession(database) {
      protected var doRollback = false

      def rollback(): Unit = {
        if(conn.getAutoCommit) {
          val msg = "Cannot roll back session in auto-commit mode"
          throw new SlickException(msg)
        }
        doRollback = true
      }

      def withTransaction[T](f: => T): T = if (isInTransaction) f else {
        startInTransaction
        try {
          var done = false
          try {
            doRollback = false
            val res = f
            if (doRollback) conn.rollback()
            else conn.commit()
            done = true
            res
          } finally if (!done) conn.rollback()
        } finally endInTransaction(())
      }
    }
  }
}
