package com.derekwinters.chores.chores

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A person as returned by `GET /v1/people` (backend/app/routers/people.py in chores-web).
 * Used to populate the Completer-picker dialog when a chore has no Assignee.
 */
@Serializable
data class Person(
    val username: String,
    val name: String,
    @SerialName("is_admin") val isAdmin: Boolean = false
)
