# Foundation Vertical Slice Verification Checklist

This checklist verifies the first Android vertical slice of Social AI Keyboard. It covers the offline IME, sensitive-field gate, Accessibility context bridge, encrypted local history, Personal OpenRouter mode, automatic Best Smart Reply, explicit tap-to-insert, and manual Send boundary.

## Automated verification

Run from the project root on a development machine with JDK 17, Android SDK 36, and network access for the first Gradle dependency download:

```bash
./gradlew clean testDebugUnitTest assembleDebug
./gradlew lintDebug
```

Expected result: both commands finish successfully and the APK exists at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Current sandbox status — 2026-09-13

The source/static verification steps below were completed in the build sandbox. Full Gradle tests, Android lint, and APK assembly could not run because the sandbox has no Android SDK/Gradle distribution installed and DNS/network access is blocked, so the Gradle wrapper cannot download Gradle 9.6.0. This is an environment blocker, not a recorded passing build.

Completed local checks:

- XML resources parse successfully.
- `git diff --check` reports no whitespace errors.
- Pure-Kotlin language/prompt smoke checks pass.
- Pure-Kotlin ReplyOrchestrator cache/blocking behavior smoke checks pass.
- No production `Log.*`, `println`, or `print` calls were found.
- No Accessibility click, gesture dispatch, global action, or autonomous Send code was found.
- Conversation Room entities persist ciphertext + IV, not a plaintext message-body column.
- Personal OpenRouter storage persists ciphertext + IV + a last-four mask, not the raw key.

## Manual Android device/emulator checklist

- [ ] 1. Install the debug APK, open **Social AI Keyboard**, enable the keyboard in Android settings, and select it as the current keyboard.
- [ ] 2. Turn network access off. Verify ordinary English typing still works immediately.
- [ ] 3. While an AI request is loading, verify letters, Space, Shift, Backspace, and Enter remain responsive.
- [ ] 4. Open password, PIN, OTP, card/payment, and banking-sensitive fields. Verify the AI Smart Reply area is hidden and no conversational context is persisted for those fields.
- [ ] 5. Leave Accessibility disabled. Verify normal typing works and AI waits for conversation context rather than blocking the keyboard.
- [ ] 6. In the app, read and accept the prominent AI Context Access disclosure, then enable the Accessibility service. Open a normal chat composer and verify a generic visible-context snapshot can be captured.
- [ ] 7. Reopen the same conversation without changing visible recipient content. Verify the same context fingerprint does not trigger an unnecessary second model request.
- [ ] 8. Receive/show a new recipient message. Verify a new **Best Reply** is generated.
- [ ] 9. With an empty composer, tap the Best Reply once. Verify the reply is inserted only after the tap.
- [ ] 10. Type a manual draft first, then tap the Best Reply. Verify the draft is not overwritten automatically and explicit Insert/Replace choices appear.
- [ ] 11. Verify the app never presses **Send** automatically. Sending must remain a user action in the host app.
- [ ] 12. Save an invalid OpenRouter key and run the connection test. Verify a safe invalid-key message appears and the key itself is never displayed or logged.
- [ ] 13. Tap **Disable AI Context Access** in the app. Verify visible conversations stop being read even if the Android Accessibility service remains enabled.
- [ ] 14. Remove the saved API key. Verify AI shows an API-key-required state while normal typing continues.

## Device verification gate

Do not create the `foundation-v0.1.0` tag until every manual item above passes on a real device or emulator and any discovered defect has been fixed and re-tested.
