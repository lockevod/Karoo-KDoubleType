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
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun ConfRolling(sharedViewModel: HeadwindSharedViewModel = viewModel()) {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var ispalettezwift by remember { mutableStateOf(true) }
    var iscenteralign by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscentervertical by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterrolling by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterkaroo by remember { mutableStateOf(false) }



    var isheadwindenabled by remember { mutableStateOf(false) }

    var doubleFieldSettingsList = remember { mutableStateListOf<DoubleFieldSettings> (DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings(),DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings()) }

    val isGeneralHeadwindEnabled by sharedViewModel.isGeneralHeadwindEnabled.collectAsState()

    LaunchedEffect(isGeneralHeadwindEnabled) {
        isheadwindenabled = isGeneralHeadwindEnabled
    }

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

    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            ispalettezwift = settings.ispalettezwift
            iscenteralign = settings.iscenteralign
            iscenterrolling = settings.iscenterrolling
            iscentervertical = settings.iscentervertical
            iscenterkaroo = settings.iscenterkaroo
        }
    }


    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.padding(5.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            oneFieldSettingsDerived.value.forEachIndexed { index, oneFieldSettings ->
                TopAppBar(title = { Text("Rolling Field ${index + 1}") })
                DropdownOneField(true,
                    "First Field",
                    oneFieldSettings.onefield
                ) { newAction -> oneFieldSettingsList[index] = oneFieldSettings.copy(onefield = newAction) }
                ZoneSwitch(oneFieldSettings.onefield.iszone, oneFieldSettings.onefield.kaction.zone != "none") { newZone -> oneFieldSettingsList[index].onefield= oneFieldSettings.onefield.copy(iszone = newZone) }
                DropdownOneField(false,
                    "Second Field",
                    oneFieldSettings.secondfield
                ) { newAction -> oneFieldSettingsList[index] = oneFieldSettings.copy(secondfield = newAction) }
                ZoneSwitch(oneFieldSettings.secondfield.iszone, oneFieldSettings.secondfield.kaction.zone != "none") { newZone -> oneFieldSettingsList[index].secondfield= oneFieldSettings.secondfield.copy(iszone = newZone) }
                DropdownOneField(false,
                    "Third Field",
                    oneFieldSettings.thirdfield
                ) { newAction -> oneFieldSettingsList[index] = oneFieldSettings.copy(thirdfield = newAction) }
                ZoneSwitch(oneFieldSettings.thirdfield.iszone, oneFieldSettings.thirdfield.kaction.zone != "none") { newZone -> oneFieldSettingsList[index].thirdfield= oneFieldSettings.thirdfield.copy(iszone = newZone) }
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
            }

            FilledTonalButton(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                val newGeneralSettings = GeneralSettings(
                    ispalettezwift = ispalettezwift,
                    iscenteralign = iscenteralign,
                    iscentervertical = iscentervertical,
                    iscenterkaroo = iscenterkaroo
                )

                coroutineScope.launch {
                    savedDialogVisible = true
                    saveDoubleFieldSettings(ctx, doubleFieldSettingsList)
                    saveGeneralSettings(ctx, newGeneralSettings)
                    saveOneFieldSettings(ctx, oneFieldSettingsList)
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
fun ConfFields(sharedViewModel: HeadwindSharedViewModel = viewModel()) {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var ispalettezwift by remember { mutableStateOf(true) }
    var iscenteralign by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscentervertical by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterrolling by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterkaroo by remember { mutableStateOf(false) }
    var savedDialogVisible by remember { mutableStateOf(false) }
    var isheadwindenabled by remember { mutableStateOf(false) }

    var doubleFieldSettingsList = remember { mutableStateListOf<DoubleFieldSettings> (DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings(),DoubleFieldSettings(), DoubleFieldSettings(), DoubleFieldSettings()) }

    val isGeneralHeadwindEnabled by sharedViewModel.isGeneralHeadwindEnabled.collectAsState()

    LaunchedEffect(isGeneralHeadwindEnabled) {
        isheadwindenabled = isGeneralHeadwindEnabled
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

    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            ispalettezwift = settings.ispalettezwift
            iscenteralign = settings.iscenteralign
            iscentervertical = settings.iscentervertical
            iscenterkaroo = settings.iscenterkaroo
            isheadwindenabled = settings.isheadwindenabled
            iscenterrolling= settings.iscenterrolling
            sharedViewModel.setHeadwindEnabled(isheadwindenabled)
        }
    }


    Timber.d("isGeneralHeadwindEnabled $isGeneralHeadwindEnabled")

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
                    isGeneralHeadwindEnabled
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
                    isGeneralHeadwindEnabled
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
                val newGeneralSettings = GeneralSettings(
                    ispalettezwift = ispalettezwift,
                    iscenteralign = iscenteralign,
                    iscentervertical = iscentervertical,
                    iscenterkaroo = iscenterkaroo,
                    isheadwindenabled = isheadwindenabled
                )
                coroutineScope.launch {
                    savedDialogVisible = true
                    saveGeneralSettings(ctx, newGeneralSettings)
                    saveDoubleFieldSettings(ctx, doubleFieldSettingsList)
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
fun ConfGeneral(sharedViewModel: HeadwindSharedViewModel = viewModel()) {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isGeneralHeadwindEnabled by sharedViewModel.isGeneralHeadwindEnabled.collectAsState()

    var ispalettezwift by remember { mutableStateOf(true) }
    var iscenteralign by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscentervertical by remember { mutableStateOf(FieldPosition.CENTER) }
    var iscenterkaroo by remember { mutableStateOf(false) }
    var isheadwindenabled by remember { mutableStateOf(isGeneralHeadwindEnabled) }


    var savedDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctx.streamGeneralSettings().collect { settings ->
            ispalettezwift = settings.ispalettezwift
            iscenteralign = settings.iscenteralign
            iscentervertical = settings.iscentervertical
            iscenterkaroo = settings.iscenterkaroo
            isheadwindenabled = settings.isheadwindenabled
            sharedViewModel.setHeadwindEnabled(isheadwindenabled)
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
                Text("Horizontal Fields alignment (icon/text) ?")
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
                    sharedViewModel.setHeadwindEnabled(it)
                })
                Spacer(modifier = Modifier.width(10.dp))
                Text("Enable Headwind Datafield (you need to have Headwind extension installed)?")
            }

            FilledTonalButton(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                val newGeneralSettings = GeneralSettings(
                    ispalettezwift = ispalettezwift,
                    iscenteralign = iscenteralign,
                    iscentervertical = iscentervertical,
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
fun DropdownDoubleField(label: String, action: DoubleFieldType, isheadwindenabled: Boolean, onActionChange: (DoubleFieldType) -> Unit) {
    var dropdownOptions = KarooAction.entries.map { DropdownOption(it.action.toString(), it.label) }

    // Filtrar la opción HEADWIND si isheadwindenabled es false
    if (!isheadwindenabled) {
        dropdownOptions = dropdownOptions.filter { it.id != KarooAction.HEADWIND.action.toString() }
    }

    val dropdownInitialSelection by remember(action) {
        mutableStateOf(
            dropdownOptions.find { it.id == action.kaction.action.toString() } ?: dropdownOptions.first()
        )
    }

    // Cambiar la selección a SPEED si HEADWIND estaba seleccionado y isheadwindenabled es false
    LaunchedEffect(isheadwindenabled) {
        if (!isheadwindenabled && action.kaction == KarooAction.HEADWIND) {
            onActionChange(action.copy(kaction = KarooAction.SPEED))
        }
    }

    KarooKeyDropdown(remotekey = label, options = dropdownOptions, selectedOption = dropdownInitialSelection) { selectedOption ->
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
        }
    }
}