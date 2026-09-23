# Bubble Flight To Caret Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every eligible Bubble Key effect start at the exact pressed key and fly to the active editor caret, visually merging with the character that was just inserted, without delaying typing.

**Architecture:** Keep typing and animation independent. The IME captures the pressed-key screen center before the key action, performs the key action first, then dispatches an immutable flight request through an in-process bus. `CursorAnchorInfo` is the primary target; the existing accessibility service supplies a transient editable-field fallback and owns a non-touchable `TYPE_ACCESSIBILITY_OVERLAY` for cross-window animation. If no trustworthy remote target or overlay is available, the existing IME-local renderer performs a short key-origin fallback.

**Tech Stack:** Kotlin, Android `InputMethodService`, `CursorAnchorInfo`, `AccessibilityService`, `TYPE_ACCESSIBILITY_OVERLAY`, `ValueAnimator`, Robolectric/JUnit4, existing Python/Kotlin verification scripts.

**Spec:** `docs/superpowers/specs/2026-09-22-bubble-flight-to-caret-design.md`

## Global Constraints

- The character is committed to the editor immediately; animation never delays or gates typing.
- The bubble starts at the center of the exact pressed key, never from a keyboard corner or fixed origin.
- Target priority is exact cursor anchor, then matching accessibility editable bounds, then local IME rise/fade.
- Do not introduce `SYSTEM_ALERT_WINDOW` or any new draw-over-other-apps permission.
- Maximum simultaneous bubbles remains 8 and views are recycled.
- Existing safety gates remain authoritative: sensitive/password/OTP/payment fields, glide gestures, disabled animations, and non-letter layers emit no remote flight.
- Classic Dark, Glass Modern, Clean Light, and Gradient Pro share the same flight geometry; only visual tokens differ by theme.
- No editor text is persisted by Bubble Flight; only key label, transient coordinates, editor identity, timestamps, and immutable theme data are carried.
- Old cursor/fallback targets must be rejected by editor generation and age.
- Overlay failure must degrade to the local effect and must never affect text input.

## Build Prerequisite

Before any Gradle-backed RED/GREEN run, execute:

```bash
python3 scripts/check_gradle_wrapper_completeness.py
```

Expected: `GRADLE WRAPPER COMPLETENESS: PASS`.

If it exits with `BOOTSTRAP REQUIRED`, restore the verified wrapper only with Gradle 9.6.0:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
python3 scripts/check_gradle_wrapper_completeness.py
```

Do not treat Gradle tests as executed until the wrapper check passes.

## Review Focus

1. Target app never publishes cursor-anchor data: a matching accessibility editable target is used; otherwise the flight stays local and never guesses a screen destination. Covered in Task 3 and Task 4 tests.
2. User types more than eight characters rapidly: oldest active flight is recycled and active view count never exceeds 8. Covered in Task 3 renderer tests.
3. Editor/package changes while bubbles are active: stale target generations are rejected and active flights for the old editor are cancelled. Covered in Task 1 and Task 4 tests.
4. RTL or narrow/multiline editable fields use fallback geometry: the target remains inside the editable bounds and uses the trailing edge for current layout direction. Covered in Task 3 mapper tests.
5. Accessibility service is disabled, disconnected, or overlay attachment throws: dispatch reports unhandled and IME local fallback runs while text entry continues. Covered in Task 1 bus tests, Task 3 host tests, and Task 4 integration verification.

---

### Task 1: Define Flight Contracts, Editor Identity, Target Freshness, And Bus

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightModels.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTargetResolver.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightBus.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicy.kt:3-54`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt:66-72`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleFlightTargetResolverTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleFlightBusTest.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicyTest.kt`

**Interfaces:**
- Consumes: existing `KeyboardTheme`, `BubbleKeyAnimationSpec`, `ImeSession`, and `FieldSafety`.
- Produces: `BubbleFlightPoint`, `BubbleFlightEditorToken`, `BubbleFlightTarget`, `BubbleFlightTargetSource`, `BubbleFlightRequest`, `BubbleFlightSink`, `BubbleFlightBus`, `BubbleFlightTargetResolver`, and `ImeSession.generation`.

- [ ] **Step 1: Write target freshness and editor-match tests**

Create `BubbleFlightTargetResolverTest.kt` with these cases:

```kotlin
package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BubbleFlightTargetResolverTest {
    private val resolver = BubbleFlightTargetResolver(
        exactMaxAgeMs = 600L,
        fallbackMaxAgeMs = 900L
    )
    private val editor = BubbleFlightEditorToken("org.example.chat", 7, 3L)

    @Test fun exactTargetWinsWhenFreshAndMatching() {
        val exact = BubbleFlightTarget(
            point = BubbleFlightPoint(300f, 200f),
            editor = editor,
            capturedAtUptimeMs = 1_000L,
            source = BubbleFlightTargetSource.CURSOR_ANCHOR
        )
        val fallback = BubbleFlightTarget(
            point = BubbleFlightPoint(250f, 210f),
            editor = editor,
            capturedAtUptimeMs = 1_050L,
            source = BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS
        )

        assertEquals(exact, resolver.resolve(editor, exact, fallback, nowUptimeMs = 1_200L))
    }

    @Test fun fallbackIsUsedWhenExactIsStale() {
        val exact = BubbleFlightTarget(
            BubbleFlightPoint(300f, 200f), editor, 100L, BubbleFlightTargetSource.CURSOR_ANCHOR
        )
        val fallback = BubbleFlightTarget(
            BubbleFlightPoint(250f, 210f), editor, 900L, BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS
        )

        assertEquals(fallback, resolver.resolve(editor, exact, fallback, nowUptimeMs = 1_100L))
    }

    @Test fun mismatchedGenerationIsRejected() {
        val oldEditor = editor.copy(generation = 2L)
        val stale = BubbleFlightTarget(
            BubbleFlightPoint(300f, 200f), oldEditor, 1_000L, BubbleFlightTargetSource.CURSOR_ANCHOR
        )

        assertNull(resolver.resolve(editor, stale, null, nowUptimeMs = 1_100L))
    }

    @Test fun noTrustworthyTargetReturnsNull() {
        assertNull(resolver.resolve(editor, null, null, nowUptimeMs = 1_100L))
    }
}
```

