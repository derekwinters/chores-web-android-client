package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.CurrentThemeDto
import com.derekwinters.chores.data.network.dto.ThemeDto

/**
 * Domain model for one theme (issue #24: 6 built-ins + custom; issue #25: personal picks from
 * this same set). Named `ThemeOption` (not `Theme`) to avoid clashing with Compose's own `Theme`
 * naming conventions (see ui/theme/ChoresTheme.kt).
 */
data class ThemeOption(
    val id: Int,
    val name: String,
    val isBuiltin: Boolean,
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
    isBuiltin = is_builtin,
    background = bg,
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
    theme = theme.toDomain(),
    isPersonalOverride = is_personal_override
)
