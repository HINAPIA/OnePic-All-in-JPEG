package com.goldenratio.onepic.EditModule

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.renderscript.Allocation
import androidx.renderscript.Element
import androidx.renderscript.RenderScript
import androidx.renderscript.ScriptIntrinsicBlur


object BlurBitmapUtil {

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun blur(context: Context, image: Bitmap, blurRadius: Float = 18F): Bitmap {
        val bitmap = Bitmap.createBitmap(
            image.width, image.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(/* bitmap = */ bitmap)
        val paint = Paint().apply {
            flags = Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG
        }

        canvas.drawBitmap(image, 0f, 0f, paint)

        val rsContext: RenderScript = RenderScript.create(context)
        val input = Allocation.createFromBitmap(
            rsContext, bitmap, Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )
        val output = Allocation.createTyped(rsContext, input.type)

        val blurScript = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
        blurScript.setInput(input)
        blurScript.setRadius(blurRadius)
        blurScript.forEach(output)

        output.copyTo(bitmap)

        rsContext.destroy()

        return bitmap
    }
}