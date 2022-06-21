package com.jnj.vaccinetracker.register.screens

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * @author maartenvangiel
 * @version 1
 */
class TakePictureCutoutView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private companion object {
        private const val OVAL_SIZE_RATIO = 0.67f
        private const val OVAL_HEIGHT_PERCENT = 0.6f
        private const val STROKE_WIDTH_DP = 2f
        private const val DARK_OVERLAY_ALPHA = 64
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(DARK_OVERLAY_ALPHA, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * STROKE_WIDTH_DP
    }

    private val rectWithCutoutPath = Path()
    private var ovalRectF: RectF = RectF(0f, 0f, 0f, 0f)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val middleX = w / 2f
        val middleY = h / 2f
        val ovalHeight = h * OVAL_HEIGHT_PERCENT
        val ovalWidth = ovalHeight * OVAL_SIZE_RATIO

        ovalRectF = RectF(middleX - (ovalWidth / 2f), middleY - (ovalHeight / 2f), middleX + (ovalWidth / 2f), middleY + (ovalHeight / 2f))

        rectWithCutoutPath.reset()
        rectWithCutoutPath.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
        rectWithCutoutPath.addOval(ovalRectF, Path.Direction.CCW)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(rectWithCutoutPath, shadowPaint)
        canvas.drawOval(ovalRectF, outlinePaint)
    }

}