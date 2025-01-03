package com.enderthor.kCustomField.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enderthor.kCustomField.datatype.*
import com.enderthor.kCustomField.extensions.*
import kotlinx.coroutines.launch

val alignmentOptions = listOf(FieldPosition.LEFT, FieldPosition.CENTER, FieldPosition.RIGHT)

@Composable
fun TabLayout() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Horizo.", "Verti.", "General")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title, fontSize = 11.sp) },
                )
            }
        }

        when (selectedTabIndex) {
            0 -> ConfH()
            1 -> ConfV()
            2 -> ConfGeneral()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfV() {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var ispalettezwift by remember { mutableStateOf(true) }
    var iscenteralign by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscentervertical by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterkaroo by remember { mutableStateOf(false) }

    val fieldStates = rememberFieldStates()

    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->
            fieldStates.updateFromSettings(settings)
        }
    }

    var savedDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            ispalettezwift = settings.ispalettezwift
            iscenteralign = settings.iscenteralign
            iscentervertical = settings.iscentervertical
            iscenterkaroo = settings.iscenterkaroo
        }
    }

    LaunchedEffect(fieldStates.allFields) {
        fieldStates.updateZones()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(5.dp).verticalScroll(rememberScrollState()).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TopAppBar(title = { Text("Vertical Field 1") })
            FieldConfiguration(fieldStates, 1, true)
            TopAppBar(title = { Text("Vertical Field 2") })
            FieldConfiguration(fieldStates, 2, true)
            TopAppBar(title = { Text("Vertical Field 3") })
            FieldConfiguration(fieldStates, 3, true)

            FilledTonalButton(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                val newSettings = fieldStates.toCustomFieldSettings()
                val newGeneralSettings = GeneralSettings(
                    ispalettezwift = ispalettezwift,
                    iscenteralign = iscenteralign,
                    iscentervertical = iscentervertical,
                    iscenterkaroo = iscenterkaroo
                )
                coroutineScope.launch {
                    savedDialogVisible = true
                    saveSettings(ctx, newSettings)
                    saveGeneralSettings(ctx, newGeneralSettings)
                }
            }) {
                Icon(Icons.Default.Done, contentDescription = "Save")
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save")
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    if (savedDialogVisible) {
        AlertDialog(onDismissRequest = { savedDialogVisible = false },
            confirmButton = { Button(onClick = { savedDialogVisible = false }) { Text("OK") } },
            text = { Text("Settings saved successfully.") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfH() {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var ispalettezwift by remember { mutableStateOf(true) }
    var iscenteralign by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscentervertical by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterkaroo by remember { mutableStateOf(false) }

    val fieldStates = rememberFieldStates()

    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->
            fieldStates.updateFromSettings(settings)
        }
    }

    var savedDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            ispalettezwift = settings.ispalettezwift
            iscenteralign = settings.iscenteralign
            iscentervertical = settings.iscentervertical
            iscenterkaroo = settings.iscenterkaroo
        }
    }

    LaunchedEffect(fieldStates.allFields) {
        fieldStates.updateZones()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(5.dp).verticalScroll(rememberScrollState()).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TopAppBar(title = { Text("Horizontal Field 1") })
            FieldConfiguration(fieldStates, 1, false)
            TopAppBar(title = { Text("Horizontal Field 2") })
            FieldConfiguration(fieldStates, 2, false)
            TopAppBar(title = { Text("Horizontal Field 3") })
            FieldConfiguration(fieldStates, 3, false)

            FilledTonalButton(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                val newSettings = fieldStates.toCustomFieldSettings()
                val newGeneralSettings = GeneralSettings(
                    ispalettezwift = ispalettezwift,
                    iscenteralign = iscenteralign,
                    iscentervertical = iscentervertical,
                    iscenterkaroo = iscenterkaroo
                )
                coroutineScope.launch {
                    savedDialogVisible = true
                    saveSettings(ctx, newSettings)
                    saveGeneralSettings(ctx, newGeneralSettings)
                }
            }) {
                Icon(Icons.Default.Done, contentDescription = "Save")
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save")
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    if (savedDialogVisible) {
        AlertDialog(onDismissRequest = { savedDialogVisible = false },
            confirmButton = { Button(onClick = { savedDialogVisible = false }) { Text("OK") } },
            text = { Text("Settings saved successfully.") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfGeneral() {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var ispalettezwift by remember { mutableStateOf(true) }
    var iscenteralign by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscentervertical by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterkaroo by remember { mutableStateOf(false) }

    val fieldStates = rememberFieldStates()

    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->
            fieldStates.updateFromSettings(settings)
        }
    }

    var savedDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            ispalettezwift = settings.ispalettezwift
            iscenteralign = settings.iscenteralign
            iscentervertical = settings.iscentervertical
            iscenterkaroo = settings.iscenterkaroo
        }
    }

    LaunchedEffect(fieldStates.allFields) {
        fieldStates.updateZones()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(5.dp).verticalScroll(rememberScrollState()).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TopAppBar(title = { Text("General Settings") })

            TopAppBar(title = { Text("Fields Alignment") })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = false, onCheckedChange = {
                    iscenterkaroo = it
                },enabled = false)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Use default Karoo Alignment ?")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Horizontal Fields alignment (icon/text) ?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MultiToggleButton(alignmentOptions.indexOf(iscenteralign), alignmentOptions.map { it.name }, onToggleChange = { iscentervertical = alignmentOptions[it] })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vertical Fields alignment (icon/text) ?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MultiToggleButton(alignmentOptions.indexOf(iscentervertical), alignmentOptions.map { it.name }, onToggleChange = { iscentervertical = alignmentOptions[it] })
            }

            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text("Color Palette") })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ispalettezwift, onCheckedChange = {
                    ispalettezwift = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text("Zwift Color palette?")
            }

            FilledTonalButton(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                val newSettings = fieldStates.toCustomFieldSettings()
                val newGeneralSettings = GeneralSettings(
                    ispalettezwift = ispalettezwift,
                    iscenteralign = iscenteralign,
                    iscentervertical = iscentervertical,
                    iscenterkaroo = iscenterkaroo
                )
                coroutineScope.launch {
                    savedDialogVisible = true
                    saveSettings(ctx, newSettings)
                    saveGeneralSettings(ctx, newGeneralSettings)
                }
            }) {
                Icon(Icons.Default.Done, contentDescription = "Save")
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save")
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    if (savedDialogVisible) {
        AlertDialog(onDismissRequest = { savedDialogVisible = false },
            confirmButton = { Button(onClick = { savedDialogVisible = false }) { Text("OK") } },
            text = { Text("Settings saved successfully.") }
        )
    }
}

