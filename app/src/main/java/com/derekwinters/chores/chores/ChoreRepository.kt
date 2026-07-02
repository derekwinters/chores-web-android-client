package com.derekwinters.chores.chores

import javax.inject.Inject
import javax.inject.Singleton

/** Read + complete access to chores. Create/edit/delete/skip/reassign/mark-due are out of scope for issue #5. */
@Singleton
class ChoreRepository @Inject constructor(
    private val choresApi: ChoresApi
) {

    suspend fun getChores(): List<Chore> = choresApi.getChores()

    /**
     * Completes a chore. [completedBy] must be supplied (the Completer's username) when the
     * chore has no Assignee (`current_assignee == null`); otherwise it should be left null and
     * the backend attributes the Completion to the authenticated user.
     */
    suspend fun completeChore(choreId: Int, completedBy: String? = null): Chore =
        choresApi.completeChore(choreId, CompleteBody(completedBy = completedBy))
}
