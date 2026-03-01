package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.AssignRolesRequest
import com.zeiterfassung.model.dto.ChangePasswordRequest
import com.zeiterfassung.model.dto.CreateUserRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.ResetPasswordRequest
import com.zeiterfassung.model.dto.UpdateRfidRequest
import com.zeiterfassung.model.dto.UpdateUserRequest
import com.zeiterfassung.model.dto.UserResponse
import com.zeiterfassung.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserControllerTest {
    @Mock
    private lateinit var userService: UserService

    private lateinit var controller: UserController

    private val actorId = UUID.randomUUID().toString()
    private val userId = UUID.randomUUID()

    private fun createUserResponse(id: UUID = userId): UserResponse =
        UserResponse(
            id = id.toString(),
            email = "user@test.com",
            firstName = "Max",
            lastName = "Mustermann",
            employeeNumber = "EMP001",
            phone = "+49123456789",
            photoUrl = null,
            managerId = null,
            substituteId = null,
            isActive = true,
            roles = listOf("EMPLOYEE"),
            permissions = listOf("time.edit.own"),
            dateFormat = null,
            timeFormat = null,
        )

    @BeforeEach
    fun setUp() {
        controller = UserController(userService)
    }

    @Test
    fun `createUser should return created status with user response`() {
        val request =
            CreateUserRequest(
                email = "new@test.com",
                password = "password123",
                firstName = "New",
                lastName = "User",
            )
        val expectedResponse = createUserResponse()
        `when`(userService.createUser(request, UUID.fromString(actorId))).thenReturn(expectedResponse)

        val response = controller.createUser(request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).isEqualTo(expectedResponse)
    }

    @Test
    fun `getUsers should return paginated users with default sorting`() {
        val pageResponse =
            PageResponse(
                content = listOf(createUserResponse()),
                totalElements = 1L,
                totalPages = 1,
                pageNumber = 0,
                pageSize = 20,
            )
        val pageable = PageRequest.of(0, 20, Sort.by("lastName").ascending())
        `when`(userService.getUsers(pageable)).thenReturn(pageResponse)

        val response = controller.getUsers(0, 20, "lastName", "asc")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
    }

    @Test
    fun `getUsers should use descending sort when specified`() {
        val pageResponse =
            PageResponse<UserResponse>(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
                pageNumber = 0,
                pageSize = 20,
            )
        val pageable = PageRequest.of(0, 20, Sort.by("lastName").descending())
        `when`(userService.getUsers(pageable)).thenReturn(pageResponse)

        val response = controller.getUsers(0, 20, "lastName", "desc")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getUserById should return user`() {
        val expectedResponse = createUserResponse()
        `when`(userService.getUserById(userId)).thenReturn(expectedResponse)

        val response = controller.getUserById(userId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(expectedResponse)
    }

    @Test
    fun `updateUser should return updated user`() {
        val request = UpdateUserRequest(firstName = "Updated", lastName = "Name")
        val expectedResponse = createUserResponse()
        `when`(userService.updateUser(userId, request, UUID.fromString(actorId))).thenReturn(expectedResponse)

        val response = controller.updateUser(userId, request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(expectedResponse)
    }

    @Test
    fun `deleteUser should return no content`() {
        doNothing().`when`(userService).deleteUser(userId, UUID.fromString(actorId))

        val response = controller.deleteUser(userId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(response.body).isNull()
        verify(userService).deleteUser(userId, UUID.fromString(actorId))
    }

    @Test
    fun `assignRoles should return updated user with roles`() {
        val request = AssignRolesRequest(roles = listOf("ADMIN", "EMPLOYEE"))
        val expectedResponse = createUserResponse()
        `when`(userService.assignRoles(userId, request, UUID.fromString(actorId))).thenReturn(expectedResponse)

        val response = controller.assignRoles(userId, request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(expectedResponse)
    }

    @Test
    fun `updateRfid should return updated user`() {
        val request = UpdateRfidRequest(rfidTagId = "RFID12345")
        val expectedResponse = createUserResponse()
        `when`(userService.updateRfid(userId, request, UUID.fromString(actorId))).thenReturn(expectedResponse)

        val response = controller.updateRfid(userId, request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(expectedResponse)
    }

    @Test
    fun `changePassword should return no content`() {
        val request =
            ChangePasswordRequest(
                currentPassword = "oldpass",
                newPassword = "newpass123",
                confirmPassword = "newpass123",
            )
        doNothing().`when`(userService).changePassword(userId, request, UUID.fromString(actorId))

        val response = controller.changePassword(userId, request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(response.body).isNull()
    }

    @Test
    fun `resetPassword should return no content`() {
        val request =
            ResetPasswordRequest(
                newPassword = "newpass123",
                confirmPassword = "newpass123",
            )
        doNothing().`when`(userService).resetPassword(userId, request, UUID.fromString(actorId))

        val response = controller.resetPassword(userId, request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(response.body).isNull()
    }

    @Test
    fun `updateOwnProfile should return updated profile`() {
        val request = UpdateUserRequest(firstName = "Updated")
        val expectedResponse = createUserResponse()
        `when`(userService.updateOwnProfile(UUID.fromString(actorId), request)).thenReturn(expectedResponse)

        val response = controller.updateOwnProfile(request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(expectedResponse)
    }

    @Test
    fun `getAllSubordinates should return list of subordinates`() {
        val subordinates = listOf(createUserResponse(), createUserResponse(UUID.randomUUID()))
        `when`(userService.getAllSubordinates(userId)).thenReturn(subordinates)

        val response = controller.getAllSubordinates(userId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(2)
    }

    @Test
    fun `getTeamMembers should return list of team members`() {
        val teamMembers = listOf(createUserResponse())
        `when`(userService.getTeamMembers(userId)).thenReturn(teamMembers)

        val response = controller.getTeamMembers(userId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
    }
}
