package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.UserInfoDto

/**
 * Minimal current-session identity (issue #10: drives admin-only nav visibility; issue #11:
 * result of first-run setup / password reset flows also resolve to a session for this user).
 */
data class CurrentUser(
    val username: String,
    val isAdmin: Boolean
)

fun UserInfoDto.toDomain(): CurrentUser = CurrentUser(
    username = username,
    isAdmin = is_admin
)
