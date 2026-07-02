package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.CurrentThemeDto
import com.derekwinters.chores.data.network.dto.ThemeColorsDto
import com.derekwinters.chores.data.network.dto.ThemeDefaultInfoDto
import com.derekwinters.chores.data.network.dto.ThemeDto

/**
 * Domain model for one theme (issue #24: 6 built-ins + custom; issue #25: personal picks from
 * this same set). Named `ThemeOption` (not `Theme`) to avoid clashing with Compose's own `Theme`
 * naming conventions (see ui/theme/ChoresTheme.kt). Flattens the wire format's nested `colors`
 * object onto this type for convenient call sites (`theme.primary`, not `theme.colors.primary`);
 * [background] is this domain type's name for the wire format's `bg` field.
 *
 * There is no `isBuiltin` flag: the real `ThemeOut` schema doesn't expose one, so built-in
 * protection can't be pre-emptively gated client-side. Attempting to rename/delete a protected
 * theme is expected to fail server-side; that failure surfaces as a normal [UiState.Error] via
 * the existing action-state error handling.
 */
data class ThemeOption(
    val id: String,
    val name: String,
    val background: String,
    val surface: String,
    val surface2: String,
    val accent: String,
    val primary: String,
    val secondary: String,
    val success: String,
    val warning: String,
    val error: String
)

fun ThemeDto.toDomain(): ThemeOption = ThemeOption(
    id = id,
    name = name,
    background = colors.bg,
    surface = colors.surface,
    surface2 = colors.surface2,
    accent = colors.accent,
    primary = colors.primary,
    secondary = colors.secondary,
    success = colors.success,
    warning = colors.warning,
    error = colors.error
)

/** Rebuilds the wire-format 9-color palette from a [ThemeOption], e.g. to copy it into a new theme. */
fun ThemeOption.toColorsDto(): ThemeColorsDto = ThemeColorsDto(
    bg = background,
    surface = surface,
    surface2 = surface2,
    accent = accent,
    primary = primary,
    secondary = secondary,
    success = success,
    warning = warning,
    error = error
)

/** Issue #25: the effective theme plus whether it came from a personal override. */
data class CurrentTheme(
    val theme: ThemeOption,
    val isPersonalOverride: Boolean
)

fun CurrentThemeDto.toDomain(): CurrentTheme = CurrentTheme(
    theme = ThemeOption(
        id = id,
        name = name,
        background = colors.bg,
        surface = colors.surface,
        surface2 = colors.surface2,
        accent = colors.accent,
        primary = colors.primary,
        secondary = colors.secondary,
        success = colors.success,
        warning = colors.warning,
        error = colors.error
    ),
    isPersonalOverride = is_personal
)

/**
 * Issue #25: the site-wide default theme's id/name only (no colors), from the non-admin-gated
 * `GET /v1/theme/default-info`. See [com.derekwinters.chores.data.repository.ThemeRepository.getDefaultThemeInfo].
 */
data class ThemeDefaultInfo(val id: String, val name: String)

fun ThemeDefaultInfoDto.toDomain(): ThemeDefaultInfo = ThemeDefaultInfo(id = id, name = name)
