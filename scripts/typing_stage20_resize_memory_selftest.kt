import com.socialaiassistant.keyboard.ime.BackgroundContentSignature
import com.socialaiassistant.keyboard.ime.BackgroundDecodePolicy
import com.socialaiassistant.keyboard.ime.BackgroundRenderDecision
import com.socialaiassistant.keyboard.ime.BackgroundRenderGate
import com.socialaiassistant.keyboard.ime.BackgroundRenderSignature
import com.socialaiassistant.keyboard.ime.BoundedValueCache
import com.socialaiassistant.keyboard.ime.KeyboardLanguage
import com.socialaiassistant.keyboard.ime.KeyboardLayoutRequest
import com.socialaiassistant.keyboard.ime.KeyboardUiMode
import com.socialaiassistant.keyboard.ime.ToolbarOrderGate
import com.socialaiassistant.keyboard.ime.ToolbarOrderSignature

private var checks = 0
private fun checkThat(value: Boolean, message: String) {
    checks++
    check(value) { message }
}

private fun background(
    enabled: Boolean = true,
    file: String = "photo.jpg",
    opacity: Int = 80,
    width: Int = 1024,
    height: Int = 512
) = BackgroundRenderSignature(
    content = BackgroundContentSignature(
        enabled = enabled,
        localFileName = if (enabled) file else "",
        scope = "FULL_KEYBOARD",
        fit = "CENTER_CROP",
        opacityPercent = opacity,
        darkOverlayPercent = 35,
        blurAmount = 4
    ),
    targetWidthPx = width,
    targetHeightPx = height
)

fun main() {
    val toolbar = ToolbarOrderGate()
    val balanced = ToolbarOrderSignature("BALANCED", listOf(1, 2, 3, 4))
    checkThat(toolbar.shouldReorder(balanced, emptyList()), "new toolbar must render")
    checkThat(!toolbar.shouldReorder(balanced, listOf(1, 2, 3, 4)), "identical toolbar order must reuse views")
    checkThat(toolbar.shouldReorder(balanced, listOf(1, 3, 2, 4)), "unexpected child order must self-heal")
    checkThat(!toolbar.shouldReorder(balanced, listOf(1, 2, 3, 4)), "healed order should reuse")
    val minimal = ToolbarOrderSignature("MINIMAL", listOf(1, 2))
    checkThat(toolbar.shouldReorder(minimal, listOf(1, 2, 3, 4)), "profile change must reorder")
    toolbar.invalidate()
    checkThat(toolbar.shouldReorder(minimal, listOf(1, 2)), "invalidate must force one refresh")

    val bg = BackgroundRenderGate()
    val disabled = background(enabled = false)
    checkThat(bg.decide(disabled) == BackgroundRenderDecision.CLEAR, "first disabled background should clear")
    checkThat(bg.decide(disabled) == BackgroundRenderDecision.SKIP, "identical disabled request should coalesce")
    val enabled = background()
    checkThat(bg.decide(enabled) == BackgroundRenderDecision.LOAD_CLEAR, "new photo should clear then load")
    checkThat(bg.decide(enabled) == BackgroundRenderDecision.SKIP, "identical photo/size should not reload")
    val resized = enabled.copy(targetWidthPx = 1088)
    checkThat(bg.decide(resized) == BackgroundRenderDecision.LOAD_KEEP_VISIBLE, "resize of same photo should keep old bitmap while loading")
    val opacityChanged = resized.copy(content = resized.content.copy(opacityPercent = 55))
    checkThat(bg.decide(opacityChanged) == BackgroundRenderDecision.LOAD_CLEAR, "visual content change should replace background")
    bg.invalidate()
    checkThat(bg.decide(opacityChanged) == BackgroundRenderDecision.LOAD_CLEAR, "memory invalidation should force reload")

    val low = BackgroundDecodePolicy.target(1001, 601, lowRamDevice = true)
    checkThat(low.widthPx == 1024, "low-RAM width should bucket to 1024")
    checkThat(low.heightPx == 640, "low-RAM height should bucket to 640")
    val lowHuge = BackgroundDecodePolicy.target(4000, 3000, lowRamDevice = true)
    checkThat(lowHuge.widthPx == 1024 && lowHuge.heightPx == 640, "low-RAM target must be capped")
    val normalHuge = BackgroundDecodePolicy.target(4000, 3000, lowRamDevice = false)
    checkThat(normalHuge.widthPx == 1536 && normalHuge.heightPx == 896, "normal device target must use bounded caps")
    val tiny = BackgroundDecodePolicy.target(1, 1, lowRamDevice = false)
    checkThat(tiny.widthPx == 64 && tiny.heightPx == 64, "tiny target should still use stable buckets")
    val customBucket = BackgroundDecodePolicy.target(801, 401, lowRamDevice = false, bucketPx = 32)
    checkThat(customBucket.widthPx == 832 && customBucket.heightPx == 416, "custom bucket should round up deterministically")

    var creates = 0
    val cache = BoundedValueCache<String, String>(2)
    fun cached(key: String): String = cache.getOrPut(key) { creates++; "$key-$creates" }
    val a1 = cached("a")
    val b1 = cached("b")
    checkThat(cache.size() == 2, "bounded cache should hold max entries")
    checkThat(cached("a") == a1, "cache hit should reuse immutable value")
    checkThat(creates == 2, "cache hit must not recreate")
    cached("c")
    checkThat(cache.size() == 2, "cache must stay bounded after third key")
    val b2 = cached("b")
    checkThat(b2 != b1, "least-recently-used entry should be evicted")
    cache.clear()
    checkThat(cache.size() == 0, "memory trim should clear layout cache")

    val requestA = KeyboardLayoutRequest(KeyboardUiMode(language = KeyboardLanguage.ENGLISH), true, listOf(".com"))
    val requestB = KeyboardLayoutRequest(KeyboardUiMode(language = KeyboardLanguage.ENGLISH), true, listOf(".com"))
    checkThat(requestA == requestB, "layout request must have stable value equality")
    checkThat(requestA.hashCode() == requestB.hashCode(), "layout request hash must be stable")

    println("Stage20 resize/memory self-test: $checks/$checks PASS")
}
