package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import com.ubertob.kondor.json.*
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
