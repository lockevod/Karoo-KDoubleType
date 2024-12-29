package com.enderthor.kCustomField.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch


import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import com.enderthor.kCustomField.datatype.CustomFieldSettings
import com.enderthor.kCustomField.datatype.FieldPosition
import com.enderthor.kCustomField.datatype.GeneralSettings
import com.enderthor.kCustomField.datatype.KarooAction
import com.enderthor.kCustomField.extensions.saveGeneralSettings

import com.enderthor.kCustomField.extensions.saveSettings
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.extensions.streamSettings

import kotlinx.coroutines.launch




@Composable
fun TabLayout() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf( "Horizo.","Verti.","General")

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

    var bottomleft1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomright1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomleft2 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomright2 by remember { mutableStateOf(KarooAction.SLOPE) }
    var bottomleft3 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomright3 by remember { mutableStateOf(KarooAction.SPEED) }
    var customleft1zone by remember { mutableStateOf(false) }
    var customright1zone by remember { mutableStateOf(false) }
    var customleft2zone by remember { mutableStateOf(false) }
    var customright2zone by remember { mutableStateOf(false) }
    var customleft3zone by remember { mutableStateOf(false) }
    var customright3zone by remember { mutableStateOf(false) }
    var isverticalfield1 by remember { mutableStateOf(false) }
    var isverticalfield2 by remember { mutableStateOf(false) }
    var isverticalfield3 by remember { mutableStateOf(false) }
    var bottomverticalleft1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomverticalright1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomverticalleft2 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomverticalright2 by remember { mutableStateOf(KarooAction.SLOPE) }
    var bottomverticalleft3 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomverticalright3 by remember { mutableStateOf(KarooAction.SLOPE) }
    var customverticalleft1zone by remember { mutableStateOf(false) }
    var customverticalright1zone by remember { mutableStateOf(false) }
    var customverticalleft2zone by remember { mutableStateOf(false) }
    var customverticalright3zone by remember { mutableStateOf(false) }
    var customverticalleft3zone by remember { mutableStateOf(false) }
    var customverticalright2zone by remember { mutableStateOf(false) }
    var ishorizontalfield1 by remember { mutableStateOf(false) }
    var ishorizontalfield2 by remember { mutableStateOf(false) }
    var ishorizontalfield3 by remember { mutableStateOf(false) }



    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->

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

    LaunchedEffect(
        bottomverticalleft1, bottomverticalleft2, bottomverticalleft3,bottomverticalright1, bottomverticalright2,
        bottomverticalright3,customverticalleft1zone, customverticalright1zone, customverticalleft2zone, customverticalright2zone,
        customverticalleft3zone, customverticalright3zone
        ) {
        val actions = listOf(
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
            customverticalleft1zone to { ishorizontalfield1 = true },
            customverticalright1zone to { ishorizontalfield1 = true },
            customverticalleft2zone to { ishorizontalfield2 = true },
            customverticalright2zone to { ishorizontalfield2 = true },
            customverticalleft3zone to { ishorizontalfield3 = true },
            customverticalright3zone to { ishorizontalfield3 = true }
        )
        zones.forEach { (zone, setVerticalField) ->
            if (zone) setVerticalField()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        Column(modifier = Modifier
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TopAppBar(title = { Text("Vertical Field 1") })

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomverticalleft1) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomverticalleft1.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Left",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomverticalleft1 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customverticalleft1zone, onCheckedChange = {
                    customverticalleft1zone = it
                },enabled = bottomverticalleft1.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomverticalright1) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomverticalright1.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Right",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomverticalright1 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customverticalright1zone, onCheckedChange = {
                    customverticalright1zone = it
                },enabled = bottomverticalright1.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ishorizontalfield1, onCheckedChange = {
                    ishorizontalfield1 = it
                }, enabled = !(customverticalright1zone || customverticalleft1zone))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Horizontal Divider?")
            }
            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text("Vertical Field 2") })

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomverticalleft2) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomverticalleft2.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Left",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomverticalleft2 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customverticalleft2zone, onCheckedChange = {
                    customverticalleft2zone = it
                },enabled = bottomverticalleft2.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomverticalright2) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomverticalright2.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Right",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomverticalright2 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customverticalright2zone, onCheckedChange = {
                    customverticalright2zone = it
                },enabled = bottomverticalright2.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ishorizontalfield2, onCheckedChange = {
                    ishorizontalfield2 = it
                }, enabled = !(customverticalright2zone || customverticalleft2zone))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Horizontal Divider?")
            }

            TopAppBar(title = { Text("Vertical Field 3") })
            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomverticalleft3) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomverticalleft3.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Left",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomverticalleft3 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customverticalleft3zone, onCheckedChange = {
                    customverticalleft3zone = it
                },enabled = bottomverticalleft3.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomverticalright3) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomverticalright3.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Right",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomverticalright3 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customverticalright3zone, onCheckedChange = {
                    customverticalright3zone = it
                },enabled = bottomverticalright3.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ishorizontalfield3, onCheckedChange = {
                    ishorizontalfield3 = it
                }, enabled = !(customverticalright3zone || customverticalleft3zone))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Horizontal Divider?")
            }


            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
                val newSettings =
                    CustomFieldSettings(
                        customleft1 = bottomleft1, customright1 = bottomright1, customleft2 = bottomleft2, customright2 = bottomright2,customleft3 = bottomleft3, customright3 = bottomright3,
                        customleft1zone = customleft1zone, customright1zone = customright1zone, customleft2zone = customleft2zone, customright2zone = customright2zone,customleft3zone = customleft3zone, customright3zone = customright3zone,
                        isvertical1 = isverticalfield1, isvertical2 = isverticalfield2, isvertical3 = isverticalfield3,
                        customverticalleft1 = bottomverticalleft1, customverticalright1 = bottomverticalright1, customverticalleft2 = bottomverticalleft2, customverticalright2 = bottomverticalright2,
                        customverticalleft3 = bottomverticalleft3, customverticalright3 = bottomverticalright3,
                        customverticalleft1zone = customverticalleft1zone, customverticalright1zone = customverticalright1zone, customverticalleft2zone = customverticalleft2zone, customverticalright2zone = customverticalright2zone,
                        customverticalleft3zone = customverticalleft3zone, customverticalright3zone = customverticalright3zone,ishorizontal1 = ishorizontalfield1, ishorizontal2 = ishorizontalfield2, ishorizontal3 = ishorizontalfield3,
                    )
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
            confirmButton = { Button(onClick = {
                savedDialogVisible = false
            }) { Text("OK") } },
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

    var bottomleft1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomright1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomleft2 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomright2 by remember { mutableStateOf(KarooAction.SLOPE) }
    var bottomleft3 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomright3 by remember { mutableStateOf(KarooAction.SPEED) }
    var customleft1zone by remember { mutableStateOf(false) }
    var customright1zone by remember { mutableStateOf(false) }
    var customleft2zone by remember { mutableStateOf(false) }
    var customright2zone by remember { mutableStateOf(false) }
    var customleft3zone by remember { mutableStateOf(false) }
    var customright3zone by remember { mutableStateOf(false) }
    var isverticalfield1 by remember { mutableStateOf(false) }
    var isverticalfield2 by remember { mutableStateOf(false) }
    var isverticalfield3 by remember { mutableStateOf(false) }
    var bottomverticalleft1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomverticalright1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomverticalleft2 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomverticalright2 by remember { mutableStateOf(KarooAction.SLOPE) }
    var bottomverticalleft3 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomverticalright3 by remember { mutableStateOf(KarooAction.SLOPE) }
    var customverticalleft1zone by remember { mutableStateOf(false) }
    var customverticalright1zone by remember { mutableStateOf(false) }
    var customverticalleft2zone by remember { mutableStateOf(false) }
    var customverticalright3zone by remember { mutableStateOf(false) }
    var customverticalleft3zone by remember { mutableStateOf(false) }
    var customverticalright2zone by remember { mutableStateOf(false) }
    var ishorizontalfield1 by remember { mutableStateOf(false) }
    var ishorizontalfield2 by remember { mutableStateOf(false) }
    var ishorizontalfield3 by remember { mutableStateOf(false) }



    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->

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

    LaunchedEffect(
        bottomleft1, bottomleft2, bottomright1, bottomright2, bottomleft3, bottomleft3,customleft3zone, customright3zone,
        bottomverticalleft1, bottomverticalleft2, bottomverticalright1, bottomverticalright2,
        customleft1zone, customright1zone, customleft2zone, customright2zone,
        customverticalleft1zone, customverticalright1zone, customverticalleft2zone, customverticalright2zone
    ) {
        val actions = listOf(
            bottomleft1 to { customleft1zone = false },
            bottomleft2 to { customleft2zone = false },
            bottomleft3 to { customleft3zone = false },
            bottomright1 to { customright1zone = false },
            bottomright2 to { customright2zone = false },
            bottomright3 to { customright3zone = false },
        )
        actions.forEach { (action, applyzone) ->
            if (action.zone == "none") applyzone()
        }

        val zones = listOf(
            customleft1zone to { isverticalfield1 = true },
            customright1zone to { isverticalfield1 = true },
            customleft2zone to { isverticalfield2 = true },
            customright2zone to { isverticalfield2 = true },
            customleft3zone to { isverticalfield3 = true },
            customright3zone to { isverticalfield3 = true },
        )
        zones.forEach { (zone, setVerticalField) ->
            if (zone) setVerticalField()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        Column(modifier = Modifier
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            TopAppBar(title = { Text("Horizontal Field 1") })

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomleft1) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomleft1.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Left",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomleft1 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customleft1zone, onCheckedChange = {
                    customleft1zone = it
                },enabled = bottomleft1.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }
            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomright1) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomright1.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Right",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomright1 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customright1zone, onCheckedChange = {
                    customright1zone = it
                },enabled = bottomright1.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isverticalfield1, onCheckedChange = {
                    isverticalfield1 = it
                }, enabled = !(customleft1zone || customright1zone))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Vertical Divider?")
            }
            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text("Horizontal Field 2") })

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomleft2) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomleft2.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Left",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomleft2 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customleft2zone, onCheckedChange = {
                    customleft2zone = it
                },enabled = bottomleft2.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomright2) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomright2.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Right",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomright2 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customright2zone, onCheckedChange = {
                    customright2zone = it
                },enabled = bottomright2.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isverticalfield2, onCheckedChange = {
                    isverticalfield2 = it
                }, enabled = !(customleft2zone || customright2zone))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Vertical Divider?")
            }

            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text("Horizontal Field 3") })

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomleft3) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomleft3.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Left",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomleft3 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customleft3zone, onCheckedChange = {
                    customleft3zone = it
                },enabled = bottomleft3.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }

            apply {
                val dropdownOptions = KarooAction.entries.toList()
                    .map { unit -> DropdownOption(unit.action.toString(), unit.label) }
                val dropdownInitialSelection by remember(bottomright3) {
                    mutableStateOf(dropdownOptions.find { option -> option.id == bottomright3.action.toString() }!!)
                }
                KarooKeyDropdown(
                    remotekey = "Right",
                    options = dropdownOptions,
                    selectedOption = dropdownInitialSelection
                ) { selectedOption ->
                    bottomright3 =
                        KarooAction.entries.find { unit -> unit.action == selectedOption.id }!!
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = customright3zone, onCheckedChange = {
                    customright3zone = it
                },enabled = bottomright3.zone != "none")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coloured zone?")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isverticalfield3, onCheckedChange = {
                    isverticalfield3 = it
                }, enabled = !(customleft3zone || customright3zone))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Vertical Divider?")
            }

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
                val newSettings =
                    CustomFieldSettings(
                        customleft1 = bottomleft1, customright1 = bottomright1, customleft2 = bottomleft2, customright2 = bottomright2,customleft3 = bottomleft3, customright3 = bottomright3,
                        customleft1zone = customleft1zone, customright1zone = customright1zone, customleft2zone = customleft2zone, customright2zone = customright2zone,customleft3zone = customleft3zone, customright3zone = customright3zone,
                        isvertical1 = isverticalfield1, isvertical2 = isverticalfield2, isvertical3 = isverticalfield3,
                        customverticalleft1 = bottomverticalleft1, customverticalright1 = bottomverticalright1, customverticalleft2 = bottomverticalleft2, customverticalright2 = bottomverticalright2,
                        customverticalleft3 = bottomverticalleft3, customverticalright3 = bottomverticalright3,
                        customverticalleft1zone = customverticalleft1zone, customverticalright1zone = customverticalright1zone, customverticalleft2zone = customverticalleft2zone, customverticalright2zone = customverticalright2zone,
                        customverticalleft3zone = customverticalleft3zone, customverticalright3zone = customverticalright3zone,ishorizontal1 = ishorizontalfield1, ishorizontal2 = ishorizontalfield2, ishorizontal3 = ishorizontalfield3,
                    )
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
            confirmButton = { Button(onClick = {
                savedDialogVisible = false
            }) { Text("OK") } },
            text = { Text("Settings saved successfully.") }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfGeneral() {

    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

   // val alignmentOptions = listOf(FieldPosition.LEFT, FieldPosition.CENTER, FieldPosition.RIGHT)
    val alignmentOptions = FieldPosition.entries
   // fun FieldPosition.Companion.toList(): List<FieldPosition> = listOf(FieldPosition.LEFT, FieldPosition.CENTER, FieldPosition.RIGHT)

    var ispalettezwift by remember { mutableStateOf(true) }
    var iscenteralign by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscentervertical by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterkaroo by remember { mutableStateOf(false) }

    var bottomleft1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomright1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomleft2 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomright2 by remember { mutableStateOf(KarooAction.SLOPE) }
    var bottomleft3 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomright3 by remember { mutableStateOf(KarooAction.SPEED) }
    var customleft1zone by remember { mutableStateOf(false) }
    var customright1zone by remember { mutableStateOf(false) }
    var customleft2zone by remember { mutableStateOf(false) }
    var customright2zone by remember { mutableStateOf(false) }
    var customleft3zone by remember { mutableStateOf(false) }
    var customright3zone by remember { mutableStateOf(false) }
    var isverticalfield1 by remember { mutableStateOf(false) }
    var isverticalfield2 by remember { mutableStateOf(false) }
    var isverticalfield3 by remember { mutableStateOf(false) }
    var bottomverticalleft1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomverticalright1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomverticalleft2 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomverticalright2 by remember { mutableStateOf(KarooAction.SLOPE) }
    var bottomverticalleft3 by remember { mutableStateOf(KarooAction.CADENCE) }
    var bottomverticalright3 by remember { mutableStateOf(KarooAction.SLOPE) }
    var customverticalleft1zone by remember { mutableStateOf(false) }
    var customverticalright1zone by remember { mutableStateOf(false) }
    var customverticalleft2zone by remember { mutableStateOf(false) }
    var customverticalright3zone by remember { mutableStateOf(false) }
    var customverticalleft3zone by remember { mutableStateOf(false) }
    var customverticalright2zone by remember { mutableStateOf(false) }
    var ishorizontalfield1 by remember { mutableStateOf(false) }
    var ishorizontalfield2 by remember { mutableStateOf(false) }
    var ishorizontalfield3 by remember { mutableStateOf(false) }



    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->

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

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        Column(modifier = Modifier
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            TopAppBar(title = { Text("Fields Alignment") })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = false, onCheckedChange = {
                    iscenterkaroo = it
                },enabled = false)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Use default Karoo Alignment ?")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Horizontal Field alignment (icon/text) ?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MultiToggleButton(alignmentOptions.indexOf(iscenteralign),alignmentOptions.map { it.name }, onToggleChange = { iscenteralign = alignmentOptions[it] })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vertical Fields alignment (icon/text) ?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MultiToggleButton(alignmentOptions.indexOf(iscentervertical),alignmentOptions.map { it.name }, onToggleChange = { iscentervertical = alignmentOptions[it] })
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


            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
                val newSettings =
                    CustomFieldSettings(
                        customleft1 = bottomleft1, customright1 = bottomright1, customleft2 = bottomleft2, customright2 = bottomright2,customleft3 = bottomleft3, customright3 = bottomright3,
                        customleft1zone = customleft1zone, customright1zone = customright1zone, customleft2zone = customleft2zone, customright2zone = customright2zone,customleft3zone = customleft3zone, customright3zone = customright3zone,
                        isvertical1 = isverticalfield1, isvertical2 = isverticalfield2, isvertical3 = isverticalfield3,
                        customverticalleft1 = bottomverticalleft1, customverticalright1 = bottomverticalright1, customverticalleft2 = bottomverticalleft2, customverticalright2 = bottomverticalright2,
                        customverticalleft3 = bottomverticalleft3, customverticalright3 = bottomverticalright3,
                        customverticalleft1zone = customverticalleft1zone, customverticalright1zone = customverticalright1zone, customverticalleft2zone = customverticalleft2zone, customverticalright2zone = customverticalright2zone,
                        customverticalleft3zone = customverticalleft3zone, customverticalright3zone = customverticalright3zone,ishorizontal1 = ishorizontalfield1, ishorizontal2 = ishorizontalfield2, ishorizontal3 = ishorizontalfield3,
                    )
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
            confirmButton = { Button(onClick = {
                savedDialogVisible = false
            }) { Text("OK") } },
            text = { Text("Settings saved successfully.") }
        )
    }
}

@Preview(name = "karoo", device = "spec:width=480px,height=800px,dpi=300")
@Composable
private fun PreviewTabLayout() {
    TabLayout(
    )
}
