package modules

import com.google.inject.AbstractModule
import models.daos.{ UserDAO, UserDAOImpl }
import models.services.{ UserService, UserServiceImpl }
import net.codingwell.scalaguice.ScalaModule

/**
 * The base Guice module.
 */
class BaseModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  override def configure(): Unit = {
    bind[UserDAO].to[UserDAOImpl]
    bind[UserService].to[UserServiceImpl]
  }
}
