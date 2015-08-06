import internbookmark._
import jp.ne.hatena.intern.scalatra.HatenaOAuth
import org.scalatra._
import javax.servlet.ServletContext
import internbookmark.service.Context

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    Context.setup("db.default")
    context.mount(new internbookmark.web.BookmarkWeb, "/*")
    context.mount(new HatenaOAuth, "/auth")
  }

  override def destroy(context: ServletContext): Unit = {
    Context.destroy()
  }
}
