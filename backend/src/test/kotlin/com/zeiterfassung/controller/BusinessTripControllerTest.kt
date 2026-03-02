package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.ApproveBusinessTripRequest
import com.zeiterfassung.model.dto.BusinessTripResponse
import com.zeiterfassung.model.dto.CreateBusinessTripRequest
import com.zeiterfassung.model.dto.RejectBusinessTripRequest
import com.zeiterfassung.model.dto.UpdateBusinessTripRequest
import com.zeiterfassung.model.enums.BusinessTripStatus
import com.zeiterfassung.service.BusinessTripService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BusinessTripControllerTest {
    @Mock
    private lateinit var businessTripService: BusinessTripService

    private lateinit var controller: BusinessTripController

    private val userId = UUID.randomUUID()
    private val actorId = userId.toString()

    private fun sampleTrip(id: UUID = UUID.randomUUID()): BusinessTripResponse =
        BusinessTripResponse(
            id = id,
            userId = userId,
            userName = "Max Mustermann",
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 8, 3),
            destination = "Berlin",
            purpose = "Client meeting",
            status = BusinessTripStatus.REQUESTED,
            approvedById = null,
            approvedByName = null,
            rejectionReason = null,
            notes = null,
            estimatedCost = BigDecimal("500"),
            actualCost = null,
            costCenter = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = BusinessTripController(businessTripService)
    }

    @Test
    fun `createTrip should return 201 with trip response`() {
        val dto =
            CreateBusinessTripRequest(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                "Berlin",
                "Client meeting",
                null,
                BigDecimal("500"),
                null,
            )
        val expected = sampleTrip()
        `when`(businessTripService.createTrip(userId, dto)).thenReturn(expected)

        val response = controller.createTrip(dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `updateTrip should return updated trip`() {
        val tripId = UUID.randomUUID()
        val dto = UpdateBusinessTripRequest(destination = "Munich")
        val expected = sampleTrip(tripId)
        `when`(businessTripService.updateTrip(tripId, userId, dto)).thenReturn(expected)

        val response = controller.updateTrip(tripId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `cancelTrip should return no content`() {
        val tripId = UUID.randomUUID()

        val response = controller.cancelTrip(tripId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(businessTripService).cancelTrip(tripId, userId)
    }

    @Test
    fun `getMyTrips should return paginated trips`() {
        val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
        val page = PageImpl(listOf(sampleTrip()), pageable, 1)
        `when`(businessTripService.getUserTrips(userId, pageable)).thenReturn(page)

        val response = controller.getMyTrips(actorId, 0, 20)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
    }

    @Test
    fun `getTrip should return trip by id`() {
        val tripId = UUID.randomUUID()
        val expected = sampleTrip(tripId)
        `when`(businessTripService.getTrip(tripId)).thenReturn(expected)

        val response = controller.getTrip(tripId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `approveTrip should return approved trip`() {
        val tripId = UUID.randomUUID()
        val dto = ApproveBusinessTripRequest(notes = "Approved")
        val expected = sampleTrip(tripId)
        `when`(businessTripService.approveTrip(tripId, userId, dto)).thenReturn(expected)

        val response = controller.approveTrip(tripId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `approveTrip should use default dto when null`() {
        val tripId = UUID.randomUUID()
        val defaultDto = ApproveBusinessTripRequest()
        val expected = sampleTrip(tripId)
        `when`(businessTripService.approveTrip(tripId, userId, defaultDto)).thenReturn(expected)

        val response = controller.approveTrip(tripId, null, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `rejectTrip should return rejected trip`() {
        val tripId = UUID.randomUUID()
        val dto = RejectBusinessTripRequest("Budget constraints")
        val expected = sampleTrip(tripId)
        `when`(businessTripService.rejectTrip(tripId, userId, dto)).thenReturn(expected)

        val response = controller.rejectTrip(tripId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `completeTrip should return completed trip`() {
        val tripId = UUID.randomUUID()
        val actualCost = BigDecimal("450")
        val expected = sampleTrip(tripId)
        `when`(businessTripService.completeTrip(tripId, userId, actualCost)).thenReturn(expected)

        val response = controller.completeTrip(tripId, actorId, actualCost)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getPendingTrips should return paginated pending trips`() {
        val pageable = PageRequest.of(0, 20, Sort.by("createdAt").ascending())
        val page = PageImpl(listOf(sampleTrip()), pageable, 1)
        `when`(businessTripService.getPendingTrips(userId, pageable)).thenReturn(page)

        val response = controller.getPendingTrips(actorId, 0, 20)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
    }
}
