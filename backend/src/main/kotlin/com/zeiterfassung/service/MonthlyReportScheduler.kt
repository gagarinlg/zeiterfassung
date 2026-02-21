package com.zeiterfassung.service

import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.DailySummaryRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.repository.VacationBalanceRepository
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Sends monthly hours and vacation summary reports via email.
 *
 * Schedule: 1st of every month at 07:00 local time.
 *  - Each active employee receives a personal report for the previous month.
 *  - Each manager additionally receives a team summary.
 */
@Service
class MonthlyReportScheduler(
    private val userRepository: UserRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val vacationBalanceRepository: VacationBalanceRepository,
    private val emailService: EmailService,
    private val messageSource: MessageSource,
) {
    private val logger = LoggerFactory.getLogger(MonthlyReportScheduler::class.java)
    private val locale = Locale.GERMAN

    private fun msg(
        key: String,
        vararg args: Any,
    ) = messageSource.getMessage(key, args.toList().toTypedArray(), locale)

    /** Cron: minute hour day-of-month month day-of-week */
    @Scheduled(cron = "0 0 7 1 * *")
    fun sendMonthlyReports() {
        val previousMonth = YearMonth.now().minusMonths(1)
        logger.info("Sending monthly reports for {}", previousMonth)

        val activeUsers = userRepository.findAll().filter { it.isActive && !it.isDeleted }

        activeUsers.forEach { user ->
            try {
                sendPersonalReport(user, previousMonth)
            } catch (e: Exception) {
                logger.error("Failed to send personal report to {}: {}", user.email, e.message)
            }
        }

        // Managers: additionally send team summary
        activeUsers.filter { it.subordinates.isNotEmpty() }.forEach { manager ->
            try {
                sendTeamReport(manager, previousMonth)
            } catch (e: Exception) {
                logger.error("Failed to send team report to {}: {}", manager.email, e.message)
            }
        }
    }

    private fun sendPersonalReport(
        user: UserEntity,
        month: YearMonth,
    ) {
        if (user.email.isBlank()) return

        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()

        val summaries = dailySummaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(user.id, startDate, endDate)
        val totalWorkMinutes = summaries.sumOf { it.totalWorkMinutes }
        val totalBreakMinutes = summaries.sumOf { it.totalBreakMinutes }
        val totalOvertimeMinutes = summaries.sumOf { it.overtimeMinutes }
        val nonCompliantDays = summaries.count { !it.isCompliant }

        val balance = vacationBalanceRepository.findByUserIdAndYear(user.id, LocalDate.now().year)
        val remainingVacation =
            balance?.let {
                (it.totalDays + it.carriedOverDays - it.usedDays).max(java.math.BigDecimal.ZERO)
            } ?: java.math.BigDecimal.ZERO

        val subject = msg("email.report.personal.subject", month.month.getDisplayName(java.time.format.TextStyle.FULL, locale), month.year)
        val body =
            msg(
                "email.report.personal.body",
                user.firstName,
                month.month.getDisplayName(java.time.format.TextStyle.FULL, locale),
                month.year,
                formatHours(totalWorkMinutes),
                formatHours(totalBreakMinutes),
                formatHours(totalOvertimeMinutes),
                nonCompliantDays,
                remainingVacation,
            )

        emailService.sendAsync(user.email, subject, body)
    }

    private fun sendTeamReport(
        manager: UserEntity,
        month: YearMonth,
    ) {
        if (manager.email.isBlank()) return

        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()

        val lines =
            manager.subordinates.map { member ->
                val summaries = dailySummaryRepository.findByUserIdAndDateBetweenOrderByDateAsc(member.id, startDate, endDate)
                val workH = formatHours(summaries.sumOf { it.totalWorkMinutes })
                val otH = formatHours(summaries.sumOf { it.overtimeMinutes })
                val balance = vacationBalanceRepository.findByUserIdAndYear(member.id, LocalDate.now().year)
                val remaining =
                    balance?.let {
                        (it.totalDays + it.carriedOverDays - it.usedDays).max(java.math.BigDecimal.ZERO)
                    } ?: java.math.BigDecimal.ZERO
                "  ${member.firstName} ${member.lastName}: " +
                    msg("email.report.team.member.line", workH, otH, remaining)
            }

        val subject =
            msg(
                "email.report.team.subject",
                month.month.getDisplayName(java.time.format.TextStyle.FULL, locale),
                month.year,
            )
        val body =
            msg(
                "email.report.team.body",
                manager.firstName,
                month.month.getDisplayName(java.time.format.TextStyle.FULL, locale),
                month.year,
                lines.joinToString("\n"),
            )

        emailService.sendAsync(manager.email, subject, body)
    }

    private fun formatHours(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%d:%02d".format(h, m)
    }
}
