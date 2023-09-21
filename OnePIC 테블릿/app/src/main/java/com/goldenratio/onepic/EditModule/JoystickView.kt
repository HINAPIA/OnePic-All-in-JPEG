package com.goldenratio.onepic.EditModule

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.goldenratio.onepic.R
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("ResourceType")
class JoystickView(context: Context?, attrs: AttributeSet?) : View(context, attrs), Runnable {
    private val basePaint: Paint = Paint()
    private val stickPaint: Paint = Paint()
    private var baseColor: Int = 0
    private var stickColor: Int = 0
    private var moveListener: OnMoveListener? = null
    private var useSpring = true
    private var mThread = Thread(this)
    private var moveUpdateInterval = DEFAULT_UPDATE_INTERVAL

    private var mPosX: Float = 0.0f
    private var mPosY: Float = 0.0f

    private var mCenterX: Float = 0.0f
    private var mCenterY: Float = 0.0f

    private var stickRatio = 0.0f
    private var baseRatio = 0.0f

    private var stickRadius = 0.0f
    private var baseRadius = 0.0f

    init {
        context?.theme?.obtainStyledAttributes(
            attrs,
            R.styleable.Joystick,
            0, 0
        )?.apply {

            try {
                baseColor = getColor(R.styleable.Joystick_joystickBaseColor, Color.LTGRAY)
                stickColor = getColor(R.styleable.Joystick_joystickStickColor,Color.GRAY)
                stickRatio = getFraction(R.styleable.Joystick_joystickStickRatio, 1, 1, 0.45f)
                baseRatio = getFraction(R.styleable.Joystick_joystickBaseRatio, 1, 1, 0.85f)
                useSpring = getBoolean(R.styleable.Joystick_joystickUseSpring, true)
            } finally {
                recycle()
            }

            basePaint.isAntiAlias = true
            basePaint.color = baseColor
            basePaint.style = Paint.Style.FILL

            stickPaint.isAntiAlias = true
            stickPaint.color = stickColor
            stickPaint.style = Paint.Style.FILL
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mPosX = (width / 2).toFloat()
        mPosY = (width / 2).toFloat()
        mCenterX = mPosX
        mCenterY = mPosY

        val d = w.coerceAtMost(h)
        stickRadius = d / 2 * stickRatio
        baseRadius = d / 2 * baseRatio
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mPosX = event!!.x
        mPosY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (mThread.isAlive)
                    mThread.interrupt()
                mThread = Thread(this)
                mThread.start()
                moveListener?.onMove(getAngle(), getStrength())
            }
            MotionEvent.ACTION_UP -> {
                mThread.interrupt()
                if (useSpring) {
                    mPosX = mCenterX
                    mPosY = mCenterY
                    moveListener?.onMove(getAngle(), getStrength())
                }
            }
        }

        val length = sqrt((mPosX - mCenterX).pow(2) + (mPosY - mCenterY).pow(2))

        if (length > baseRadius) {
            // length:radius = (mPosX - mCenterX):new mPosX
            // length:radius = (mPosY - mCenterY):new mPosY
            mPosX = (mPosX - mCenterX) * baseRadius / length + mCenterX
            mPosY = (mPosY - mCenterY) * baseRadius / length + mCenterY
        }

        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawCircle((width / 2).toFloat(), (width / 2).toFloat(), baseRadius, basePaint)
        canvas?.drawCircle(mPosX, mPosY, stickRadius, stickPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // setting the measured values to resize the view to a certain width and height
        val d: Int = measure(widthMeasureSpec).coerceAtMost(measure(heightMeasureSpec))
        setMeasuredDimension(d, d)
    }

    private fun measure(measureSpec: Int): Int {
        return if (MeasureSpec.getMode(measureSpec) == MeasureSpec.UNSPECIFIED) {
            // if no bounds are specified return a default size (200)
            DEFAULT_SIZE
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            MeasureSpec.getSize(measureSpec)
        }
    }

    private fun getAngle(): Int {
        val xx = mPosX - mCenterX
        val yy = mCenterY - mPosY
        val angle = Math.toDegrees(atan2(yy, xx).toDouble()).toInt()
        // The bottom of the horizontal line is a negative value.
        return if (angle < 0) angle + 360 else angle
    }

    private fun getStrength(): Int {
        val length = sqrt((mPosX - mCenterX).pow(2) + (mPosY - mCenterY).pow(2))
        return (length / baseRadius * 100).toInt()
    }


    override fun run() {
        while (!Thread.interrupted()) {
            post {
                moveListener?.onMove(getAngle(), getStrength())
            }
            try {
                Thread.sleep(moveUpdateInterval.toLong())
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    fun setOnMoveListener(listener: OnMoveListener, intervalMs: Int) {
        moveListener = listener
        moveUpdateInterval = intervalMs
    }

    fun setOnMoveListener(listener: OnMoveListener) {
        setOnMoveListener(listener, DEFAULT_UPDATE_INTERVAL)
    }

    fun interface OnMoveListener {
        fun onMove(x: Int, y: Int)
    }

    companion object {
        private val TAG = JoystickView::class.java.simpleName
        private const val DEFAULT_SIZE = 200
        private const val DEFAULT_UPDATE_INTERVAL = 50
    }
}