package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.R
import com.derekwinters.chores.chores.Person

/**
 * Completer-picker dialog, shown when completing a chore whose `current_assignee == null`.
 * Mirrors chores-web's `CompleteWithActorModal.jsx`: lists [people] by display name, and reports
 * the selected person's username to [onConfirm].
 */
@Composable
fun CompleterPickerDialog(
    choreName: String,
    people: List<Person>,
    onConfirm: (username: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(people) { mutableStateOf<Person?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.completer_picker_title, choreName)) },
        text = {
            Column {
                people.forEach { person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = person }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == person, onClick = { selected = person })
                        Text(text = person.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let { onConfirm(it.username) } }
            ) {
                Text(stringResource(R.string.completer_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.completer_picker_cancel))
            }
        }
    )
}
