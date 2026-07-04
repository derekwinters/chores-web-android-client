package com.derekwinters.chores.ui.settings

/**
 * Hour option data class that includes the hour value (0-23) and its display label.
 */
data class HourOption(
    val hour: Int,
    val displayLabel: String
)

/**
 * Utility object for hour operations.
 * Generates a list of all available hours with their human-readable labels.
 */
object DueHourUtils {
    /**
     * Get all available hours (0-23) with their display labels.
     * Hours are formatted as 12-hour time (e.g., "12:00 AM", "1:00 AM", "12:00 PM", etc.).
     */
    fun getAvailableHours(): List<HourOption> {
        return (0..23).map { hour ->
            val displayLabel = formatHourLabel(hour)
            HourOption(hour = hour, displayLabel = displayLabel)
        }
    }

    /**
     * Format an hour (0-23) as a 12-hour time label.
     * Examples: 0 -> "12:00 AM", 1 -> "1:00 AM", 12 -> "12:00 PM", 13 -> "1:00 PM", 23 -> "11:00 PM"
     */
    fun formatHourLabel(hour: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour:00 $period"
    }

    /**
     * Find a hour option by its hour value (0-23).
     */
    fun findHourOption(hour: Int): HourOption? {
        return getAvailableHours().find { it.hour == hour }
    }
}
