package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.network.dto.AuthLogPageDto
import com.derekwinters.chores.data.network.dto.ChoreCreateRequestDto
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.ChoreUpdateRequestDto
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
import com.derekwinters.chores.data.network.dto.UserStatsDto
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
import com.derekwinters.chores.data.network.dto.UpdateCheckStatusDto
import com.derekwinters.chores.data.network.dto.UpdatePersonRequestDto
import com.derekwinters.chores.data.network.dto.UpdatePointsLogRequestDto
import com.derekwinters.chores.data.network.dto.UpdateThemeRequestDto
import com.derekwinters.chores.data.network.dto.UserInfoDto
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * chores-web backend API surface. Paths are relative to the placeholder Retrofit base URL and
 * are rewritten per-request by [BaseUrlInterceptor]; the `v1/` segment matches the backend's
 * `V1_PREFIX` (backend/app/main.py) applied to all routers except status/metrics.
 *
 * Started in issue #5 (login/chores/complete) and grown by the v1.0.0 feature-parity milestone
 * (issues #10-#25) as each screen's Impact Areas table calls for new endpoints; see each
 * function's KDoc for which issue introduced it.
 */
interface ChoresApi {

    // --- Auth (issue #5, extended by #10/#11) ---

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @GET("v1/auth/me")
    suspend fun getCurrentUser(): UserInfoDto

    /** Issue #10: manual Logout action in the user menu. */
    @POST("v1/auth/logout")
    suspend fun logout()

    /** Issue #11: first-run setup gate. */
    @GET("v1/auth/setup-status")
    suspend fun getSetupStatus(): SetupStatusDto

    /** Issue #11: creates the first (admin) user; response shape matches login's. */
    @POST("v1/auth/setup")
    suspend fun setup(@Body request: SetupRequestDto): LoginResponseDto

    /**
     * Issue #11: forced-password-reset flow. [authorization] carries the one-time `reset_token`
     * (e.g. "Bearer <token>") rather than the normal session token, since login hasn't
     * succeeded yet.
     */
    @PUT("v1/auth/password/reset")
    suspend fun resetPassword(
        @Header("Authorization") authorization: String,
        @Body request: ResetPasswordRequestDto
    )

    /** Issue #11: DB-readiness gate polled before rendering the authenticated app shell. */
    @GET("status/db-status")
    suspend fun getDbStatus(): DbStatusDto

    // --- Chores (issue #5, extended by #13/#14/#15/#16) ---

    @GET("v1/chores")
    suspend fun getChores(): List<ChoreDto>

    @POST("v1/chores/{id}/complete")
    suspend fun completeChore(
        @Path("id") choreId: Int,
        @Body request: CompleteChoreRequestDto
    ): ChoreDto

    /** Issue #15: moves to next cycle without awarding points. */
    @POST("v1/chores/{id}/skip")
    suspend fun skipChore(@Path("id") choreId: Int): ChoreDto

    /** Issue #15: marks a not-yet-due chore due now. */
    @POST("v1/chores/{id}/mark-due")
    suspend fun markChoreDue(@Path("id") choreId: Int): ChoreDto

    /** Issue #15: also removes all points history for this chore server-side. */
    @DELETE("v1/chores/{id}")
    suspend fun deleteChore(@Path("id") choreId: Int)

    /** Issue #16. */
    @POST("v1/chores")
    suspend fun createChore(@Body request: ChoreCreateRequestDto): ChoreDto

    /** Issue #16. */
    @PUT("v1/chores/{id}")
    suspend fun updateChore(@Path("id") choreId: Int, @Body request: ChoreUpdateRequestDto): ChoreDto

    /** Issue #16: editing an `open` chore's assignee field IS reassignment. */
    @POST("v1/chores/{id}/reassign")
    suspend fun reassignChore(@Path("id") choreId: Int, @Body request: ReassignRequestDto): ChoreDto

    // --- People (issues #12, #17, #18) ---

    @GET("v1/people")
    suspend fun getPeople(): List<PersonDto>

    /**
     * Issue #12: per-person rolling point totals for the Dashboard progress bars. Each entry is
     * keyed by `person` (username string), not a person id — see [PointsSummaryDto].
     */
    @GET("v1/points/summary")
    suspend fun getPointsSummary(): List<PointsSummaryDto>

    /**
     * Issue #17. Keyed by the person's **username** (a string path segment), not their numeric
     * person id — matches chores-web's `GET /v1/points/stats/{person}`.
     */
    @GET("v1/points/stats/{person}")
    suspend fun getPersonStats(@Path("person") username: String): UserStatsDto

    /** Issue #18. */
    @POST("v1/people")
    suspend fun createPerson(@Body request: CreatePersonRequestDto): PersonDto

    /** Issue #18: chores-web's `PersonUpdate` route is a PUT (partial update), not a PATCH. */
    @PUT("v1/people/{person_id}")
    suspend fun updatePerson(@Path("person_id") personId: Int, @Body request: UpdatePersonRequestDto): PersonDto

    /** Issue #18. */
    @DELETE("v1/people/{person_id}")
    suspend fun deletePerson(@Path("person_id") personId: Int)

    /**
     * Issue #17. Returns the updated `PersonOut`, not a stats payload — callers should re-fetch
     * [getPersonStats] for the display balance rather than trusting this response body.
     */
    @POST("v1/people/{person_id}/redeem")
    suspend fun redeemPoints(@Path("person_id") personId: Int, @Body request: RedeemRequestDto): PersonDto

    /** Issue #17. */
    @GET("v1/people/{person_id}/redemptions")
    suspend fun getRedemptions(@Path("person_id") personId: Int): List<RedemptionDto>

    // --- Activity Log (issues #15, #17, #19) ---

    /** Issue #19, reused (with a subset of filters) by issues #15/#17's deep-linked views. */
    @GET("v1/log")
    suspend fun getLog(
        @Query("person") person: String? = null,
        @Query("chore") chore: String? = null,
        @Query("action") action: String? = null,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("page") page: Int = 1
    ): LogPageDto

    // --- Auth Event Log (issue #21) ---

    @GET("v1/auth/log")
    suspend fun getAuthLog(
        @Query("username") username: String? = null,
        @Query("action") action: String? = null,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("page") page: Int = 1
    ): AuthLogPageDto

    // --- Config / Settings (issues #12, #20, #22) ---

    @GET("v1/config")
    suspend fun getConfig(): ConfigDto

    @PUT("v1/config")
    suspend fun updateConfig(@Body request: ConfigDto): ConfigDto

    /** Issue #20: "About" tab's version info, fetched on Settings load (admin only). */
    @GET("v1/config/updates/status")
    suspend fun getUpdateCheckStatus(): UpdateCheckStatusDto

    /** Issue #20: "Check Now" manual update check (admin only). */
    @POST("v1/config/updates/check")
    suspend fun checkForUpdates(): UpdateCheckStatusDto

    // --- Data export/import (issue #22) ---

    /** Raw JSON backup; written to a file untouched rather than parsed into a typed model. */
    @GET("v1/export/config")
    suspend fun exportConfig(): ResponseBody

    @POST("v1/import/config")
    suspend fun importConfig(@Body body: RequestBody): ImportResultDto

    // --- Admin Points Log editor (issue #23) ---

    @GET("v1/admin/db/points-log")
    suspend fun getPointsLog(@Query("page") page: Int = 1): PointsLogPageDto

    @PATCH("v1/admin/db/points-log/{id}")
    suspend fun updatePointsLogEntry(
        @Path("id") entryId: Int,
        @Body request: UpdatePointsLogRequestDto
    ): PointsLogEntryDto

    @DELETE("v1/admin/db/points-log/{id}")
    suspend fun deletePointsLogEntry(@Path("id") entryId: Int)

    // --- Theming (issues #24, #25) ---

    @GET("v1/theme")
    suspend fun getThemes(): List<ThemeDto>

    /** Issue #24: create-via-copy of an existing theme. */
    @POST("v1/theme")
    suspend fun createTheme(@Body request: CreateThemeRequestDto): ThemeDto

    @PUT("v1/theme/{id}")
    suspend fun updateTheme(@Path("id") themeId: Int, @Body request: UpdateThemeRequestDto): ThemeDto

    /** Issue #24: non-built-in themes only; enforced server-side too. */
    @DELETE("v1/theme/{id}")
    suspend fun deleteTheme(@Path("id") themeId: Int)

    /** Issue #24: sets the household default theme. */
    @PUT("v1/theme/default/{id}")
    suspend fun setDefaultTheme(@Path("id") themeId: Int): ThemeDto

    /** Issue #25: resolved theme (personal override, else household default). */
    @GET("v1/theme/current")
    suspend fun getCurrentTheme(): CurrentThemeDto

    /**
     * Issue #25: sets the caller's personal theme override to [themeId]. The "Default (household
     * theme name)" tile clears the override instead; see
     * [com.derekwinters.chores.data.repository.ThemeRepository.CLEAR_PERSONAL_THEME_ID] for the
     * sentinel id it sends for that case.
     */
    @POST("v1/theme/set/{themeId}")
    suspend fun setPersonalTheme(@Path("themeId") themeId: Int)
}
