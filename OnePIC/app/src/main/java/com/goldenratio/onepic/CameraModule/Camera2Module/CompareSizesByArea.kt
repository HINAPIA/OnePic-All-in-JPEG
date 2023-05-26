package com.example.test_camera2.CameraHelper

import android.util.Size
import java.lang.Long
import java.util.Comparator

internal class CompareSizesByArea : Comparator<Size> {

    // We cast here to ensure the multiplications won't overflow
    override fun compare(lhs: Size, rhs: Size) =
        Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)

}