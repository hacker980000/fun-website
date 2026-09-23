# Typing Core v2 — Stage 11: Build + IME lifecycle hardening

Stage 11 focuses on build/toolchain trust and Android IME lifecycle correctness rather than new AI features.

## IME lifecycle hardening

- Numeric, phone, and date/time editors initially open the number layer; normal text editors open letters.
- Enter key labels follow the editor action (`Go`, `Search`, `Send`, `Next`, `Done`, `Previous`) while `IME_FLAG_NO_ENTER_ACTION` stays a newline key.
- Enter execution uses `InputMethodService.sendDefaultEditorAction(true)` and falls back to a physical Enter key event.
- `onStartInputView` / `onFinishInputView` explicitly reset transient gestures and stale suggestion UI.
- `onUpdateSelection` discards local Roman/composing buffers if the user moves the cursor away from the active composing region. Normal callbacks caused by `setComposingText()` do not cancel composition.
- Backspace repeat is service-owned and is cancelled on input changes, view hiding, configuration change, and service destruction to prevent phantom deletion.
- Glide gesture state is cancelled on the same lifecycle boundaries.
- Rotation/configuration change re-renders the active keyboard surface and reapplies the theme/one-handed geometry.
- Fullscreen extract mode is disabled for a more consistent modern keyboard experience in landscape.

## Gradle/toolchain hardening

- Android Gradle Plugin remains 9.4.0 with Gradle 9.6.0 and Java 17.
- The Gradle 9.6.0 binary distribution is pinned with SHA-256:
  `bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`.
- The trusted Gradle 9.6.x wrapper JAR SHA-256 is pinned as:
  `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`.
- The inherited source does not contain the official wrapper JAR. `scripts/bootstrap_gradle_wrapper.sh` restores it only from an installed Gradle 9.6.0 distribution and refuses a checksum mismatch.
- GitHub Actions now installs Gradle 9.6.0, bootstraps the wrapper, verifies both wrapper/distribution checksum contracts, then runs tests/lint/APK or AAB builds with `./gradlew`.

## Verification boundary

Pure Kotlin policy/regression tests and static project checks can run in the current environment. A full Android Gradle build still requires network access to Gradle/Google/Maven artifacts or a populated local cache. The source must not claim an APK/device pass until CI or an Android SDK environment actually completes those tasks.
