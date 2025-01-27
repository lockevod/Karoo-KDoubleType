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
fun NumberRow(number: String, zoneColor: ColorProvider, layout: FieldPosition, fieldSize: FieldSize, onlyOne: Boolean, isheadwind: Boolean, iszone: Boolean, textColor: ColorProvider) {
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
                color = if (iszone) textColor else ColorProvider(Color.Black, Color.White)
            )
        )
       Spacer(modifier = GlanceModifier.fillMaxHeight().width(2.dp).background(zoneColor))
    }
}


@Composable
fun OneIconRow(icon: Int, iconColor: ColorProvider, text:String, iszone: Boolean, fieldSize: FieldSize) {
    //Timber.d("OneIconRow text = $text icon = $icon iconColor = $iconColor iszone = $iszone fieldSize = $fieldSize")
    Row(
        modifier = if(fieldSize==FieldSize.SMALL) GlanceModifier.fillMaxWidth().height(31.dp) else GlanceModifier.fillMaxWidth().height(37.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = if(fieldSize==FieldSize.SMALL) GlanceModifier.height(20.dp).width(24.dp)else GlanceModifier.height(24.dp).width(24.dp)

            ){
            Image(
                provider = ImageProvider(icon),
                contentDescription = "Icon",
                modifier = if(fieldSize==FieldSize.SMALL) GlanceModifier.size(16.dp).padding(top = (-2).dp) else GlanceModifier.size(20.dp).padding(top = (-1).dp),
                colorFilter = ColorFilter.tint(iconColor)
                //ColorFilter.tint(ColorProvider(Color.Black, Color.White))
            )
        }
        Column(
            modifier = if(fieldSize==FieldSize.SMALL) GlanceModifier
                .height(32.dp).fillMaxWidth().padding(end=3.dp)else GlanceModifier
                .height(36.dp).fillMaxWidth().padding(end=3.dp),
            horizontalAlignment = Alignment.End,
            verticalAlignment = Alignment.CenterVertically) {
            Text(
                text =
                if (text.length > 10) {
                    val parts = text.split(" ", limit = 2)
                    if (parts.size > 1) "${parts[0]}\n${parts[1]}" else text}
                else text,
                maxLines = 2,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = if(fieldSize==FieldSize.SMALL) 15.sp else 18.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (iszone) iconColor else ColorProvider(Color.Black, Color.White),
                    textAlign = TextAlign.End
                )
            )
        }
    }
}

@Composable
fun OneNumberRow(number: String, layout: FieldPosition, fieldSize: FieldSize, textSize: Int, iszone: Boolean, textColor: ColorProvider) {
    val padding = if (fieldSize == FieldSize.LARGE) 7.dp  else 2.dp
    //Timber.d("OneNumberRow FieldSize = $fieldSize padding= $padding Text = $number")
    Row(
        modifier = GlanceModifier.fillMaxHeight().fillMaxWidth().padding(bottom =padding,end=4.dp),
        verticalAlignment = Alignment.CenterVertically ,
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
                fontSize = textSize.sp,
                fontFamily = FontFamily.Monospace,
                color = if (iszone) textColor else ColorProvider(Color.Black, Color.White)
            ),
            modifier = GlanceModifier.padding(top= -padding)
        )
    }

}

