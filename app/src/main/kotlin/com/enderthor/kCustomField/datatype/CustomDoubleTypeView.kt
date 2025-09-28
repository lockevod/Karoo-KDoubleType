package com.enderthor.kCustomField.datatype

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import timber.log.Timber
import java.text.DateFormat
import java.util.Calendar
import kotlin.math.roundToInt
import java.util.Locale


fun formatTimeRemaining(timeMs: Double): String {
    val totalMinutes = (timeMs / 60000).toInt() // Convertir ms a minutos
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d", hours, minutes)
    } else {
        String.format(Locale.US, "%d", minutes)
    }
}

private fun epochToTimeOfDay(epochMillis: Double): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = epochMillis.toLong()
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
}


fun formatNumber(number: Double, isInt: Boolean, isTime: Boolean = false, isCivil: Boolean = false, isClimb: Boolean = false): String = buildString {

    if (isClimb) {
        if (isCivil) {
            val timeStr = epochToTimeOfDay(number)
            append(timeStr.split(':').firstOrNull() ?: "")
        } else if (isTime) {
            val timeStr = formatTimeRemaining(number)
            append(timeStr.split(':').firstOrNull() ?: "")
        } else {

            val numValue = number.roundToInt()
            when {
                numValue < 1000 -> {
                    append(numValue.toString())
                }

                numValue < 10000 -> {
                    val intPart = numValue / 1000
                    val decPart = (numValue % 1000) / 10
                    append(String.format("%d.%02d", intPart, decPart))
                }

                else -> {
                    // Para distancias >= 10 km, usar formato más compacto: "10.5" en lugar de "10.50"
                    val intPart = numValue / 1000
                    val decPart = (numValue % 1000) / 100
                    append(String.format("%d.%d", intPart, decPart))
                }
            }
        }
    } else {
        if (isCivil) {
            append(epochToTimeOfDay(number))
        } else if (isTime) {
            append(formatTimeRemaining(number))
        } else {
            if (isInt) append(number.roundToInt().toString().take(5))
            else append(((number * 10.0).roundToInt() / 10.0).toString().take(5))
        }

    }
}


@Composable
private fun VerticalDivider(isTopField: Boolean, fieldSize: FieldSize, isdivider: Boolean,isClimbField: Boolean = false) {
    val height = when {
        isClimbField -> 1.dp
        isTopField -> 10.dp
        fieldSize == FieldSize.LARGE -> 28.dp
        else -> 14.dp
    }
    Box(modifier = GlanceModifier.fillMaxWidth().height(height)) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight()) {}
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Column(modifier = GlanceModifier.defaultWeight()) {}
        }
    }
}

@Composable
private fun IconRow(
    icon: Int,
    colorFilter: ColorFilter,
    layout: FieldPosition,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth()
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = when (layout) {
            FieldPosition.CENTER -> Alignment.CenterHorizontally
            FieldPosition.RIGHT -> Alignment.End
            FieldPosition.LEFT -> Alignment.Start
        }
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = colorFilter
        )
    }
}


@Composable
private fun NumberRow(
    number: String,
    zoneColor: ColorProvider,
    layout: FieldPosition,
    fieldSize: FieldSize,
    onlyOne: Boolean,
    isheadwind: Boolean = false,
    iszone: Boolean = false,
    textColor: ColorProvider
) {
    val padding = if (fieldSize == FieldSize.LARGE) 8.dp else 2.dp
    val fontSize = when {
        onlyOne -> 42.sp
        number.length > 3 -> 32.sp
        else -> 38.sp
    }

    Row(
        modifier = GlanceModifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(bottom = padding),
        verticalAlignment = if (isheadwind) Alignment.CenterVertically else Alignment.Bottom,
        horizontalAlignment = when (layout) {
            FieldPosition.CENTER -> Alignment.CenterHorizontally
            FieldPosition.RIGHT -> Alignment.End
            FieldPosition.LEFT -> Alignment.Start
        }
    ) {
        Text(
            text = number,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                color = if (iszone) textColor else ColorProvider(Color.Black, Color.White)
            )
        )
        Spacer(
            modifier = GlanceModifier
                .fillMaxHeight()
                .width(2.dp)
                .background(zoneColor)
        )
    }
}



