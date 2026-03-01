package com.zeiterfassung.service

import com.zeiterfassung.model.entity.BusinessTripEntity
import com.zeiterfassung.model.entity.SickLeaveEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.entity.VacationRequestEntity
import org.springframework.context.MessageSource
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.Locale

/**
 * Sends notification emails for vacation request lifecycle events.
 * All methods are @Async so they never block the calling thread.
 *
 * Email language: German (primary) — Locale.GERMAN is the application default.
 */
@Service
class NotificationService(
    private val emailService: EmailService,
    private val messageSource: MessageSource,
) {
    private val locale = Locale.GERMAN

    private fun msg(
        key: String,
        vararg args: Any,
    ): String = messageSource.getMessage(key, args.toList().toTypedArray(), locale)

    private val noNotes by lazy { msg("common.not_specified") }

    // ---- Vacation request events -----------------------------------------------

    /**
     * Notify all managers of the requesting employee when a new vacation request
     * is submitted. Called after the request is persisted.
     */
    @Async
    fun notifyVacationRequestCreated(
        request: VacationRequestEntity,
        managers: List<UserEntity>,
    ) {
        val employee = request.user
        val subject = msg("email.vacation.created.subject", employee.firstName, employee.lastName)
        val body =
            msg(
                "email.vacation.created.body",
                employee.firstName,
                employee.lastName,
                request.startDate,
                request.endDate,
                request.totalDays,
                request.notes ?: noNotes,
            )
        managers.filter { it.email.isNotBlank() }.forEach { manager ->
            emailService.sendAsync(manager.email, subject, body)
        }
    }

    /**
     * Notify the employee that their vacation request was approved.
     */
    @Async
    fun notifyVacationApproved(
        request: VacationRequestEntity,
        approverName: String,
    ) {
        val employee = request.user
        if (employee.email.isBlank()) return
        val subject = msg("email.vacation.approved.subject")
        val body =
            msg(
                "email.vacation.approved.body",
                employee.firstName,
                request.startDate,
                request.endDate,
                request.totalDays,
                approverName,
            )
        emailService.sendAsync(employee.email, subject, body)
    }

    /**
     * Notify the employee that their vacation request was rejected.
     */
    @Async
    fun notifyVacationRejected(
        request: VacationRequestEntity,
        rejectionReason: String,
    ) {
        val employee = request.user
        if (employee.email.isBlank()) return
        val subject = msg("email.vacation.rejected.subject")
        val body =
            msg(
                "email.vacation.rejected.body",
                employee.firstName,
                request.startDate,
                request.endDate,
                rejectionReason,
            )
        emailService.sendAsync(employee.email, subject, body)
    }

    /**
     * Notify the approver that a previously-approved vacation was cancelled by the employee.
     * This allows the manager to update team planning.
     */
    @Async
    fun notifyVacationCancelledByEmployee(
        request: VacationRequestEntity,
        notifyUser: UserEntity,
    ) {
        if (notifyUser.email.isBlank()) return
        val employee = request.user
        val subject = msg("email.vacation.cancelled.subject", employee.firstName, employee.lastName)
        val body =
            msg(
                "email.vacation.cancelled.body",
                employee.firstName,
                employee.lastName,
                request.startDate,
                request.endDate,
            )
        emailService.sendAsync(notifyUser.email, subject, body)
    }

    // ---- Sick leave events --------------------------------------------------

    @Async
    fun notifySickLeaveReported(
        sickLeave: SickLeaveEntity,
        manager: UserEntity,
    ) {
        val employee = sickLeave.user
        val subject = msg("email.sick_leave.reported.subject", employee.firstName, employee.lastName)
        val body =
            msg(
                "email.sick_leave.reported.body",
                employee.firstName,
                employee.lastName,
                sickLeave.startDate,
                sickLeave.endDate,
                sickLeave.notes ?: noNotes,
            )
        emailService.sendAsync(manager.email, subject, body)
    }

    // ---- Business trip events -----------------------------------------------

    @Async
    fun notifyBusinessTripRequested(
        trip: BusinessTripEntity,
        manager: UserEntity,
    ) {
        val employee = trip.user
        val subject = msg("email.business_trip.requested.subject", employee.firstName, employee.lastName)
        val body =
            msg(
                "email.business_trip.requested.body",
                employee.firstName,
                employee.lastName,
                trip.startDate,
                trip.endDate,
                trip.destination,
                trip.purpose,
            )
        emailService.sendAsync(manager.email, subject, body)
    }

    @Async
    fun notifyBusinessTripApproved(
        trip: BusinessTripEntity,
        approverName: String,
    ) {
        val employee = trip.user
        if (employee.email.isBlank()) return
        val subject = msg("email.business_trip.approved.subject")
        val body =
            msg(
                "email.business_trip.approved.body",
                employee.firstName,
                trip.startDate,
                trip.endDate,
                trip.destination,
                approverName,
            )
        emailService.sendAsync(employee.email, subject, body)
    }

    @Async
    fun notifyBusinessTripRejected(
        trip: BusinessTripEntity,
        rejectionReason: String,
    ) {
        val employee = trip.user
        if (employee.email.isBlank()) return
        val subject = msg("email.business_trip.rejected.subject")
        val body =
            msg(
                "email.business_trip.rejected.body",
                employee.firstName,
                trip.startDate,
                trip.endDate,
                trip.destination,
                rejectionReason,
            )
        emailService.sendAsync(employee.email, subject, body)
    }
}
