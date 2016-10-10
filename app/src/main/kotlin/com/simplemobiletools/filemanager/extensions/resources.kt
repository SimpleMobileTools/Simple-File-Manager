package com.simplemobiletools.filemanager.extensions

import android.content.res.Resources
import android.graphics.*

fun Resources.getColoredIcon(colorId: Int, resId: Int): Bitmap {
    val options = BitmapFactory.Options()
    options.inMutable = true
    val bitmap = BitmapFactory.decodeResource(this, resId, options)
    val paint = Paint()
    val filter = PorterDuffColorFilter(getColor(colorId), PorterDuff.Mode.SRC_IN)
    paint.colorFilter = filter
    val canvas = Canvas(bitmap)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return bitmap
}
