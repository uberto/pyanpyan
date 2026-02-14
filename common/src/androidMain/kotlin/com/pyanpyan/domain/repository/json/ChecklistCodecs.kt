package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import com.ubertob.kondor.json.*

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
