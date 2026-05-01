package org.openjwc.client.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.openjwc.client.data.models.Period
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.data.models.TableMetadata
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class TableConfigViewModel(private val initialMetadata: TableMetadata? = null) : ViewModel() {
    var tableName by mutableStateOf(initialMetadata?.tableName ?: "")

    var selectedDateMillis by mutableLongStateOf(
        initialMetadata?.semesterConfig?.startDate?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()?.toEpochMilli()
            ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    val selectedLocalDate: LocalDate by derivedStateOf {
        Instant.ofEpochMilli(selectedDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    var weeks by mutableIntStateOf(initialMetadata?.semesterConfig?.weeks ?: 16)

    var showWeekend by mutableStateOf(
        initialMetadata?.semesterConfig?.visibleDays?.containsAll(java.time.DayOfWeek.entries)
            ?: true
    )

    var periods by mutableStateOf(
        initialMetadata?.semesterConfig?.periods ?: SemesterConfig.default().periods
    )

    val isPeriodsValid: Boolean
        get() = periods.indices.all { getPeriodErrorType(it) == null }

    fun addPeriod() {
        val lastPeriod = periods.lastOrNull()
        val nextIndex = (lastPeriod?.index ?: 0) + 1
        val startTime = lastPeriod?.end?.plusMinutes(10) ?: LocalTime.of(8, 0)
        val endTime = startTime.plusMinutes(45)
        periods = periods + Period(nextIndex, startTime, endTime)
    }

    fun removeLastPeriod() {
        if (periods.isNotEmpty()) {
            periods = periods.dropLast(1)
        }
    }

    fun updatePeriodTime(index: Int, isStartTime: Boolean, newTime: LocalTime) {
        periods = periods.toMutableList().apply {
            val oldPeriod = this[index]
            this[index] = if (isStartTime) {
                oldPeriod.copy(start = newTime)
            } else {
                oldPeriod.copy(end = newTime)
            }
        }
    }

    fun getPeriodErrorType(index: Int): String? {
        val period = periods[index]
        if (period.end <= period.start) return "End before start"

        // Check overlap with other periods
        periods.forEachIndexed { i, other ->
            if (i != index) {
                if (maxOf(period.start, other.start) < minOf(period.end, other.end)) {
                    return "Overlap"
                }
            }
        }
        return null
    }

    fun getFinalMetadata(): TableMetadata {
        val visibleDays = if (showWeekend) {
            java.time.DayOfWeek.entries.toSet()
        } else {
            java.time.DayOfWeek.entries.filter { it.value <= 5 }.toSet()
        }

        return TableMetadata(
            id = initialMetadata?.id ?: 0L, // Should be handled by Room or caller
            tableName = tableName,
            semesterConfig = SemesterConfig(
                startDate = selectedLocalDate,
                weeks = weeks,
                visibleDays = visibleDays,
                periods = periods
            ),
            isCurrent = initialMetadata?.isCurrent?:false
        )
    }
}