- [ ] **Step 2: Run the resolver tests and verify RED**

Run:

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.ime.BubbleFlightTargetResolverTest
```

Expected: FAIL because `BubbleFlightTargetResolver` and flight model types do not exist.

- [ ] **Step 3: Add immutable flight models and target resolver**

Create `BubbleFlightModels.kt`:

```kotlin
package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.theme.KeyboardTheme

data class BubbleFlightPoint(val x: Float, val y: Float) {
    fun isFinite(): Boolean = x.isFinite() && y.isFinite()
}

data class BubbleFlightEditorToken(
    val packageName: String?,
    val fieldId: Int,
    val generation: Long
)

enum class BubbleFlightTargetSource { CURSOR_ANCHOR, ACCESSIBILITY_BOUNDS }

data class BubbleFlightTarget(
    val point: BubbleFlightPoint,
    val editor: BubbleFlightEditorToken,
    val capturedAtUptimeMs: Long,
    val source: BubbleFlightTargetSource
)

data class BubbleFlightRequest(
    val id: Long,
    val label: String,
    val source: BubbleFlightPoint,
    val editor: BubbleFlightEditorToken,
    val exactTarget: BubbleFlightTarget?,
    val spec: BubbleKeyAnimationSpec,
    val theme: KeyboardTheme,
    val createdAtUptimeMs: Long
)
```

Create `BubbleFlightTargetResolver.kt`:

```kotlin
package com.socialaiassistant.keyboard.ime

class BubbleFlightTargetResolver(
    private val exactMaxAgeMs: Long = 600L,
    private val fallbackMaxAgeMs: Long = 900L
) {
    fun resolve(
        editor: BubbleFlightEditorToken,
        exact: BubbleFlightTarget?,
        fallback: BubbleFlightTarget?,
        nowUptimeMs: Long
    ): BubbleFlightTarget? {
        return exact.takeIf { valid(it, editor, nowUptimeMs, exactMaxAgeMs) }
            ?: fallback.takeIf { valid(it, editor, nowUptimeMs, fallbackMaxAgeMs) }
    }

    private fun valid(
        target: BubbleFlightTarget,
        editor: BubbleFlightEditorToken,
        nowUptimeMs: Long,
        maxAgeMs: Long
    ): Boolean {
        if (target.editor != editor || !target.point.isFinite()) return false
        val age = nowUptimeMs - target.capturedAtUptimeMs
        return age in 0L..maxAgeMs
    }
}
```

Extend `ImeSession` with a defaulted generation so existing tests and callers remain source-compatible:

```kotlin
data class ImeSession(
    val packageName: String?,
    val safety: FieldSafety,
    val inputType: Int,
    val hintText: String?,
    val fieldId: Int = 0,
    val generation: Long = 0L
)
```

- [ ] **Step 4: Run the resolver tests and verify GREEN**

Run the same Gradle command.

Expected: PASS for all resolver cases.

- [ ] **Step 5: Write bus lifecycle tests**

Create `BubbleFlightBusTest.kt` using a fake sink and assert:

```kotlin
@Test fun dispatchReturnsFalseWithoutRegisteredSink() {
    BubbleFlightBus.clearForTest()
    assertFalse(BubbleFlightBus.dispatch(request()))
}

@Test fun registeredSinkReceivesRequestAndCanBeUnregistered() {
    val sink = RecordingSink(accept = true)
    BubbleFlightBus.register(sink)
    assertTrue(BubbleFlightBus.dispatch(request()))
    assertEquals(1, sink.requests.size)
    BubbleFlightBus.unregister(sink)
    assertFalse(BubbleFlightBus.dispatch(request().copy(id = 2L)))
}

@Test fun retargetIsForwardedOnlyToCurrentSink() {
    val sink = RecordingSink(accept = true)
    BubbleFlightBus.register(sink)
    val target = exactTarget()
    assertTrue(BubbleFlightBus.retarget(1L, target))
    assertEquals(listOf(1L to target), sink.retargets)
    BubbleFlightBus.unregister(sink)
}
```

The fake implements the exact interface:

```kotlin
private class RecordingSink(private val accept: Boolean) : BubbleFlightSink {
    val requests = mutableListOf<BubbleFlightRequest>()
    val retargets = mutableListOf<Pair<Long, BubbleFlightTarget>>()

    override fun submit(request: BubbleFlightRequest): Boolean {
        requests += request
        return accept
    }

    override fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean {
        retargets += flightId to target
        return accept
    }

    override fun cancelEditor(editor: BubbleFlightEditorToken) = Unit
    override fun cancelAll() = Unit
}
```

- [ ] **Step 6: Run bus tests and verify RED**

Run:

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.ime.BubbleFlightBusTest
```

Expected: FAIL because `BubbleFlightSink` and `BubbleFlightBus` do not exist.

- [ ] **Step 7: Implement the in-process bus**

