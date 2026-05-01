package org.openjwc.client.data.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// --- 1. 定义自定义序列化器 ---

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalTime = LocalTime.parse(decoder.decodeString())
}

object DayOfWeekSerializer : KSerializer<DayOfWeek> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DayOfWeek", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: DayOfWeek) = encoder.encodeInt(value.value)
    override fun deserialize(decoder: Decoder): DayOfWeek = DayOfWeek.of(decoder.decodeInt())
}

object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeInt(value.toArgb())
    override fun deserialize(decoder: Decoder): Color = Color(decoder.decodeInt())
}

// --- 2. 实体与模型定义 ---

@Entity(
    tableName = "courses",
    indices = [Index(value = ["tableId"])],
    foreignKeys = [
        ForeignKey(
            entity = TableMetadata::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val name: String,
    val teacher: String,
    val location: String,
    @Serializable(with = DayOfWeekSerializer::class)
    val dayOfWeek: DayOfWeek,
    val startPeriod: Int,
    val duration: Int,
    @Serializable(with = ColorSerializer::class)
    val color: Color,
    val weekRule: Set<Int>,
    val note: String,
){
    infix fun isConflictingWith(other: Course): Boolean { //TODO
        if (this.tableId != other.tableId) return false
        if (this.id != 0L && this.id == other.id) return false
        if (this.dayOfWeek != other.dayOfWeek) return false
        val thisEnd = this.startPeriod + this.duration - 1
        val otherEnd = other.startPeriod + other.duration - 1
        return maxOf(this.startPeriod, other.startPeriod) <= minOf(thisEnd, otherEnd) &&
                (this.weekRule intersect other.weekRule).isNotEmpty()
    }
}

@Serializable
data class Period(
    val index: Int, // 第几节
    @Serializable(with = LocalTimeSerializer::class)
    val start: LocalTime,
    @Serializable(with = LocalTimeSerializer::class)
    val end: LocalTime
)

@Serializable
data class SemesterConfig(
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    val weeks: Int,
    val visibleDays: Set<@Serializable(with = DayOfWeekSerializer::class) DayOfWeek> = DayOfWeek.entries.toSet(),
    val periods: List<Period>
) {
    fun calculateCurrentWeek(targetDate: LocalDate = LocalDate.now()): Int? {
        val startMonday = startDate.with(DayOfWeek.MONDAY)
        val endSunday = startMonday.plusWeeks(weeks.toLong()).minusDays(1)
        if (targetDate.isBefore(startMonday) || targetDate.isAfter(endSunday)) return null
        val daysBetween = ChronoUnit.DAYS.between(startMonday, targetDate)
        return ((daysBetween / 7).toInt() + 1).coerceIn(1, weeks)
    }
    
    companion object {
        fun default(): SemesterConfig {
            val currentYear = LocalDate.now().year
            return SemesterConfig(
                startDate = LocalDate.of(currentYear, 3, 2),
                weeks = 16,
                visibleDays = DayOfWeek.entries.toSet(),
                periods = listOf(
                    Period(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
                    Period(2, LocalTime.of(8, 50), LocalTime.of(9, 35)),
                    Period(3, LocalTime.of(9, 50), LocalTime.of(10, 35)),
                    Period(4, LocalTime.of(10, 40), LocalTime.of(11, 25)),
                    Period(5, LocalTime.of(11, 30), LocalTime.of(12, 15)),
                    Period(6, LocalTime.of(14, 0), LocalTime.of(14, 45)),
                    Period(7, LocalTime.of(14, 50), LocalTime.of(15, 35)),
                    Period(8, LocalTime.of(15, 50), LocalTime.of(16, 35)),
                    Period(9, LocalTime.of(16, 40), LocalTime.of(17, 25)),
                    Period(10, LocalTime.of(17, 30), LocalTime.of(18, 15)),
                    Period(11, LocalTime.of(19, 0), LocalTime.of(19, 45)),
                    Period(12, LocalTime.of(19, 50), LocalTime.of(20, 35)),
                    Period(13, LocalTime.of(20, 40), LocalTime.of(21, 25))
                )
            )
        }
    }
}

@Entity(tableName = "table_metadata")
@Serializable
data class TableMetadata(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    val semesterConfig: SemesterConfig,
    val isCurrent: Boolean = false
)
