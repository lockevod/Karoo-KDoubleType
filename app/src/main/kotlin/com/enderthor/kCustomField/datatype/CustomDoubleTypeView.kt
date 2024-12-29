package com.enderthor.kCustomField.datatype

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import timber.log.Timber
import kotlin.math.roundToInt


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesScreen(leftNumber: Int, rightNumber: Int, leftIcon: Int, rightIcon: Int, iconColorLeft: ColorFilter, iconColorRight: ColorFilter, isVertical: Boolean, zonecolor1: ColorProvider, zonecolor2: ColorProvider, isbigfield: Boolean, iskaroo3: Boolean, clayout: FieldPosition) {


   // Timber.d("NumberWithIcon isvertical: $isVertical")
    if (isVertical) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(8.dp)) {
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
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .padding(start = 1.dp, end = 1.dp)
    ) {
        Row(
            modifier = if (iskaroo3) GlanceModifier.fillMaxSize().cornerRadius(8.dp) else GlanceModifier.fillMaxSize(),
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight().background(zonecolor1),
            ) {
                if(isbigfield)  Spacer(modifier = GlanceModifier.height(7.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = if (clayout.name=="CENTER") Alignment.CenterHorizontally else if (clayout.name=="RIGHT") Alignment.End else Alignment.Start
                ) {
                    Image(
                        provider = ImageProvider(leftIcon),
                        contentDescription = "Left Icon",
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = iconColorLeft
                    )
                }
                Spacer(modifier = GlanceModifier.height(5.dp))
                Row(
                    modifier = GlanceModifier.fillMaxHeight().fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalAlignment =  if (clayout.name=="CENTER") Alignment.CenterHorizontally else if (clayout.name=="RIGHT") Alignment.End else Alignment.Start

                ) {
                    Text(
                        text = leftNumber.toString().take(4),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (leftNumber.toString().length > 3) 32.sp else 38.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorProvider(Color.Black, Color.White),
                        )
                    )
                    Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(zonecolor1))
                }
            }

            if (isVertical) Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Column(
                modifier = GlanceModifier.defaultWeight().background(zonecolor2),
            ) {
                if(isbigfield)  Spacer(modifier = GlanceModifier.height(7.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment =  if (clayout.name=="CENTER") Alignment.CenterHorizontally else if (clayout.name=="RIGHT") Alignment.End else Alignment.Start
                ) {
                    Image(
                        provider = ImageProvider(rightIcon),
                        contentDescription = "Right Icon",
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = iconColorRight
                    )
                }
                Spacer(modifier = GlanceModifier.height(5.dp))
                Row(
                    modifier = GlanceModifier.fillMaxHeight().fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalAlignment =  if (clayout.name=="CENTER") Alignment.CenterHorizontally else if (clayout.name=="RIGHT") Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = rightNumber.toString().take(4),
                        style = TextStyle(
                            fontSize = if (leftNumber.toString().length > 3) 32.sp else 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ColorProvider(Color.Black, Color.White)
                        )
                    )
                    Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(zonecolor2))
                }
            }
        }
    }
    if (isVertical) {
        Box(modifier= if (isbigfield) GlanceModifier.fillMaxWidth().height(28.dp) else GlanceModifier.fillMaxWidth().height(14.dp) ) {
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
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesVerticalScreen(leftNumber: Double, rightNumber: Double, leftIcon: Int, rightIcon: Int, iconColorLeft: ColorFilter, iconColorRight: ColorFilter, isVertical: Boolean, zonecolor1: ColorProvider, zonecolor2: ColorProvider, isbigfield: Boolean,isKaroo3: Boolean,isLeftInt: Boolean, isRightInt: Boolean, clayout: FieldPosition) {


    val newleft = if (isLeftInt) leftNumber.roundToInt().toString().take(5) else String.format("%.1f", leftNumber).take(5)
    val newright = if (isRightInt) rightNumber.roundToInt().toString().take(5) else String.format("%.1f", rightNumber).take(5)

    if (isbigfield) DoubleTypesVerticalScreen1(newleft, newright, leftIcon, rightIcon, iconColorLeft, iconColorRight, isVertical, zonecolor1, zonecolor2, isKaroo3,clayout)
    else DoubleTypesVerticalScreen2(newleft, newright, leftIcon, rightIcon, iconColorLeft, iconColorRight, isVertical, zonecolor1, zonecolor2, isKaroo3,clayout)
}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesVerticalScreen1(leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int, iconColorLeft: ColorFilter, iconColorRight: ColorFilter, isVertical: Boolean, zonecolor1: ColorProvider, zonecolor2: ColorProvider, isKaroo3: Boolean, clayout: FieldPosition) {
        Timber.d("Isvertical is $isVertical")
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .padding(start = 1.dp, end = 1.dp)
        ) {
            Column(
                modifier= if (isKaroo3) GlanceModifier.fillMaxSize().background(zonecolor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zonecolor1),
            ) {
                Column(
                    modifier = GlanceModifier.defaultWeight().background(zonecolor1),
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = if (clayout.name=="LEFT") Alignment.Start else Alignment.End
                    ) {
                        Image(
                            provider = ImageProvider(leftIcon),
                            contentDescription = "Left Icon",
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = iconColorLeft
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalAlignment = if (clayout.name=="CENTER") Alignment.CenterHorizontally else if (clayout.name=="RIGHT") Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = leftNumber,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 38.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ColorProvider(Color.Black, Color.White)
                            )
                        )
                    }

                }
                if (isVertical)
                    Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
                Spacer(modifier = GlanceModifier.fillMaxWidth().height(3.dp).background(zonecolor2))
                Column(
                    modifier = GlanceModifier.defaultWeight().background(zonecolor2),
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = if (clayout.name=="LEFT") Alignment.Start else Alignment.End
                    ) {
                        Image(
                            provider = ImageProvider(rightIcon),
                            contentDescription = "Right Icon",
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = iconColorRight
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalAlignment = if (clayout.name=="CENTER") Alignment.CenterHorizontally else if (clayout.name=="RIGHT") Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = rightNumber,
                            style = TextStyle(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ColorProvider(Color.Black, Color.White)
                            )
                        )
                    }
                }
            }
        }
    }

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun DoubleTypesVerticalScreen2(leftNumber: String, rightNumber: String, leftIcon: Int, rightIcon: Int, iconColorLeft: ColorFilter, iconColorRight: ColorFilter, isVertical: Boolean, zonecolor1: ColorProvider, zonecolor2: ColorProvider,isKaroo3: Boolean, clayout: FieldPosition) {
    //Timber.d("Iskaroo3 is $isKaroo3")
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .padding(start = 1.dp, end = 1.dp)
    ) {
        Column(
            modifier= if (isKaroo3) GlanceModifier.fillMaxSize().background(zonecolor1).cornerRadius(8.dp) else GlanceModifier.fillMaxSize().background(zonecolor1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,

        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = if (clayout.name=="CENTER") Alignment.CenterHorizontally else if (clayout.name=="RIGHT") Alignment.End else Alignment.Start
            ) {
                if (clayout.name == "LEFT") {
                    Image(
                        provider = ImageProvider(leftIcon),
                        contentDescription = "Left Icon",
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = iconColorLeft
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }
                Text(
                    text = leftNumber,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorProvider(Color.Black, Color.White),
                        textAlign = if (clayout.name == "CENTER") TextAlign.Center else if (clayout.name == "RIGHT") TextAlign.End else TextAlign.Start
                    ),
                    modifier = GlanceModifier.defaultWeight(),

                    )
                if (clayout.name != "LEFT") {
                    Image(
                        provider = ImageProvider(leftIcon),
                        contentDescription = "Left Icon",
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = iconColorLeft
                    )
                }
                if (isVertical) Spacer(modifier = GlanceModifier.height(1.dp).background(ColorProvider(Color.Black, Color.White)))
            }
            if (isVertical) Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(5.dp).background(zonecolor2))
            Row(
                modifier = GlanceModifier.fillMaxWidth().background(zonecolor2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (clayout.name =="LEFT") {
                    Image(
                        provider = ImageProvider(rightIcon),
                        contentDescription = "Right Icon",
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = iconColorRight
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }
                Text(
                    text = rightNumber,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ColorProvider(Color.Black, Color.White),
                        textAlign = if (clayout.name=="CENTER") TextAlign.Center else if (clayout.name=="RIGHT") TextAlign.End else TextAlign.Start
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (clayout.name !="LEFT") {
                    Image(
                        provider = ImageProvider(rightIcon),
                        contentDescription = "Right Icon",
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = iconColorRight
                    )
                }
            }
        }
    }
}
