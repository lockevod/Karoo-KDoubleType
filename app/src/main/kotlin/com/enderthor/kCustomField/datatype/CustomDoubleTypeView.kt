package com.enderthor.kCustomField.datatype

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun formatNumber(number: Double, isInt: Boolean): String {
    return if (isInt) number.roundToInt().toString().take(5) else ((number * 10.0).roundToInt() / 10.0).toString().take(5)
}

@Composable
fun VerticalDivider(isTopField: Boolean, fieldSize: FieldSize) {
    val height = when {
        isTopField -> 10.dp
        fieldSize == FieldSize.LARGE -> 28.dp
        else -> 14.dp
    }
    Box(modifier = GlanceModifier.fillMaxWidth().height(height)) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight()) {}
            Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Column(modifier = GlanceModifier.defaultWeight()) {}
        }
    }
}

@Composable
fun IconRow(icon: Int, colorFilter: ColorFilter, layout: FieldPosition) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = when (layout.name) {
            "CENTER" -> Alignment.CenterHorizontally
            "RIGHT" -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = "Icon",
            modifier = GlanceModifier.size(20.dp),
            colorFilter = colorFilter
        )
    }
}

@Composable
fun NumberRow(number: String, zoneColor: ColorProvider, layout: FieldPosition, fieldSize: FieldSize, onlyOne: Boolean, isheadwind: Boolean = false) {
    val padding = if (fieldSize == FieldSize.LARGE) 8.dp else 2.dp
    val fontSize = when {
        onlyOne -> 42.sp
        number.length > 3 -> 32.sp
        else -> 38.sp
    }
    Row(
        modifier = GlanceModifier.fillMaxHeight().fillMaxWidth().padding(bottom = padding),
        verticalAlignment = if (isheadwind) Alignment.CenterVertically else Alignment.Bottom,
        horizontalAlignment = when (layout.name) {
            "CENTER" -> Alignment.CenterHorizontally
            "RIGHT" -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        Text(
            text = number,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                color = ColorProvider(Color.Black, Color.White)
            )
        )
       Spacer(modifier = GlanceModifier.fillMaxHeight().width(2.dp).background(zoneColor))
    }
}

@Composable
fun HorizontalScreenContent(number: String, icon: Int, colorFilter: ColorFilter, layout: FieldPosition) {
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
                colorFilter = colorFilter
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
        }
        Text(
            text = number,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                fontFamily = FontFamily.Monospace,
                color = ColorProvider(Color.Black, Color.White),
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
                colorFilter = colorFilter
            )
        }
        Spacer(modifier = GlanceModifier.height(1.dp).background(ColorProvider(Color.Black, Color.White)))
    }
}



