package com.socialaiassistant.keyboard.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class ThemePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var theme: KeyboardTheme = ThemePreset.socialAiNeon
    private var surface: KeyboardThemeSurface = KeyboardThemeSurface.ENGLISH

    fun setTheme(value: KeyboardTheme) = setPreview(value, surface)

    fun setPreview(value: KeyboardTheme, surface: KeyboardThemeSurface) {
        theme = value.normalized()
        this.surface = surface
        invalidate()
    }

    internal fun previewSurfaceForTest(): KeyboardThemeSurface = surface

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(dp(260))
        setMeasuredDimension(width, resolveSize(dp(210), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = theme
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = t.rootBackground
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), dpF(18f), dpF(18f), paint)

        val pad = dpF(12f)
        val contentWidth = width - pad * 2
        drawBar(canvas, pad, pad, contentWidth, dpF(34f), t.toolbarSurface, t.primaryNeon)
        drawLabel(canvas, surface.displayName, pad + dpF(12f), pad + dpF(22f), t.textPrimary, 10.5f, alignLeft = true)

        val rows = rowsFor(surface)
        val keysTop = pad + dpF(46f)
        val bottom = height - pad
        val rowGap = dpF(5f)
        val available = (bottom - keysTop - rowGap * (rows.size - 1)).coerceAtLeast(dpF(60f))
        val keyHeight = available / rows.size

        rows.forEachIndexed { rowIndex, labels ->
            val keyGap = dpF(5f)
            val keyWidth = (contentWidth - keyGap * (labels.size - 1)) / labels.size
            labels.forEachIndexed { colIndex, label ->
                val special = label == "↵" || label == "space" || surface == KeyboardThemeSurface.SETTINGS
                val fill = if (special) t.specialKeySurface else t.keySurface
                val accent = if (label == "↵") t.actionAccent else if (special) t.secondaryNeon else t.primaryNeon
                val rect = RectF(
                    pad + colIndex * (keyWidth + keyGap),
                    keysTop + rowIndex * (keyHeight + rowGap),
                    pad + colIndex * (keyWidth + keyGap) + keyWidth,
                    keysTop + rowIndex * (keyHeight + rowGap) + keyHeight
                )
                drawMiniButton(canvas, rect, fill, accent, special)
                val labelColor = if (special) t.specialKeyLabel else t.keyLabel
                val textSize = when {
                    label.length > 8 -> 7.5f
                    label.length > 4 -> 8.5f
                    else -> 10.5f
                }
                drawCenteredLabel(canvas, label, rect, labelColor, textSize * t.keyLabelScale)
            }
        }
    }

    private fun rowsFor(surface: KeyboardThemeSurface): List<List<String>> = when (surface) {
        KeyboardThemeSurface.ENGLISH -> listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM").map { row -> row.map(Char::toString) }
        KeyboardThemeSurface.NUMBER -> listOf(
            listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("0", ".", "↵")
        )
        KeyboardThemeSurface.SYMBOL -> listOf(
            listOf("@", "#", "\$", "%"), listOf("&", "*", "(", ")"), listOf("+", "-", "=", "/")
        )
        KeyboardThemeSurface.BANGLA -> listOf(
            listOf("অ", "আ", "ই", "ঈ"), listOf("ক", "খ", "গ", "ঘ"), listOf("ত", "থ", "দ", "ধ")
        )
        KeyboardThemeSurface.PHONETIC -> listOf(
            listOf("a", "m", "i", "t"), listOf("আমি", "তুমি", "কি"), listOf("space", "↵")
        )
        KeyboardThemeSurface.BIJOY -> listOf(
            listOf("ক", "ি", "া", "র"), listOf("ে", "ন", "ম", "ত"), listOf("space", "↵")
        )
        KeyboardThemeSurface.SETTINGS -> listOf(
            listOf("Theme Pack"), listOf("Customize"), listOf("Background"), listOf("Reset")
        )
    }

    private fun drawBar(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, fill: Int, stroke: Int) {
        val rect = RectF(x, y, x + w, y + h)
        paint.style = Paint.Style.FILL
        paint.shader = gradientShader(rect, theme.panelGradientStart, theme.panelGradientEnd)
        paint.color = fill
        canvas.drawRoundRect(rect, dpF(10f), dpF(10f), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1f)
        paint.color = stroke
        canvas.drawRoundRect(rect, dpF(10f), dpF(10f), paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawMiniButton(canvas: Canvas, rect: RectF, fill: Int, stroke: Int, special: Boolean) {
        val radius = min(dpF(theme.keyCornerRadiusDp), rect.height() / 2f)
        paint.style = Paint.Style.FILL
        paint.shader = if (theme.fillStyle == ThemeFillStyle.GRADIENT) {
            if (special) gradientShader(rect, theme.specialGradientStart, theme.specialGradientEnd)
            else gradientShader(rect, theme.keyGradientStart, theme.keyGradientEnd)
        } else null
        paint.color = fill
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1.2f)
        paint.color = stroke
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.style = Paint.Style.FILL
    }

    private fun gradientShader(rect: RectF, start: Int, end: Int): Shader? =
        if (theme.fillStyle == ThemeFillStyle.GRADIENT) {
            LinearGradient(rect.left, rect.top, rect.right, rect.bottom, start, end, Shader.TileMode.CLAMP)
        } else {
            null
        }

    private fun drawCenteredLabel(canvas: Canvas, label: String, rect: RectF, color: Int, sizeSp: Float) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = spF(sizeSp)
        val y = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(label, rect.centerX(), y, paint)
    }

    private fun drawLabel(
        canvas: Canvas,
        label: String,
        x: Float,
        y: Float,
        color: Int,
        sizeSp: Float,
        alignLeft: Boolean
    ) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textAlign = if (alignLeft) Paint.Align.LEFT else Paint.Align.CENTER
        paint.textSize = spF(sizeSp)
        canvas.drawText(label, x, y, paint)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dpF(value: Float): Float = value * resources.displayMetrics.density
    private fun spF(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
