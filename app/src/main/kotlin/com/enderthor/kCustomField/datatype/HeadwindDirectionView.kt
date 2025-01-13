package com.enderthor.kCustomField.datatype

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.roundToInt

// This file is from the KarooHeadwind project by Tim Kluge
data class BitmapWithBearing(val bitmap: Bitmap, val bearing: Int)

val bitmapsByBearing = mutableMapOf<BitmapWithBearing, Bitmap>()


fun getArrowBitmapByBearing(baseBitmap: Bitmap, bearing: Int): Bitmap {
    synchronized(bitmapsByBearing) {
        val bearingRounded = (((bearing + 360) / 10.0).roundToInt() * 10) % 360

        val bitmapWithBearing = BitmapWithBearing(baseBitmap, bearingRounded)
        val storedBitmap = bitmapsByBearing[bitmapWithBearing]
        if (storedBitmap != null) return storedBitmap

        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
//            strokeWidth = 15f
            isAntiAlias = true
        }

        canvas.save()
        canvas.scale((bitmap.width / baseBitmap.width.toFloat()), (bitmap.height / baseBitmap.height.toFloat()), (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
        canvas.rotate(bearing.toFloat(), (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
        canvas.drawBitmap(baseBitmap, ((bitmap.width - baseBitmap.width) / 2).toFloat(), ((bitmap.height - baseBitmap.height) / 2).toFloat(), paint)
        canvas.restore()

        bitmapsByBearing[bitmapWithBearing] = bitmap

        return bitmap
    }
}