package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.Checklist

class ResetDailyState {
    fun execute(checklist: Checklist): Checklist {
        return checklist.resetAllItems()
    }
}
