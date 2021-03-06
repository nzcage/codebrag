package com.softwaremill.codebrag.usecases.user

import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import com.softwaremill.codebrag.dao.user.UserDAO
import org.mockito.Mockito._
import com.softwaremill.codebrag.domain.builder.UserAssembler
import com.softwaremill.codebrag.finders.user.{LoggedInUserView, UserFinder}
import com.softwaremill.codebrag.finders.browsingcontext.UserBrowsingContext

class LoginUserUseCaseSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with MockitoSugar {

  val userDao = mock[UserDAO]
  val userFinder = mock[UserFinder]
  val loginUseCase = new LoginUserUseCase(userDao, userFinder)

  before {
    reset(userDao, userFinder)
  }

  it should "not try to authenticate when user is inactive" in {
    // given
    val inactiveUser = UserAssembler.randomUser.withActive(set = false).get
    when(userDao.findByLoginOrEmail(inactiveUser.emailLowerCase)).thenReturn(Some(inactiveUser))

    // when
    val loginForm = LoginForm(inactiveUser.emailLowerCase, "dummy", false)
    val Left(result) = loginUseCase.execute(loginForm) {
      fail("Authenticatin block should not be called when user is inactive")
    }

    // then
    val expectedErrors = Map("general" -> List("User account inactive"))
    result should be(expectedErrors)
  }

  it should "not try to authenticate when user not found by login/email" in {
    // given
    val nonExistingUser = UserAssembler.randomUser.withActive(set = false).get
    when(userDao.findByLoginOrEmail(nonExistingUser.emailLowerCase)).thenReturn(None)

    // when
    val loginForm = LoginForm(nonExistingUser.emailLowerCase, "dummy", false)
    val exceptionCaught = intercept[LoginFailedException] {
      loginUseCase.execute(loginForm) {
        fail("Authenticatin block should not be called when user is inactive")
      }
    }

    // then
    exceptionCaught.msg should be("Invalid login credentials")
  }

  it should "return logged in user view" in {
    // given
    val user = UserAssembler.randomUser.get
    val userContext = UserBrowsingContext(user.id, "codebrag", "master")
    when(userDao.findByLoginOrEmail(user.emailLowerCase)).thenReturn(Some(user))
    val loggedInUserView = LoggedInUserView(user, userContext)
    when(userFinder.findLoggedInUser(user)).thenReturn(loggedInUserView)

    // when
    val loginForm = LoginForm(user.emailLowerCase, "dummy", false)
    val Right(loggedInUser) = loginUseCase.execute(loginForm) { Some(user) }

    // then
    loggedInUser should be(loggedInUserView)
  }

  it should "raise exception when use cannot authenticate due to bad credentials" in {
    // given
    val user = UserAssembler.randomUser.get
    when(userDao.findByLoginOrEmail(user.emailLowerCase)).thenReturn(Some(user))

    // when
    val loginForm = LoginForm(user.emailLowerCase, "dummy", false)
    val exceptionCaught = intercept[LoginFailedException] {
      loginUseCase.execute(loginForm) { None }
    }

    // then
    exceptionCaught.msg should be("Invalid login credentials")
  }

}
