package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.PersonDto
import com.derekwinters.chores.data.network.dto.PersonStatsDto
import com.derekwinters.chores.data.network.dto.PointsSummaryDto
import com.derekwinters.chores.data.network.dto.RedemptionDto

/** Domain model for a household member (issues #12, #17, #18). */
data class Person(
    val id: Int,
    val username: String,
    val displayName: String,
    val isAdmin: Boolean,
    val goal7d: Int,
    val goal30d: Int
)

fun PersonDto.toDomain(): Person = Person(
    id = id,
    username = username,
    displayName = display_name,
    isAdmin = is_admin,
    goal7d = goal_7d,
    goal30d = goal_30d
)

/** Issue #12: rolling 7/30-day point totals used to drive the Dashboard progress bars. */
data class PointsSummary(
    val personId: Int,
    val points7d: Int,
    val points30d: Int
)

fun PointsSummaryDto.toDomain(): PointsSummary = PointsSummary(
    personId = person_id,
    points7d = points_7d,
    points30d = points_30d
)

/** Issue #17: User Detail stats panel. */
data class PersonStats(
    val availablePoints: Int,
    val points7d: Int,
    val points30d: Int,
    val redeemedTotal: Int,
    val completedCount: Int
)

fun PersonStatsDto.toDomain(): PersonStats = PersonStats(
    availablePoints = available_points,
    points7d = points_7d,
    points30d = points_30d,
    redeemedTotal = redeemed_total,
    completedCount = completed_count
)

/** Issue #17: a single redemption-history row. */
data class Redemption(
    val id: Int,
    val amount: Int,
    val redeemedBy: String,
    val timestamp: String
)

fun RedemptionDto.toDomain(): Redemption = Redemption(
    id = id,
    amount = amount,
    redeemedBy = redeemed_by,
    timestamp = timestamp
)
