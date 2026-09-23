# Multilingual Keyboard and Toolbar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the foundation IME into a usable English/Bangla keyboard with Avro-style phonetic input, a Bijoy-style fixed layout, language/layer switching, emoji, clipboard, voice shortcut, and settings access without blocking typing or AI reply generation.

**Architecture:** Keep all layout and transliteration rules in pure Kotlin so they are testable without Android. `SocialAiInputMethodService` owns only IME state, `InputConnection` effects, and rendering. A compact toolbar exposes language mode, AI panel entry point, emoji, clipboard, voice, and settings; auxiliary panels render inside the IME window and never perform network work.

**Tech Stack:** Kotlin 2.3.21, Android InputMethodService, Android SDK 36, JUnit 4, coroutines already present in the foundation.

**Spec:** `docs/superpowers/specs/2026-09-13-social-ai-keyboard-design.md`

## Global Constraints

- Package remains `com.socialaiassistant.keyboard` and app name remains `Social AI Keyboard`.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- Normal typing must remain fully usable offline and must not wait on AI/network operations.
- Existing AI reply insertion remains explicit-tap only; Send remains user-controlled.
- Password/PIN/OTP/payment/banking-sensitive fields continue to hard-block AI context and suggestions.
- Existing foundation behavior and tests must remain intact.
- Phase 2A does not add cloud sync, managed-AI backend, or media capture.

---

### Task 1: Keyboard state and layout model

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardMode.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardAction.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardLayout.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/KeyboardModeTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/KeyboardLayoutBanglaTest.kt`

**Interfaces:**
- Produces: `enum class KeyboardLanguage { ENGLISH, BANGLA }`, `enum class BanglaInputMode { PHONETIC, BIJOY }`, `enum class KeyboardLayer { LETTERS, NUMBERS, SYMBOLS }`, and `data class KeyboardUiMode(...)`.
- Produces actions: `ToggleLanguage`, `ToggleBanglaMode`, `ShowNumbers`, `ShowLetters`, `ShowSymbols`, `OpenEmoji`, `OpenClipboard`, `OpenVoice`, `OpenSettings`, `OpenAiPanel`.
- `KeyboardLayout.forMode(mode)` returns the full key rows for current state.

- [ ] **Step 1: Write failing state-model test**

```kotlin
@Test
fun `english toggles to bangla and keeps letters layer`() {
    val start = KeyboardUiMode()
    val changed = start.reduce(KeyboardAction.ToggleLanguage)
    assertEquals(KeyboardLanguage.BANGLA, changed.language)
    assertEquals(KeyboardLayer.LETTERS, changed.layer)
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew testDebugUnitTest --tests '*KeyboardModeTest*'`

Expected: FAIL because `KeyboardUiMode`/`ToggleLanguage` do not exist.

- [ ] **Step 3: Implement the pure state model**

```kotlin
enum class KeyboardLanguage { ENGLISH, BANGLA }
enum class BanglaInputMode { PHONETIC, BIJOY }
enum class KeyboardLayer { LETTERS, NUMBERS, SYMBOLS }

data class KeyboardUiMode(
    val language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val banglaMode: BanglaInputMode = BanglaInputMode.PHONETIC,
    val layer: KeyboardLayer = KeyboardLayer.LETTERS,
    val shifted: Boolean = false
) {
    fun reduce(action: KeyboardAction): KeyboardUiMode = when (action) {
        KeyboardAction.ToggleLanguage -> copy(
            language = if (language == KeyboardLanguage.ENGLISH) KeyboardLanguage.BANGLA else KeyboardLanguage.ENGLISH,
            layer = KeyboardLayer.LETTERS,
            shifted = false
        )
        KeyboardAction.ToggleBanglaMode -> copy(
            banglaMode = if (banglaMode == BanglaInputMode.PHONETIC) BanglaInputMode.BIJOY else BanglaInputMode.PHONETIC,
            layer = KeyboardLayer.LETTERS,
            shifted = false
        )
        KeyboardAction.ShowNumbers -> copy(layer = KeyboardLayer.NUMBERS, shifted = false)
        KeyboardAction.ShowSymbols -> copy(layer = KeyboardLayer.SYMBOLS, shifted = false)
        KeyboardAction.ShowLetters -> copy(layer = KeyboardLayer.LETTERS)
        KeyboardAction.Shift -> copy(shifted = !shifted)
        else -> this
    }
}
```

- [ ] **Step 4: Write failing layout tests for English, Bangla Bijoy, numbers, symbols**

```kotlin
@Test
fun `bijoy letters expose bengali glyphs`() {
    val layout = KeyboardLayout.forMode(
        KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.BIJOY)
    )
    assertTrue(layout.rows.flatten().any { it.output == "ক" })
    assertTrue(layout.rows.flatten().any { it.output == "া" })
}
```

- [ ] **Step 5: Implement `KeyboardLayout.forMode`**

Use these deterministic layouts:
- English letters: existing QWERTY rows.
- Bangla Bijoy-style glyph rows: `ৌৈাীূবহগদজড`, `োে্িুপরকতচট`, `ংঁমুনলসয়ষ`, with shifted variants exposing common alternates such as `ঔঐআঈঊভঙঘধঝঢ`.
- Number layer: `1 2 3 4 5 6 7 8 9 0`, then `@ # ৳ _ & - + ( ) /`, then symbol switch/backspace.
- Symbol layer: `[ ] { } # % ^ * + =`, then `_ \\ | ~ < > € £ ¥ •`, then letter switch/backspace.