@Composable
private fun OneIconRow(
    icon: Int,
    iconColor: ColorProvider,
    text: String,
    iszone: Boolean,
    fieldSize: FieldSize,
    isClimb: Boolean = false
) {
    val isSmall = fieldSize == FieldSize.SMALL
    val rowHeight = if (isSmall) 31.dp else 37.dp
    val iconSize = if (isSmall) 16.dp else 20.dp
    val fontSize = if (isSmall) 15.sp else 18.sp
    val topPadding = if (isSmall) (-2).dp else (-1).dp

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(rowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (isClimb) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Column(
            horizontalAlignment = if (isClimb) Alignment.CenterHorizontally else Alignment.Start,
            modifier = GlanceModifier
                .height(if (isSmall) 20.dp else 24.dp)
                .width(24.dp)
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                modifier = GlanceModifier.size(iconSize).padding(top = topPadding),
                colorFilter = ColorFilter.tint(iconColor)

            )
        }
        if (!isClimb) {
            Column(
                modifier = GlanceModifier
                    .height(if (isSmall) 32.dp else 36.dp)
                    .fillMaxWidth()
                    .padding(end = 3.dp),
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                val displayText = text.takeIf { it.length <= 10 } ?: text.split(" ", limit = 2)
                    .let { parts -> if (parts.size > 1) "${parts[0]}\n${parts[1]}" else text }

                val adjustedFontSize =
                    if ((displayText.count { it == '\n' } + 1) == 2) (fontSize.value * 0.85).sp else fontSize

                Text(
                    text = displayText,
                    maxLines = 2,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = adjustedFontSize,
                        fontFamily = FontFamily.Monospace,
                        color = if (iszone) iconColor else ColorProvider(Color.Black, Color.White),
                        textAlign = TextAlign.End
                    )
                )
            }
        }
    }
}

@Composable
private fun OneNumberRow(
    number: String,
    layout: FieldPosition,
    fieldSize: FieldSize,
    textSize: Int,
    iszone: Boolean,
    textColor: ColorProvider,
    ispower: Boolean,
    secondValue: String,
    isClimb: Boolean = false
) {
    val padding = if (fieldSize == FieldSize.LARGE) 7.dp else 2.dp
    val displayNumber =
        if (!ispower) {
        number
    } else {
        "${number.take(3)}-${secondValue.take(3)}"
    }

    // Ajustar el tamaño de fuente para el campo de subida cuando tiene 4 caracteres
    val adjustedTextSize = if (isClimb && displayNumber.length == 4) {
        // Reducir el tamaño de fuente 25% para que 4 caracteres ocupen el mismo ancho que 3
        (textSize * 0.8).roundToInt()
    } else {
        textSize
    }

    Row(
        modifier = GlanceModifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(bottom = padding, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (isClimb) Alignment.CenterHorizontally
        else when (layout) {
            FieldPosition.CENTER -> Alignment.CenterHorizontally
            FieldPosition.RIGHT -> Alignment.End
            FieldPosition.LEFT -> Alignment.Start
        }
    ) {
        Text(
            text = displayNumber,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = adjustedTextSize.sp,
                fontFamily = FontFamily.Monospace,
                color = if (iszone) textColor else ColorProvider(Color.Black, Color.White)
            ),
            modifier = GlanceModifier.padding(top = -padding)
        )
    }
}


