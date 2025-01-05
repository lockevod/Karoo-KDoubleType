package com.enderthor.kCustomField.datatype

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
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
fun VerticalDivider(istopfield:Boolean, fieldsize:FieldSize) {

    Box(modifier = if (istopfield) GlanceModifier.fillMaxWidth().height(10.dp) else if (fieldsize == FieldSize.LARGE) GlanceModifier.fillMaxWidth().height(28.dp) else GlanceModifier.fillMaxWidth().height(14.dp)) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                }
            }
            Spacer(
                modifier = GlanceModifier.fillMaxHeight().width(1.dp)
                    .background(ColorProvider(Color.Black, Color.White))
            )
            Column(
                modifier = GlanceModifier.defaultWeight(),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                }
            }
        }
    }
}


@Composable
fun IconRow(icon: Int, colorFilter: ColorFilter, clayout: FieldPosition) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = when (clayout.name) {
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
fun NumberRow(number: String, zonecolor: ColorProvider, clayout: FieldPosition, fieldsize: FieldSize ) {
    Row(
        modifier = if (fieldsize == FieldSize.LARGE) GlanceModifier.fillMaxHeight().fillMaxWidth().padding(bottom= 8.dp) else  GlanceModifier.fillMaxHeight().fillMaxWidth().padding(bottom= 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalAlignment = when (clayout.name) {
            "CENTER" -> Alignment.CenterHorizontally
            "RIGHT" -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        Text(
            text = number,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = if (number.length > 3) 32.sp else 38.sp,
                fontFamily = FontFamily.Monospace,
                color = ColorProvider(Color.Black, Color.White)
            )
        )
        Spacer(modifier = GlanceModifier.fillMaxHeight().width(2.dp).background(zonecolor))
    }
}

@Composable
fun HorizontalScreenContent(number: String, icon: Int, colorFilter: ColorFilter, clayout: FieldPosition, isVertical: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = when (clayout.name) {
            "CENTER" -> Alignment.CenterHorizontally
            "RIGHT" -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        if (clayout.name == "LEFT") {
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
                textAlign = when (clayout.name) {
                    "CENTER" -> TextAlign.Center
                    "RIGHT" -> TextAlign.End
                    else -> TextAlign.Start
                }
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        if (clayout.name != "LEFT") {
            Image(
                provider = ImageProvider(icon),
                contentDescription = "Icon",
                modifier = GlanceModifier.size(20.dp),
                colorFilter = colorFilter
            )
        }
        if (isVertical) Spacer(modifier = GlanceModifier.height(1.dp).background(ColorProvider(Color.Black, Color.White)))
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleScreenSelector(
    showh: Boolean, leftNumber: Double, rightNumber: Double, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorFilter, iconColorRight: ColorFilter, isVertical: Boolean,
    zonecolor1: ColorProvider, zonecolor2: ColorProvider, fieldsize: FieldSize,
    isKaroo3: Boolean, isLeftInt: Boolean, isRightInt: Boolean, leftlabel: String, rightlabel: String, clayout: FieldPosition
) {
   // Timber.d("IN DoubleScreenSelector zonecolor1 is $zonecolor1 and zonecolor2 is $zonecolor2")
    var newleft: String
    var newright: String

    if (showh) {

        val newleft = if (leftlabel == "IF") ((leftNumber * 10.0).roundToInt() / 10.0).toString().take(3) else leftNumber.roundToInt().toString()
        val newright = if (rightlabel == "IF") ((rightNumber * 10.0).roundToInt() / 10.0).toString().take(3) else rightNumber.roundToInt().toString()

        DoubleTypesScreenHorizontal(
            newleft, newright, leftIcon, rightIcon,
            iconColorLeft, iconColorRight, isVertical, zonecolor1, zonecolor2,
            fieldsize, isKaroo3, clayout
        )
    } else {
            newleft = formatNumber(leftNumber, isLeftInt)
            newright = formatNumber(rightNumber, isRightInt)

        when (fieldsize) {
            FieldSize.SMALL -> DoubleTypesVerticalScreenSmall(newleft, newright, leftIcon, rightIcon, iconColorLeft, iconColorRight, isVertical, zonecolor1, zonecolor2, isKaroo3, clayout)
            FieldSize.MEDIUM -> DoubleTypesVerticalScreenBig(newleft, newright, leftIcon, rightIcon, iconColorLeft, iconColorRight, isVertical, zonecolor1, zonecolor2, isKaroo3, clayout)
            FieldSize.LARGE -> DoubleTypesVerticalScreenBig(newleft, newright, leftIcon, rightIcon, iconColorLeft, iconColorRight, isVertical, zonecolor1, zonecolor2, isKaroo3, clayout)
            FieldSize.EXTRA_LARGE -> TODO()
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesScreenHorizontal(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorFilter, iconColorRight: ColorFilter, isVertical: Boolean,
    zonecolor1: ColorProvider, zonecolor2: ColorProvider, fieldsize: FieldSize,
    iskaroo3: Boolean, clayout: FieldPosition
) {
   // Timber.d("IN DoubleTypesScreen zonecolor1 is $zonecolor1 and zonecolor2 is $zonecolor2")
    if (isVertical) VerticalDivider(true,fieldsize)

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
                NumberRow(leftNumber.take(4), zonecolor1, clayout, fieldsize)
            }
            if (isVertical) Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Column(modifier = GlanceModifier.defaultWeight().background(zonecolor2)) {
                when (fieldsize) {
                    FieldSize.LARGE -> Spacer(modifier = GlanceModifier.height(12.dp))
                    FieldSize.SMALL -> Spacer(modifier = GlanceModifier.height(6.dp))
                    FieldSize.MEDIUM -> Spacer(modifier = GlanceModifier.height(9.dp))
                    FieldSize.EXTRA_LARGE -> TODO()
                }
                IconRow(rightIcon, iconColorRight, clayout)
                Spacer(modifier = GlanceModifier.height(5.dp))
                NumberRow(rightNumber.take(4), zonecolor2, clayout,fieldsize)
            }
        }
    }
    if (isVertical) VerticalDivider(false,fieldsize)

}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesVerticalScreenSmall(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorFilter, iconColorRight: ColorFilter, isVertical: Boolean,
    zonecolor1: ColorProvider, zonecolor2: ColorProvider, isKaroo3: Boolean, clayout: FieldPosition
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(
            modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zonecolor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zonecolor1)
        ) {
            Column(modifier = GlanceModifier.defaultWeight().background(zonecolor1)) {
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, clayout,isVertical)
            }
            if (isVertical) Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zonecolor2))
            Column(modifier = GlanceModifier.defaultWeight().background(zonecolor2)) {
                HorizontalScreenContent(rightNumber, rightIcon, iconColorRight, clayout,isVertical)
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesVerticalScreenBig(
    leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int,
    iconColorLeft: ColorFilter, iconColorRight: ColorFilter, isVertical: Boolean,
    zonecolor1: ColorProvider, zonecolor2: ColorProvider, isKaroo3: Boolean, clayout: FieldPosition
) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = 1.dp, end = 1.dp)) {
        Column(
            modifier = if (isKaroo3) GlanceModifier.fillMaxSize().background(zonecolor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zonecolor1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = GlanceModifier.defaultWeight().background(zonecolor1)) {
                HorizontalScreenContent(leftNumber, leftIcon, iconColorLeft, clayout, isVertical)
            }
            if (isVertical) Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(7.dp).background(zonecolor2))
            Column(modifier = GlanceModifier.defaultWeight().background(zonecolor2)) {
                HorizontalScreenContent(rightNumber, rightIcon, iconColorRight, clayout, isVertical)
            }
        }
    }
}