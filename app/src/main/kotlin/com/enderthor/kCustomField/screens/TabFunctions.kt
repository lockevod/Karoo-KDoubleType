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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.enderthor.kCustomField.datatype.DoubleFieldType
import com.enderthor.kCustomField.datatype.GeneralSettings
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
    enabled: Boolean,
    onToggleChange: (Int) -> Unit
) {
    val selectedTint = MaterialTheme.colorScheme.primary
    val unselectedTint = Color.Unspecified

    Row(modifier = Modifier
        .height(IntrinsicSize.Min)
        .border(BorderStroke(1.dp, Color.LightGray))) {
        toggleStates.forEachIndexed { index, toggleState ->
            val isSelected = currentSelection == index
            val backgroundTint = if (isSelected && enabled) selectedTint else unselectedTint
            val textColor = if (isSelected && enabled) Color.White else Color.Unspecified

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
                        enabled = enabled,
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
fun KarooKeyDropdown(remotekey: String, options: List<DropdownOption>, selectedOption: DropdownOption, enabled: Boolean, onSelect: (selectedOption: DropdownOption) -> Unit) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
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
                        if (enabled) {
                            expanded = false
                            onSelect(option)
                        }
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun DropdownOneField(enabled: Boolean, firstpos: Boolean, label: String, action: OneFieldType,  generalSettings: GeneralSettings = GeneralSettings(), onActionChange: (OneFieldType) -> Unit) {

    // Orden alfabético por label (igual que el resto): así KPW.../Ghost... no caen al final.
    var dropdownOptions = KarooAction.entries.map { DropdownOption(it.action, it.label) }
        .sortedBy { it.name }

    if (!generalSettings.isheadwindenabled) {
        dropdownOptions = dropdownOptions.filter { it.id != KarooAction.HEADWIND.action }
    }
    if (!generalSettings.iskpowerenabled) {
        dropdownOptions = dropdownOptions.filter { !it.id.contains("::kpower::") }
    }
    if (!generalSettings.iskghostenabled) {
        dropdownOptions = dropdownOptions.filter { !it.id.contains("::kghost::") }
    }

    // Mantener visible el campo YA seleccionado aunque su toggle esté apagado: el toggle solo
    // descongestiona el picker, no destruye lo configurado (KPower/KGhost pintan "---" sin la
    // extensión, no hace falta resetearlos).
    if (action.isactive && dropdownOptions.none { it.id == action.kaction.action }) {
        dropdownOptions = dropdownOptions + DropdownOption(action.kaction.action, action.kaction.label)
    }


    if (!firstpos) dropdownOptions = listOf(DropdownOption("none", "None")) + dropdownOptions


    val dropdownInitialSelection by remember(action) {
        mutableStateOf(
            if (!action.isactive || !enabled) {
                dropdownOptions.find { it.id == "none" } ?: dropdownOptions.first()
            } else {
                dropdownOptions.find { it.id == action.kaction.action } ?: dropdownOptions.first()
            }
        )
    }
    //Timber.d("DROPDOWN INITIAL SELECTION Label $label Action $action InitialSelection $dropdownInitialSelection enabled $enabled")
    KarooKeyDropdown(enabled=enabled,remotekey = label, options = dropdownOptions, selectedOption = dropdownInitialSelection) { selectedOption ->
        val newAction = if (selectedOption.id == "none" || !enabled) {
            OneFieldType(KarooAction.SPEED, false, false)
        } else {
            OneFieldType(KarooAction.entries.find { it.action == selectedOption.id } ?: KarooAction.SPEED, false, true)
        }
       // Timber.d("NEW ACTION $newAction")
        onActionChange(newAction)
    }
}



@Composable
fun DropdownDoubleField(label: String, action: DoubleFieldType, generalSettings: GeneralSettings, onActionChange: (DoubleFieldType) -> Unit) {
    // Orden alfabético por label (igual que el resto): así KPW.../Ghost... no caen al final.
    var dropdownOptions = KarooAction.entries.map { DropdownOption(it.action, it.label) }
        .sortedBy { it.name }

    // Filtrar opciones de extensiones cuyo toggle esté apagado.
    if (!generalSettings.isheadwindenabled) {
        dropdownOptions = dropdownOptions.filter { it.id != KarooAction.HEADWIND.action }
    }
    if (!generalSettings.iskpowerenabled) {
        dropdownOptions = dropdownOptions.filter { !it.id.contains("::kpower::") }
    }
    if (!generalSettings.iskghostenabled) {
        dropdownOptions = dropdownOptions.filter { !it.id.contains("::kghost::") }
    }

    // Mantener visible el campo YA seleccionado aunque su toggle esté apagado: declutter sin
    // destruir la configuración. KPower/KGhost pintan "---" sin la extensión, así que (al revés
    // que headwind) no se resetean a SPEED al ocultarlos.
    if (dropdownOptions.none { it.id == action.kaction.action }) {
        dropdownOptions = dropdownOptions + DropdownOption(action.kaction.action, action.kaction.label)
    }

    val dropdownInitialSelection by remember(action) {
        mutableStateOf(
            dropdownOptions.find { it.id == action.kaction.action } ?: dropdownOptions.first()
        )
    }

    // HEADWIND sí se resetea a SPEED si se desactiva (sin la extensión Headwind el campo no funciona).
    LaunchedEffect(generalSettings.isheadwindenabled) {
        if (!generalSettings.isheadwindenabled && action.kaction == KarooAction.HEADWIND) {
            onActionChange(action.copy(kaction = KarooAction.SPEED))
        }
    }

    KarooKeyDropdown(enabled=true,remotekey = label, options = dropdownOptions, selectedOption = dropdownInitialSelection) { selectedOption ->
        val newAction = action.copy(kaction = KarooAction.entries.find { it.action == selectedOption.id } ?: KarooAction.SPEED)
        onActionChange(newAction)
    }
}



@Composable
fun ZoneMultiSwitch(option: Int, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var isChecked by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        isChecked = checked
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onCheckedChange(it)
            },
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(10.dp))
        when (option) {
            0 -> Text("Coloured Zone?")
            1 -> Text("Horizontal Field?")
            2 -> Text("Enabled Field?")
            3 -> Text("Climb Field Always Active (No Climber Measure when climber is out)?")
        }
    }
}
