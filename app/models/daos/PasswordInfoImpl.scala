package models.daos

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

/**
 * PasswordInfo repository.
 */
class PasswordInfoImpl @Inject() (userDAO: UserDAO)(implicit val classTag: ClassTag[PasswordInfo], ec: ExecutionContext)
  extends DelegableAuthInfoDAO[PasswordInfo] {

  /**
   * Finds passwordInfo for specified loginInfo
   *
   * @param loginInfo user's email
   * @return user's hashed password
   */
  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = userDAO.find(loginInfo).map(_.map(_.passwordInfo))

  /**
   * Adds new passwordInfo for specified loginInfo
   *
   * @param loginInfo user's email
   * @param passwordInfo user's hashed password
   */
  override def add(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo] = update(loginInfo, passwordInfo)

  /**
   * Updates passwordInfo for specified loginInfo
   *
   * @param loginInfo user's email
   * @param passwordInfo user's hashed password
   */
  override def update(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo] = userDAO.find(loginInfo).flatMap {
    case Some(user) => userDAO.update(user.copy(password = Some(passwordInfo.password))).map(_.passwordInfo)
    case None => Future.failed(new Exception("user not found"))
  }

  /**
   * Adds new passwordInfo for specified loginInfo
   *
   * @param loginInfo user's email
   * @param passwordInfo user's hashed password
   */
  override def save(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo] = update(loginInfo, passwordInfo)

  /**
   * Removes passwordInfo for specified loginInfo
   *
   * @param loginInfo user's email
   */
  override def remove(loginInfo: LoginInfo): Future[Unit] = update(loginInfo, PasswordInfo("", "")).map(_ => ())
}

