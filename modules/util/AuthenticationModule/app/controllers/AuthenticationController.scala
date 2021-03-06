/*
 * Desarrollado por: Sebastián Flórez
 * Universidad de los Andes
 * Ingeniería de Sistemas y Computación
 * Pregrado
 */
package controllers.auth

import auth.controllers.AuthenticationManager
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasher, PasswordInfo}
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import auth.logic.AuthLogic
import auth.settings.AuthenticationEnvironment
import auth.models.forms._
import play.api.Configuration
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, Controller}
import net.ceedubs.ficus.Ficus._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import auth.logic.Implicits.SumDateTime
import auth.models.user.{User, UserMin}

/**
  * Controlador encargado de manejar la autenticación de usuarios
  * @param silhouette
  * @param env
  * @param authInfoRepository
  * @param credentialsProvider
  * @param authLogic
  * @param passwordHasher
  * @param configuration
  * @param clock
  */
class AuthenticationController @Inject() (
                                           override val silhouette: Silhouette[AuthenticationEnvironment],
                                           env:Environment[AuthenticationEnvironment],
                                           authInfoRepository: AuthInfoRepository,
                                           credentialsProvider: CredentialsProvider,
                                           authLogic: AuthLogic,
                                           passwordHasher: PasswordHasher,
                                           configuration: Configuration,
                                           clock:Clock) extends Controller with AuthenticationManager{

  /**
    * El formato del formulario de registro
    */
  implicit val signUpFormat:Format[SignUpForm] = Json.format[SignUpForm]

  /**
    * El formato del formulario de inicio de sesion
    */
  implicit val signInFormat:Format[SignInForm] = Json.format[SignInForm]

  /**
    * El formato del usuario autenticado
    */
  implicit  val format = Json.format[UserMin]

  /**
    * Servicio que registra a un usuario
    */
  def signUp = Action.async(parse.json){implicit request =>
    request.body.validateOpt[SignUpForm].getOrElse(None) match {
      case Some(form) => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, form.email)
        authLogic.retrieve(loginInfo).flatMap{
          case Some(_) => Future(BadRequest("El correo dado ya tiene un usuario asignado"))
          case None => {
            for {
              user <- authLogic.create(User.generateUser(form, loginInfo, PasswordInfo("", "", None)))
              _ <- authLogic.confirm(loginInfo)
              _ <- authInfoRepository.add(loginInfo, passwordHasher.hash(form.password))
            } yield {
              silhouette.env.eventBus.publish(SignUpEvent(user, request))
              Ok("Usuario creado correctamente")
            }
          }
        }
      }
      case None => Future(BadRequest("Error en formato de formulario de creacion"))
    }
  }

  /**
    * Servicio que inicia la sesión de un usuario
    */
  def signIn = Action.async(parse.json) { implicit request =>
    request.body.validateOpt[SignInForm].getOrElse(None) match {
      case None => Future(BadRequest("Error en formato de formulario"))
      case Some(form) => {
        val credentials = Credentials(form.email, form.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          authLogic.retrieve(loginInfo).flatMap {
            case None => Future(InternalServerError("Error autenticando usuario, contacta al administrador"))
            case Some(user) if !user.confirmed => Future(BadRequest("El usuario debe confirmar su cuenta"))
            case Some(user) => for {
              authenticator <- env.authenticatorService.create(loginInfo).map(authenticatorWithRememberMe(_, form.rememberMe))
              value <- env.authenticatorService.init(authenticator)
            } yield {
              silhouette.env.eventBus.publish(LoginEvent(user, request))
              Ok(Json.obj("token"->value))
            }
          }
        }.recover {
          case _:ProviderException => BadRequest("Usuario y/o contraseña invalidos")
        }
      }
    }
  }

  /**
    * Servicio que cierra la sesion de un usuario
    * @return
    */
  def signOut = SecuredAction.async { implicit request =>
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, Ok)
  }

  /**
    * Retoran el autenticador del usuario que inicia sesión
    * @param authenticator
    * @param rememberMe
    * @return
    */
  private def authenticatorWithRememberMe(authenticator: JWTAuthenticator, rememberMe: Boolean) = {
    if (rememberMe) {
      authenticator.copy(
        expirationDateTime = clock.now + rememberMeParams._1,
        idleTimeout = rememberMeParams._2
      )
    }
    else
      authenticator
  }

  /**
    * Define la fecha de expiración del token a crear
    */
  private lazy val rememberMeParams: (FiniteDuration, Option[FiniteDuration]) = {
    val cfg = configuration.getConfig("silhouette.authenticator.rememberMe").get.underlying
    (
      cfg.as[FiniteDuration]("authenticatorExpiry"),
      cfg.getAs[FiniteDuration]("authenticatorIdleTimeout")
    )
  }
}
