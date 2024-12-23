package com.enderthor.kCustomField.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.enderthor.kCustomField.datatype.CustomFieldSettings
import com.enderthor.kCustomField.datatype.KarooAction

import com.enderthor.kCustomField.extensions.saveSettings
import com.enderthor.kCustomField.extensions.streamSettings


import kotlinx.coroutines.launch



@Composable
fun TabLayout(
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf( "Conf Custom Field")

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
            0 -> Config()
            //0 -> Help()
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class,)
@Composable
fun Config() {

    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var bottomleft1 by remember { mutableStateOf(KarooAction.HR) }
    var bottomright1 by remember { mutableStateOf(KarooAction.SPEED) }
    var bottomleft2 by remember { mutableStateOf(KarooAction.HR) }
    var bottomright2 by remember { mutableStateOf(KarooAction.SPEED) }


    var savedDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctx.streamSettings().collect { settings ->

            bottomright1 = settings.customright1
            bottomleft1 = settings.customleft1
            bottomright2 = settings.customright2
            bottomleft2 = settings.customleft2
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        Column(modifier = Modifier
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            TopAppBar(title = { Text("Custom Field 1") })
            Spacer(modifier = Modifier.height(8.dp))

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

            TopAppBar(title = { Text("Custom Field 2") })
            Spacer(modifier = Modifier.height(8.dp))

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



            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
                val newSettings =
                    CustomFieldSettings(
                        customleft1 = bottomleft1, customright1 = bottomright1, customleft2 = bottomleft2, customright2 = bottomright2
                    )

                coroutineScope.launch {
                    savedDialogVisible = true
                    saveSettings(ctx, newSettings)
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
