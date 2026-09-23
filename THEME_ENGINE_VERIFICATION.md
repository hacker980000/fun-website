# Premium Neon Theme Engine Verification

Date: 2026-09-19
Branch: `feature/premium-neon-theme`
Baseline application: Social AI Keyboard `35.3.1` (`versionCode 350301`)
Design reference: `docs/design-reference/social-ai-keyboard-neon-reference.jpg`

## Implemented scope

- Locked `Social AI Neon` as the canonical default: deep navy/AMOLED glass surfaces with cyan/blue and violet neon accents.
- Added eight immutable built-in presets plus an editable `Custom` theme.
- Added a central normalized theme model, preferences codec, dedicated DataStore repository, cached neon drawable factory, renderer, and isolated preview view.
- Added live Custom controls for root/panel/key colors, key-label color, primary/secondary neon, AI accent, Enter/action accent, glow, glass opacity, radius, spacing, and key font scale.
- Added manual private background-photo support with exactly three scopes: `FULL_KEYBOARD`, `KEYS_ONLY`, `AI_PANEL_ONLY`.
- Added Fill / Fit / Center Crop, opacity, dark overlay/dim, and blur controls.
- Background images are picked through `OpenDocument`, decoded off-main, downsampled, re-encoded as JPEG in app-private storage, and original EXIF/GPS metadata is not copied into the theme image.
- Added live IME integration without changing typing actions, AI action routing, privacy gates, quota/account/device rules, context adapters, caption flows, Preserve Draft, or explicit Insert/Send behavior.
- Added test sources for presets/repository, renderer, private background manager, Theme Settings, and IME scope integration.

## Checks actually executed in this environment

### PASS — pure Kotlin theme-core self-test

Command:

```bash
kotlinc app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreset.kt \
  scripts/theme_core_selftest.kt -include-runtime -d /tmp/theme_core_selftest.jar
java -jar /tmp/theme_core_selftest.jar
```

Result: `theme core self-test: PASS`

### PASS — pure Kotlin theme preference-codec self-test

Command:

```bash
kotlinc app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreset.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreferencesCodec.kt \
  scripts/theme_codec_selftest.kt -include-runtime -d /tmp/theme_codec_selftest.jar
java -jar /tmp/theme_codec_selftest.jar
```

Result: `theme codec self-test: PASS`

### PASS — release/security static verifier

Command: `python3 scripts/verify_release_ready.py`

Verified API 36 contract, v35.3.1 release contract, Gradle 9.6 wrapper, cleartext/backup disabled, Accessibility non-tool/no-gesture declaration, constrained manifest permissions, and no likely embedded OpenRouter secret.

### PASS — XML/resource preflight

All `app/src/main/res/**/*.xml` files plus `AndroidManifest.xml` parsed successfully. Kotlin application-resource references were checked against declared IDs/strings, with the intentional `android.R` use excluded.

### PASS — manifest/version constraints

- Manifest permission list remains only `android.permission.INTERNET`.
- No broad storage/media permission was added.
- `android:allowBackup="false"` remains enabled.
- `android:usesCleartextTraffic="false"` remains enabled.
- `versionCode = 350301` and `versionName = "35.3.1"` remain unchanged.

### PASS — source whitespace check

`git diff --check` completed without whitespace errors.

## Gradle/Android build limitation in this execution environment

The canonical commands were attempted:

```bash
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon lintDebug assembleDebug
```

Both stopped before project configuration because the wrapper attempted to download `https://services.gradle.org/distributions/gradle-9.6.0-bin.zip` and this environment could not resolve `services.gradle.org` (`java.net.UnknownHostException`). Therefore Robolectric/JUnit Android tests, Android lint, and APK compilation were **not executed**, and this report does not claim they passed.

Before production release, run the canonical commands in Android Studio/CI with JDK 17, Android SDK 36, and Gradle network/cache availability.

## Explicit AI / Premium Settings / Product Identity update — 2026-09-19

Additional source changes verified on branch `feature/explicit-ai-premium-settings`:

- Conversational AI no longer treats context changes as generation triggers. A request starts only from an explicit toolbar AI tap or an explicit AI action button.
- Tap-time intent is resolved as `REPLY`, `CONTINUE`, `START`, or `NEEDS_CONTEXT`; `CONTINUE` explicitly forbids pretending the recipient replied after the user's latest outgoing message.
- Duplicate taps while a request is loading are ignored/disabled.
- Model envelopes are normalized before parsing; malformed JSON-looking output is rejected rather than rendered as raw JSON.
- The plain consent paragraph is replaced by a compact Neon/Glass `AI Features Locked` banner that deep-links to AI & Privacy settings.
- `MainActivity` is now a six-card Premium Neon Settings Hub and retains existing keyboard/theme/privacy/account/API/training/support actions.
- Keyboard backend requests identify the product with `X-SocialAI-Product: KEYBOARD` on authenticated/product-sensitive calls without adding the product header to the device-proof canonical signature.
- Account state supports independent `KEYBOARD` and `ASSISTANT_PRO` entitlements and shows their status/expiry separately. When the old backend omits the `entitlements` field entirely, legacy subscription state is mirrored temporarily to both products for rollout compatibility; an explicit empty entitlement array remains no-access.
- `PRODUCT_ACCESS_REQUIRED` and `PRODUCT_ACCESS_EXPIRED` are mapped to Keyboard-specific user messages.

### Additional checks executed

PASS:

- `python3 scripts/verify_release_ready.py`
- all resource XML + `AndroidManifest.xml` parse checks
- string/resource reference checks
- manifest check confirms no broad media/storage permission
- pure Kotlin `ConversationAiIntentResolver` harness
- pure Kotlin explicit-AI trigger harness
- pure Kotlin model-envelope normalizer harness
- pure Kotlin product-entitlement/error-message harness
- `git diff --check`

The canonical Gradle commands were attempted again:

```bash
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon lintDebug assembleDebug
```

Both still stop at Gradle wrapper download because this execution environment cannot resolve `services.gradle.org` (`java.net.UnknownHostException`). Therefore this environment does not claim JUnit/Robolectric, lint, or APK build success.

### External real-device gate still required

On Android Studio/GitHub CI, run the canonical Gradle commands and then verify on WhatsApp/Messenger:

1. opening a chat makes zero AI requests;
2. incoming/outgoing message changes make zero AI requests;
3. one toolbar AI tap makes one request;
4. only reply text is shown, never JSON wrappers;
5. inserting/sending a result makes no new request;
6. a second explicit tap makes the next request;
7. consent banner disappears after all required consent is enabled;
8. Settings Hub, Theme Settings, and photo backgrounds remain usable;
9. Account & Subscription shows Keyboard and Assistant Pro status independently.