All layers end with language/mode key, space, enter.

- [ ] **Step 6: Run focused tests, then commit**

Run: `./gradlew testDebugUnitTest --tests '*KeyboardModeTest*' --tests '*KeyboardLayoutBanglaTest*'`

Commit: `git commit -am 'feat: add multilingual keyboard state and layouts'`

---

### Task 2: Avro-style phonetic Bangla composer

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaPhoneticComposer.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/BanglaPhoneticComposerTest.kt`

**Interfaces:**
- `data class PhoneticResult(val committed: String, val composing: String)`.
- `class BanglaPhoneticComposer { fun reset(); fun acceptLatin(token: String): PhoneticResult; fun backspace(): PhoneticResult; fun flush(): String }`.
- Composer handles a compact deterministic subset required for first usable release and leaves unknown sequences as Latin instead of deleting user input.

- [ ] **Step 1: Write failing behavior tests**

```kotlin
@Test fun `ami produces আমি`() {
    val c = BanglaPhoneticComposer()
    "ami".forEach { c.acceptLatin(it.toString()) }
    assertEquals("আমি", c.flush())
}

@Test fun `bangla produces বাংলা`() {
    val c = BanglaPhoneticComposer()
    "bangla".forEach { c.acceptLatin(it.toString()) }
    assertEquals("বাংলা", c.flush())
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew testDebugUnitTest --tests '*BanglaPhoneticComposerTest*'`

Expected: FAIL because composer does not exist.

- [ ] **Step 3: Implement a longest-match transliteration engine**

The engine keeps a Latin buffer and resolves the longest suffix using ordered mappings. Include at minimum:

```kotlin
private val words = mapOf(
    "ami" to "আমি",
    "bangla" to "বাংলা",
    "banglay" to "বাংলায়",
    "tumi" to "তুমি",
    "apni" to "আপনি",
    "valo" to "ভালো",
    "bhalo" to "ভালো",
    "kemon" to "কেমন",
    "achi" to "আছি",
    "acchi" to "আছি",
    "dhonnobad" to "ধন্যবাদ"
)
```

and grapheme mappings for common sequences: `kh→খ`, `gh→ঘ`, `ch→চ`, `jh→ঝ`, `th→থ`, `dh→ধ`, `ph→ফ`, `bh→ভ`, `sh→শ`, `ng→ং`, `kkh→ক্ষ`, plus consonants/vowels needed by the tests. Word-boundary flush must prioritize whole-word mappings, then grapheme mapping. Unknown input is preserved.

- [ ] **Step 4: Add backspace and unknown-input tests, run GREEN**

```kotlin
@Test fun `unknown sequence is preserved`() {
    val c = BanglaPhoneticComposer()
    "xyz".forEach { c.acceptLatin(it.toString()) }
    assertEquals("xyz", c.flush())
}
```

Run: `./gradlew testDebugUnitTest --tests '*BanglaPhoneticComposerTest*'`

- [ ] **Step 5: Commit**

Commit: `git add app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaPhoneticComposer.kt app/src/test/java/com/socialaiassistant/keyboard/ime/BanglaPhoneticComposerTest.kt && git commit -m 'feat: add bangla phonetic composer'`

---

### Task 3: Integrate multilingual typing into the IME

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/main/res/layout/ime_keyboard.xml`
- Test: `app/src/androidTest/java/com/socialaiassistant/keyboard/ime/ImeInsertionInstrumentedTest.kt`

**Interfaces:**
- `SocialAiInputMethodService` stores `KeyboardUiMode` and one `BanglaPhoneticComposer`.
- Letter taps in English commit immediately.
- Letter taps in Bangla/BIJOY commit Bengali output immediately.
- Letter taps in Bangla/PHONETIC feed the Latin token to the composer and use `InputConnection.setComposingText`.
- Space/enter/language/layer changes flush composition before their own action.

- [ ] **Step 1: Add an instrumentation test contract for composition**

```kotlin
@Test
fun phoneticMode_commitsBanglaWordBeforeSpace() {
    // In a host EditText, type a m i then space through the IME action API.
    // Expected editor text: "আমি "
}
```

- [ ] **Step 2: Implement mode-aware key dispatch**

Add helpers:

```kotlin
private fun commitTextKey(value: String) {
    val connection = currentInputConnection ?: return
    val mode = keyboardMode
    if (mode.language == KeyboardLanguage.BANGLA && mode.banglaMode == BanglaInputMode.PHONETIC) {
        val result = phoneticComposer.acceptLatin(value.lowercase())
        if (result.committed.isNotEmpty()) connection.commitText(result.committed, 1)
        connection.setComposingText(result.composing, 1)
    } else {
        connection.commitText(value, 1)
    }
}

private fun flushPhoneticComposition() {
    val connection = currentInputConnection ?: return
    val word = phoneticComposer.flush()
    if (word.isNotEmpty()) {
        connection.setComposingText(word, 1)
        connection.finishComposingText()
    }
}
```

State-changing actions flush first, then reduce `keyboardMode` and call `renderKeys()`.

- [ ] **Step 3: Make the toolbar row visible in XML**

Add `@id/keyboard_toolbar` above `keyboard_rows` with seven 44dp buttons: `AI`, `EN/বাংলা`, `অ/বিজয়`, `☺`, `📋`, `🎤`, `⚙`. Keep IDs stable for Task 4.

- [ ] **Step 4: Run unit tests and Android compile/test when environment permits**

Run: `./gradlew testDebugUnitTest`

Run: `./gradlew assembleDebug`

If Android/Gradle dependency download is unavailable, record the exact failure and additionally compile pure Kotlin files with `kotlinc`.

- [ ] **Step 5: Commit**

Commit: `git add app/src && git commit -m 'feat: integrate english and bangla ime modes'`

---

### Task 4: Emoji, clipboard, voice, and settings toolbar actions

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardPanel.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/EmojiCatalog.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/main/res/layout/ime_keyboard.xml`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/EmojiCatalogTest.kt`

**Interfaces:**
- `enum class KeyboardPanel { KEYS, EMOJI, CLIPBOARD, AI }`.
- `EmojiCatalog.recentAndCommon()` returns a fixed initial list of safe common emoji.
- Clipboard panel reads `ClipboardManager.primaryClip` only after explicit user tap and shows at most 8 textual clips from current primary clip; it does not persist clipboard content.
- Voice button launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`; returned text is committed only after user completes the system voice UI.
- Settings button launches `MainActivity` with `FLAG_ACTIVITY_NEW_TASK`.

- [ ] **Step 1: Write failing emoji catalog test**

```kotlin
@Test fun `common emoji list is bounded and unique`() {
    val values = EmojiCatalog.recentAndCommon()
    assertTrue(values.size in 12..40)
    assertEquals(values.size, values.distinct().size)
    assertTrue("🙂" in values)
}
```

- [ ] **Step 2: Implement emoji catalog and panel enum**

Use this initial catalog:

```kotlin
listOf("🙂", "😊", "😂", "❤️", "👍", "🙏", "😍", "🥰", "😅", "😢", "😮", "🔥", "🎉", "✅", "💯", "🤝", "👏", "👌", "🤔", "🙌")
```

- [ ] **Step 3: Add reusable auxiliary panel container to XML**

Add `@id/keyboard_panel_container` below the toolbar and above key rows. When panel is not `KEYS`, hide `keyboard_rows`, render panel content dynamically, and show a `ABC` back button.

- [ ] **Step 4: Implement toolbar handlers**

- AI: set panel to `AI` but for Phase 2A render a non-network placeholder text `AI actions` plus a back button; Phase 2B owns actual actions.
- Emoji: render catalog as tap buttons; tap commits emoji.
- Clipboard: read primary text on explicit tap, render non-empty text snippets; selecting commits the clip text.
- Voice: start Android speech recognizer intent using language hint from current keyboard language.
- Settings: open `MainActivity`.

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew testDebugUnitTest --tests '*EmojiCatalogTest*'`

Run: `./gradlew testDebugUnitTest`

Commit: `git add app/src && git commit -m 'feat: add keyboard utility toolbar panels'`

---

### Task 5: Phase verification and packaging

**Files:**
- Create: `docs/testing/multilingual-keyboard-toolbar-checklist.md`
- Modify: `README.md`

**Interfaces:** None.

- [ ] **Step 1: Run fresh verification**

Run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Also run pure Kotlin fallback verification if Gradle cannot resolve dependencies:

```bash
kotlinc \
  app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardAction.kt \
  app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardMode.kt \
  app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardLayout.kt \
  app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaPhoneticComposer.kt \
  app/src/main/java/com/socialaiassistant/keyboard/ime/EmojiCatalog.kt \
  -d /tmp/social-ai-keyboard-phase2.jar
```

- [ ] **Step 2: Create device checklist**

Checklist must cover:
- English typing and Shift.
- Bangla phonetic `ami` → `আমি`, `bangla` → `বাংলা`.
- Bijoy-style Bengali glyph entry.
- EN/বাংলা and Phonetic/Bijoy switching.
- Number/symbol layers.
- Emoji insertion.
- Clipboard panel only after explicit tap.
- Voice recognizer return insertion.
- Settings launch.
- Existing Smart Reply still renders and inserts only on tap.
- Password/OTP fields keep AI hidden.

- [ ] **Step 3: Update README and commit**

Commit: `git add README.md docs/testing/multilingual-keyboard-toolbar-checklist.md && git commit -m 'docs: add multilingual keyboard verification guide'`
