package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.network.dto.AuthLogPageDto
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.ChoreRequestDto
import com.derekwinters.chores.data.network.dto.CompleteChoreRequestDto
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.network.dto.CreatePersonRequestDto
import com.derekwinters.chores.data.network.dto.CreateThemeRequestDto
import com.derekwinters.chores.data.network.dto.CurrentThemeDto
import com.derekwinters.chores.data.network.dto.DbStatusDto
import com.derekwinters.chores.data.network.dto.ImportResultDto
import com.derekwinters.chores.data.network.dto.LoginRequestDto
import com.derekwinters.chores.data.network.dto.LoginResponseDto
import com.derekwinters.chores.data.network.dto.LogPageDto
import com.derekwinters.chores.data.network.dto.PersonDto
import com.derekwinters.chores.data.network.dto.PersonStatsDto
import com.derekwinters.chores.data.network.dto.PointsLogEntryDto
import com.derekwinters.chores.data.network.dto.PointsLogPageDto
import com.derekwinters.chores.data.network.dto.PointsSummaryDto
import com.derekwinters.chores.data.network.dto.ReassignRequestDto
import com.derekwinters.chores.data.network.dto.RedeemRequestDto
import com.derekwinters.chores.data.network.dto.RedemptionDto
import com.derekwinters.chores.data.network.dto.ResetPasswordRequestDto
import com.derekwinters.chores.data.network.dto.SetupRequestDto
import com.derekwinters.chores.data.network.dto.SetupStatusDto
import com.derekwinters.chores.data.network.dto.ThemeDto
import com.derekwinters.chores.data.network.dto.UpdatePersonRequestDto
import com.derekwinters.chores.data.network.dto.UpdatePointsLogRequestDto
import com.derekwinters.chores.data.network.dto.UpdateThemeRequestDto
import com.derekwinters.chores.data.network.dto.UserInfoDto
import okhttp3.RequestBody
import okhttp3.ResponseBody

/**
 * In-memory [ChoresApi] test double. Its suspend functions never perform real I/O (no real
 * thread hop), so ViewModel tests driving it through `viewModelScope` on a shared
 * `StandardTestDispatcher` (see MainDispatcherRule) stay fully deterministic under
 * `advanceUntilIdle()` — unlike a real MockWebServer-backed call, whose completion crosses a
 * real background thread. MockWebServer is used instead for the repository/interceptor-level
 * tests, where the suspend call is awaited directly rather than through a fire-and-forget
 * `launch`.
 *
 * Only the chore/login surface (issue #5) is configurable via constructor params, since that's
 * all today's ViewModel tests exercise; every endpoint added by the v1.0.0 milestone
 * (issues #10-#25) has a trivial "not configured" stub here purely so this class keeps compiling
 * against the full [ChoresApi] interface.
 */
