package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import kotlinx.datetime.*

// Value class codecs
object JChecklistId : JStringRepresentable<ChecklistId>() {
    override val cons: (String) -> ChecklistId = ::ChecklistId
    override val render: (ChecklistId) -> String = ChecklistId::value
}

object JChecklistItemId : JStringRepresentable<ChecklistItemId>() {
    override val cons: (String) -> ChecklistItemId = ::ChecklistItemId
    override val render: (ChecklistItemId) -> String = ChecklistItemId::value
}

object JItemIconId : JStringRepresentable<ItemIconId>() {
    override val cons: (String) -> ItemIconId = ::ItemIconId
    override val render: (ItemIconId) -> String = ItemIconId::value
}

// Enum codecs
object JChecklistColor : JStringRepresentable<ChecklistColor>() {
    override val cons: (String) -> ChecklistColor = { ChecklistColor.valueOf(it) }
    override val render: (ChecklistColor) -> String = { it.name }
}

object JStatePersistenceDuration : JStringRepresentable<StatePersistenceDuration>() {
    override val cons: (String) -> StatePersistenceDuration = { StatePersistenceDuration.valueOf(it) }
    override val render: (StatePersistenceDuration) -> String = { it.name }
}

object JDayOfWeek : JStringRepresentable<DayOfWeek>() {
    override val cons: (String) -> DayOfWeek = { DayOfWeek.valueOf(it) }
    override val render: (DayOfWeek) -> String = { it.name }
}

// DateTime codecs
object JLocalTime : JStringRepresentable<LocalTime>() {
    override val cons: (String) -> LocalTime = { LocalTime.parse(it) }
    override val render: (LocalTime) -> String = { it.toString() }
}

object JInstant : JStringRepresentable<Instant>() {
    override val cons: (String) -> Instant = { Instant.parse(it) }
    override val render: (Instant) -> String = { it.toString() }
}

// TimeRange sealed class
object JTimeRange : JSealed<TimeRange>() {
    override val discriminatorFieldName = "type"
    override val subConverters: Map<String, ObjectNodeConverter<out TimeRange>> = mapOf(
        "AllDay" to JAllDay,
        "Specific" to JSpecificTime
    )
    override fun extractTypeName(obj: TimeRange) = when (obj) {
        is TimeRange.AllDay -> "AllDay"
        is TimeRange.Specific -> "Specific"
    }
}

object JAllDay : JAny<TimeRange.AllDay>() {
    override fun JsonNodeObject.deserializeOrThrow() = TimeRange.AllDay
}

object JSpecificTime : JAny<TimeRange.Specific>() {
    private val start_time by str(JLocalTime, TimeRange.Specific::startTime)
    private val end_time by str(JLocalTime, TimeRange.Specific::endTime)

    override fun JsonNodeObject.deserializeOrThrow() =
        TimeRange.Specific(+start_time, +end_time)
}

// ChecklistItemState sealed interface
object JChecklistItemState : JSealed<ChecklistItemState>() {
    override val discriminatorFieldName = "type"
    override val subConverters: Map<String, ObjectNodeConverter<out ChecklistItemState>> = mapOf(
        "Pending" to JPending,
        "Done" to JDone,
        "IgnoredToday" to JIgnoredToday
    )
    override fun extractTypeName(obj: ChecklistItemState) = when (obj) {
        ChecklistItemState.Pending -> "Pending"
        ChecklistItemState.Done -> "Done"
        ChecklistItemState.IgnoredToday -> "IgnoredToday"
    }
}

object JPending : JAny<ChecklistItemState.Pending>() {
    override fun JsonNodeObject.deserializeOrThrow() = ChecklistItemState.Pending
}

object JDone : JAny<ChecklistItemState.Done>() {
    override fun JsonNodeObject.deserializeOrThrow() = ChecklistItemState.Done
}

object JIgnoredToday : JAny<ChecklistItemState.IgnoredToday>() {
    override fun JsonNodeObject.deserializeOrThrow() = ChecklistItemState.IgnoredToday
}