@Composable
fun HorizontalScreenContent(number: String, icon: Int, colorFilter: ColorProvider, layout: FieldPosition, iszone: Boolean) {
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
fun SingleHorizontalField(icon: Int, iconColor: ColorProvider, layout: FieldPosition, fieldSize: FieldSize, zoneColor: ColorProvider, number: String, isheadwind: Boolean, iszone: Boolean) {
    val height = when (fieldSize) {
        FieldSize.LARGE -> 12.dp
        FieldSize.SMALL -> 6.dp
        FieldSize.MEDIUM -> 9.dp
        FieldSize.EXTRA_LARGE -> TODO()
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
fun RollingFieldScreen(dNumber: Double, isInt: Boolean, action: KarooAction , iconColor: ColorProvider, zonecolor: ColorProvider, fieldsize: FieldSize, iskaroo3: Boolean, clayout: FieldPosition,windtext: String, winddiff: Int, baseBitmap: Bitmap,selector: Boolean,textSize:Int,iszone: Boolean,ispreview:Boolean)
{
    val icon = action.icon
    val label = action.label

    if(selector || (fieldsize == FieldSize.LARGE || fieldsize == FieldSize.EXTRA_LARGE))
    {
        val number = formatNumber(dNumber, isInt)
        Box(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier =  if (iskaroo3) GlanceModifier.fillMaxSize().cornerRadius(6.dp) else GlanceModifier.fillMaxSize())
            {
                Column(modifier = GlanceModifier.defaultWeight().background(zonecolor)) {
                    when (fieldsize) {

                        FieldSize.SMALL -> Spacer(modifier = GlanceModifier.height(2.dp))
                        FieldSize.MEDIUM -> Spacer(modifier = GlanceModifier.height(4.dp))
                        else -> Spacer(modifier = GlanceModifier.height(1.dp))

                    }
                    if (fieldsize == FieldSize.LARGE || fieldsize == FieldSize.EXTRA_LARGE) NotSupported("Size Not Supported", 24)
                    else {
                        OneIconRow(icon, iconColor, label.uppercase(),iszone,fieldsize)
                        //Spacer(modifier = GlanceModifier.height(1.dp))
                        OneNumberRow(number.take(6), clayout, fieldsize, (textSize * (if (ispreview) 0.8 else 1.0)).roundToInt(),iszone,iconColor)
                    }
                }
            }
        }
    }
    else HeadwindDirection(baseBitmap, winddiff, textSize, windtext)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleScreenSelector(
    selector: Int, showH: Boolean, leftNumber: Double, rightNumber: Double, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColorLeft: ColorProvider, zoneColorRight: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, isLeftInt: Boolean, isRightInt: Boolean, leftLabel: String, rightLabel: String, layout: FieldPosition, text: String, windDirection: Int, baseBitmap: Bitmap,iszoneLeft: Boolean,iszoneRight: Boolean
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
    val iconColor1 = if (selector == 0 || selector == 3) iconColorLeft else ColorProvider(Color.Black, Color.Black)
    val iconColor2 = if (selector == 1 || selector == 3) iconColorRight else ColorProvider(Color.Black, Color.Black)
    val zoneColor1 = if (selector == 0 || selector == 3) zoneColorLeft else ColorProvider(Color.Black, Color.Black)
    val zoneColor2 = if (selector == 1 || selector == 3) zoneColorRight else ColorProvider(Color.Black, Color.Black)

    if (!showH) {
        when (fieldSize) {
            FieldSize.SMALL -> DoubleTypesVerticalScreenSmall(newLeft, newRight, leftIcon, rightIcon, iconColorLeft, iconColorRight, zoneColorLeft, zoneColorRight, isKaroo3, layout,iszoneLeft,iszoneRight)
            FieldSize.MEDIUM, FieldSize.LARGE -> DoubleTypesVerticalScreenBig(newLeft, newRight, leftIcon, rightIcon, iconColorLeft, iconColorRight, zoneColorLeft, zoneColorRight, isKaroo3, layout,iszoneLeft,iszoneRight)
            FieldSize.EXTRA_LARGE -> NotSupported("Size Not Supported", 24)
        }
    } else {
        DoubleTypesScreenHorizontal(newLeft, newRight, icon1, icon2, iconColor1, iconColor2, zoneColor1, zoneColor2, fieldSize, isKaroo3, layout, selector, text, windDirection, baseBitmap,iszoneLeft,iszoneRight)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesScreenHorizontal(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, fieldSize: FieldSize,
    isKaroo3: Boolean, layout: FieldPosition, selector: Int, text: String, windDirection: Int, baseBitmap: Bitmap,iszoneLeft: Boolean,iszoneRight: Boolean
) {

    VerticalDivider(true, fieldSize)
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {

        Row(modifier = GlanceModifier.fillMaxSize().let { if (isKaroo3) it.cornerRadius(8.dp) else it }) {
            Column(modifier = GlanceModifier.defaultWeight().background(if (selector in 1..2) ColorProvider(Color.White, Color.Black) else zoneColor1)) {
                when (selector) {
                    1, 2 -> HeadwindDirectionDoubleType(baseBitmap, windDirection, 38, text)
                    0 -> SingleHorizontalField(leftIcon, iconColorLeft, layout, fieldSize, zoneColor1, leftNumber, true,iszoneLeft)
                    else -> SingleHorizontalField(leftIcon, iconColorLeft, layout, fieldSize, zoneColor1, leftNumber, false,iszoneLeft)
                }
            }
            Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Column(modifier = GlanceModifier.defaultWeight().background(if (selector in listOf(0, 2)) ColorProvider(Color.White, Color.Black) else zoneColor2)) {
                when (selector) {
                    0, 2 -> HeadwindDirectionDoubleType(baseBitmap, windDirection, 38, text)
                    1 -> SingleHorizontalField(rightIcon, iconColorRight, layout, fieldSize, zoneColor2, rightNumber, true,iszoneRight)
                    else -> SingleHorizontalField(rightIcon, iconColorRight, layout, fieldSize, zoneColor2, rightNumber, false,iszoneRight)
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
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, isKaroo3: Boolean, layout: FieldPosition, iszoneLeft: Boolean, iszoneRight: Boolean
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zoneColor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zoneColor1)) {
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(zoneColor1))
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, layout,iszoneLeft)
            }
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
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
fun DoubleTypesVerticalScreenBig(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorProvider, iconColorRight: ColorProvider,
    zoneColor1: ColorProvider, zoneColor2: ColorProvider, isKaroo3: Boolean, layout: FieldPosition,iszoneLeft: Boolean,iszoneRight: Boolean
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zoneColor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zoneColor1)) {
            Column(modifier = GlanceModifier.defaultWeight().background(zoneColor1)) {
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(2.dp).background(zoneColor1))
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, layout,iszoneLeft)
            }
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
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