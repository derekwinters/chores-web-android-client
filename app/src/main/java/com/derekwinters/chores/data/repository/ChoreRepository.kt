package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.model.ChoreDraft
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.model.toRequestDto
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.CompleteChoreRequestDto
import com.derekwinters.chores.data.network.dto.ReassignRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Issue #5 behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/
 * points/state/next_due" and "Complete-chore action: POST /chores/{id}/complete, with
 * Completer-picker dialog when current_assignee == null". Extended by issues #15/#16 with the
 * remaining chore card actions and the create/edit form.
 */
@Singleton
class ChoreRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getChores(): Result<List<Chore>> =
        safeApiCall { api.getChores() }.map { chores -> chores.map { it.toDomain() } }

    /**
     * @param completedBy the chosen Completer's username, required when the chore has no
     *   [Chore.currentAssignee]; null lets the server default to the current assignee.
     */
    suspend fun completeChore(choreId: Int, completedBy: String? = null): Result<Chore> =
        safeApiCall { api.completeChore(choreId, CompleteChoreRequestDto(completedBy)) }
            .map { it.toDomain() }

    /** Issue #15: moves the chore to its next cycle without awarding points. */
    suspend fun skipChore(choreId: Int): Result<Chore> =
        safeApiCall { api.skipChore(choreId) }.map { it.toDomain() }

    /** Issue #15: marks a not-yet-due chore due now. */
    suspend fun markChoreDue(choreId: Int): Result<Chore> =
        safeApiCall { api.markChoreDue(choreId) }.map { it.toDomain() }

    /** Issue #15: also removes all points history for this chore server-side. */
    suspend fun deleteChore(choreId: Int): Result<Unit> =
        safeApiCall { api.deleteChore(choreId) }

    /** Issue #16. */
    suspend fun createChore(draft: ChoreDraft): Result<Chore> =
        safeApiCall { api.createChore(draft.toRequestDto()) }.map { it.toDomain() }

    /** Issue #16. */
    suspend fun updateChore(choreId: Int, draft: ChoreDraft): Result<Chore> =
        safeApiCall { api.updateChore(choreId, draft.toRequestDto()) }.map { it.toDomain() }

    /**
     * Issue #16: editing an `open` chore's assignee field in edit mode IS reassignment (no
     * separate quick-reassign button in the live web app).
     */
    suspend fun reassignChore(choreId: Int, assignee: String?): Result<Chore> =
        safeApiCall { api.reassignChore(choreId, ReassignRequestDto(assignee)) }.map { it.toDomain() }
}