class FakeChoresApi(
    private val loginResult: LoginResponseDto? = null,
    private val loginError: Throwable? = null,
    private val choresResult: List<ChoreDto> = emptyList(),
    private val choresError: Throwable? = null,
    private val completeResult: ChoreDto? = null,
    private val completeError: Throwable? = null,
    private val currentUserResult: UserInfoDto = UserInfoDto("", false),
    private val currentUserError: Throwable? = null,
    private val setupStatusResult: SetupStatusDto = SetupStatusDto(setup_needed = false),
    private val dbStatusResult: DbStatusDto = DbStatusDto(ready = true),
    private val pointsSummaryResult: List<PointsSummaryDto> = emptyList(),
    private val peopleResult: List<PersonDto> = emptyList(),
    private val configResult: ConfigDto = ConfigDto(),
    private val skipResult: ChoreDto? = null,
    private val markDueResult: ChoreDto? = null,
    private val createChoreResult: ChoreDto? = null,
    private val updateChoreResult: ChoreDto? = null,
    private val personStatsResult: PersonStatsDto = PersonStatsDto(),
    private val redemptionsResult: List<RedemptionDto> = emptyList(),
    private val redeemResult: PersonStatsDto? = null,
    private val logResult: LogPageDto = LogPageDto(),
    private val createPersonResult: PersonDto? = null,
    private val updatePersonResult: PersonDto? = null,
    private val updatePersonError: Throwable? = null,
    private val authLogResult: AuthLogPageDto = AuthLogPageDto(),
    private val importConfigResult: ImportResultDto? = null
) : ChoresApi {

    var lastCompleteChoreId: Int? = null
        private set
    var lastCompleteRequest: CompleteChoreRequestDto? = null
        private set
    var lastSkipChoreId: Int? = null
        private set
    var lastMarkDueChoreId: Int? = null
        private set
    var lastDeleteChoreId: Int? = null
        private set
    var lastCreateChoreRequest: ChoreRequestDto? = null
        private set
    var lastUpdateChoreId: Int? = null
        private set
    var lastUpdateChoreRequest: ChoreRequestDto? = null
        private set
    var lastCreatePersonRequest: CreatePersonRequestDto? = null
        private set
    var lastUpdatePersonId: Int? = null
        private set
    var lastDeletePersonId: Int? = null
        private set

    override suspend fun login(request: LoginRequestDto): LoginResponseDto {
        loginError?.let { throw it }
        return loginResult ?: error("FakeChoresApi.loginResult not configured")
    }

    override suspend fun getCurrentUser(): UserInfoDto {
        currentUserError?.let { throw it }
        return currentUserResult
    }

    override suspend fun logout() = Unit

    override suspend fun getSetupStatus(): SetupStatusDto = setupStatusResult

    override suspend fun setup(request: SetupRequestDto): LoginResponseDto =
        loginResult ?: error("FakeChoresApi.loginResult not configured")

    override suspend fun resetPassword(authorization: String, request: ResetPasswordRequestDto) = Unit

    override suspend fun getDbStatus(): DbStatusDto = dbStatusResult

    override suspend fun getChores(): List<ChoreDto> {
        choresError?.let { throw it }
        return choresResult
    }

    override suspend fun completeChore(choreId: Int, request: CompleteChoreRequestDto): ChoreDto {
        lastCompleteChoreId = choreId
        lastCompleteRequest = request
        completeError?.let { throw it }
        return completeResult ?: error("FakeChoresApi.completeResult not configured")
    }

    override suspend fun skipChore(choreId: Int): ChoreDto {
        lastSkipChoreId = choreId
        return skipResult ?: error("FakeChoresApi.skipResult not configured")
    }

    override suspend fun markChoreDue(choreId: Int): ChoreDto {
        lastMarkDueChoreId = choreId
        return markDueResult ?: error("FakeChoresApi.markDueResult not configured")
    }

    override suspend fun deleteChore(choreId: Int) {
        lastDeleteChoreId = choreId
    }

    override suspend fun createChore(request: ChoreRequestDto): ChoreDto {
        lastCreateChoreRequest = request
        return createChoreResult ?: error("FakeChoresApi.createChoreResult not configured")
    }

    override suspend fun updateChore(choreId: Int, request: ChoreRequestDto): ChoreDto {
        lastUpdateChoreId = choreId
        lastUpdateChoreRequest = request
        return updateChoreResult ?: error("FakeChoresApi.updateChoreResult not configured")
    }

    override suspend fun reassignChore(choreId: Int, request: ReassignRequestDto): ChoreDto =
        error("not configured")

    override suspend fun getPeople(): List<PersonDto> = peopleResult

    override suspend fun getPointsSummary(): List<PointsSummaryDto> = pointsSummaryResult

    override suspend fun getPersonStats(personId: Int): PersonStatsDto = personStatsResult

    override suspend fun createPerson(request: CreatePersonRequestDto): PersonDto {
        lastCreatePersonRequest = request
        return createPersonResult ?: error("FakeChoresApi.createPersonResult not configured")
    }

    override suspend fun updatePerson(personId: Int, request: UpdatePersonRequestDto): PersonDto {
        lastUpdatePersonId = personId
        updatePersonError?.let { throw it }
        return updatePersonResult ?: error("FakeChoresApi.updatePersonResult not configured")
    }

    override suspend fun deletePerson(personId: Int) {
        lastDeletePersonId = personId
    }

    override suspend fun redeemPoints(personId: Int, request: RedeemRequestDto): PersonStatsDto =
        redeemResult ?: error("FakeChoresApi.redeemResult not configured")

    override suspend fun getRedemptions(personId: Int): List<RedemptionDto> = redemptionsResult

    override suspend fun getLog(
        person: String?,
        chore: String?,
        action: String?,
        start: String?,
        end: String?,
        page: Int
    ): LogPageDto = logResult

    override suspend fun getAuthLog(
        username: String?,
        action: String?,
        start: String?,
        end: String?,
        page: Int
    ): AuthLogPageDto = authLogResult

    override suspend fun getConfig(): ConfigDto = configResult

    override suspend fun updateConfig(request: ConfigDto): ConfigDto = request

    override suspend fun checkForUpdates(): ConfigDto = ConfigDto()

    override suspend fun exportConfig(): ResponseBody = error("not configured")

    override suspend fun importConfig(body: RequestBody): ImportResultDto =
        importConfigResult ?: error("FakeChoresApi.importConfigResult not configured")

    override suspend fun getPointsLog(page: Int): PointsLogPageDto = PointsLogPageDto()

    override suspend fun updatePointsLogEntry(
        entryId: Int,
        request: UpdatePointsLogRequestDto
    ): PointsLogEntryDto = error("not configured")

    override suspend fun deletePointsLogEntry(entryId: Int) = Unit

    override suspend fun getThemes(): List<ThemeDto> = emptyList()

    override suspend fun createTheme(request: CreateThemeRequestDto): ThemeDto =
        error("not configured")

    override suspend fun updateTheme(themeId: Int, request: UpdateThemeRequestDto): ThemeDto =
        error("not configured")

    override suspend fun deleteTheme(themeId: Int) = Unit

    override suspend fun setDefaultTheme(themeId: Int): ThemeDto = error("not configured")

    override suspend fun getCurrentTheme(): CurrentThemeDto = error("not configured")

    override suspend fun setPersonalTheme(themeId: Int) = Unit
}
