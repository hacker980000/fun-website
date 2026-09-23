# Stage 26.1 — A–Z Quality, Performance & Release-Readiness Audit

Date: 2026-09-22  
Project: Social AI Keyboard / Social AI Assistant Pro v35.3.1 Android IME

## Executive status

**Source-level readiness: PASS after hardening.** The project passes every source-contract verifier currently shipped in the repository and all Android-independent runtime policy self-tests that can run in this environment.

**Android build/device readiness: NOT YET PROVEN in this execution environment.** The source archive does not vendor `gradle/wrapper/gradle-wrapper.jar`, and this environment has neither the Android SDK nor an installed Gradle 9.6.0 distribution and cannot reach the required binary/dependency hosts from its shell. Therefore `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `connectedDebugAndroidTest`, and `bundleRelease` have not been claimed as green here.

## Faults found and fixed in Stage 26.1

### 1. Unbounded encrypted conversation history — fixed

Previously `ConversationRepository` loaded/decrypted the full stored conversation and the Room database could grow without a per-conversation retention bound. Long-lived chats could therefore create increasing I/O, decrypt work, memory pressure, and local data retention.

Stage 26.1 now:

- Reads only the recent bounded tail from Room.
- Keeps at most **200 encrypted messages per stable conversation**.
- Prunes older rows after snapshot merges.
- Preserves chronological order for returned history.
- Retains the existing no-persistence rule when a stable conversation identity is unavailable.
- Adds a repository regression test for the 200-message retention contract.

A standalone SQLite simulation verified that 205 rows are reduced to the newest 200 while a different conversation remains untouched.

### 2. Coroutine cancellation swallowed by lifecycle error handlers — fixed

Several Activity/theme flows previously used generic `Throwable` catches or `runCatching` around suspend work. That can transform normal lifecycle cancellation into a user-visible failure or allow work to continue after the owning lifecycle is cancelled.

Stage 26.1 explicitly rethrows `CancellationException` in:

- `AuthActivity`
- `CaptionActivity`
- `SettingsCategoryActivity` account refresh/logout
- `ThemeSettingsActivity`
- `ThemeOnboardingActivity`
- `SettingsThemeOnboardingActivity`
- `ThemeBackgroundManager`

### 3. Android 12+ backup / device-transfer policy — hardened

The app already had `android:allowBackup="false"` and legacy full-backup disabled. Stage 26.1 additionally attaches `@xml/data_extraction_rules` and explicitly excludes root/files/databases/shared preferences/external app data from both cloud backup and device transfer. This is particularly important for a keyboard app holding local encrypted conversation/clipboard/account state.

### 4. Theme photo import cancellation/error cleanup — fixed

Theme image import now deletes temporary/partial output files on cancellation or failure, recycles temporary bitmaps deterministically, and the Activity performs cancellation cleanup non-cancellably before propagating cancellation. This prevents orphan private files after an interrupted import.

### 5. Windows Gradle-wrapper bootstrap parity — fixed

The PowerShell bootstrap now mirrors the Unix script:

- verifies the exact Gradle 9.6.0 wrapper-JAR SHA-256;
- attempts the official Gradle repository first;
- falls back to a locally installed **exact Gradle 9.6.0** to generate the wrapper;
- rejects mismatched wrapper binaries.

### 6. New Stage 26.1 A–Z source gate — added

`scripts/verify_stage26_1_a_to_z_readiness.py` now enforces the new retention, backup, WebView/network, lifecycle, bounded-state, and wrapper-bootstrap invariants. The existing release verifier also enforces the new backup and conversation-retention contracts.

## Verification results

| Area | Result | Evidence |
|---|---|---|
| Complete `scripts/verify*.py` suite | **PASS — 51/51** | Includes release + Stage 26.1 A–Z gate |
| Python verifier syntax | **PASS** | `python3 -m py_compile scripts/*.py` |
| Shell syntax | **PASS — 22/22** | `bash -n` on project shell entry points |
| Production Manifest/resource XML parse | **PASS — 36/36** | XML parser sweep |
| Conversation-context self-test | **PASS — 18/18** | Stage 25.4 |
| Network endpoint-policy self-test | **PASS — 9/9** | Stage 25.6 |
| Sensitive-field AI gate self-test | **PASS** | Stage 25.3 |
| Settings navigation self-test | **PASS — 8/8** | Stage 24.3 |
| Bubble-flight policy self-test | **PASS — 8/8 + 4/4 dispatch** | Stage 24.1 |
| Theme-bubble policy self-test | **PASS — 8/8** | Stage 24.2 |
| Web-navigation policy self-test | **PASS — 21/21** | Stage 25.5 |
| Modified conversation Kotlin source | **PASS (stub compile)** | Kotlin compiler + Room/crypto stubs, coroutines runtime |
| Room prune SQL behavior | **PASS** | SQLite retention simulation |
| Production permission surface | **PASS** | Only `android.permission.INTERNET` |
| Cleartext network policy | **PASS** | Manifest + Network Security Config deny cleartext |
| WebView dangerous API scan | **PASS** | no JS bridge, no mixed-content allow, file/content access disabled |
| Native HTTP egress | **PASS** | TLS-only, exact allowlist, no redirects/replay retries |
| Local secret/clipboard storage | **PASS source review** | Android Keystore AES-GCM; bounded encrypted clipboard |
| Accessibility traversal | **PASS source review** | no gesture automation; node/depth bounds; disclosure gating |
| Actual Gradle Android compilation | **UNVERIFIED HERE** | Android SDK / wrapper binary unavailable in this environment |
| Emulator/device instrumentation | **UNVERIFIED HERE** | requires Android SDK/emulator/device |
| Signed release AAB | **UNVERIFIED HERE** | requires signing secrets + Gradle build |

## Security/privacy review

The production Manifest requests only Internet access. The Accessibility service requires the platform bind permission, does not request gesture capability, and traversal is bounded. WebViews disable local file/content access, universal file URL access, mixed content, third-party cookies, geolocation and popup windows; WebView debugging is tied to `BuildConfig.DEBUG`. Native OkHttp traffic is restricted to HTTPS port 443 and the expected backend/OpenRouter hosts, with redirects and automatic connection-failure replay disabled.

OpenRouter keys, managed sessions, account training, conversation message bodies, and recent clipboard history are protected with Android Keystore-backed AES-GCM storage paths. Recent clipboard data is bounded to 20 items / 4,000 characters each with a 24-hour retention window; conversation history is now bounded to 200 encrypted messages per stable conversation.

No trust-all TLS manager, permissive hostname verifier, JavaScript bridge, production storage/SMS/microphone/overlay permission, or cleartext endpoint was found in the source scan.

## Performance review

Positive controls already present include deferred typing-learning persistence, bounded accessibility traversal (250 nodes / depth 8), bounded conversation extraction (40 messages / 12,000 characters), bounded AI payloads, a 16 MiB theme-background bitmap cache, off-main-thread image decoding/processing, render coalescing, bounded bubble animations, and memory-trim cleanup in the IME.

Stage 26.1 removes the largest newly identified long-term growth path: unbounded Room message history. The remaining main-thread SharedPreferences reads/writes are small/bounded and are not the per-keystroke persistence path.

## Build / release blockers that remain

### A. `gradle-wrapper.jar` is not vendored

`python3 scripts/check_gradle_wrapper_completeness.py` correctly reports **BOOTSTRAP REQUIRED**. The wrapper properties and SHA pins are present, and both Unix/Windows bootstrap scripts restore only the checksum-verified Gradle 9.6.0 wrapper JAR. For the strongest clone/extract-and-build experience, vendor the verified wrapper JAR in source control.

### B. Hosted authentication callback requires external verification

`BackendConfig.ANDROID_AUTH_CALLBACK` remains the existing `https://aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.chromiumapp.org/auth` browser-identity callback because the source comments state that the current hosted v35.3.1 auth flow accepts that callback. The backend page was not reachable from the available web execution environment, so this contract could not be independently proven. **Do not change it blindly**; verify login/register/code exchange on the production backend and a real Android build before release.

### C. Full Android build is still required

On a machine/CI runner with JDK 17, Android SDK Platform 36, Build Tools 36.0.0, and network/caches available, the mandatory release gate remains:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
python3 scripts/check_gradle_wrapper_completeness.py
bash scripts/preflight_local_build.sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
./gradlew --no-daemon lintRelease bundleRelease
```

Then run the API 36 smoke job and the API 26/30/36 compatibility matrix before Play release.

## Non-blocking maintainability debt

- `SocialAiInputMethodService.kt` is ~2,700 lines; extracting rendering, panel orchestration, and lifecycle coordinators would reduce regression risk.
- `SettingsCategoryActivity.kt` and `ThemeSettingsActivity.kt` are also large and should gradually move toward smaller controllers/binders.
- Room still uses `exportSchema = false`. Before database version 2, enable schema export and preserve migration fixtures.
- The project is still on legacy KAPT for Room. Migrate to KSP in a separately build-verified dependency/tooling stage rather than combining it with production behavior changes.
- Only one Android instrumentation Kotlin test is present; the project compensates with extensive shell/device evidence scripts, but more direct instrumentation around IME lifecycle/context/auth boundaries would improve confidence.

## Release decision

Stage 26.1 is **source-level hardened and internally consistent**, but should not be labeled a fully build-verified release until the canonical Gradle unit/lint/APK/AAB jobs and Android emulator/device gates complete successfully, and the hosted auth callback is exercised end-to-end.
