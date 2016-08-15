package internbookmark
import slick.dbio.{NoStream, DBIOAction}

package object repository {
  private[repository] def run[R](a: DBIOAction[R, NoStream, Nothing])(implicit ctx: Context): R = Context.runDBIO(a)
}
