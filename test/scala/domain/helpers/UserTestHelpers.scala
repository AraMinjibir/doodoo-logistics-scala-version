package scala.domain.helpers

import controllers.dto.{UsersCreationDto, UsersUpdateDto}
import domain.models
import domain.models.{User, UserUpdateData, UsersRole}

import java.util.UUID

trait UserTestHelpers {

//  Test fixtures

def userId = UUID.fromString("22222222-2222-2222-2222-222222222222")
 def username = "doodoologistics@gmail.com"
 def name = "DooDoo Logistics"
 def password = "doodoo12345"
 def phone = "00001234345"
 def role = UsersRole.Admin


 def createTestUser() = User(
    id = userId,
    name = name,
    email =username, hashPassword = password, phoneNumber = phone, role = role
  )

  def validUserCreation():User = User(
    id = userId,
    name = name,
    email = username, hashPassword = password, phoneNumber = phone, role = role
  )

  def validUserUpdate():UserUpdateData = UserUpdateData(
    name = name, email = username, phoneNumber = phone, role = role
  )

}