Create `BubbleFlightBus.kt`:

```kotlin
package com.socialaiassistant.keyboard.ime

import java.util.concurrent.atomic.AtomicReference

interface BubbleFlightSink {
    fun submit(request: BubbleFlightRequest): Boolean
    fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean
    fun cancelEditor(editor: BubbleFlightEditorToken)
    fun cancelAll()
}

object BubbleFlightBus {
    private val sink = AtomicReference<BubbleFlightSink?>(null)

    fun register(value: BubbleFlightSink) {
        sink.set(value)
    }

    fun unregister(value: BubbleFlightSink) {
        sink.compareAndSet(value, null)
    }

    fun dispatch(request: BubbleFlightRequest): Boolean = sink.get()?.submit(request) == true

    fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean =
        sink.get()?.retarget(flightId, target) == true

    fun cancelEditor(editor: BubbleFlightEditorToken) {
        sink.get()?.cancelEditor(editor)
    }

    fun cancelAll() {
        sink.get()?.cancelAll()
    }

    internal fun clearForTest() {
        sink.set(null)
    }
}
```

- [ ] **Step 8: Extend Bubble Key motion specs for remote flight**

First add policy tests that assert all three modes preserve old local values and expose increasing curve amplitude plus decreasing end scale:

```kotlin
@Test fun flightMotionScalesByIntensity() {
    val soft = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.SOFT))!!
    val normal = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.NORMAL))!!
    val playful = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.PLAYFUL))!!

    assertTrue(soft.flightCurveDp < normal.flightCurveDp)
    assertTrue(normal.flightCurveDp < playful.flightCurveDp)
    assertTrue(soft.flightEndScale > normal.flightEndScale)
    assertTrue(normal.flightEndScale > playful.flightEndScale)
}
```

Run the policy test and confirm RED because the two new properties are absent. Then extend `BubbleKeyAnimationSpec` with:

```kotlin
val flightCurveDp: Float,
val flightEndScale: Float
```

Use these exact mode values while preserving existing duration/rise/drift/size:

```kotlin
SOFT -> Motion(380L, 52f, 7f, 28f, flightCurveDp = 56f, flightEndScale = 0.62f)
NORMAL -> Motion(470L, 68f, 11f, 32f, flightCurveDp = 82f, flightEndScale = 0.52f)
PLAYFUL -> Motion(560L, 86f, 16f, 36f, flightCurveDp = 118f, flightEndScale = 0.44f)
```

Run `BubbleKeyPolicyTest` and the existing Stage 23.3 self-test; expected GREEN.

- [ ] **Step 9: Run all Task 1 tests**

Run:

```bash
./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.ime.BubbleFlight*' --tests com.socialaiassistant.keyboard.ime.BubbleKeyPolicyTest
```

Expected: PASS, 0 failures.

- [ ] **Step 10: Commit Task 1**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightModels.kt app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTargetResolver.kt app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightBus.kt app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicy.kt app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleFlightTargetResolverTest.kt app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleFlightBusTest.kt app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicyTest.kt
git commit -m "feat: define bubble flight contracts"
```

---

### Task 2: Capture Exact Key Origin, Track Cursor Anchor, Commit Before Dispatch

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleCursorAnchorMapper.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTapCoordinator.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyEffectRenderer.kt:23-63`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt:112-503, 700-725, 1170-1190, 1375-1388`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleCursorAnchorMapperTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleFlightTapCoordinatorTest.kt`

**Interfaces:**
- Consumes: Task 1 flight models/bus/resolver and existing `BubbleKeyPolicy`.
- Produces: `BubbleCursorAnchorMapper.map(CursorAnchorInfo)`, `BubbleFlightTapCoordinator.commitThenDispatch`, IME session generation, cursor monitoring, exact source point capture, one-shot early retarget, and local fallback by screen coordinate.

- [ ] **Step 1: Write cursor matrix mapping tests**

Create `BubbleCursorAnchorMapperTest.kt` with Robolectric and construct `CursorAnchorInfo` using a translated/scaled matrix. Assert that the mapped point equals the insertion marker transformed into screen coordinates and that NaN markers return null.

The main positive test must use these values:

```kotlin
val matrix = Matrix().apply {
    setScale(2f, 2f)
    postTranslate(40f, 60f)
}
val info = CursorAnchorInfo.Builder()
    .setInsertionMarkerLocation(10f, 20f, 24f, 28f, 0)
    .setMatrix(matrix)
    .build()

assertEquals(BubbleFlightPoint(60f, 108f), BubbleCursorAnchorMapper.map(info))
```

The Y source is the vertical midpoint `(20 + 28) / 2 = 24`, transformed by the matrix.

- [ ] **Step 2: Run mapper tests and verify RED**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.ime.BubbleCursorAnchorMapperTest
```

Expected: FAIL because `BubbleCursorAnchorMapper` does not exist.

- [ ] **Step 3: Implement cursor-anchor mapping**

Create `BubbleCursorAnchorMapper.kt`:

```kotlin
package com.socialaiassistant.keyboard.ime

import android.view.inputmethod.CursorAnchorInfo

