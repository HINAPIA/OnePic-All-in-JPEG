/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.test_camera2.CameraHelper

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.View

/**
 * A [TextureView] that can be adjusted to a specified aspect ratio.
 */
class AutoFitTexutreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int  = 0
) : TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width : Int, height: Int){
        if(width < 0 || height < 0){
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height
        Log.v("CameraFragment", "[setAspectRatio] AutoFitTextureView ratio width x height = ${ratioWidth} x ${ratioHeight}")
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.v("CameraFragment", "[onMeasure] MeasureSpec width x height = ${widthMeasureSpec} x ${heightMeasureSpec}")
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        Log.v("CameraFragment", "[onMeasure] width x height = ${width} x ${height}")
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                Log.v("CameraFragment", "[onMeasure] height * ratioWidth / ratioHeight = ${height * ratioWidth / ratioHeight}")
                Log.v("CameraFragment", "[onMeasure] width * ratioHeight / ratioWidth = ${width * ratioHeight / ratioWidth}")
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            } else {
                Log.v("CameraFragment", "[onMeasure] height * ratioWidth / ratioHeight = ${height * ratioWidth / ratioHeight}")
                Log.v("CameraFragment", "[onMeasure] width * ratioHeight / ratioWidth = ${width * ratioHeight / ratioWidth}")
                setMeasuredDimension(height * ratioWidth / ratioHeight, height)
            }
        }
    }
}