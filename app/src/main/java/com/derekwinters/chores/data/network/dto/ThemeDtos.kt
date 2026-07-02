package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * The 9 individually-named theme colors (`components.schemas.ThemeColors` in the real OpenAPI
 * spec), matching chores-web's CSS-custom-property set (`utils/theme.js`). All 9 fields are
 * required with no defaults — this is a fixed, fully-specified palette, NOT a generic
 * `Map<String, String>`.
 */
@Serializable
data class ThemeColorsDto(
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

/**
 * Response element for `GET /v1/theme/list` and the mutation endpoints that return a single theme
 * (`save`, `update`, `rename`, `default/{id}`, `set/{id}`) — matches `ThemeOut` exactly: id, name,
 * and a nested [ThemeColorsDto] (not flattened onto this type at the wire level).
 */
@Serializable
data class ThemeDto(
    val id: String,
    val name: String,
    val colors: ThemeColorsDto
)

/**
 * Response for `GET /v1/theme/current` (issue #25): the effective theme after resolution
 * (personal override, else household default) plus whether that resolution used a personal
 * override, so the Preferences screen can highlight "Default" vs. a specific theme. Matches
 * `ThemeCurrentOut` — flat fields, not a nested `theme` object.
 */
@Serializable
data class CurrentThemeDto(
    val id: String,
    val name: String,
    val colors: ThemeColorsDto,
    val is_personal: Boolean
)

/**
 * Response for `GET /v1/theme/default-info` (`ThemeDefaultInfo`): the site-wide default theme's
 * id/name only, no colors. Unlike `GET /v1/theme/default` (admin-only, returns a full
 * [ThemeDto]), this endpoint is accessible to all authenticated users, which is what lets the
 * personal-preference screen (issue #25) correctly label the "Default (name)" tile even while a
 * *different* theme is active as the caller's own override — `/v1/theme/current` alone can't
 * distinguish "the household default" from "my override" once one is set.
 */
@Serializable
data class ThemeDefaultInfoDto(
    val id: String,
    val name: String
)

/** Request body for `POST /v1/theme/save` (`ThemeSave`): create a new custom theme. */
@Serializable
data class ThemeSaveRequestDto(
    val name: String,
    val colors: ThemeColorsDto
)

/**
 * Request body for `PATCH /v1/theme/update/{theme_id}` (`ThemeUpdate`): partial update — either
 * field may be omitted to leave it unchanged, but per the real schema, [colors] is all-or-nothing
 * (a full 9-color [ThemeColorsDto]), not per-field-nullable.
 */
@Serializable
data class ThemeUpdateRequestDto(
    val name: String? = null,
    val colors: ThemeColorsDto? = null
)

/**
 * Request body for `PATCH /v1/theme/rename/{theme_id}`. The real OpenAPI spec leaves this
 * endpoint's request body untyped (`{"type": "object", "additionalProperties": true}`, no schema
 * ref), so this shape is a best-effort guess based on the operation being literally
 * "Rename Theme" and this backend's other rename-style payloads elsewhere in the API.
 */
@Serializable
data class ThemeRenameRequestDto(
    val name: String
)