object BubbleCursorAnchorMapper {
    fun map(info: CursorAnchorInfo): BubbleFlightPoint? {
        val x = info.insertionMarkerHorizontal
        val top = info.insertionMarkerTop
        val bottom = info.insertionMarkerBottom
        val baseline = info.insertionMarkerBaseline
        if (!x.isFinite()) return null

        val y = when {
            top.isFinite() && bottom.isFinite() -> (top + bottom) / 2f
            baseline.isFinite() -> baseline
            else -> return null
        }
        val points = floatArrayOf(x, y)
        info.matrix.mapPoints(points)
        return BubbleFlightPoint(points[0], points[1]).takeIf(BubbleFlightPoint::isFinite)
    }
}
```

Run the mapper tests again; expected GREEN.

- [ ] **Step 4: Write commit-before-dispatch coordinator tests**

Create `BubbleFlightTapCoordinatorTest.kt`:

```kotlin
@Test fun commitRunsBeforeFlightDispatch() {
    val events = mutableListOf<String>()
    val coordinator = BubbleFlightTapCoordinator { events += "dispatch" }

    coordinator.commitThenDispatch(preparedFlight()) { events += "commit" }

    assertEquals(listOf("commit", "dispatch"), events)
}

@Test fun nullPreparedFlightStillCommits() {
    val events = mutableListOf<String>()
    val coordinator = BubbleFlightTapCoordinator { events += "dispatch" }

    coordinator.commitThenDispatch(null) { events += "commit" }

    assertEquals(listOf("commit"), events)
}
```

- [ ] **Step 5: Run coordinator tests and verify RED**

Expected: FAIL because `BubbleFlightTapCoordinator` and `PreparedBubbleFlight` do not exist.

- [ ] **Step 6: Implement the commit/dispatch seam**

Add to `BubbleFlightModels.kt`:

```kotlin
data class PreparedBubbleFlight(
    val request: BubbleFlightRequest
)
```

Create `BubbleFlightTapCoordinator.kt`:

```kotlin
package com.socialaiassistant.keyboard.ime

class BubbleFlightTapCoordinator(
    private val dispatch: (PreparedBubbleFlight) -> Unit
) {
    fun commitThenDispatch(prepared: PreparedBubbleFlight?, commit: () -> Unit) {
        commit()
        if (prepared != null) dispatch(prepared)
    }
}
```

Run the coordinator tests; expected GREEN.

- [ ] **Step 7: Convert local renderer to use captured screen source**

Write a Robolectric test that creates a fake overlay at a known screen location, calls a new `showLocal(sourceScreen, spec, theme)` API, and asserts the bubble starts from `sourceScreen - overlayScreenOrigin - size/2`. Confirm RED against the existing `show(View, ...)` API.

Change `BubbleKeyEffectRenderer` to expose:

```kotlin
fun showLocal(sourceScreen: BubbleFlightPoint, spec: BubbleKeyAnimationSpec, theme: KeyboardTheme)
```

The method must keep the existing max-8 pool, local rise/drift behavior, and use screen-to-overlay conversion. Remove the requirement that the pressed key view still be attached after text commit.

- [ ] **Step 8: Add IME cursor session state and monitoring**

In `SocialAiInputMethodService`, add:

```kotlin
private var bubbleEditorGeneration = 0L
private var latestBubbleCursorTarget: BubbleFlightTarget? = null
private var nextBubbleFlightId = 1L
private val pendingBubbleRetargets = ArrayDeque<PendingBubbleRetarget>()
private val bubbleFlightTapCoordinator = BubbleFlightTapCoordinator(::dispatchPreparedBubbleFlight)

private data class PendingBubbleRetarget(
    val flightId: Long,
    val editor: BubbleFlightEditorToken,
    val expiresAtUptimeMs: Long
)
```

On every non-null `onStartInput`, increment generation before publishing `ImeSession` and store it in `ImeSession.generation`. Clear cursor/retarget state when editor is null, on finish, and on destroy.

Add:

```kotlin
private fun currentBubbleEditorToken(): BubbleFlightEditorToken? {
    val session = ImeSessionRegistry.session.value ?: return null
    return BubbleFlightEditorToken(session.packageName, session.fieldId, session.generation)
}
```

In `onStartInputView`, call `requestBubbleCursorUpdates()` after `super` and before rendering.

Implement API-compatible cursor monitoring:

```kotlin
private fun requestBubbleCursorUpdates() {
    val connection = currentInputConnection ?: return
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            connection.requestCursorUpdates(
                android.view.inputmethod.InputConnection.CURSOR_UPDATE_MONITOR or
                    android.view.inputmethod.InputConnection.CURSOR_UPDATE_IMMEDIATE,
                android.view.inputmethod.InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER
            )
        } else {
            connection.requestCursorUpdates(
                android.view.inputmethod.InputConnection.CURSOR_UPDATE_MONITOR or
                    android.view.inputmethod.InputConnection.CURSOR_UPDATE_IMMEDIATE
            )
        }
    }
}
```

Override `onUpdateCursorAnchorInfo(info: CursorAnchorInfo?)`; map the point, tag it with the current editor token and `SystemClock.uptimeMillis()`, save it, then retarget only the newest pending flight whose editor matches and whose expiry has not passed. Remove that pending entry after one retarget attempt so a flight can be retargeted at most once.

- [ ] **Step 9: Prepare source before commit, then commit, then dispatch**

Replace `maybeShowBubbleKey` with two methods:

```kotlin
private fun prepareBubbleFlight(view: View, key: KeySpec, glideGesture: Boolean): PreparedBubbleFlight? {
    val spec = BubbleKeyPolicy.resolve(
        BubbleKeyRequest(
            enabled = currentSettings.bubbleKeyEnabled,
            label = key.label,
            sensitiveField = ImeSessionRegistry.session.value?.safety == FieldSafety.BLOCK_AI,
            glideGesture = glideGesture,
            animationsEnabled = ValueAnimator.areAnimatorsEnabled(),
            letterLayer = keyboardMode.layer == KeyboardLayer.LETTERS,
            intensity = currentSettings.bubbleKeyIntensity
        )
    ) ?: return null
    val editor = currentBubbleEditorToken() ?: return null
    if (view.width <= 0 || view.height <= 0 || !view.isShown) return null
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    val source = BubbleFlightPoint(
        location[0] + view.width / 2f,
        location[1] + view.height / 2f
    )
    val exact = latestBubbleCursorTarget?.takeIf { it.editor == editor }
    return PreparedBubbleFlight(
        BubbleFlightRequest(
            id = nextBubbleFlightId++,
            label = spec.label,
            source = source,
            editor = editor,
            exactTarget = exact,
            spec = spec,
            theme = currentSurfaceTheme,
            createdAtUptimeMs = SystemClock.uptimeMillis()
        )
    )
}

