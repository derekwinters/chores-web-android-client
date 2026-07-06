package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.CHORE_POINT_OPTIONS
import com.derekwinters.chores.ui.UiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Issue #100: named day-abbreviation labels for the weekly-schedule day picker, indexed to match
 * the existing `weeklyDays` data model (0=Sun ... 6=Sat) — display-only, no change to the
 * underlying weekday indices.
 */
private val WEEKDAY_ABBREVIATIONS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

/**
 * Issue #16: chore create/edit form, chores-web's `ChoreForm.jsx` equivalent — the richest
 * screen in this app. Field/validation rules live in [ChoreFormState]/ChoreFormState.validate();
 * this composable is "dumb" UI over that state.
 *
 * Thin Hilt-wired wrapper around [ChoreFormContent].
 */
@Composable
fun ChoreFormScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ChoreFormViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val availablePeople by viewModel.availablePeople.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val loadState by viewModel.loadState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) onSaved()
    }

    if (loadState is UiState.Loading) {
        CircularProgressIndicator()
        return
    }

    ChoreFormContent(
        formState = formState,
        availablePeople = availablePeople,
        saveState = saveState,
        isEditMode = viewModel.isEditMode,
        onFormChange = viewModel::updateForm,
        onSave = viewModel::save,
        onCancel = onCancel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreFormContent(
    formState: ChoreFormState,
    availablePeople: List<String>,
    saveState: UiState<Unit>,
    isEditMode: Boolean,
    onFormChange: ((ChoreFormState) -> ChoreFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSaving = saveState is UiState.Loading

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isEditMode) "Edit Chore" else "New Chore",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            value = formState.name,
            onValueChange = { name -> onFormChange { it.copy(name = name) } },
            label = { Text("Name") },
            singleLine = true,
            enabled = !isSaving
        )

        SectionLabel("Points")
        Row(modifier = Modifier.fillMaxWidth()) {
            CHORE_POINT_OPTIONS.forEach { option ->
                FilterChip(
                    modifier = Modifier.padding(end = 4.dp),
                    selected = formState.points == option,
                    onClick = { onFormChange { it.copy(points = option) } },
                    label = { Text(option.toString()) },
                    enabled = !isSaving
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enabled")
            Switch(
                checked = !formState.disabled,
                onCheckedChange = { enabled -> onFormChange { it.copy(disabled = !enabled) } },
                enabled = !isSaving
            )
        }

        if (isEditMode) {
            var showDatePicker by remember { mutableStateOf(false) }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                value = formState.nextDue.orEmpty(),
                onValueChange = {},
                label = { Text("Next Due") },
                singleLine = true,
                readOnly = true,
                enabled = !isSaving,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }, enabled = !isSaving) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Next Due")
                    }
                }
            )

            if (showDatePicker) {
                val initialSelectedMillis = formState.nextDue?.let { raw ->
                    try {
                        LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    } catch (e: DateTimeParseException) {
                        null
                    }
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedMillis)

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                                onFormChange { it.copy(nextDue = selectedDate.toString()) }
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }

        SectionLabel("Assignment")
        RadioGrid(
            options = AssignmentType.ALL,
            selected = formState.assignmentType,
            enabled = !isSaving
        ) { type -> onFormChange { it.copy(assignmentType = type) } }
        when (formState.assignmentType) {
            AssignmentType.FIXED -> {
                SectionLabel("Assignee")
                availablePeople.forEach { person ->
                    RadioRow(person, formState.assignee == person, !isSaving) {
                        onFormChange { it.copy(assignee = person) }
                    }
                }
            }
            AssignmentType.OPEN -> {
                SectionLabel("Eligible people (optional)")
                availablePeople.forEach { person ->
                    CheckboxRow(person, person in formState.eligiblePeople, !isSaving) { checked ->
                        onFormChange {
                            it.copy(eligiblePeople = if (checked) it.eligiblePeople + person else it.eligiblePeople - person)
                        }
                    }
                }
            }
            AssignmentType.ROTATING -> {
                SectionLabel("Rotation (2+ people)")
                availablePeople.forEach { person ->
                    CheckboxRow(person, person in formState.eligiblePeople, !isSaving) { checked ->
                        onFormChange {
                            it.copy(eligiblePeople = if (checked) it.eligiblePeople + person else it.eligiblePeople - person)
                        }
                    }
                }
            }
        }

        SectionLabel("Schedule")
        RadioGrid(
            options = ScheduleType.ALL,
            selected = formState.scheduleType,
            enabled = !isSaving
        ) { type -> onFormChange { it.copy(scheduleType = type) } }
        when (formState.scheduleType) {
            ScheduleType.WEEKLY -> {
                SectionLabel("Days of week")
                Row(modifier = Modifier.fillMaxWidth()) {
                    WEEKDAY_ABBREVIATIONS.forEachIndexed { day, abbreviation ->
                        FilterChip(
                            modifier = Modifier.padding(end = 4.dp),
                            selected = day in formState.weeklyDays,
                            onClick = {
                                // Derive the toggle from the state passed into the updater (`it`),
                                // not from a boolean captured at composition time -- FilterChip's
                                // onClick (unlike Checkbox's onCheckedChange) has no built-in
                                // "new value" parameter, so we must compute membership ourselves;
                                // reading it fresh off `it.weeklyDays` here avoids ever toggling
                                // against a stale/composition-time snapshot of selection state.
                                onFormChange {
                                    it.copy(weeklyDays = if (day in it.weeklyDays) it.weeklyDays - day else it.weeklyDays + day)
                                }
                            },
                            label = { Text(abbreviation) },
                            enabled = !isSaving
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = formState.everyOtherWeek,
                        onCheckedChange = { checked -> onFormChange { it.copy(everyOtherWeek = checked) } },
                        enabled = !isSaving
                    )
                    Text("Every other week")
                }
            }
            ScheduleType.INTERVAL -> {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    value = formState.intervalDays?.toString().orEmpty(),
                    onValueChange = { value -> onFormChange { it.copy(intervalDays = value.toIntOrNull()) } },
                    label = { Text("Every N days") },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
            ScheduleType.MONTHLY -> {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    value = formState.dayOfMonth?.toString().orEmpty(),
                    onValueChange = { value -> onFormChange { it.copy(dayOfMonth = value.toIntOrNull()) } },
                    label = { Text("Day of month") },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
            ScheduleType.YEARLY -> {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    value = formState.month?.toString().orEmpty(),
                    onValueChange = { value -> onFormChange { it.copy(month = value.toIntOrNull()) } },
                    label = { Text("Month (1-12)") },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
        }

        if (formState.scheduleType != ScheduleType.YEARLY) {
            SectionLabel("Constraints")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = formState.evenOddConstraint == "even",
                    onClick = { onFormChange { it.copy(evenOddConstraint = if (it.evenOddConstraint == "even") null else "even") } },
                    enabled = !isSaving
                )
                Text("Even days only")
                RadioButton(
                    selected = formState.evenOddConstraint == "odd",
                    onClick = { onFormChange { it.copy(evenOddConstraint = if (it.evenOddConstraint == "odd") null else "odd") } },
                    enabled = !isSaving
                )
                Text("Odd days only")
            }
        }

        if (saveState is UiState.Error) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = saveState.message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, enabled = !isSaving) { Text("Cancel") }
            Button(modifier = Modifier.padding(start = 8.dp), onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text("Save")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun RadioRow(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Issue #97: assignment-type/schedule-type radio options laid out in a 3-column grid
 * (matching web) instead of a vertical stack. Options wrap into rows of 3; a short final row
 * is padded with empty [Spacer]s so cells stay aligned to the 3-column grid.
 */
@Composable
private fun RadioGrid(
    options: List<String>,
    selected: String,
    enabled: Boolean,
    labelFor: (String) -> String = { it.replaceFirstChar(Char::uppercase) },
    onSelect: (String) -> Unit
) {
    options.chunked(3).forEach { rowOptions ->
        Row(modifier = Modifier.fillMaxWidth()) {
            rowOptions.forEach { option ->
                RadioGridCell(
                    modifier = Modifier.weight(1f),
                    label = labelFor(option),
                    selected = selected == option,
                    enabled = enabled,
                    onClick = { onSelect(option) }
                )
            }
            repeat(3 - rowOptions.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RadioGridCell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .selectable(selected = selected, enabled = enabled, onClick = onClick)
            .padding(top = 2.dp, bottom = 2.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(text = label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .selectable(selected = checked, enabled = enabled, onClick = { onCheckedChange(!checked) })
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Text(text = label, modifier = Modifier.padding(start = 4.dp))
    }
}
