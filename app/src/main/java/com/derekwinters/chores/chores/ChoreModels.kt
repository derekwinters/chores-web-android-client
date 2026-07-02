package com.derekwinters.chores.chores

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A chore as returned by `GET /v1/chores` and `POST /v1/chores/{id}/complete`
 * (backend/app/routers/chores.py in chores-web).
 *
 * [currentAssignee] is the Assignee's username, or null if the chore has no Assignee — in which
 * case a Completer must be picked explicitly at completion time (see chores-web's CONTEXT.md).
 */
@Serializable
data class Chore(
    val id: Int,
    val name: String,
    @SerialName("current_assignee") val currentAssignee: String? = null,
    val points: Int = 0,
    val state: String,
    @SerialName("next_due") val nextDue: String? = null
)

/** Request body for `POST /v1/chores/{id}/complete`. */
@Serializable
data class CompleteBody(
    @SerialName("completed_by") val completedBy: String? = null
)
