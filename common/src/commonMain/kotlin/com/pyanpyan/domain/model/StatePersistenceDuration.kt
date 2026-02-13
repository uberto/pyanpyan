package com.pyanpyan.domain.model

enum class StatePersistenceDuration(
    val milliseconds: Long?,
    val displayName: String
) {
    ZERO(0L, "Reset immediately"),
    ONE_MINUTE(60_000L, "1 minute"),
    FIFTEEN_MINUTES(900_000L, "15 minutes"),
    ONE_HOUR(3_600_000L, "1 hour"),
    ONE_DAY(86_400_000L, "1 day"),
    NEVER(null, "Never");

    companion object {
        val DEFAULT = FIFTEEN_MINUTES
    }
}