@Composable
private fun HorizontalScreenContent(number: String, icon: Int, colorFilter: ColorProvider, layout: FieldPosition, iszone: Boolean) {
   val colorIcon= ColorFilter.tint(colorFilter)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = when (layout.name) {
            "CENTER" -> Alignment.CenterHorizontally
            "RIGHT" -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        if (layout.name == "LEFT") {
            Image(
                provider = ImageProvider(icon),
                contentDescription = "Icon",
                modifier = GlanceModifier.size(20.dp),
                colorFilter = colorIcon
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
        }
        Text(
            text = number,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                fontFamily = FontFamily.Monospace,
                color = if (iszone) colorFilter else ColorProvider(Color.Black, Color.White),
                textAlign = when (layout.name) {
                    "CENTER" -> TextAlign.Center
                    "RIGHT" -> TextAlign.End
                    else -> TextAlign.Start
                }
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        if (layout.name != "LEFT") {
            Spacer(modifier = GlanceModifier.width(5.dp).fillMaxHeight())
            Image(
                provider = ImageProvider(icon),
                contentDescription = "Icon",
                modifier = GlanceModifier.size(20.dp),
                colorFilter = colorIcon
            )
        }
        Spacer(modifier = GlanceModifier.height(1.dp).background(ColorProvider(Color.Black, Color.White)))
    }
}


@Composable
private fun SingleHorizontalField(icon: Int, iconColor: ColorProvider, layout: FieldPosition, fieldSize: FieldSize, zoneColor: ColorProvider, number: String, isheadwind: Boolean, iszone: Boolean) {
    val height = when (fieldSize) {
        FieldSize.LARGE -> 12.dp
        FieldSize.SMALL -> 6.dp
        FieldSize.MEDIUM -> 9.dp
        FieldSize.EXTRA_LARGE ->15.dp
    }


    Spacer(modifier = GlanceModifier.height(height))
    IconRow(icon, ColorFilter.tint(iconColor), layout)
    Spacer(modifier = GlanceModifier.height(5.dp))
    if (isheadwind && fieldSize == FieldSize.MEDIUM) NumberRow(number.take(4), zoneColor, layout, fieldSize, false,true,iszone,iconColor)
    else NumberRow(number.take(4), zoneColor, layout, fieldSize, false,false,iszone,iconColor)

}


@Composable
fun NotSupported(overlayText: String, fontSize: Int)
{
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(5.dp),
        contentAlignment = Alignment(
            vertical = Alignment.Vertical.CenterVertically,
            horizontal = Alignment.Horizontal.CenterHorizontally,
        ),
    ) {
        Text(
            overlayText,
            maxLines = 2,
            style = TextStyle(
                ColorProvider(Color.Black, Color.White),
                fontSize = (0.8 * fontSize).sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = GlanceModifier.background(ColorProvider(Color.White, Color.Black)
            ).padding(1.dp)
        )
    }

}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun RollingFieldScreen(
    dNumber: Double,
    isInt: Boolean,
    action: KarooAction,
    iconColor: ColorProvider,
    zonecolor: ColorProvider,
    fieldsize: FieldSize,
    iskaroo3: Boolean,
    clayout: FieldPosition,
    windtext: String,
    winddiff: Int,
    baseBitmap: Bitmap,
    selector: Boolean,
    textSize: Int,
    iszone: Boolean,
    ispreview: Boolean,
    secondValue: Double,
    isClimb: Boolean = false
) {

    val icon = action.icon
    val label = action.label
    val ispower = action.powerField
    val isTime = action.action == KarooAction.TIMETODEST.action
    val isCivil =
        action.action == KarooAction.CIVIL_DUSK.action || action.action == KarooAction.CIVIL_DAWN.action

    val isRealZone =
        if( (action.name =="AVERAGE_PEDAL_BALANCE" || action.name =="PEDAL_BALANCE") && iszone && action.powerField) {
            if (dNumber > secondValue * 0.85 && dNumber < secondValue * 1.07) {
                Timber.d("isRealZone: $dNumber")
                false
            } else true
        }
        else iszone



    if (selector) {
        val newInt = if (isClimb) true else isInt

        val number = formatNumber(dNumber, newInt, isTime, isCivil, isClimb)
        val numberSecond = formatNumber(secondValue, isInt, isTime, isCivil, isClimb)


        Box(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = if (iskaroo3) GlanceModifier.fillMaxSize()
                    .cornerRadius(6.dp) else GlanceModifier.fillMaxSize()
            )
            {
                Column(modifier = GlanceModifier.defaultWeight().background(zonecolor)) {
                    when (fieldsize) {

                        FieldSize.SMALL -> Spacer(modifier = GlanceModifier.height(2.dp))
                        FieldSize.MEDIUM -> Spacer(modifier = GlanceModifier.height(4.dp))
                        else -> Spacer(modifier = GlanceModifier.height(1.dp))

                    }
                    if (fieldsize == FieldSize.LARGE || fieldsize == FieldSize.EXTRA_LARGE) NotSupported(
                        "Size Not Supported",
                        24
                    )
                    else {
                        OneIconRow(icon, iconColor, label.uppercase(), isRealZone, fieldsize, isClimb)
                        OneNumberRow(
                            if (isClimb) number.take(4) else number.take(6),
                            clayout,
                            fieldsize,
                            (textSize * (if (ispreview) 0.8 else if (isClimb) 1.1 else 1.0)).roundToInt(),
                            isRealZone,
                            iconColor,
                            ispower,
                            numberSecond.take(3),
                            isClimb
                        )
                    }
                }
            }
        }
    } else HeadwindDirection(baseBitmap, winddiff, textSize, windtext)
}

fun checkRealZone(action: KarooAction, iszone: Boolean, dNumber: Double, secondValue: Double): Boolean {

    return if( (action.name =="AVERAGE_PEDAL_BALANCE" || action.name =="PEDAL_BALANCE") && iszone && action.powerField)
            !((dNumber > secondValue * 0.85 && dNumber < secondValue * 1.07) || (dNumber == 0.0))
        else true
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleScreenSelector(
    selector: Int, showH: Boolean, leftNumber: Double, rightNumber: Double,leftField: DoubleFieldType, rightField: DoubleFieldType,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColorLeft: ColorProvider, zoneColorRight: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, layout: FieldPosition, text: String, windDirection: Int, baseBitmap: Bitmap, isdivider:Boolean,  leftNumberSecond:Double = 0.0, rightNumberSecond:Double = 0.0, isClimb: Boolean = false,isClimbField: Boolean = false){


    val leftIcon= leftField.kaction.icon
    val rightIcon= rightField.kaction.icon
    val leftLabel= leftField.kaction.label
    val rightLabel= rightField.kaction.label

    val ispowerLeft= leftField.kaction.powerField
    val ispowerRight= rightField.kaction.powerField
    val isLeftInt= (!(leftField.kaction.convert == "speed" || leftField.kaction.zone == "slopeZones" || leftField.kaction.label == "IF")) || (ispowerLeft) || (isClimb)
    val isRightInt= !(rightField.kaction.convert == "speed" || rightField.kaction.zone == "slopeZones" || rightField.kaction.label == "IF")  || (ispowerRight) || (isClimb)
    val leftCivil=leftField.kaction.action==KarooAction.CIVIL_DUSK.action ||  leftField.kaction.action==KarooAction.CIVIL_DAWN.action
    val rightCivil=rightField.kaction.action==KarooAction.CIVIL_DUSK.action ||  rightField.kaction.action==KarooAction.CIVIL_DAWN.action
    val leftTime=leftField.kaction.action==KarooAction.TIMETODEST.action
    val rightTime=rightField.kaction.action==KarooAction.TIMETODEST.action


    val iszoneLeft= if (checkRealZone(leftField.kaction,leftField.iszone,leftNumber,leftNumberSecond)) leftField.iszone else false
    val iszoneRight= if (checkRealZone(rightField.kaction,rightField.iszone,rightNumber,rightNumberSecond)) rightField.iszone else false

    val newLeft = if (ispowerLeft) (formatNumber(leftNumber, true) + "-" + formatNumber(
        leftNumberSecond,
        true
    ))
    else if (!showH) formatNumber(leftNumber, isLeftInt, leftTime, leftCivil)
    else when (selector) {
        0, 3 -> if (leftLabel == "IF") ((leftNumber * 10.0).roundToInt() / 10.0).toString()
            .take(3) else formatNumber(leftNumber, true, leftTime, leftCivil)

        else -> "0.0"
    }


    val newRight = if (ispowerRight) (formatNumber(rightNumber, true) + "-" + formatNumber(
        rightNumberSecond,
        true
    ))
    else if (!showH) formatNumber(rightNumber, isRightInt, rightTime, rightCivil)
    else when (selector) {
        1, 3 -> if (rightLabel == "IF") ((rightNumber * 10.0).roundToInt() / 10.0).toString()
            .take(3) else formatNumber(rightNumber, true, rightTime, rightCivil)

        else -> "0.0"
    }


    val icon1 = if (selector == 0 || selector == 3) leftIcon else 1
    val icon2 = if (selector == 1 || selector == 3) rightIcon else 1
    val iconColor1 = if (selector == 0 || selector == 3) iconColorLeft else ColorProvider(
        Color.Black,
        Color.Black
    )
    val iconColor2 = if (selector == 1 || selector == 3) iconColorRight else ColorProvider(
        Color.Black,
        Color.Black
    )
    val zoneColor1 = if (selector == 0 || selector == 3) zoneColorLeft else ColorProvider(
        Color.Black,
        Color.Black
    )
    val zoneColor2 = if (selector == 1 || selector == 3) zoneColorRight else ColorProvider(
        Color.Black,
        Color.Black
    )


  // Timber.w("newLeft: $newLeft  iconColorLeft: $iconColorLeft  zoneColorLeft: $zoneColorLeft  iszoneLeft: $iszoneLeft")
    if (!showH || ispowerLeft || ispowerRight) {
        when (fieldSize) {
            FieldSize.SMALL -> DoubleTypesVerticalScreenSmall(
                newLeft,
                newRight,
                leftIcon,
                rightIcon,
                iconColorLeft,
                iconColorRight,
                zoneColorLeft,
                zoneColorRight,
                isKaroo3,
                layout,
                iszoneLeft,
                iszoneRight,
                isdivider
            )

            FieldSize.MEDIUM, FieldSize.LARGE -> DoubleTypesVerticalScreenBig(
                newLeft,
                newRight,
                leftIcon,
                rightIcon,
                iconColorLeft,
                iconColorRight,
                zoneColorLeft,
                zoneColorRight,
                isKaroo3,
                layout,
                iszoneLeft,
                iszoneRight,
                isdivider
            )

            FieldSize.EXTRA_LARGE -> NotSupported("Size Not Supported", 24)
        }
    } else {
        DoubleTypesScreenHorizontal(
            newLeft,
            newRight,
            icon1,
            icon2,
            iconColor1,
            iconColor2,
            zoneColor1,
            zoneColor2,
            fieldSize,
            isKaroo3,
            layout,
            selector,
            text,
            windDirection,
            baseBitmap,
            iszoneLeft,
            iszoneRight,
            isdivider,
            isClimbField
        )
    }
}


fun getFieldTypeSelector(firstFieldState:String,secondFieldState:String) :Int
{

    return when {
        firstFieldState == "HEADWIND"  && secondFieldState=="HEADWIND" -> 2
        firstFieldState == "HEADWIND" -> 1
        secondFieldState =="HEADWIND" -> 0
        else -> 3
    }

}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
fun ClimbScreenSelector(
    firstValue: Double,
    secondValue: Double,
    thirdValue: Double,
    fourthValue: Double,
    climbValue: Double,
    firstField: DoubleFieldType,
    secondField: DoubleFieldType,
    thirdField: DoubleFieldType,
    fourthField: DoubleFieldType,
    climbField: DoubleFieldType,
    firstIconColor: ColorProvider,
    secondIconColor: ColorProvider,
    thirdIconColor: ColorProvider,
    fourthIconColor: ColorProvider,
    climbIconColor: ColorProvider,
    firstZoneColor: ColorProvider,
    secondZoneColor: ColorProvider,
    thirdZoneColor: ColorProvider,
    fourthZoneColor: ColorProvider,
    climbZoneColor: ColorProvider,
    fieldSize: Int,
    isKaroo3: Boolean,
    layout: FieldPosition,
    windText: String,
    windDirection: Int,
    baseBitmap: Bitmap,
    isDivider: Boolean,
    firstValueRight: Double,
    secondValueRight: Double,
    thirdValueRight: Double,
    fourthValueRight: Double,
    climbValueRight: Double,
    isFirsthorizontal: Boolean,
    isSecondhorizontal: Boolean,
    isClimbEnabled: Boolean
) {
    if (fieldSize < 400) {
        NotSupported("Size Not Supported", 24)
        return
    }

    if (!isClimbEnabled)
    {

        Row(modifier = GlanceModifier.fillMaxSize()) {

            Column(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
                DoubleScreenSelector(
                    selector = getFieldTypeSelector(firstField.kaction.name, secondField.kaction.name),
                    showH = isFirsthorizontal,
                    leftNumber = firstValue,
                    rightNumber = secondValue,
                    leftField = firstField,
                    rightField = secondField,
                    iconColorLeft = firstIconColor,
                    iconColorRight = secondIconColor,
                    zoneColorLeft = firstZoneColor,
                    zoneColorRight = secondZoneColor,
                    fieldSize = FieldSize.MEDIUM,
                    isKaroo3 = isKaroo3,
                    layout = layout,
                    text = windText,
                    windDirection = windDirection,
                    baseBitmap = baseBitmap,
                    isdivider = isDivider,
                    leftNumberSecond = firstValueRight,
                    rightNumberSecond = secondValueRight,
                    isClimb = false,
                    isClimbField = true
                )
            }


            if (isDivider) {
                Spacer(
                    modifier = GlanceModifier.fillMaxHeight()
                        .width(1.dp)
                        .background(ColorProvider(Color.Black, Color.White))
                )
            }


            Column(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
                DoubleScreenSelector(
                    selector = getFieldTypeSelector(thirdField.kaction.name, fourthField.kaction.name),
                    showH = isSecondhorizontal,
                    leftNumber = thirdValue,
                    rightNumber = fourthValue,
                    leftField = thirdField,
                    rightField = fourthField,
                    iconColorLeft = thirdIconColor,
                    iconColorRight = fourthIconColor,
                    zoneColorLeft = thirdZoneColor,
                    zoneColorRight = fourthZoneColor,
                    fieldSize = FieldSize.MEDIUM,
                    isKaroo3 = isKaroo3,
                    layout = layout,
                    text = windText,
                    windDirection = windDirection,
                    baseBitmap = baseBitmap,
                    isdivider = isDivider,
                    leftNumberSecond = thirdValueRight,
                    rightNumberSecond = fourthValueRight,
                    isClimb = false,
                    isClimbField = true
                )
            }
        }
    } else {

        Row(modifier = GlanceModifier.fillMaxSize()) {

            Column(modifier = GlanceModifier.fillMaxHeight().width(90.dp)) {
                DoubleScreenSelector(
                    selector = getFieldTypeSelector(firstField.kaction.name, secondField.kaction.name),
                    showH = isFirsthorizontal,
                    leftNumber = firstValue,
                    rightNumber = secondValue,
                    leftField = firstField,
                    rightField = secondField,
                    iconColorLeft = firstIconColor,
                    iconColorRight = secondIconColor,
                    zoneColorLeft = firstZoneColor,
                    zoneColorRight = secondZoneColor,
                    fieldSize = FieldSize.MEDIUM,
                    isKaroo3 = isKaroo3,
                    layout = layout,
                    text = windText,
                    windDirection = windDirection,
                    baseBitmap = baseBitmap,
                    isdivider = isDivider,
                    leftNumberSecond = firstValueRight,
                    rightNumberSecond = secondValueRight,
                    isClimb = true,
                    isClimbField = true
                )
            }


            if (isDivider) {
                Spacer(
                    modifier = GlanceModifier.fillMaxHeight()
                        .width(1.dp)
                        .background(ColorProvider(Color.Black, Color.White))
                )
            }


            Column(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
                RollingFieldScreen(
                    dNumber = climbValue,
                    isInt = !(climbField.kaction.convert == "speed" || climbField.kaction.zone == "slopeZones" || climbField.kaction.label == "IF") || climbField.kaction.powerField,
                    action = climbField.kaction,
                    iconColor = climbIconColor,
                    zonecolor = climbZoneColor,
                    fieldsize = FieldSize.MEDIUM,
                    iskaroo3 = isKaroo3,
                    clayout = FieldPosition.CENTER,
                    windtext = windText,
                    winddiff = windDirection,
                    baseBitmap = baseBitmap,
                    selector = true,
                    textSize = 40,
                    iszone = climbField.iszone,
                    ispreview = false,
                    secondValue = climbValueRight,
                    isClimb = true,
                )
            }


            if (isDivider) {
                Spacer(
                    modifier = GlanceModifier.fillMaxHeight()
                        .width(1.dp)
                        .background(ColorProvider(Color.Black, Color.White))
                )
            }


            Column(modifier = GlanceModifier.fillMaxHeight().width(90.dp)) {
                DoubleScreenSelector(
                    selector = getFieldTypeSelector(thirdField.kaction.name, fourthField.kaction.name),
                    showH = isSecondhorizontal,
                    leftNumber = thirdValue,
                    rightNumber = fourthValue,
                    leftField = thirdField,
                    rightField = fourthField,
                    iconColorLeft = thirdIconColor,
                    iconColorRight = fourthIconColor,
                    zoneColorLeft = thirdZoneColor,
                    zoneColorRight = fourthZoneColor,
                    fieldSize = FieldSize.MEDIUM,
                    isKaroo3 = isKaroo3,
                    layout = layout,
                    text = windText,
                    windDirection = windDirection,
                    baseBitmap = baseBitmap,
                    isdivider = isDivider,
                    leftNumberSecond = thirdValueRight,
                    rightNumberSecond = fourthValueRight,
                    isClimb = true,
                    isClimbField = true
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun DoubleTypesScreenHorizontal(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, layout: FieldPosition, selector: Int, text: String, windDirection: Int, baseBitmap: Bitmap,iszoneLeft: Boolean,iszoneRight: Boolean,isdivider:Boolean,isClimbField: Boolean = false
) {


    if (!isClimbField) VerticalDivider(true, fieldSize,isdivider)
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {

        Row(modifier = GlanceModifier.fillMaxSize().let { if (isKaroo3) it.cornerRadius(8.dp) else it }) {
            Column(modifier = GlanceModifier.defaultWeight().background(if (selector in 1..2) ColorProvider(Color.White, Color.Black) else zoneColor1)) {
                when (selector) {
                    1, 2 -> HeadwindDirectionDoubleType(baseBitmap, windDirection, 38, text)
                    0 -> SingleHorizontalField(leftIcon, iconColorLeft, layout, fieldSize, zoneColor1, leftNumber, true,iszoneLeft)
                    else -> SingleHorizontalField(leftIcon, iconColorLeft, layout, fieldSize, zoneColor1, leftNumber, false,iszoneLeft)
                }
            }
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Column(modifier = GlanceModifier.defaultWeight().background(if (selector in listOf(0, 2)) ColorProvider(Color.White, Color.Black) else zoneColor2)) {
                when (selector) {
                    0, 2 -> HeadwindDirectionDoubleType(baseBitmap, windDirection, 38, text)
                    1 -> SingleHorizontalField(rightIcon, iconColorRight, layout, fieldSize, zoneColor2, rightNumber, true,iszoneRight)
                    else -> SingleHorizontalField(rightIcon, iconColorRight, layout, fieldSize, zoneColor2, rightNumber, false,iszoneRight)
                }
            }
        }
    }
    VerticalDivider(false, fieldSize,isdivider)
}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun DoubleTypesVerticalScreenSmall(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, isKaroo3: Boolean, layout: FieldPosition, iszoneLeft: Boolean, iszoneRight: Boolean, isdivider: Boolean
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zoneColor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zoneColor1)) {
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(zoneColor1))
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, layout,iszoneLeft)
            }
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor2))
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor2)) {
                HorizontalScreenContent(rightNumber, rightIcon, iconColorRight, layout,iszoneRight)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun DoubleTypesVerticalScreenBig(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, isKaroo3: Boolean, layout: FieldPosition,iszoneLeft: Boolean,iszoneRight: Boolean,isdivider: Boolean
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zoneColor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zoneColor1)) {
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(2.dp).background(zoneColor1))
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, layout,iszoneLeft)
            }
            if (isdivider) Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(7.dp).background(zoneColor2))
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor2)) {
                HorizontalScreenContent(rightNumber, rightIcon, iconColorRight, layout,iszoneRight)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
private fun HeadwindDirectionDoubleType(baseBitmap: Bitmap, bearing: Int, fontSize: Int, overlayText: String) {
    Spacer(modifier = GlanceModifier.fillMaxWidth().height(10.dp))
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = GlanceModifier.fillMaxSize(),
            provider = ImageProvider(getArrowBitmapByBearing(baseBitmap, bearing)),
            contentDescription = "Relative wind direction indicator",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(ColorProvider(Color.Black, Color.White))
        )
        if (overlayText.isNotEmpty()) {
            Text(
                overlayText,
                style = TextStyle(ColorProvider(Color.Black, Color.White), fontSize = (0.6 * fontSize).sp, fontFamily = FontFamily.Monospace),
                modifier = GlanceModifier.background(Color(1f, 1f, 1f, 0.4f), Color(0f, 0f, 0f, 0.4f)).padding(1.dp)
            )
        }
    }
}
