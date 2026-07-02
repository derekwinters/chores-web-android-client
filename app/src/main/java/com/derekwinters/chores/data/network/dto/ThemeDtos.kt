package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/theme` (issue #24): the 6 built-in themes plus any custom ones.
 * The 9 individually-editable colors match chores-web's CSS-custom-property set
 * (`utils/theme.js`); `is_builtin` gates rename/delete (protected) vs. copy (always allowed).
 */
@Serializable
data class ThemeDto(
    val id: Int,
    val name: String,
    val is_builtin: Boolean = false,
    val bg: String,
    val surface: String,
    val surface2: String,
    val accent: String,
    val primary: String,
    val secondary: String,
    val success: String,
    val warning: String,
    val error: String
)

/** Request body for `POST /v1/theme` (create-via-copy), issue #24. */
@Serializable
data class CreateThemeRequestDto(
    val name: String,
    val source_theme_id: Int
)

/** Request body for `PUT /v1/theme/{id}` (rename + recolor), issue #24. All fields optional. */
@Serializable
data class UpdateThemeRequestDto(
    val name: String? = null,
    val bg: String? = null,
    val surface: String? = null,
    val surface2: String? = null,
    val accent: String? = null,
    val primary: String? = null,
    val secondary: String? = null,
    val success: String? = null,
    val warning: String? = null,
    val error: String? = null
)

/**
 * Response for `GET /v1/theme/current` (issue #25): the effective theme after resolution
 * (personal override, else household default) plus whether that resolution used a personal
 * override, so the Preferences screen can highlight "Default" vs. a specific theme.
 */
@Serializable
data class CurrentThemeDto(
    val theme: ThemeDto,
    val is_personal_override: Boolean = false
)