@Composable
fun SingleHorizontalField(icon: Int, iconColor: ColorFilter, layout: FieldPosition, fieldSize: FieldSize, zoneColor: ColorProvider, number: String, isheadwind: Boolean) {
    val height = when (fieldSize) {
        FieldSize.LARGE -> 12.dp
        FieldSize.SMALL -> 6.dp
        FieldSize.MEDIUM -> 9.dp
        FieldSize.EXTRA_LARGE -> TODO()
    }
    Spacer(modifier = GlanceModifier.height(height))
    IconRow(icon, iconColor, layout)
    Spacer(modifier = GlanceModifier.height(5.dp))
    if (isheadwind && fieldSize == FieldSize.MEDIUM) NumberRow(number.take(4), zoneColor, layout, fieldSize, false,true)
    else NumberRow(number.take(4), zoneColor, layout, fieldSize, false)

}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun RollingFieldScreen(number: Double, leftIcon: Int, iconColorLeft: ColorFilter, zonecolor1: ColorProvider, fieldsize: FieldSize, iskaroo3: Boolean, leftlabel: String, clayout: FieldPosition)
{
    val leftNumber = if (leftlabel == "IF") ((number * 10.0).roundToInt() / 10.0).toString().take(3) else number.roundToInt().toString()
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Row(modifier = if (iskaroo3) GlanceModifier.fillMaxSize().cornerRadius(8.dp) else GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight().background(zonecolor1)) {
                when (fieldsize) {
                    FieldSize.LARGE -> Spacer(modifier = GlanceModifier.height(12.dp))
                    FieldSize.SMALL -> Spacer(modifier = GlanceModifier.height(6.dp))
                    FieldSize.MEDIUM -> Spacer(modifier = GlanceModifier.height(9.dp))
                    FieldSize.EXTRA_LARGE -> TODO()
                }
                IconRow(leftIcon, iconColorLeft, clayout)
                Spacer(modifier = GlanceModifier.height(5.dp))
                NumberRow(leftNumber.take(5), zonecolor1, clayout, fieldsize, true)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleScreenSelector(
    selector: Int, showH: Boolean, leftNumber: Double, rightNumber: Double, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorFilter, iconColorRight: ColorFilter,
    zoneColorLeft: ColorProvider, zoneColorRight: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, isLeftInt: Boolean, isRightInt: Boolean, leftLabel: String, rightLabel: String, layout: FieldPosition, text: String, windDirection: Int, baseBitmap: Bitmap
) {
    val newLeft = if (!showH) formatNumber(leftNumber, isLeftInt)
    else when (selector) {
            0, 3 -> if (leftLabel == "IF") ((leftNumber * 10.0).roundToInt() / 10.0).toString().take(3) else formatNumber(leftNumber, true)
            else -> "0.0"
        }


    val newRight = if (!showH) formatNumber(rightNumber, isRightInt)
    else when (selector) {
            1, 3 -> if (rightLabel == "IF") ((rightNumber * 10.0).roundToInt() / 10.0).toString().take(3) else formatNumber(rightNumber, true)
            else -> "0.0"
        }


    val icon1 = if (selector == 0 || selector == 3) leftIcon else 1
    val icon2 = if (selector == 1 || selector == 3) rightIcon else 1
    val iconColor1 = if (selector == 0 || selector == 3) iconColorLeft else ColorFilter.tint(ColorProvider(Color.Black, Color.Black))
    val iconColor2 = if (selector == 1 || selector == 3) iconColorRight else ColorFilter.tint(ColorProvider(Color.Black, Color.Black))
    val zoneColor1 = if (selector == 0 || selector == 3) zoneColorLeft else ColorProvider(Color.Black, Color.Black)
    val zoneColor2 = if (selector == 1 || selector == 3) zoneColorRight else ColorProvider(Color.Black, Color.Black)

    if (!showH) {
        when (fieldSize) {
            FieldSize.SMALL -> DoubleTypesVerticalScreenSmall(newLeft, newRight, leftIcon, rightIcon, iconColorLeft, iconColorRight, zoneColorLeft, zoneColorRight, isKaroo3, layout)
            FieldSize.MEDIUM, FieldSize.LARGE -> DoubleTypesVerticalScreenBig(newLeft, newRight, leftIcon, rightIcon, iconColorLeft, iconColorRight, zoneColorLeft, zoneColorRight, isKaroo3, layout)
            FieldSize.EXTRA_LARGE -> TODO()
        }
    } else {
        DoubleTypesScreenHorizontal(newLeft, newRight, icon1, icon2, iconColor1, iconColor2, zoneColor1, zoneColor2, fieldSize, isKaroo3, layout, selector, text, windDirection, baseBitmap)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesScreenHorizontal(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorFilter, iconColorRight: ColorFilter,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, layout: FieldPosition, selector: Int, text: String, windDirection: Int, baseBitmap: Bitmap
) {

    VerticalDivider(true, fieldSize)
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {

        Row(modifier = GlanceModifier.fillMaxSize().let { if (isKaroo3) it.cornerRadius(8.dp) else it }) {
            Column(modifier = GlanceModifier.defaultWeight().background(if (selector in 1..2) ColorProvider(Color.White, Color.Black) else zoneColor1)) {
                when (selector) {
                    1, 2 -> HeadwindDirectionDoubleType(baseBitmap, windDirection, 38, text)
                    0 -> SingleHorizontalField(leftIcon, iconColorLeft, layout, fieldSize, zoneColor1, leftNumber, true)
                    else -> SingleHorizontalField(leftIcon, iconColorLeft, layout, fieldSize, zoneColor1, leftNumber, false)
                }
            }
            Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Column(modifier = GlanceModifier.defaultWeight().background(if (selector in listOf(0, 2)) ColorProvider(Color.White, Color.Black) else zoneColor2)) {
                when (selector) {
                    0, 2 -> HeadwindDirectionDoubleType(baseBitmap, windDirection, 38, text)
                    1 -> SingleHorizontalField(rightIcon, iconColorRight, layout, fieldSize, zoneColor2, rightNumber, true)
                    else -> SingleHorizontalField(rightIcon, iconColorRight, layout, fieldSize, zoneColor2, rightNumber, false)
                }
            }
        }
    }
    VerticalDivider(false, fieldSize)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesVerticalScreenSmall(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorFilter, iconColorRight: ColorFilter,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, isKaroo3: Boolean, layout: FieldPosition
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zoneColor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zoneColor1)) {
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(zoneColor1))
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, layout)
            }
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zoneColor2))
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor2)) {
                HorizontalScreenContent(rightNumber, rightIcon, iconColorRight, layout)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesVerticalScreenBig(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorFilter, iconColorRight: ColorFilter,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, isKaroo3: Boolean, layout: FieldPosition
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zoneColor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zoneColor1)) {
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(2.dp).background(zoneColor1))
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, layout)
            }
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(7.dp).background(zoneColor2))
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor2)) {
                HorizontalScreenContent(rightNumber, rightIcon, iconColorRight, layout)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun HeadwindDirectionDoubleType(baseBitmap: Bitmap, bearing: Int, fontSize: Int, overlayText: String) {
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