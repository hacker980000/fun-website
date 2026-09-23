package com.socialaiassistant.keyboard.settingsui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class SettingsThemePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pack: SettingsThemePack = SettingsThemePack.CLEAN_MODERN

    fun setPack(value: SettingsThemePack) {
        pack = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val spec = SettingsThemeCatalog.spec(pack)
        val widthF = width.toFloat()
        val heightF = height.toFloat()
        val radius = min(widthF, heightF) * 0.06f

        paint.color = spec.rootTop
        canvas.drawRoundRect(RectF(0f, 0f, widthF, heightF), radius, radius, paint)

        when (pack) {
            SettingsThemePack.CLEAN_MODERN -> drawClean(canvas, spec, widthF, heightF)
            SettingsThemePack.CARD_STYLE -> drawCards(canvas, spec, widthF, heightF)
            SettingsThemePack.PREMIUM -> drawPremium(canvas, spec, widthF, heightF)
            SettingsThemePack.PRO_STYLE -> drawPro(canvas, spec, widthF, heightF)
        }
    }

    private fun drawClean(canvas: Canvas, spec: SettingsThemeSpec, w: Float, h: Float) {
        drawPanel(canvas, spec.panelAlt, spec.border, 0.04f * w, 0.10f * h, 0.32f * w, 0.82f * h, 12f)
        paint.color = spec.accent2
        canvas.drawCircle(0.18f * w, 0.31f * h, 0.07f * h, paint)
        drawTextBar(canvas, spec.textPrimary, 0.10f * w, 0.48f * h, 0.22f * w, 0.035f * h)
        drawTextBar(canvas, spec.textSecondary, 0.10f * w, 0.56f * h, 0.16f * w, 0.022f * h)

        drawPanel(canvas, spec.panel, spec.border, 0.35f * w, 0.10f * h, 0.96f * w, 0.82f * h, 12f)
        repeat(4) { index ->
            val top = (0.17f + index * 0.15f) * h
            drawPanel(canvas, spec.row, spec.border, 0.39f * w, top, 0.92f * w, top + 0.10f * h, 8f)
            paint.color = spec.categoryAccents[index]
            canvas.drawRoundRect(RectF(0.42f * w, top + 0.02f * h, 0.47f * w, top + 0.08f * h), 5f, 5f, paint)
            drawTextBar(canvas, spec.textPrimary, 0.50f * w, top + 0.03f * h, 0.18f * w, 0.018f * h)
        }
    }

    private fun drawCards(canvas: Canvas, spec: SettingsThemeSpec, w: Float, h: Float) {
        val left = 0.08f * w
        val gap = 0.035f * w
        val cardW = 0.39f * w
        val cardH = 0.20f * h
        repeat(6) { index ->
            val col = index % 2
            val row = index / 2
            val l = left + col * (cardW + gap)
            val t = 0.10f * h + row * (cardH + 0.055f * h)
            drawPanel(canvas, spec.panelAlt, spec.categoryAccents[index], l, t, l + cardW, t + cardH, 10f)
            paint.color = spec.categoryAccents[index]
            canvas.drawRoundRect(RectF(l + 0.04f * w, t + 0.05f * h, l + 0.10f * w, t + 0.13f * h), 6f, 6f, paint)
            drawTextBar(canvas, spec.textPrimary, l + 0.13f * w, t + 0.07f * h, 0.17f * w, 0.018f * h)
        }
    }

    private fun drawPremium(canvas: Canvas, spec: SettingsThemeSpec, w: Float, h: Float) {
        drawPanel(canvas, spec.panel, spec.border, 0.06f * w, 0.08f * h, 0.94f * w, 0.88f * h, 13f)
        repeat(4) { index ->
            val t = (0.16f + index * 0.17f) * h
            paint.color = spec.categoryAccents[index]
            canvas.drawRoundRect(RectF(0.11f * w, t, 0.19f * w, t + 0.10f * h), 6f, 6f, paint)
            drawTextBar(canvas, spec.textPrimary, 0.23f * w, t + 0.02f * h, 0.34f * w, 0.022f * h)
            drawTextBar(canvas, spec.textSecondary, 0.23f * w, t + 0.06f * h, 0.47f * w, 0.014f * h)
            drawTextBar(canvas, spec.textSecondary, 0.84f * w, t + 0.035f * h, 0.03f * w, 0.018f * h)
        }
    }

    private fun drawPro(canvas: Canvas, spec: SettingsThemeSpec, w: Float, h: Float) {
        drawPanel(canvas, spec.panel, spec.border, 0.04f * w, 0.08f * h, 0.70f * w, 0.88f * h, 12f)
        repeat(4) { index ->
            val t = (0.15f + index * 0.16f) * h
            paint.color = spec.categoryAccents[index]
            canvas.drawRoundRect(RectF(0.09f * w, t, 0.16f * w, t + 0.09f * h), 6f, 6f, paint)
            drawTextBar(canvas, spec.textPrimary, 0.20f * w, t + 0.02f * h, 0.24f * w, 0.02f * h)
            drawTextBar(canvas, spec.textSecondary, 0.20f * w, t + 0.06f * h, 0.35f * w, 0.013f * h)
        }
        drawPanel(canvas, spec.panelAlt, spec.accent2, 0.73f * w, 0.08f * h, 0.96f * w, 0.88f * h, 12f)
        drawTextBar(canvas, spec.textPrimary, 0.77f * w, 0.31f * h, 0.14f * w, 0.022f * h)
        drawTextBar(canvas, spec.textPrimary, 0.77f * w, 0.40f * h, 0.12f * w, 0.022f * h)
        drawTextBar(canvas, spec.textPrimary, 0.77f * w, 0.49f * h, 0.08f * w, 0.022f * h)
    }

    private fun drawPanel(canvas: Canvas, fill: Int, border: Int, l: Float, t: Float, r: Float, b: Float, radius: Float) {
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawRoundRect(RectF(l, t, r, b), radius, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * resources.displayMetrics.density
        paint.color = border
        canvas.drawRoundRect(RectF(l, t, r, b), radius, radius, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawTextBar(canvas: Canvas, color: Int, l: Float, t: Float, width: Float, height: Float) {
        paint.color = color
        canvas.drawRoundRect(RectF(l, t, l + width, t + height), height / 2f, height / 2f, paint)
    }
}
