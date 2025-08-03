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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.launch

import com.enderthor.kCustomField.datatype.*
import com.enderthor.kCustomField.extensions.*
import com.enderthor.kCustomField.R


val alignmentOptions = listOf(FieldPosition.LEFT, FieldPosition.CENTER, FieldPosition.RIGHT)
val bellOptions = listOf(KarooKey.BELL4, KarooKey.BELL5)
val timeOptions = defaultRollingTimes

@Composable
fun TabLayout() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_field),
        stringResource(R.string.tab_rolling),
        stringResource(R.string.tab_smart),
        stringResource(R.string.tab_wbal),
        stringResource(R.string.tab_config)
    )
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
                    text = { Text(text = title, fontSize = 10.sp) },

                )
            }
        }



            when (selectedTabIndex) {
                0 -> ConfFields(ctx)
                1 -> ConfRolling(ctx)
                2 -> ConfSmart(ctx)
                3 -> ConfWBal(ctx)
                4 -> ConfGeneral()

            }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfRolling(ctx: Context) {

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
                        stringResource(R.string.rolling_warning),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                TopAppBar(title = { Text(stringResource(R.string.rolling_field_title, index + 1)) })
                DropdownOneField(
                    enabled = true,
                    firstpos = true,
                    label = stringResource(R.string.first_field),
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
                    label = stringResource(R.string.second_field),
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
                    label = stringResource(R.string.third_field),
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
                    Text(stringResource(R.string.rolling_time_question))
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
                    Text(stringResource(R.string.enable_extra_time))
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
                Icon(Icons.Default.Done, contentDescription = stringResource(R.string.save_rolling_desc))
                Spacer(modifier = Modifier.width(5.dp))
                Text(stringResource(R.string.save_rolling))
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    if (savedDialogVisible) {
        AlertDialog(onDismissRequest = { savedDialogVisible = false },
            confirmButton = { Button(onClick = { savedDialogVisible = false }) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(R.string.settings_saved)) }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfFields(ctx: Context) {

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

    //imber.d("List size ${doubleFieldSettingsDerived.value.size} and iskaroo3 $iskaroo")

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
                if (index < 3 || (index in 3..5) ) {
                    if(index>4) {
                     Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.custom_warning),
                            fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    TopAppBar(title = { Text(stringResource(R.string.custom_field_title, index + 1)) })

                    DropdownDoubleField(
                        stringResource(R.string.first_field),
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
                        stringResource(R.string.second_field),
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
                Icon(Icons.Default.Done, contentDescription = stringResource(R.string.save_custom_desc))
                Spacer(modifier = Modifier.width(5.dp))
                Text(stringResource(R.string.save_custom))
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    if (savedDialogVisible) {
        AlertDialog(onDismissRequest = { savedDialogVisible = false },
            confirmButton = { Button(onClick = { savedDialogVisible = false }) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(R.string.settings_saved)) }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfSmart(ctx: Context) {
    val coroutineScope = rememberCoroutineScope()
    var savedDialogVisible by remember { mutableStateOf(false) }
    var climbFieldSettingsList = remember { mutableStateListOf<ClimbFieldSettings>(ClimbFieldSettings()) }
    var generalSettings by remember { mutableStateOf(GeneralSettings()) }

    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            generalSettings = settings
        }
    }


    LaunchedEffect(Unit) {
        ctx.streamClimbFieldSettings().collect { settings ->
            if (settings.isNotEmpty()) {
                climbFieldSettingsList.clear()
                climbFieldSettingsList.addAll(settings)
            }
        }
    }

    val climbFieldSettingsDerived = remember {
        derivedStateOf { climbFieldSettingsList.toList() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .padding(5.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            climbFieldSettingsDerived.value.forEachIndexed { index, climbFieldSettings ->
                TopAppBar(title = { Text(stringResource(R.string.climb_field_title)) })

                DropdownDoubleField(
                    stringResource(R.string.first_field),
                    climbFieldSettings.onefield,
                    generalSettings.isheadwindenabled
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else climbFieldSettings.onefield.iszone
                    val updatedNewAction = newAction.copy(iszone = updatedZone)
                    climbFieldSettingsList[index] = climbFieldSettings.copy(onefield = updatedNewAction)
                }
                ZoneMultiSwitch(
                    0,
                    climbFieldSettings.onefield.iszone,
                    climbFieldSettings.onefield.kaction.zone != "none"
                ) { newZone ->
                    val updatedZone = if (climbFieldSettings.onefield.kaction.zone == "none") false else newZone
                    climbFieldSettingsList[index] = climbFieldSettings.copy(
                        onefield = climbFieldSettings.onefield.copy(iszone = updatedZone)
                    )
                }

                DropdownDoubleField(
                    stringResource(R.string.second_field),
                    climbFieldSettings.secondfield,
                    generalSettings.isheadwindenabled
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else climbFieldSettings.secondfield.iszone
                    val updatedNewAction = newAction.copy(iszone = updatedZone)
                    climbFieldSettingsList[index] = climbFieldSettings.copy(secondfield = updatedNewAction)
                }
                ZoneMultiSwitch(
                    0,
                    climbFieldSettings.secondfield.iszone,
                    climbFieldSettings.secondfield.kaction.zone != "none"
                ) { newZone ->
                    val updatedZone = if (climbFieldSettings.secondfield.kaction.zone == "none") false else newZone
                    climbFieldSettingsList[index] = climbFieldSettings.copy(
                        secondfield = climbFieldSettings.secondfield.copy(iszone = updatedZone)
                    )
                }

                ZoneMultiSwitch(1, climbFieldSettings.isfirsthorizontal, true) { newHorizontal ->
                    climbFieldSettingsList[index] = climbFieldSettings.copy(isfirsthorizontal = newHorizontal)
                }

                DropdownDoubleField(
                    stringResource(R.string.third_field),
                    climbFieldSettings.thirdfield,
                    generalSettings.isheadwindenabled
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else climbFieldSettings.thirdfield.iszone
                    val updatedNewAction = newAction.copy(iszone = updatedZone)
                    climbFieldSettingsList[index] = climbFieldSettings.copy(thirdfield = updatedNewAction)
                }
                ZoneMultiSwitch(
                    0,
                    climbFieldSettings.thirdfield.iszone,
                    climbFieldSettings.thirdfield.kaction.zone != "none"
                ) { newZone ->
                    val updatedZone = if (climbFieldSettings.thirdfield.kaction.zone == "none") false else newZone
                    climbFieldSettingsList[index] = climbFieldSettings.copy(
                        thirdfield = climbFieldSettings.thirdfield.copy(iszone = updatedZone)
                    )
                }

                DropdownDoubleField(
                    stringResource(R.string.fourth_field),
                    climbFieldSettings.fourthfield,
                    generalSettings.isheadwindenabled
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else climbFieldSettings.fourthfield.iszone
                    val updatedNewAction = newAction.copy(iszone = updatedZone)
                    climbFieldSettingsList[index] = climbFieldSettings.copy(fourthfield = updatedNewAction)
                }
                ZoneMultiSwitch(
                    0,
                    climbFieldSettings.fourthfield.iszone,
                    climbFieldSettings.fourthfield.kaction.zone != "none"
                ) { newZone ->
                    val updatedZone = if (climbFieldSettings.fourthfield.kaction.zone == "none") false else newZone
                    climbFieldSettingsList[index] = climbFieldSettings.copy(
                        fourthfield = climbFieldSettings.fourthfield.copy(iszone = updatedZone)
                    )
                }

                ZoneMultiSwitch(1, climbFieldSettings.issecondhorizontal, true) { newHorizontal ->
                    climbFieldSettingsList[index] = climbFieldSettings.copy(issecondhorizontal = newHorizontal)
                }

                TopAppBar(title = { Text(stringResource(R.string.climb_field_conf), fontSize = 12.sp) }, windowInsets = WindowInsets(0.dp) )

                DropdownDoubleField(
                    stringResource(R.string.on_climber_measure),
                    climbFieldSettings.climbOnfield,
                    generalSettings.isheadwindenabled
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else climbFieldSettings.climbOnfield.iszone
                    val updatedNewAction = newAction.copy(iszone = updatedZone)
                    climbFieldSettingsList[index] = climbFieldSettings.copy(climbOnfield = updatedNewAction)
                }
                ZoneMultiSwitch(
                    0,
                    climbFieldSettings.climbOnfield.iszone,
                    climbFieldSettings.climbOnfield.kaction.zone != "none"
                ) { newZone ->
                    val updatedZone = if (climbFieldSettings.climbOnfield.kaction.zone == "none") false else newZone
                    climbFieldSettingsList[index] = climbFieldSettings.copy(
                        climbOnfield = climbFieldSettings.climbOnfield.copy(iszone = updatedZone)
                    )
                }
                DropdownDoubleField(
                    stringResource(R.string.no_climber_measure),
                    climbFieldSettings.climbfield,
                    generalSettings.isheadwindenabled
                ) { newAction ->
                    val updatedZone = if (newAction.kaction.zone == "none") false else climbFieldSettings.climbfield.iszone
                    val updatedNewAction = newAction.copy(iszone = updatedZone)
                    climbFieldSettingsList[index] = climbFieldSettings.copy(climbfield = updatedNewAction)
                }
                ZoneMultiSwitch(
                    0,
                    climbFieldSettings.climbfield.iszone,
                    climbFieldSettings.climbfield.kaction.zone != "none"
                ) { newZone ->
                    val updatedZone = if (climbFieldSettings.climbfield.kaction.zone == "none") false else newZone
                    climbFieldSettingsList[index] = climbFieldSettings.copy(
                        climbfield = climbFieldSettings.climbfield.copy(iszone = updatedZone)
                    )
                }
                ZoneMultiSwitch(3, climbFieldSettings.isAlwaysClimbPos, true) { newValue ->
                    climbFieldSettingsList[index] = climbFieldSettings.copy(isAlwaysClimbPos = newValue)
                }
            }

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onClick = {
                    coroutineScope.launch {
                        savedDialogVisible = true
                        saveClimbFieldSettings(ctx, climbFieldSettingsList)
                    }
                }
            ) {
                Icon(Icons.Default.Done, contentDescription = stringResource(R.string.save_smart_desc))
                Spacer(modifier = Modifier.width(5.dp))
                Text(stringResource(R.string.save_smart))
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    if (savedDialogVisible) {
        AlertDialog(
            onDismissRequest = { savedDialogVisible = false },
            confirmButton = { Button(onClick = { savedDialogVisible = false }) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(R.string.settings_saved)) }
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
    var bellsong by remember {mutableStateOf(KarooKey.BELL4) }

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
            bellsong = settings.bellBeepKey
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

            TopAppBar(title = { Text(stringResource(R.string.fields_alignment_title)) })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = iscenterkaroo, onCheckedChange = {
                    iscenterkaroo = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.use_default_karoo_alignment))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.horizontal_alignment_question))
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
                Text(stringResource(R.string.vertical_alignment_question))
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
            TopAppBar(title = { Text(stringResource(R.string.use_headwind_datafield_title)) })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isheadwindenabled, onCheckedChange = {
                    isheadwindenabled = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.enable_headwind_question))
            }

            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text(stringResource(R.string.color_palette_title)) })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ispalettezwift, onCheckedChange = {
                    ispalettezwift = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.zwift_color_palette_question))
            }

            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text(stringResource(R.string.divider_enabled_title)) })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isdivider, onCheckedChange = {
                    isdivider= it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.divider_enabled_question))
            }

            Spacer(modifier = Modifier.height(2.dp))
            TopAppBar(title = { Text(stringResource(R.string.bell_sound_title)) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .alpha(1f)
                        .clickable(enabled=true) {}
                ) {
                    MultiToggleButton(
                        enabled=true,
                        currentSelection=bellOptions.indexOf(bellsong),
                        toggleStates=bellOptions.map { it.label },
                        onToggleChange = { bellsong = bellOptions[it] }
                    )
                }
            }

           /* Spacer(modifier = Modifier.height(2.dp))


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

*/
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
                    isdivider = isdivider,
                    bellBeepKey = bellsong

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
                Icon(Icons.Default.Done, contentDescription = stringResource(R.string.save_general_desc))
                Spacer(modifier = Modifier.width(5.dp))
                Text(stringResource(R.string.save_general))
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    if (savedDialogVisible) {
        AlertDialog(onDismissRequest = { savedDialogVisible = false },
            confirmButton = { Button(onClick = { savedDialogVisible = false }) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(R.string.settings_saved)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfWBal(ctx: Context) {
    val coroutineScope = rememberCoroutineScope()
    var savedDialogVisible by remember { mutableStateOf(false) }

    // Variables para los parámetros de W' Balance Prime
    var criticalPower by remember { mutableStateOf("250.0") }
    var wPrime by remember { mutableStateOf("20000.0") }
    // tauWPlus y tauWMinus ahora usan valores predeterminados de WPrimeBalanceSettings
    var useUserFTPAsCP by remember { mutableStateOf(true) }
    var useVisualZones by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        ctx.streamWPrimeBalanceSettings().collect { settings ->
            criticalPower = settings.criticalPower
            wPrime = settings.wPrime
            // tauWPlus y tauWMinus se omiten ya que usamos valores predeterminados
            useUserFTPAsCP = settings.useUserFTPAsCP
            useVisualZones = settings.useVisualZones
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .padding(5.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TopAppBar(title = { Text(stringResource(R.string.wbal_settings_title)) })

            Text(
                stringResource(R.string.wbal_description),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Switch para usar FTP del usuario como CP
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = useUserFTPAsCP, onCheckedChange = {
                    useUserFTPAsCP = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.use_user_ftp_as_cp))
            }

            // Campo para Critical Power (solo habilitado si no usa FTP del usuario)
            OutlinedTextField(
                value = criticalPower,
                modifier = Modifier.fillMaxWidth(),
                enabled = !useUserFTPAsCP,
                onValueChange = { criticalPower = it },
                label = { Text(stringResource(R.string.critical_power_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text(stringResource(R.string.critical_power_hint)) }
            )

            // Campo para W'
            OutlinedTextField(
                value = wPrime,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { wPrime = it },
                label = { Text(stringResource(R.string.wprime_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text(stringResource(R.string.wprime_hint)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TopAppBar(title = { Text(stringResource(R.string.visual_zones_title)) })

            // Switch para usar zonas visuales de colores
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = useVisualZones, onCheckedChange = {
                    useVisualZones = it
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.use_visual_zones))
            }

            Text(
                stringResource(R.string.visual_zones_description),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            TopAppBar(title = { Text(stringResource(R.string.temporal_constants_title)) })

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.wbal_note),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
                onClick = {
                    val newWPrimeBalanceSettings = WPrimeBalanceSettings(
                        criticalPower = criticalPower,
                        wPrime = wPrime,
                        useUserFTPAsCP = useUserFTPAsCP,
                        useVisualZones = useVisualZones
                    )
                    coroutineScope.launch {
                        savedDialogVisible = true
                        saveWPrimeBalanceSettings(ctx, newWPrimeBalanceSettings)
                    }
                }) {
                Icon(Icons.Default.Done, contentDescription = stringResource(R.string.save_wbal_desc))
                Spacer(modifier = Modifier.width(5.dp))
                Text(stringResource(R.string.save_wbal))
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }

    if (savedDialogVisible) {
        AlertDialog(
            onDismissRequest = { savedDialogVisible = false },
            confirmButton = { Button(onClick = { savedDialogVisible = false }) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(R.string.wbal_settings_saved)) }
        )
    }
}
