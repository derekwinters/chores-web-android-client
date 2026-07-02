package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.CurrentTheme
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.CreateThemeRequestDto
import com.derekwinters.chores.data.network.dto.UpdateThemeRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Household default theme management (issue #24) and personal theme preference (issue #25). The
 * 6 built-in themes (dark, light, charcoal, paper, pink, frog) come back from [getThemes] like
 * any other row; [ThemeOption.isBuiltin] gates rename/delete.
 */
@Singleton
class ThemeRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getThemes(): Result<List<ThemeOption>> =
        safeApiCall { api.getThemes() }.map { themes -> themes.map { it.toDomain() } }

    /** Issue #24: create-via-copy of an existing theme (the only way to create a custom theme). */
    suspend fun createTheme(name: String, sourceThemeId: Int): Result<ThemeOption> =
        safeApiCall { api.createTheme(CreateThemeRequestDto(name, sourceThemeId)) }.map { it.toDomain() }

    /** Issue #24: rename and/or recolor a non-built-in theme. */
    suspend fun updateTheme(
        themeId: Int,
        name: String? = null,
        background: String? = null,
        surface: String? = null,
        surface2: String? = null,
        accent: String? = null,
        primary: String? = null,
        secondary: String? = null,
        success: String? = null,
        warning: String? = null,
        error: String? = null
    ): Result<ThemeOption> = safeApiCall {
        api.updateTheme(
            themeId,
            UpdateThemeRequestDto(name, background, surface, surface2, accent, primary, secondary, success, warning, error)
        )
    }.map { it.toDomain() }

    /** Issue #24: non-built-in themes only; enforced server-side too. */
    suspend fun deleteTheme(themeId: Int): Result<Unit> = safeApiCall { api.deleteTheme(themeId) }

    /** Issue #24: sets the household default theme. */
    suspend fun setDefaultTheme(themeId: Int): Result<ThemeOption> =
        safeApiCall { api.setDefaultTheme(themeId) }.map { it.toDomain() }

    /** Issue #25: resolved theme (personal override, else household default). */
    suspend fun getCurrentTheme(): Result<CurrentTheme> =
        safeApiCall { api.getCurrentTheme() }.map { it.toDomain() }

    /**
     * Issue #25: sets the caller's personal override to [themeId], or clears it back to the
     * household default when [themeId] is null (the "Default (household theme name)" tile).
     */
    suspend fun setPersonalTheme(themeId: Int?): Result<Unit> =
        safeApiCall { api.setPersonalTheme(themeId ?: CLEAR_PERSONAL_THEME_ID) }

    companion object {
        /** Sentinel id the backend treats as "no personal override" (issue #25). */
        const val CLEAR_PERSONAL_THEME_ID = 0
    }
}
