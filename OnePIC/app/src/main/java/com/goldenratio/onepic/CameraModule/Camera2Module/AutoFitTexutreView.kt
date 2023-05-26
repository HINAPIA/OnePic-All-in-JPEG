package com.example.test_camera2.CameraHelper

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.View

class AutoFitTexutreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    fun setAspectRatio(width : Int, height: Int){
        if(width < 0 || height < 0){
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height
        Log.v("CameraFragment", "[setAspectRatio] AutoFitTextureView ratio width x height = ${ratioWidth} x ${ratioHeight}")
        requestLayout()
        Thread.sleep(500)
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