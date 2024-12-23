package com.enderthor.kCustomField.datatype

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.fillMaxHeight

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun NumberWithIcon(leftNumber: Int, rightNumber: Int, leftIcon: Int, rightIcon: Int) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(start = 3.dp, top = 10.dp, end = 3.dp, bottom = 15.dp),

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
                        colorFilter = ColorFilter.tint(ColorProvider(day = Color.Green, night = Color.Green))
                    )
                }
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
                        colorFilter = ColorFilter.tint(ColorProvider(day = Color.Green, night = Color.Green))
                    )
                }
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
}