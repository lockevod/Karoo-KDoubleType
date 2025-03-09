package com.enderthor.kCustomField.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.launch

import com.enderthor.kCustomField.datatype.*
import com.enderthor.kCustomField.extensions.*


val alignmentOptions = listOf(FieldPosition.LEFT, FieldPosition.CENTER, FieldPosition.RIGHT)
val timeOptions = defaultRollingTimes

@Composable
fun TabLayout() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fields","Rolling","Conf.")
    val ctx = LocalContext.current



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
                0 -> ConfFields(ctx, true)
                1 -> ConfRolling(ctx, true)
                2 -> ConfGeneral()
            }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfRolling(ctx: Context, iskaroo3: Boolean) {

    val coroutineScope = rememberCoroutineScope()


    var savedDialogVisible by remember { mutableStateOf(false) }
    var oneFieldSettingsList = remember { mutableStateListOf<OneFieldSettings> (OneFieldSettings(), OneFieldSettings()) }
    var generalSettings by remember { mutableStateOf(GeneralSettings()) }

    // Añadir stream de configuración general
    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            generalSettings = settings
        }
    }

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


    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .padding(5.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            oneFieldSettingsDerived.value.forEachIndexed { index, oneFieldSettings ->
                if (index >= 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Be careful to use several custom fields simultaneously (custom and rolling) in the same profile, Hammerhead extension are in early versions of Karoo and it may cause performance issues",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                TopAppBar(title = { Text("Rolling Field ${index + 1}") })
                DropdownOneField(
                    enabled = true,
                    firstpos = true,
                    label = "First Field",
                    action = oneFieldSettings.onefield,
                    isheadwindenabled = generalSettings.isheadwindenabled
                ) { newAction ->
                    val updatedZone =
                        if (newAction.kaction.zone == "none") false else oneFieldSettings.onefield.iszone
                    val updatednewAction = newAction.copy(iszone = updatedZone)
                    oneFieldSettingsList[index] =
                        oneFieldSettings.copy(onefield = updatednewAction)
                }
                ZoneMultiSwitch(
                    0,
                    oneFieldSettings.onefield.iszone,
                    oneFieldSettings.onefield.kaction.zone != "none"
                ) { newZone ->
                    oneFieldSettingsList[index].onefield =
                        oneFieldSettings.onefield.copy(iszone = newZone)
                }
                DropdownOneField(
                    enabled = true,
                    firstpos = false,
                    label = "Second Field",
                    action = oneFieldSettings.secondfield,
                    isheadwindenabled = generalSettings.isheadwindenabled
                ) { newAction ->

                    val updatedZone =
                        if (newAction.kaction.zone == "none") false else oneFieldSettings.secondfield.iszone
                    val updatednewAction = newAction.copy(iszone = updatedZone)
                    // Timber.d("NEW ACTION SECONDFIELD $updatednewAction")
                    oneFieldSettingsList[index] =
                        oneFieldSettings.copy(secondfield = updatednewAction)
                    if (!newAction.isactive) {
                        val noneThirdField = oneFieldSettings.thirdfield.copy(
                            isactive = false,
                            kaction = newAction.kaction
                        )
                        oneFieldSettingsList[index] = oneFieldSettings.copy(
                            secondfield = updatednewAction,
                            thirdfield = noneThirdField
                        )
                    } else {
                        oneFieldSettingsList[index] = oneFieldSettings.copy(secondfield = updatednewAction)
                    }
                }
                ZoneMultiSwitch(
                    0,
                    oneFieldSettings.secondfield.iszone,
                    oneFieldSettings.secondfield.kaction.zone != "none"
                ) { newZone ->
                    oneFieldSettingsList[index].secondfield =
                        oneFieldSettings.secondfield.copy(iszone = newZone)
                }
                DropdownOneField(
                    firstpos = false,
                    label = "Third Field",
                    action = oneFieldSettings.thirdfield,
                    enabled = oneFieldSettings.secondfield.isactive,
                    isheadwindenabled = generalSettings.isheadwindenabled
                ) { newAction ->
                    val updatedZone =
                        if (newAction.kaction.zone == "none") false else oneFieldSettings.thirdfield.iszone
                    val updatednewAction = newAction.copy(iszone = updatedZone)
                    oneFieldSettingsList[index] =
                        oneFieldSettings.copy(thirdfield = updatednewAction)
                }
                ZoneMultiSwitch(
                    0,
                    oneFieldSettings.thirdfield.iszone,
                    oneFieldSettings.thirdfield.kaction.zone != "none"
                ) { newZone ->
                    oneFieldSettingsList[index].thirdfield =
                        oneFieldSettings.thirdfield.copy(iszone = newZone)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rolling Time?")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MultiToggleButton(
                        enabled = oneFieldSettings.secondfield.isactive,
                        currentSelection = if (oneFieldSettings.secondfield.isactive) timeOptions.indexOf(
                            oneFieldSettings.rollingtime
                        ) else 0,
                        toggleStates = timeOptions.map { it.name },
                        onToggleChange = {
                            oneFieldSettingsList[index] =
                                oneFieldSettingsList[index].copy(rollingtime = timeOptions[it])
                        })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = oneFieldSettings.isextratime, onCheckedChange = {
                        oneFieldSettingsList[index] = oneFieldSettingsList[index].copy(isextratime = it)
                    })
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Enable Extra Time for First Field (x3 time)?")
                }


                if (!oneFieldSettings.secondfield.isactive) oneFieldSettingsList[index] =
                    oneFieldSettingsList[index].copy(rollingtime = RollingTime("ZERO", "0", 0L))



            }


            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
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
fun ConfFields(ctx: Context,iskaroo3: Boolean) {

    val coroutineScope = rememberCoroutineScope()

    var savedDialogVisible by remember { mutableStateOf(false) }

    var doubleFieldSettingsList = remember { mutableStateListOf<DoubleFieldSettings> (DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings(),DoubleFieldSettings(), DoubleFieldSettings()) }

    var generalSettings by remember { mutableStateOf(GeneralSettings()) }

    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            generalSettings = settings
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

    //imber.d("List size ${doubleFieldSettingsDerived.value.size} and iskaroo3 $iskaroo3")

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .padding(5.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            doubleFieldSettingsDerived.value.forEachIndexed { index, doubleFieldSettings ->
                if (index < 3 || (iskaroo3 && index in 3..5) ) {
                    if(index>4) {
                     Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Be careful to use more than 4 custom fields simultaneously in the same profile, Hammerhead extension are in early versions of Karoo and it may cause performance issues",
                            fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    TopAppBar(title = { Text("Field ${index + 1}") })

                    DropdownDoubleField(
                        "First Field",
                        doubleFieldSettings.onefield,
                        generalSettings.isheadwindenabled
                    ) { newAction ->
                        val updatedZone =
                            if (newAction.kaction.zone == "none") false else doubleFieldSettings.onefield.iszone
                        val updatednewAction = newAction.copy(iszone = updatedZone)
                        doubleFieldSettingsList[index] =
                            doubleFieldSettings.copy(onefield = updatednewAction)
                    }
                    ZoneMultiSwitch(
                        0,
                        doubleFieldSettings.onefield.iszone,
                        doubleFieldSettings.onefield.kaction.zone != "none"
                    ) { newZone ->
                        val updatedZone =
                            if (doubleFieldSettings.onefield.kaction.zone == "none") false else newZone
                        doubleFieldSettingsList[index] = doubleFieldSettings.copy(
                            onefield = doubleFieldSettings.onefield.copy(iszone = updatedZone)
                        )
                    }

                    DropdownDoubleField(
                        "Second Field",
                        doubleFieldSettings.secondfield,
                        generalSettings.isheadwindenabled
                    ) { newAction ->
                        val updatedZone =
                            if (newAction.kaction.zone == "none") false else doubleFieldSettings.secondfield.iszone
                        val updatednewAction = newAction.copy(iszone = updatedZone)
                        doubleFieldSettingsList[index] =
                            doubleFieldSettings.copy(secondfield = updatednewAction)
                    }
                    ZoneMultiSwitch(
                        0,
                        doubleFieldSettings.secondfield.iszone,
                        doubleFieldSettings.secondfield.kaction.zone != "none"
                    ) { newZone ->
                        val updatedZone =
                            if (doubleFieldSettings.secondfield.kaction.zone == "none") false else newZone
                        doubleFieldSettingsList[index] = doubleFieldSettings.copy(
                            secondfield = doubleFieldSettings.secondfield.copy(iszone = updatedZone)
                        )
                    }
                    ZoneMultiSwitch(1, doubleFieldSettings.ishorizontal, true) { newHorizontal ->
                        doubleFieldSettingsList[index] =
                            doubleFieldSettings.copy(ishorizontal = newHorizontal)
                    }
                }
            }

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
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
    var iscenterkaroo by remember { mutableStateOf(false) }
    var isheadwindenabled by remember { mutableStateOf(false) }
    var isdivider by remember { mutableStateOf(true) }

    var powerLoss by remember { mutableStateOf("2.2") }
    var rollingResistanceCoefficient by remember { mutableStateOf("0.0095") }
    var bikeMass: String by remember { mutableStateOf("14.0") }

    var savedDialogVisible by remember { mutableStateOf(false) }



    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            ispalettezwift = settings.ispalettezwift
            iscenteralign = settings.iscenteralign
            iscentervertical = settings.iscentervertical
            iscenterkaroo = settings.iscenterkaroo
            isheadwindenabled = settings.isheadwindenabled
            isdivider = settings.isdivider
        }
        ctx.streamStoredPowerSettings().collect { settings ->
            powerLoss = settings.powerLoss
            rollingResistanceCoefficient = settings.rollingResistanceCoefficient
            bikeMass = settings.bikeMass
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
                        enabled=true,
                        currentSelection=alignmentOptions.indexOf(iscenteralign),
                        toggleStates=alignmentOptions.map { it.name },
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
                        enabled=true,
                        currentSelection=alignmentOptions.indexOf(iscentervertical),
                        toggleStates=alignmentOptions.map { it.name },
                        onToggleChange = { iscentervertical = alignmentOptions[it] }
                    )
                }
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
            TopAppBar(title = { Text("Color Palette") })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ispalettezwift, onCheckedChange = {
                    ispalettezwift = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text("Zwift Color palette?")
            }

            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text("Power Settings") })


            OutlinedTextField(value = bikeMass, modifier = Modifier.fillMaxWidth(),
                onValueChange = { bikeMass = it },
                label = { Text("Bike Mass") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(value = rollingResistanceCoefficient, modifier = Modifier.fillMaxWidth(),
                onValueChange = { rollingResistanceCoefficient = it },
                label = { Text("Crr") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(value = powerLoss, modifier = Modifier.fillMaxWidth(),
                onValueChange = { powerLoss = it },
                label = { Text("Power Loss") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )


            Spacer(modifier = Modifier.height(16.dp))


            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), onClick = {
                val newGeneralSettings = GeneralSettings(
                    ispalettezwift = ispalettezwift,
                    iscenteralign = iscenteralign,
                    iscentervertical = iscentervertical,
                    iscenterkaroo = iscenterkaroo,
                    isheadwindenabled = isheadwindenabled,
                    isdivider = isdivider

                )

                val newPowerSettings = powerSettings(
                    powerLoss = powerLoss,
                    rollingResistanceCoefficient = rollingResistanceCoefficient,
                    bikeMass = bikeMass
                )
                coroutineScope.launch {
                    savedDialogVisible = true
                    saveGeneralSettings(ctx, newGeneralSettings)
                    savePowerSettings(ctx, newPowerSettings)
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