@Composable
fun FieldConfiguration(fieldStates: FieldStates, fieldIndex: Int, isVertical: Boolean) {
    val (leftAction, rightAction, leftZone, rightZone, horizontalField) = fieldStates.getField(fieldIndex, isVertical)

    DropdownField("Left", leftAction) { newAction -> fieldStates.updateLeftAction(fieldIndex, newAction, isVertical) }
    ZoneSwitch(leftZone, leftAction.zone != "none") { newZone -> fieldStates.updateLeftZone(fieldIndex, newZone, isVertical) }
    DropdownField("Right", rightAction) { newAction -> fieldStates.updateRightAction(fieldIndex, newAction, isVertical) }
    ZoneSwitch(rightZone, rightAction.zone != "none") { newZone -> fieldStates.updateRightZone(fieldIndex, newZone, isVertical) }
    DividerSwitch(horizontalField, !(leftZone || rightZone)) { newField -> fieldStates.updateHorizontalField(fieldIndex, newField, isVertical) }
}

@Composable
fun DropdownField(label: String, action: KarooAction, onActionChange: (KarooAction) -> Unit) {
    val dropdownOptions = KarooAction.entries.toList().map { unit -> DropdownOption(unit.action.toString(), unit.label) }
    val dropdownInitialSelection by remember(action) { mutableStateOf(dropdownOptions.find { option -> option.id == action.action.toString() }!!) }
    KarooKeyDropdown(remotekey = label, options = dropdownOptions, selectedOption = dropdownInitialSelection) { selectedOption ->
        onActionChange(KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!)
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

@Composable
fun DividerSwitch(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Spacer(modifier = Modifier.width(10.dp))
        Text("Horizontal Divider?")
    }
}

@Composable
fun rememberFieldStates(): FieldStates {
    return remember { FieldStates() }
}
class FieldStates {
    var bottomleft1 by mutableStateOf(KarooAction.SPEED)
    var bottomright1 by mutableStateOf(KarooAction.SPEED)
    var bottomleft2 by mutableStateOf(KarooAction.CADENCE)
    var bottomright2 by mutableStateOf(KarooAction.SLOPE)
    var bottomleft3 by mutableStateOf(KarooAction.SPEED)
    var bottomright3 by mutableStateOf(KarooAction.SPEED)
    var customleft1zone by mutableStateOf(false)
    var customright1zone by mutableStateOf(false)
    var customleft2zone by mutableStateOf(false)
    var customright2zone by mutableStateOf(false)
    var customleft3zone by mutableStateOf(false)
    var customright3zone by mutableStateOf(false)
    var isverticalfield1 by mutableStateOf(false)
    var isverticalfield2 by mutableStateOf(false)
    var isverticalfield3 by mutableStateOf(false)
    var bottomverticalleft1 by mutableStateOf(KarooAction.SPEED)
    var bottomverticalright1 by mutableStateOf(KarooAction.SPEED)
    var bottomverticalleft2 by mutableStateOf(KarooAction.CADENCE)
    var bottomverticalright2 by mutableStateOf(KarooAction.SLOPE)
    var bottomverticalleft3 by mutableStateOf(KarooAction.CADENCE)
    var bottomverticalright3 by mutableStateOf(KarooAction.SLOPE)
    var customverticalleft1zone by mutableStateOf(false)
    var customverticalright1zone by mutableStateOf(false)
    var customverticalleft2zone by mutableStateOf(false)
    var customverticalright2zone by mutableStateOf(false)
    var customverticalleft3zone by mutableStateOf(false)
    var customverticalright3zone by mutableStateOf(false)
    var ishorizontalfield1 by mutableStateOf(false)
    var ishorizontalfield2 by mutableStateOf(false)
    var ishorizontalfield3 by mutableStateOf(false)

    val allFields
        get() = listOf(
            bottomleft1, bottomright1, bottomleft2, bottomright2, bottomleft3, bottomright3,
            customleft1zone, customright1zone, customleft2zone, customright2zone, customleft3zone, customright3zone,
            bottomverticalleft1, bottomverticalright1, bottomverticalleft2, bottomverticalright2, bottomverticalleft3, bottomverticalright3,
            customverticalleft1zone, customverticalright1zone, customverticalleft2zone, customverticalright2zone, customverticalleft3zone, customverticalright3zone
        )

    fun updateFromSettings(settings: CustomFieldSettings) {
        bottomright1 = settings.customright1
        bottomleft1 = settings.customleft1
        bottomright3 = settings.customright3
        bottomleft3 = settings.customleft3
        bottomright2 = settings.customright2
        bottomleft2 = settings.customleft2
        customleft1zone = settings.customleft1zone
        customright1zone = settings.customright1zone
        customleft3zone = settings.customleft3zone
        customright3zone = settings.customright3zone
        customleft2zone = settings.customleft2zone
        customright2zone = settings.customright2zone
        isverticalfield1 = settings.isvertical1
        isverticalfield2 = settings.isvertical2
        isverticalfield3 = settings.isvertical3
        bottomverticalright1 = settings.customverticalright1
        bottomverticalleft1 = settings.customverticalleft1
        bottomverticalright2 = settings.customverticalright2
        bottomverticalleft2 = settings.customverticalleft2
        bottomverticalright3 = settings.customverticalright3
        bottomverticalleft3 = settings.customverticalleft3
        customverticalleft1zone = settings.customverticalleft1zone
        customverticalright1zone = settings.customverticalright1zone
        customverticalleft2zone = settings.customverticalleft2zone
        customverticalright2zone = settings.customverticalright2zone
        customverticalleft3zone = settings.customverticalleft3zone
        customverticalright3zone = settings.customverticalright3zone
        ishorizontalfield1 = settings.ishorizontal1
        ishorizontalfield2 = settings.ishorizontal2
        ishorizontalfield3 = settings.ishorizontal3
    }

    fun updateZones() {
        val actions = listOf(
            bottomleft1 to { customleft1zone = false },
            bottomleft2 to { customleft2zone = false },
            bottomleft3 to { customleft3zone = false },
            bottomright1 to { customright1zone = false },
            bottomright2 to { customright2zone = false },
            bottomright3 to { customright3zone = false },
            bottomverticalleft1 to { customverticalleft1zone = false },
            bottomverticalleft2 to { customverticalleft2zone = false },
            bottomverticalleft3 to { customverticalleft3zone = false },
            bottomverticalright1 to { customverticalright1zone = false },
            bottomverticalright2 to { customverticalright2zone = false },
            bottomverticalright3 to { customverticalright3zone = false }
        )
        actions.forEach { (action, applyzone) ->
            if (action.zone == "none") applyzone()
        }

        val zones = listOf(
            customleft1zone to { ishorizontalfield1 = true },
            customright1zone to { ishorizontalfield1 = true },
            customleft2zone to { ishorizontalfield2 = true },
            customright2zone to { ishorizontalfield2 = true },
            customleft3zone to { ishorizontalfield3 = true },
            customright3zone to { ishorizontalfield3 = true },
            customverticalleft1zone to { isverticalfield1 = true },
            customverticalright1zone to { isverticalfield1 = true },
            customverticalleft2zone to { isverticalfield2 = true },
            customverticalright2zone to { isverticalfield2 = true },
            customverticalleft3zone to { isverticalfield3 = true },
            customverticalright3zone to { isverticalfield3 = true }
        )
        zones.forEach { (zone, setField) ->
            if (zone) setField()
        }
    }

    fun getField(index: Int, isVertical: Boolean): FieldState {
        return if (isVertical) {
            when (index) {
                1 -> FieldState(bottomverticalleft1, bottomverticalright1, customverticalleft1zone, customverticalright1zone, isverticalfield1)
                2 -> FieldState(bottomverticalleft2, bottomverticalright2, customverticalleft2zone, customverticalright2zone, isverticalfield2)
                3 -> FieldState(bottomverticalleft3, bottomverticalright3, customverticalleft3zone, customverticalright3zone, isverticalfield3)
                else -> throw IllegalArgumentException("Invalid field index")
            }
        } else {
            when (index) {
                1 -> FieldState(bottomleft1, bottomright1, customleft1zone, customright1zone, ishorizontalfield1)
                2 -> FieldState(bottomleft2, bottomright2, customleft2zone, customright2zone, ishorizontalfield2)
                3 -> FieldState(bottomleft3, bottomright3, customleft3zone, customright3zone, ishorizontalfield3)
                else -> throw IllegalArgumentException("Invalid field index")
            }
        }
    }

    fun updateLeftAction(index: Int, action: KarooAction, isVertical: Boolean) {
        if (isVertical) {
            when (index) {
                1 -> bottomverticalleft1 = action
                2 -> bottomverticalleft2 = action
                3 -> bottomverticalleft3 = action
            }
        } else {
            when (index) {
                1 -> bottomleft1 = action
                2 -> bottomleft2 = action
                3 -> bottomleft3 = action
            }
        }
    }

    fun updateRightAction(index: Int, action: KarooAction, isVertical: Boolean) {
        if (isVertical) {
            when (index) {
                1 -> bottomverticalright1 = action
                2 -> bottomverticalright2 = action
                3 -> bottomverticalright3 = action
            }
        } else {
            when (index) {
                1 -> bottomright1 = action
                2 -> bottomright2 = action
                3 -> bottomright3 = action
            }
        }
    }

    fun updateLeftZone(index: Int, zone: Boolean, isVertical: Boolean) {
        if (isVertical) {
            when (index) {
                1 -> customverticalleft1zone = zone
                2 -> customverticalleft2zone = zone
                3 -> customverticalleft3zone = zone
            }
        } else {
            when (index) {
                1 -> customleft1zone = zone
                2 -> customleft2zone = zone
                3 -> customleft3zone = zone
            }
        }
    }

    fun updateRightZone(index: Int, zone: Boolean, isVertical: Boolean) {
        if (isVertical) {
            when (index) {
                1 -> customverticalright1zone = zone
                2 -> customverticalright2zone = zone
                3 -> customverticalright3zone = zone
            }
        } else {
            when (index) {
                1 -> customright1zone = zone
                2 -> customright2zone = zone
                3 -> customright3zone = zone
            }
        }
    }

    fun updateHorizontalField(index: Int, field: Boolean, isVertical: Boolean) {
        if (isVertical) {
            when (index) {
                1 -> isverticalfield1 = field
                2 -> isverticalfield2 = field
                3 -> isverticalfield3 = field
            }
        } else {
            when (index) {
                1 -> ishorizontalfield1 = field
                2 -> ishorizontalfield2 = field
                3 -> ishorizontalfield3 = field
            }
        }
    }

    fun toCustomFieldSettings(): CustomFieldSettings {
        return CustomFieldSettings(
            customleft1 = bottomleft1, customright1 = bottomright1, customleft2 = bottomleft2, customright2 = bottomright2, customleft3 = bottomleft3, customright3 = bottomright3,
            customleft1zone = customleft1zone, customright1zone = customright1zone, customleft2zone = customleft2zone, customright2zone = customright2zone, customleft3zone = customleft3zone, customright3zone = customright3zone,
            isvertical1 = isverticalfield1, isvertical2 = isverticalfield2, isvertical3 = isverticalfield3,
            customverticalleft1 = bottomverticalleft1, customverticalright1 = bottomverticalright1, customverticalleft2 = bottomverticalleft2, customverticalright2 = bottomverticalright2,
            customverticalleft3 = bottomverticalleft3, customverticalright3 = bottomverticalright3,
            customverticalleft1zone = customverticalleft1zone, customverticalright1zone = customverticalright1zone, customverticalleft2zone = customverticalleft2zone, customverticalright2zone = customverticalright2zone,
            customverticalleft3zone = customverticalleft3zone, customverticalright3zone = customverticalright3zone, ishorizontal1 = ishorizontalfield1, ishorizontal2 = ishorizontalfield2, ishorizontal3 = ishorizontalfield3
        )
    }
}

data class FieldState(
    val leftAction: KarooAction,
    val rightAction: KarooAction,
    val leftZone: Boolean,
    val rightZone: Boolean,
    val horizontalField: Boolean
)