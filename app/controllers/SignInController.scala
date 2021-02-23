package controllers

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.Credentials
import javax.inject.Inject
import play.api.i18n.Lang
import play.api.libs.json.{ JsString, Json }
import play.api.mvc.{ AnyContent, Request }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * The `Sign In` controller.
 */
class SignInController @Inject() (
  scc: SilhouetteControllerComponents
)(implicit ex: ExecutionContext) extends SilhouetteController(scc) {

  case class SignInModel(email: String, password: String)

  implicit val signInFormat = Json.format[SignInModel]

  /**
   * Handles sign in request
   *
   * @return JWT token in header if login is successful or Bad request if credentials are invalid
   */
  def signIn = UnsecuredAction.async { implicit request: Request[AnyContent] =>
    implicit val lang: Lang = supportedLangs.availables.head
    request.body.asJson.flatMap(_.asOpt[SignInModel]) match {
      case Some(signInModel) =>
        val credentials = Credentials(signInModel.email, signInModel.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          userService.retrieve(loginInfo).flatMap {
            case Some(_) =>
              for {
                authenticator <- authenticatorService.create(loginInfo)
                token <- authenticatorService.init(authenticator)
                result <- authenticatorService.embed(token, Ok)
              } yield {
                logger.debug(s"User ${loginInfo.providerKey} signed success")
                result
              }
            case None => Future.successful(BadRequest(JsString(messagesApi("could.not.find.user"))))
          }
        }.recover {
          case _: ProviderException => BadRequest(JsString(messagesApi("invalid.credentials")))
        }
      case None => Future.successful(BadRequest(JsString(messagesApi("could.not.find.user"))))
    }
  }
}