private fun dispatchPreparedBubbleFlight(prepared: PreparedBubbleFlight) {
    val request = prepared.request
    val handled = BubbleFlightBus.dispatch(request)
    if (handled) {
        pendingBubbleRetargets.addLast(
            PendingBubbleRetarget(request.id, request.editor, SystemClock.uptimeMillis() + 180L)
        )
        while (pendingBubbleRetargets.size > BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES) {
            pendingBubbleRetargets.removeFirst()
        }
    } else {
        bubbleKeyRenderer?.showLocal(request.source, request.spec, request.theme)
    }
}
```

In normal key click handling use this order:

```kotlin
button.setOnClickListener { view ->
    performKeyFeedback(view)
    val prepared = prepareBubbleFlight(view, key, glideGesture = false)
    bubbleFlightTapCoordinator.commitThenDispatch(prepared) {
        key.action?.let(::handleAction)
    }
}
```

Apply the same order to the non-drag glide ACTION_UP path: capture from the released key view, call `handleAction`, then dispatch. A true glide remains suppressed by the existing policy.

- [ ] **Step 10: Cancel old editor flights on lifecycle changes**

Before replacing a non-null `ImeSession` in `onStartInput`, capture the old editor token and call `BubbleFlightBus.cancelEditor(oldToken)`. On `onFinishInput`, `onFinishInputView`, and `onDestroy`, clear pending retargets and cursor target; use `cancelEditor` or `cancelAll` as appropriate.

- [ ] **Step 11: Run Task 2 tests and focused regressions**

Run:

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.ime.BubbleCursorAnchorMapperTest --tests com.socialaiassistant.keyboard.ime.BubbleFlightTapCoordinatorTest --tests com.socialaiassistant.keyboard.ime.BubbleKeyPolicyTest
python3 scripts/verify_typing_stage23_3.py
```

Expected: all commands PASS with 0 failures.

- [ ] **Step 12: Commit Task 2**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleCursorAnchorMapper.kt app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTapCoordinator.kt app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightModels.kt app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyEffectRenderer.kt app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleCursorAnchorMapperTest.kt app/src/test/java/com/socialaiassistant/keyboard/ime/BubbleFlightTapCoordinatorTest.kt
git commit -m "feat: capture key origin and cursor target"
```

---

### Task 3: Build Accessibility Target Mapping And Cross-Window Overlay Renderer

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/BubbleAccessibilityTargetMapper.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/BubbleFlightPath.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/AccessibilityBubbleOverlayWindowHost.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/BubbleFlightOverlayRenderer.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/BubbleAccessibilityTargetMapperTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/BubbleFlightPathTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/AccessibilityBubbleOverlayWindowHostTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/BubbleFlightOverlayRendererTest.kt`

**Interfaces:**
- Consumes: Task 1 flight models and `BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES`.
- Produces: safe fallback target mapping, Bezier motion math, a non-touchable accessibility overlay host, pooled cross-window renderer with one early retarget, cancel-by-editor, and idle detach.

- [ ] **Step 1: Write accessibility-bounds mapping tests**

Test these behaviors using a 300x80 `Rect(100, 200, 400, 280)` and 16 px trailing inset:

```kotlin
assertEquals(BubbleFlightPoint(384f, 240f), BubbleAccessibilityTargetMapper.pointInside(bounds, rtl = false, trailingInsetPx = 16f))
assertEquals(BubbleFlightPoint(116f, 240f), BubbleAccessibilityTargetMapper.pointInside(bounds, rtl = true, trailingInsetPx = 16f))
assertNull(BubbleAccessibilityTargetMapper.pointInside(Rect(0, 0, 0, 0), rtl = false, trailingInsetPx = 16f))
```

Also test a narrow 20 px field: the returned X must remain inside the bounds.

- [ ] **Step 2: Run mapper tests and verify RED**

Expected: FAIL because mapper does not exist.

- [ ] **Step 3: Implement bounds mapper**

Create:

```kotlin
object BubbleAccessibilityTargetMapper {
    fun pointInside(bounds: Rect, rtl: Boolean, trailingInsetPx: Float): BubbleFlightPoint? {
        if (bounds.width() <= 0 || bounds.height() <= 0) return null
        val inset = trailingInsetPx.coerceIn(1f, (bounds.width() / 2f).coerceAtLeast(1f))
        val x = if (rtl) bounds.left + inset else bounds.right - inset
        val y = bounds.exactCenterY()
        return BubbleFlightPoint(x, y)
    }
}
```

Run mapper tests; expected GREEN.

- [ ] **Step 4: Write Bezier path tests**

Create tests for `BubbleFlightPath.pointAt(source, target, curvePx, t)`:

- `t=0` equals source.
- `t=1` equals target.
- midpoint Y is above the straight-line midpoint when target is above the keyboard.
- all returned coordinates are finite for a short path.

