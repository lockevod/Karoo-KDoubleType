package com.enderthor.kCustomField.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enderthor.kCustomField.datatype.*
import com.enderthor.kCustomField.extensions.*
import kotlinx.coroutines.launch
import timber.log.Timber


val alignmentOptions = listOf(FieldPosition.LEFT, FieldPosition.CENTER, FieldPosition.RIGHT)
val timeOptions = listOf(RollingTime.ZERO, RollingTime.FOUR, RollingTime.TEN, RollingTime.TWENTY)

@Composable
fun TabLayout() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fields","Rolling","Conf.")


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
            0 -> ConfFields()
            1 -> ConfRolling()
            2 -> ConfGeneral()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfRolling() {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var doubleFieldSettingsList = remember { mutableStateListOf<DoubleFieldSettings> (DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings(),DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings()) }


    LaunchedEffect(Unit) {
        ctx.streamDoubleFieldSettings().collect { settings ->
            if (settings.isNotEmpty()) {
                doubleFieldSettingsList.clear()
                doubleFieldSettingsList.addAll(settings)
            }
        }
    }

    var savedDialogVisible by remember { mutableStateOf(false) }
    var oneFieldSettingsList = remember { mutableStateListOf<OneFieldSettings> (OneFieldSettings(), OneFieldSettings(), OneFieldSettings()) }

    LaunchedEffect(Unit) {
        ctx.streamOneFieldSettings().collect { settings ->
            if (settings.isNotEmpty()) {
                oneFieldSettingsList.clear()
                oneFieldSettingsList.addAll(settings)
            }
        }
    }

    val oneFieldSettingsDerived = remember {
        derivedStateOf { oneFieldSettingsList.toList() }
    }


    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.padding(5.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            oneFieldSettingsDerived.value.forEachIndexed { index, oneFieldSettings ->
                //if(index==0) {
                    TopAppBar(title = { Text("Rolling Field ${index + 1}") })
                DropdownOneField(true,
                    "First Field",
                    oneFieldSettings.onefield
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else oneFieldSettings.onefield.iszone
                    val updatednewAction = newAction.copy(iszone = updatedZone)
                    oneFieldSettingsList[index] = oneFieldSettings.copy(onefield = updatednewAction)
                }
                ZoneMultiSwitch(0,oneFieldSettings.onefield.iszone, oneFieldSettings.onefield.kaction.zone != "none") { newZone -> oneFieldSettingsList[index].onefield= oneFieldSettings.onefield.copy(iszone = newZone) }
                DropdownOneField(false,
                    "Second Field",
                    oneFieldSettings.secondfield
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else oneFieldSettings.secondfield.iszone
                    val updatednewAction = newAction.copy(iszone = updatedZone)
                    oneFieldSettingsList[index] = oneFieldSettings.copy(secondfield = updatednewAction)
                }
                ZoneMultiSwitch(0,oneFieldSettings.secondfield.iszone, oneFieldSettings.secondfield.kaction.zone != "none") { newZone -> oneFieldSettingsList[index].secondfield= oneFieldSettings.secondfield.copy(iszone = newZone) }
                DropdownOneField(false,
                    "Third Field",
                    oneFieldSettings.thirdfield
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else oneFieldSettings.thirdfield.iszone
                    val updatednewAction = newAction.copy(iszone = updatedZone)
                    oneFieldSettingsList[index] = oneFieldSettings.copy(thirdfield = updatednewAction)
                }
                ZoneMultiSwitch(0,oneFieldSettings.thirdfield.iszone, oneFieldSettings.thirdfield.kaction.zone != "none") { newZone -> oneFieldSettingsList[index].thirdfield= oneFieldSettings.thirdfield.copy(iszone = newZone) }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rolling Time (0 no rolling)?")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MultiToggleButton(
                        timeOptions.indexOf(oneFieldSettings.rollingtime),
                        timeOptions.map { it.name },
                        onToggleChange = {
                            oneFieldSettingsList[index] = oneFieldSettingsList[index].copy(rollingtime = timeOptions[it])
                        })
                }
           // }
            }

            FilledTonalButton(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {

                coroutineScope.launch {
                    savedDialogVisible = true
                    saveOneFieldSettings(ctx, oneFieldSettingsList)
                }
            }) {
                Icon(Icons.Default.Done, contentDescription = "Save Rolling")
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save Rolling")
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
fun ConfFields() {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var savedDialogVisible by remember { mutableStateOf(false) }
    var isheadwindenabled by remember { mutableStateOf(false) }

    var doubleFieldSettingsList = remember { mutableStateListOf<DoubleFieldSettings> (DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings(),DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings()) }
    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            isheadwindenabled = settings.isheadwindenabled
        }
    }

    LaunchedEffect(Unit) {
        ctx.streamDoubleFieldSettings().collect { settings ->
            if (settings.isNotEmpty()) {
                doubleFieldSettingsList.clear()
                doubleFieldSettingsList.addAll(settings)
            }
        }
    }

    val doubleFieldSettingsDerived = remember {
        derivedStateOf { doubleFieldSettingsList.toList() }
    }


    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.padding(5.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            doubleFieldSettingsDerived.value.forEachIndexed { index, doubleFieldSettings ->
                TopAppBar(title = { Text("Field ${index + 1}") })

                DropdownDoubleField(
                    "First Field",
                    doubleFieldSettings.onefield,
                    isheadwindenabled
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else doubleFieldSettings.onefield.iszone
                    val updatednewAction = newAction.copy(iszone = updatedZone)
                    doubleFieldSettingsList[index] = doubleFieldSettings.copy(onefield = updatednewAction)
                }
                ZoneMultiSwitch(0,doubleFieldSettings.onefield.iszone, doubleFieldSettings.onefield.kaction.zone != "none") { newZone ->
                    val updatedZone = if (doubleFieldSettings.onefield.kaction.zone == "none") false else newZone
                    doubleFieldSettingsList[index] = doubleFieldSettings.copy(onefield = doubleFieldSettings.onefield.copy(iszone = updatedZone))
                }

                DropdownDoubleField(
                    "Second Field",
                    doubleFieldSettings.secondfield,
                    isheadwindenabled
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else doubleFieldSettings.secondfield.iszone
                    val updatednewAction = newAction.copy(iszone = updatedZone)
                    doubleFieldSettingsList[index] = doubleFieldSettings.copy(secondfield = updatednewAction)
                }
                ZoneMultiSwitch(0,doubleFieldSettings.secondfield.iszone, doubleFieldSettings.secondfield.kaction.zone != "none") { newZone ->
                    val updatedZone = if (doubleFieldSettings.secondfield.kaction.zone == "none") false else newZone
                    doubleFieldSettingsList[index] = doubleFieldSettings.copy(secondfield = doubleFieldSettings.secondfield.copy(iszone = updatedZone))
                }
                ZoneMultiSwitch(1,doubleFieldSettings.ishorizontal, true) { newHorizontal ->
                    doubleFieldSettingsList[index] = doubleFieldSettings.copy(ishorizontal = newHorizontal)
                }

            }

            FilledTonalButton(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                coroutineScope.launch {
                    savedDialogVisible = true
                    saveDoubleFieldSettings(ctx, doubleFieldSettingsList)
                }
            }) {
                Icon(Icons.Default.Done, contentDescription = "Save")
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save Custom")
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
    var iscenterrolling by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterkaroo by remember { mutableStateOf(false) }
    var isheadwindenabled by remember { mutableStateOf(false) }

    var savedDialogVisible by remember { mutableStateOf(false) }



    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            ispalettezwift = settings.ispalettezwift
            iscenteralign = settings.iscenteralign
            iscentervertical = settings.iscentervertical
            iscenterkaroo = settings.iscenterkaroo
            isheadwindenabled = settings.isheadwindenabled
            iscenterrolling= settings.iscenterrolling
        }
    }


    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(5.dp).verticalScroll(rememberScrollState()).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            TopAppBar(title = { Text("Fields Alignment") })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = iscenterkaroo, onCheckedChange = {
                    iscenterkaroo = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text("Use default Karoo Alignment ?")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Horizontal/Rolling Fields alignment (icon/text) ?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .alpha(if (iscenterkaroo) 0.5f else 1f)
                        .clickable(enabled = !iscenterkaroo) {}
                ) {
                    MultiToggleButton(
                        alignmentOptions.indexOf(iscenteralign),
                        alignmentOptions.map { it.name },
                        onToggleChange = { iscenteralign = alignmentOptions[it] })

                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vertical Fields alignment (icon/text) ?")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .alpha(if (iscenterkaroo) 0.5f else 1f)
                        .clickable(enabled = !iscenterkaroo) {}
                ) {
                    MultiToggleButton(
                        alignmentOptions.indexOf(iscentervertical),
                        alignmentOptions.map { it.name },
                        onToggleChange = { iscentervertical = alignmentOptions[it] }
                    )
                }
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

            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text("Use Headwind DataField") })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isheadwindenabled, onCheckedChange = {
                    isheadwindenabled = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text("Enable Headwind Datafield (you need to have Headwind extension installed)?")
            }

            FilledTonalButton(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                val newGeneralSettings = GeneralSettings(
                    ispalettezwift = ispalettezwift,
                    iscenteralign = iscenteralign,
                    iscentervertical = iscentervertical,
                    iscenterrolling = iscenteralign,
                    iscenterkaroo = iscenterkaroo,
                    isheadwindenabled = isheadwindenabled

                )
                coroutineScope.launch {
                    savedDialogVisible = true
                    saveGeneralSettings(ctx, newGeneralSettings)
                }
            }) {
                Icon(Icons.Default.Done, contentDescription = "Save")
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save General")
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