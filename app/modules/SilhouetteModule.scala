package modules

import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.actions.{ SecuredErrorHandler, UnsecuredErrorHandler }
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ Environment, EventBus, Silhouette, SilhouetteProvider }
import com.mohiva.play.silhouette.crypto.{ JcaCrypter, JcaCrypterSettings }
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.{ BCryptPasswordHasher, BCryptSha256PasswordHasher }
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import controllers.{ DefaultSilhouetteControllerComponents, SilhouetteControllerComponents }
import models.daos.{ PasswordInfoImpl, UserDAO }
import models.services.UserService
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient
import utils.auth.{ CustomSecuredErrorHandler, CustomUnsecuredErrorHandler, JWTEnvironment }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{ Duration, FiniteDuration }

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class SilhouetteModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  override def configure(): Unit = {
    bind[Silhouette[JWTEnvironment]].to[SilhouetteProvider[JWTEnvironment]]
    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  /**
   * Provides the HTTP layer implementation.
   *
   * @param client Play's WS client.
   * @return The HTTP layer implementation.
   */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
   * Provides the Silhouette environment.
   *
   * @param userService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[JWTAuthenticator],
    eventBus: EventBus): Environment[JWTEnvironment] = {

    Environment[JWTEnvironment](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  /**
   * Provides the crypter for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the authenticator.
   */
  @Provides
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    new JcaCrypter(JcaCrypterSettings(configuration.underlying.getString("play.http.secret.key")))
  }

  /**
   * Provides AuthenticatorService
   *
   * @param crypter encryption generic
   * @param idGenerator id generator
   * @param configuration configuration set
   * @param clock clock
   * @return AuthenticatorService implementation
   */
  @Provides
  def provideAuthenticatorService(
    crypter: Crypter,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock): AuthenticatorService[JWTAuthenticator] = {

    val encoder = new CrypterAuthenticatorEncoder(crypter)
    new JWTAuthenticatorService(JWTAuthenticatorSettings(
      fieldName = configuration.underlying.getString("silhouette.authenticator.headerName"),
      issuerClaim = configuration.underlying.getString("silhouette.authenticator.issuerClaim"),
      authenticatorExpiry = Duration(configuration.underlying.getString("silhouette.authenticator.authenticatorExpiry")).asInstanceOf[FiniteDuration],
      sharedSecret = configuration.underlying.getString("silhouette.authenticator.sharedSecret")

    ), None, encoder, idGenerator, clock)
  }

  /**
   *  Provides auth info delegable auth info repository.
   *
   * @param userDao Operations with user table in database
   * @return DelegableAuthInfoDAO implementation
   */
  @Provides
  def providePasswordDAO(userDao: UserDAO): DelegableAuthInfoDAO[PasswordInfo] = new PasswordInfoImpl(userDao)

  /**
   * Provides the auth info repository.
   *
   * @param passwordInfoDAO The implementation of the delegable password auth info DAO.
   * @return The auth info repository instance.
   */
  @Provides
  def provideAuthInfoRepository(passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo]): AuthInfoRepository = {
    new DelegableAuthInfoRepository(passwordInfoDAO)
  }

  /**
   * Provides the password hasher registry.
   *
   * @return The password hasher registry.
   */
  @Provides
  def providePasswordHasherRegistry(): PasswordHasherRegistry = {
    PasswordHasherRegistry(new BCryptSha256PasswordHasher(), Seq(new BCryptPasswordHasher()))
  }

  /**
   * Provides the credentials provider.
   *
   * @param authInfoRepository The auth info repository implementation.
   * @param passwordHasherRegistry The password hasher registry.
   * @return The credentials provider.
   */
  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository,
    passwordHasherRegistry: PasswordHasherRegistry): CredentialsProvider = {

    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
  }

  /**
   * Provides silhouette components
   *
   * @param components silhouette components implementation
   * @return silhouette components implementation
   */
  @Provides
  def providesSilhouetteComponents(components: DefaultSilhouetteControllerComponents): SilhouetteControllerComponents = {
    components
  }
}