Use a quadratic control point whose X is the midpoint and whose Y is `min(source.y, target.y) - curvePx`.

- [ ] **Step 5: Run path tests and verify RED, then implement path math**

Create:

```kotlin
object BubbleFlightPath {
    fun pointAt(source: BubbleFlightPoint, target: BubbleFlightPoint, curvePx: Float, t: Float): BubbleFlightPoint {
        val clamped = t.coerceIn(0f, 1f)
        val oneMinus = 1f - clamped
        val controlX = (source.x + target.x) / 2f
        val controlY = minOf(source.y, target.y) - curvePx.coerceAtLeast(0f)
        val x = oneMinus * oneMinus * source.x + 2f * oneMinus * clamped * controlX + clamped * clamped * target.x
        val y = oneMinus * oneMinus * source.y + 2f * oneMinus * clamped * controlY + clamped * clamped * target.y
        return BubbleFlightPoint(x, y)
    }
}
```

Run path tests; expected GREEN.

- [ ] **Step 6: Write overlay-window flag tests**

`AccessibilityBubbleOverlayWindowHostTest` must inspect `buildLayoutParams()` and assert:

```kotlin
assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
assertTrue(params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN != 0)
assertEquals(PixelFormat.TRANSLUCENT, params.format)
```

No test or production code may reference `SYSTEM_ALERT_WINDOW`.

- [ ] **Step 7: Implement the accessibility overlay window host**

Create a host that owns one full-screen `FrameLayout`, adds it lazily through `service.getSystemService(WindowManager::class.java)`, and removes it when idle. `ensureAttached()` must catch `SecurityException`, `BadTokenException`, and `IllegalStateException`, returning `false` instead of throwing into typing flow.

Use exact layout params:

```kotlin
WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.TOP or Gravity.START
}
```

Run host tests; expected GREEN.

- [ ] **Step 8: Write renderer pool, idle-detach, theme, and retarget tests**

Use a fake host with a real Robolectric `FrameLayout`. Tests must prove:

1. Nine submitted flights produce no more than 8 active views.
2. Recycling the oldest flight makes the ninth visible without growing the pool above 8.
3. When the final animator completes/cancels, the host detaches.
4. A first retarget inside 35% of duration succeeds and keeps the bubble at its current screen position before continuing to the new target.
5. A second retarget returns false.
6. Retarget after 35% returns false.
7. `cancelEditor` removes only matching flights.
8. Theme label color and gradient/glass/solid background tokens come from `request.theme`.

- [ ] **Step 9: Run renderer tests and verify RED**

Expected: FAIL because `BubbleFlightOverlayRenderer` does not exist.

- [ ] **Step 10: Implement bounded overlay renderer**

The renderer must:

- Call `host.ensureAttached()` before creating a view and return `false` if it fails.
- Maintain `LinkedHashMap<Long, ActiveFlight>` in insertion order plus an `ArrayDeque<TextView>` pool.
- Recycle the oldest active flight when size reaches 8.
- Position bubbles by center: `view.x = point.x - sizePx / 2f`, `view.y = point.y - sizePx / 2f`.
- Use `ValueAnimator.ofFloat(0f, 1f)` for each segment and `BubbleFlightPath.pointAt` on every frame.
- Keep alpha near 0.96 until fraction 0.72, then fade linearly to 0.
- Interpolate scale from 0.82 to `request.spec.flightEndScale`.
- Use `request.spec.durationMs` and `request.spec.flightCurveDp` converted by display density.
- On retarget, accept only once and only while elapsed fraction is <= 0.35. Capture current bubble center as the new segment source, cancel the current animator without recycling the view, and animate the remaining duration to the new target. This prevents a visible jump.
- On completion, recycle the view; when active count becomes zero, call `host.detach()`.
- `release()` cancels all animators, clears pool/root, and detaches host.

Use the same bubble drawable rules as the local renderer so all four themes keep their current visual identity.

- [ ] **Step 11: Run all Task 3 tests**

```bash
./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.context.Bubble*'
```

Expected: PASS, 0 failures.

