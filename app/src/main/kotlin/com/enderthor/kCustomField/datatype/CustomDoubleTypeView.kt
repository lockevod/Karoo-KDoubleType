package com.enderthor.kCustomField.datatype

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.unit.ColorProvider
import timber.log.Timber


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun NumberWithIcon(leftNumber: Int, rightNumber: Int, leftIcon: Int, rightIcon: Int, iconColorLeft: ColorFilter, iconColorRight: ColorFilter,isVertical:Boolean) {

    Timber.d("NumberWithIcon isvertical: $isVertical")
    if (isVertical) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(8.dp))
        {
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
        .padding(start = 3.dp,  end = 3.dp)

    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
            ) {
                Row(
                    modifier = GlanceModifier,
                    verticalAlignment = Alignment.CenterVertically,
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
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = leftNumber.toString().take(3),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ColorProvider(Color.Black, Color.White)
                        )
                    )
                }
            }
           if(isVertical) Spacer(modifier = GlanceModifier.fillMaxHeight().width(1.dp).background(ColorProvider(Color.Black, Color.White)))
            Column(
                modifier = GlanceModifier.defaultWeight(),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.End,
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
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = rightNumber.toString().take(3),
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
    if (isVertical) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(14.dp))
        {
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