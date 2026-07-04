package com.derekwinters.chores.ui.settings

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Timezone data class that includes the timezone ID and its UTC offset label for display.
 */
data class TimezoneOption(
    val zoneId: String,
    val displayLabel: String
)

/**
 * Utility object for timezone operations.
 * Generates a list of all available timezones with their current UTC offset labels.
 */
object TimezoneUtils {
    /**
     * Get all available timezones with their UTC offset labels.
     * Sorted by UTC offset and then by timezone name for better UX.
     */
    fun getAvailableTimezones(): List<TimezoneOption> {
        val now = LocalDateTime.now()
        val timezoneOptions = mutableListOf<TimezoneOption>()

        ZoneId.getAvailableZoneIds().forEach { zoneIdString ->
            try {
                val zoneId = ZoneId.of(zoneIdString)
                val zonedDateTime = now.atZone(zoneId)
                val offset = zonedDateTime.offset
                val offsetHours = offset.totalSeconds / 3600
                val offsetMinutes = (offset.totalSeconds % 3600) / 60

                val offsetLabel = when {
                    offsetMinutes == 0 -> String.format("UTC%+d:00", offsetHours)
                    else -> String.format("UTC%+d:%02d", offsetHours, kotlin.math.abs(offsetMinutes))
                }

                // Create a display label with the timezone name and offset
                val regionName = zoneIdString.substringAfterLast('/')
                val displayLabel = "$regionName ($offsetLabel)"

                timezoneOptions.add(
                    TimezoneOption(
                        zoneId = zoneIdString,
                        displayLabel = displayLabel
                    )
                )
            } catch (e: Exception) {
                // Skip invalid timezone IDs
            }
        }

        // Sort by offset first (numerically), then by timezone name
        return timezoneOptions.sortedWith(
            compareBy<TimezoneOption> { option ->
                try {
                    val zoneId = ZoneId.of(option.zoneId)
                    val offset = now.atZone(zoneId).offset
                    offset.totalSeconds
                } catch (e: Exception) {
                    0
                }
            }.thenBy { it.zoneId }
        )
    }

    /**
     * Find a timezone option by its ID.
     */
    fun findTimezoneOption(zoneId: String): TimezoneOption? {
        return getAvailableTimezones().find { it.zoneId == zoneId }
    }
}