- [ ] **Step 12: Commit Task 3**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/context/BubbleAccessibilityTargetMapper.kt app/src/main/java/com/socialaiassistant/keyboard/context/BubbleFlightPath.kt app/src/main/java/com/socialaiassistant/keyboard/context/AccessibilityBubbleOverlayWindowHost.kt app/src/main/java/com/socialaiassistant/keyboard/context/BubbleFlightOverlayRenderer.kt app/src/test/java/com/socialaiassistant/keyboard/context/BubbleAccessibilityTargetMapperTest.kt app/src/test/java/com/socialaiassistant/keyboard/context/BubbleFlightPathTest.kt app/src/test/java/com/socialaiassistant/keyboard/context/AccessibilityBubbleOverlayWindowHostTest.kt app/src/test/java/com/socialaiassistant/keyboard/context/BubbleFlightOverlayRendererTest.kt
git commit -m "feat: render bubble flights in accessibility overlay"
```

---

### Task 4: Wire Accessibility Fallback, Overlay Sink, Cleanup, And Privacy Disclosure

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt:13-189`
- Modify: `docs/play-store/accessibility-declaration.md`
- Modify: `docs/privacy/context-access-disclosure.md`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityBubbleFlightTest.kt`

**Interfaces:**
- Consumes: `BubbleFlightSink`, `BubbleFlightTargetResolver`, `BubbleAccessibilityTargetMapper`, `BubbleFlightOverlayRenderer`, current `ImeSessionRegistry`.
- Produces: registered accessibility flight sink, transient editable fallback snapshot, editor/session cleanup, and no-text-persistence privacy behavior.

- [ ] **Step 1: Write accessibility service flight tests**

Use Robolectric/service-controller or extracted internal methods to test:

1. A matching editable node/session creates a `BubbleFlightTarget` with `ACCESSIBILITY_BOUNDS` and the current session generation.
2. Password nodes clear/skip fallback target.
3. A request with fresh exact target uses exact target instead of fallback.
4. A request with stale exact target uses matching fallback.
5. A request with neither returns `false`, allowing IME local fallback.
6. Package or generation mismatch returns `false`.
7. `onDestroy` unregisters the sink and releases overlay renderer.

- [ ] **Step 2: Run service tests and verify RED**

Expected: FAIL because the service does not implement the flight sink.

- [ ] **Step 3: Register the accessibility service as the flight sink**

Change the class declaration to:

```kotlin
class SocialAiAccessibilityService : AccessibilityService(), BubbleFlightSink
```

Add:

```kotlin
private val bubbleTargetResolver = BubbleFlightTargetResolver()
private var latestBubbleEditableTarget: BubbleFlightTarget? = null
private var bubbleOverlayRenderer: BubbleFlightOverlayRenderer? = null
```

In `onServiceConnected()`:

```kotlin
override fun onServiceConnected() {
    super.onServiceConnected()
    bubbleOverlayRenderer = BubbleFlightOverlayRenderer(AccessibilityBubbleOverlayWindowHost(this))
    BubbleFlightBus.register(this)
}
```

In `onDestroy()` call `BubbleFlightBus.unregister(this)`, `bubbleOverlayRenderer?.release()`, clear target state, then existing cleanup.

- [ ] **Step 4: Capture editable geometry without persisting editor text**

At the beginning of each supported accessibility event, after validating package and before conversation-text extraction, call a new `updateBubbleEditableTarget(safeEvent, packageName)`.

That method must:

- Read current `ImeSessionRegistry.session.value` and require matching package plus `safety != FieldSafety.BLOCK_AI`.
- Prefer `event.source` if editable; otherwise use `rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)`.
- Reject password/non-editable/zero-sized nodes.
- Get `Rect` via `getBoundsInScreen`.
- Determine RTL from `resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL`.
- Use `BubbleAccessibilityTargetMapper.pointInside(bounds, rtl, dp(16f))`.
- Store only point, editor token, source enum, and `SystemClock.uptimeMillis()`; do not retain `AccessibilityNodeInfo` or node text.

When package/session changes or a blocked/password node is encountered, clear `latestBubbleEditableTarget`.

Conversation text extraction remains behind the existing `ContextAccessGate.allowed` check; Bubble Flight geometry tracking does not move or weaken that gate.

- [ ] **Step 5: Implement sink methods**

Use exact target first, then fallback:

```kotlin
override fun submit(request: BubbleFlightRequest): Boolean {
    val target = bubbleTargetResolver.resolve(
        editor = request.editor,
        exact = request.exactTarget,
        fallback = latestBubbleEditableTarget,
        nowUptimeMs = SystemClock.uptimeMillis()
    ) ?: return false
    return bubbleOverlayRenderer?.show(request, target) == true
}

override fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean =
    bubbleOverlayRenderer?.retarget(flightId, target) == true

override fun cancelEditor(editor: BubbleFlightEditorToken) {
    bubbleOverlayRenderer?.cancelEditor(editor)
    if (latestBubbleEditableTarget?.editor == editor) latestBubbleEditableTarget = null
}

override fun cancelAll() {
    bubbleOverlayRenderer?.cancelAll()
    latestBubbleEditableTarget = null
}
```

The overlay renderer itself re-validates the editor token on retarget against the active flight request.

- [ ] **Step 6: Run service tests and verify GREEN**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.context.SocialAiAccessibilityBubbleFlightTest
```

Expected: PASS.

- [ ] **Step 7: Update privacy/accessibility disclosure**

Add a concise disclosure to both documents stating:

- When Bubble Key is enabled and accessibility access is available, the app may use the active editable field bounds only to position the visual bubble.
- Bubble Flight does not persist field text or coordinates.
- Conversation text access remains controlled by the existing AI Context Access consent.
- No new draw-over-other-apps permission is used.

Do not claim exact-caret support from accessibility; exact caret comes from the IME cursor-anchor channel.

- [ ] **Step 8: Run context and safety regressions**

```bash
./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.context.*' --tests com.socialaiassistant.keyboard.safety.SensitiveFieldPolicyTest
```

Expected: PASS, 0 failures.

- [ ] **Step 9: Commit Task 4**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt docs/play-store/accessibility-declaration.md docs/privacy/context-access-disclosure.md app/src/test/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityBubbleFlightTest.kt
git commit -m "feat: wire caret flight accessibility fallback"
```

---

### Task 5: Stage 24.1 Verification, Full Regression, And Real-Device Acceptance

**Files:**
- Create: `scripts/verify_typing_stage24_1.py`
- Create: `scripts/typing_stage24_1_bubble_flight_selftest.kt`
- Create: `docs/STAGE24_1_BUBBLE_FLIGHT_TO_CARET.md`
- Create: `docs/STAGE24_1_VERIFICATION_REPORT.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: completed Stage 24.1 source and existing Stage 23.x/24.0 verification scripts.
- Produces: static contract verification, pure Kotlin self-test, regression evidence, and a concrete real-device checklist.

- [ ] **Step 1: Write Stage 24.1 static verifier first**

