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

// ChecklistSchedule
object JChecklistSchedule : JAny<ChecklistSchedule>() {
    val days_of_week by array(JDayOfWeek, ChecklistSchedule::daysOfWeek)
    val time_range by obj(JTimeRange, ChecklistSchedule::timeRange)

    override fun JsonNodeObject.deserializeOrThrow() =
        ChecklistSchedule(
            daysOfWeek = (+days_of_week).toSet(),
            timeRange = +time_range
        )
}

// ChecklistItem
object JChecklistItem : JAny<ChecklistItem>() {
    val id by str(JChecklistItemId, ChecklistItem::id)
    val title by str(ChecklistItem::title)
    val icon_id by str(JItemIconId, ChecklistItem::iconId)
    val state by obj(JChecklistItemState, ChecklistItem::state)

    override fun JsonNodeObject.deserializeOrThrow() =
        ChecklistItem(
            id = +id,
            title = +title,
            iconId = +icon_id,
            state = +state
        )
}

// Checklist
object JChecklist : JAny<Checklist>() {
    val id by str(JChecklistId, Checklist::id)
    val name by str(Checklist::name)
    val schedule by obj(JChecklistSchedule, Checklist::schedule)
    val items by array(JChecklistItem, Checklist::items)
    val color by str(JChecklistColor, Checklist::color)
    val state_persistence by str(JStatePersistenceDuration, Checklist::statePersistence)
    val last_accessed_at by str(JInstant, Checklist::lastAccessedAt)

    override fun JsonNodeObject.deserializeOrThrow() =
        Checklist(
            id = +id,
            name = +name,
            schedule = +schedule,
            items = +items,
            color = +color,
            statePersistence = +state_persistence,
            lastAccessedAt = +last_accessed_at
        )
}
