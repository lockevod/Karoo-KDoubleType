package com.enderthor.kCustomField.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.enderthor.kCustomField.datatype.KarooAction
import com.enderthor.kCustomField.datatype.OneFieldType
import java.util.Locale


data class DropdownOption(val id: String, val name: String)

fun String.toCapital(): String {
    return this.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault())} }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiToggleButton(
    currentSelection: Int,
    toggleStates: List<String>,
    onToggleChange: (Int) -> Unit
) {
    val selectedTint = MaterialTheme.colorScheme.primary
    val unselectedTint = Color.Unspecified

    Row(modifier = Modifier
        .height(IntrinsicSize.Min)
        .border(BorderStroke(1.dp, Color.LightGray))) {
        toggleStates.forEachIndexed { index, toggleState ->
            val isSelected = currentSelection == index
            val backgroundTint = if (isSelected) selectedTint else unselectedTint
            val textColor = if (isSelected) Color.White else Color.Unspecified

            if (index != 0) {
                HorizontalDivider(
                    color = Color.LightGray,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
            }

            Row(
                modifier = Modifier
                    .background(backgroundTint)
                    .padding(vertical = 6.dp, horizontal = 8.dp)
                    .toggleable(
                        value = isSelected,
                        enabled = true,
                        onValueChange = { selected ->
                            if (selected) {
                                onToggleChange(index)
                            }
                        })
            ) {
                Text(toggleState.toCapital(),color = textColor, modifier = Modifier.padding(1.dp))
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarooKeyDropdown(remotekey: String, options: List<DropdownOption>, selectedOption: DropdownOption, onSelect: (selectedOption: DropdownOption) -> Unit) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOption.name,
            onValueChange = { },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            label = { Text(remotekey) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun DropdownOneField(firstpos: Boolean, label: String, action: OneFieldType, onActionChange: (OneFieldType) -> Unit) {

    var dropdownOptions = KarooAction.entries.map { DropdownOption(it.action.toString(), it.label) }
    if (!firstpos) dropdownOptions = listOf(DropdownOption("none", "None")) + dropdownOptions

    val dropdownInitialSelection by remember(action) {
        mutableStateOf(
            if (!action.isactive) {
                dropdownOptions.find { it.id == "none" } ?: dropdownOptions.first()
            } else {
                dropdownOptions.find { it.id == action.kaction.action.toString() } ?: dropdownOptions.first()
            }
        )
    }

    KarooKeyDropdown(remotekey = label, options = dropdownOptions, selectedOption = dropdownInitialSelection) { selectedOption ->
        val newAction = if (selectedOption.id == "none") {
            OneFieldType(KarooAction.SPEED, false, false)
        } else {
            OneFieldType(KarooAction.entries.find { it.action == selectedOption.id } ?: KarooAction.SPEED, true, false)
        }
        onActionChange(newAction)
    }
}

@Composable
fun ZoneSwitch(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Spacer(modifier = Modifier.width(10.dp))
        Text("Coloured zone?")
    }
}