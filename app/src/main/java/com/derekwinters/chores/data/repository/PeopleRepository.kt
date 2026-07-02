package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.Person
import com.derekwinters.chores.data.model.PersonStats
import com.derekwinters.chores.data.model.PointsSummary
import com.derekwinters.chores.data.model.Redemption
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.CreatePersonRequestDto
import com.derekwinters.chores.data.network.dto.RedeemRequestDto
import com.derekwinters.chores.data.network.dto.UpdatePersonRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Household member data: the Dashboard's per-person cards (issue #12), User Detail stats/redeem
 * flow (issue #17), and User management admin CRUD (issue #18).
 */
@Singleton
class PeopleRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getPeople(): Result<List<Person>> =
        safeApiCall { api.getPeople() }.map { people -> people.map { it.toDomain() } }

    /** Issue #12: rolling 7/30-day totals for every person, in one call to avoid N+1 requests. */
    suspend fun getPointsSummary(): Result<List<PointsSummary>> =
        safeApiCall { api.getPointsSummary() }.map { summaries -> summaries.map { it.toDomain() } }

    /** Issue #17. */
    suspend fun getPersonStats(personId: Int): Result<PersonStats> =
        safeApiCall { api.getPersonStats(personId) }.map { it.toDomain() }

    /** Issue #18: username is auto-derived server-side from [displayName]. */
    suspend fun createPerson(displayName: String, password: String): Result<Person> =
        safeApiCall { api.createPerson(CreatePersonRequestDto(displayName, password)) }.map { it.toDomain() }

    /** Issue #18. [password] blank/null means "unchanged". */
    suspend fun updatePerson(
        personId: Int,
        displayName: String? = null,
        username: String? = null,
        goal7d: Int? = null,
        goal30d: Int? = null,
        password: String? = null,
        isAdmin: Boolean? = null
    ): Result<Person> = safeApiCall {
        api.updatePerson(
            personId,
            UpdatePersonRequestDto(
                display_name = displayName,
                username = username,
                goal_7d = goal7d,
                goal_30d = goal30d,
                password = password?.takeIf { it.isNotBlank() },
                is_admin = isAdmin
            )
        )
    }.map { it.toDomain() }

    /** Issue #18: does not cascade-delete history/points/log entries. */
    suspend fun deletePerson(personId: Int): Result<Unit> = safeApiCall { api.deletePerson(personId) }

    /** Issue #17. */
    suspend fun redeemPoints(personId: Int, amount: Int): Result<PersonStats> =
        safeApiCall { api.redeemPoints(personId, RedeemRequestDto(amount)) }.map { it.toDomain() }

    /** Issue #17. */
    suspend fun getRedemptions(personId: Int): Result<List<Redemption>> =
        safeApiCall { api.getRedemptions(personId) }.map { redemptions -> redemptions.map { it.toDomain() } }
}