The verifier must fail until all required source hooks exist. It checks at minimum:

- `BubbleFlightBus`, `BubbleFlightTargetResolver`, and `BubbleFlightOverlayRenderer` files exist.
- `SocialAiInputMethodService` contains `onUpdateCursorAnchorInfo`, `requestCursorUpdates`, source `getLocationOnScreen`, `commitThenDispatch`, and local fallback `showLocal`.
- `SocialAiAccessibilityService` implements `BubbleFlightSink` and registers/unregisters bus.
- `TYPE_ACCESSIBILITY_OVERLAY`, `FLAG_NOT_FOCUSABLE`, and `FLAG_NOT_TOUCHABLE` exist in overlay host.
- `SYSTEM_ALERT_WINDOW` is absent from manifest and Stage 24.1 source.
- Max simultaneous bubbles remains 8.
- Sensitive/glide/non-letter gates remain in `BubbleKeyPolicy`.
- All four theme packs still exist.

Run:

```bash
python3 scripts/verify_typing_stage24_1.py
```

Expected before implementation completion: FAIL on missing Stage 24.1 contracts. Expected after all tasks: PASS.

- [ ] **Step 2: Add pure Kotlin Stage 24.1 self-test**

The self-test compiles only Android-free flight policy/resolver logic with existing support stubs and checks:

- exact target priority;
- stale/mismatched target rejection;
- max bubbles equals 8;
- Soft/Normal/Playful curve/end-scale ordering;
- no target resolves to null rather than an invented coordinate.

Run through a new or existing shell wrapper that invokes `kotlinc`; expected PASS after Task 1.

- [ ] **Step 3: Run focused Stage 24.1 and prior Bubble regressions**

```bash
python3 scripts/verify_typing_stage24_1.py
python3 scripts/verify_typing_stage23_3.py
```

Expected: both PASS.

- [ ] **Step 4: Run full unit suite**

```bash
./gradlew testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, 0 failed tests.

- [ ] **Step 5: Run release/static regression suite**

Run the existing project verification commands that cover current user-visible work:

```bash
python3 scripts/verify_typing_stage23.py
python3 scripts/verify_theme_stage23_2.py
python3 scripts/verify_numberpad_stage23_7.py
python3 scripts/verify_typing_stage23_8.py
python3 scripts/verify_settings_theme_stage23_9.py
python3 scripts/verify_theme_stage24_0.py
python3 scripts/verify_release_ready.py
python3 scripts/verify_shared_backend_config.py
```

Expected: every existing verifier PASS.

- [ ] **Step 6: Build debug APK**

```bash
./gradlew lintDebug assembleDebug
```

Expected: BUILD SUCCESSFUL and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 7: Perform real-device Bubble Flight acceptance**

Use at least one normal multiline messaging/editor field with accessibility service enabled and Bubble Key enabled. Record evidence for:

1. Tap `a`: text appears immediately; bubble starts from the physical `a` key and ends at the caret/typed character.
2. Tap a Bangla key in Phonetic/Bijoy output path: same source-to-caret behavior.
3. Soft, Normal, Playful visibly change motion character but not source/destination rules.
4. Rapidly type at least 12 letters: no keyboard lag/crash and no more than 8 bubbles are visible concurrently.
5. Disable accessibility service: typing remains immediate and bubble falls back locally from the pressed key.
6. Test a password/OTP field: no bubble flight is emitted.
7. Switch between two editor apps/fields during active animation: old flights do not jump into the new field.
8. Verify Classic Dark, Glass Modern, Clean Light, and Gradient Pro use the same motion geometry with their own bubble styling.

Record app/device/API level and pass/fail in `docs/STAGE24_1_VERIFICATION_REPORT.md`.

- [ ] **Step 8: Update Stage 24.1 docs and README**

`docs/STAGE24_1_BUBBLE_FLIGHT_TO_CARET.md` must explain the key-origin -> commit -> cursor-target -> overlay -> visual-merge flow, fallback behavior, safety, and no-new-permission rule. README must list Stage 24.1 without changing unrelated product copy.

- [ ] **Step 9: Final verification run after documentation-only changes**

Run:

```bash
python3 scripts/verify_typing_stage24_1.py
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Expected: all PASS / BUILD SUCCESSFUL.

- [ ] **Step 10: Commit Task 5**

```bash
git add scripts/verify_typing_stage24_1.py scripts/typing_stage24_1_bubble_flight_selftest.kt docs/STAGE24_1_BUBBLE_FLIGHT_TO_CARET.md docs/STAGE24_1_VERIFICATION_REPORT.md README.md
git commit -m "test: verify stage 24.1 bubble flight"
```

## Completion Contract

Stage 24.1 is complete only when all of the following are true with fresh evidence:

- Pressed-key source coordinates are exact screen-center coordinates captured before any key-row re-render.
- Key action executes before flight dispatch.
- Exact cursor target is matrix-transformed and generation/age validated.
- One early post-commit retarget is possible without a visible jump.
- Accessibility bounds are fallback-only and never override a fresh exact cursor anchor.
- Remote overlay is accessibility-owned, non-focusable, non-touchable, capped at 8, and detached when idle.
- Missing/failed accessibility overlay uses the local key-origin effect.
- Sensitive/glide/non-letter policies remain unchanged.
- No `SYSTEM_ALERT_WINDOW` permission is added.
- All four keyboard themes use identical flight geometry with theme-specific visual styling.
- Full unit, lint, assemble, and regression verifiers pass.
- Real-device evidence demonstrates key-origin-to-caret flight in at least one common editor.
