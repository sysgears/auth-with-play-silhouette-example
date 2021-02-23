package controllers

import javax.inject.Inject
import play.api.libs.json.JsString
import play.api.mvc.{ AnyContent, Request }

import scala.concurrent.ExecutionContext

/**
 * The `Index` controller.
 */
class IndexController @Inject() (
  components: SilhouetteControllerComponents
)(implicit ex: ExecutionContext) extends SilhouetteController(components) {

  def index = UnsecuredAction { implicit request: Request[AnyContent] =>
    Ok(JsString("Hello"))
  }
}
