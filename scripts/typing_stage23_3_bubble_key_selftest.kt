import com.socialaiassistant.keyboard.ime.BubbleKeyPolicy
import com.socialaiassistant.keyboard.ime.BubbleKeyRequest
import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity

private var passed = 0
private fun checkCase(name: String, condition: Boolean) {
    if (!condition) error("FAIL: $name")
    passed++
    println("PASS: $name")
}

fun main() {
    checkCase("default intensity normal", BubbleKeyIntensity.fromSetting(null) == BubbleKeyIntensity.NORMAL)
    checkCase("invalid intensity normal", BubbleKeyIntensity.fromSetting("wat") == BubbleKeyIntensity.NORMAL)

    val base = BubbleKeyRequest(
        enabled = true,
        label = "a",
        sensitiveField = false,
        glideGesture = false,
        animationsEnabled = true,
        letterLayer = true,
        intensity = BubbleKeyIntensity.NORMAL
    )

    checkCase("english letter eligible", BubbleKeyPolicy.resolve(base)?.label == "a")
    checkCase("uppercase letter eligible", BubbleKeyPolicy.resolve(base.copy(label = "Z")) != null)
    checkCase("bangla letter eligible", BubbleKeyPolicy.resolve(base.copy(label = "ক")) != null)
    checkCase("bangla sign eligible", BubbleKeyPolicy.resolve(base.copy(label = "ৌ")) != null)
    checkCase("bangla conjunct eligible", BubbleKeyPolicy.resolve(base.copy(label = "ক্ষ")) != null)

    checkCase("disabled suppressed", BubbleKeyPolicy.resolve(base.copy(enabled = false)) == null)
    checkCase("sensitive suppressed", BubbleKeyPolicy.resolve(base.copy(sensitiveField = true)) == null)
    checkCase("glide suppressed", BubbleKeyPolicy.resolve(base.copy(glideGesture = true)) == null)
    checkCase("animations disabled suppressed", BubbleKeyPolicy.resolve(base.copy(animationsEnabled = false)) == null)
    checkCase("non-letter layer suppressed", BubbleKeyPolicy.resolve(base.copy(letterLayer = false)) == null)
    checkCase("number suppressed", BubbleKeyPolicy.resolve(base.copy(label = "7")) == null)
    checkCase("symbol suppressed", BubbleKeyPolicy.resolve(base.copy(label = "?")) == null)
    checkCase("english quick word suppressed", BubbleKeyPolicy.resolve(base.copy(label = "hello")) == null)
    checkCase("blank suppressed", BubbleKeyPolicy.resolve(base.copy(label = "")) == null)

    val soft = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.SOFT))!!
    val normal = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.NORMAL))!!
    val playful = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.PLAYFUL))!!
    checkCase("intensity size increases", soft.bubbleSizeDp < normal.bubbleSizeDp && normal.bubbleSizeDp < playful.bubbleSizeDp)
    checkCase("intensity rise increases", soft.riseDp < normal.riseDp && normal.riseDp < playful.riseDp)
    checkCase("intensity duration increases", soft.durationMs < normal.durationMs && normal.durationMs < playful.durationMs)
    checkCase("pool bounded", BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES == 8)

    println("Stage 23.3 Bubble Key policy: $passed/20 PASS")
}
