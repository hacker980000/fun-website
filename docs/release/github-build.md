# GitHub Build Pipeline

The canonical build environment for this project is GitHub Actions because it installs the exact Android SDK packages required by the project.

## Debug APK

Every push, pull request, or manual run installs Gradle 9.6.0, restores the official wrapper JAR from that trusted local Gradle installation, verifies the pinned wrapper/distribution SHA-256 values, then executes:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
python3 scripts/check_gradle_wrapper_completeness.py
python3 scripts/verify_release_ready.py
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

The workflow uploads:

```text
app/build/outputs/apk/debug/app-debug.apk
```

as the `social-ai-keyboard-debug-apk` artifact.

## Signed Play upload AAB

The manual `workflow_dispatch` run also creates a signed release app bundle when these GitHub repository secrets are configured:

- `PLAY_UPLOAD_KEYSTORE_B64` — Base64 content of the Play upload keystore (`.jks`).
- `PLAY_UPLOAD_STORE_PASSWORD` — keystore password.
- `PLAY_UPLOAD_KEY_ALIAS` — upload key alias.
- `PLAY_UPLOAD_KEY_PASSWORD` — upload key password.

The workflow restores the keystore only for the release job, passes signing values through Gradle project-property environment variables, runs `lintRelease bundleRelease` (R8 minification + resource shrinking are enabled), uploads `app-release.aab`, then removes the temporary keystore file.

Never commit the keystore or any of these values to the repository.

## Local debug build

Required local toolchain:

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Gradle 9.6.0 on `PATH` for first-time wrapper bootstrap if `gradle-wrapper.jar` is absent

Run:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
python3 scripts/check_gradle_wrapper_completeness.py
python3 scripts/verify_release_ready.py
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

The bootstrap script refuses any wrapper JAR whose SHA-256 does not equal the pinned official Gradle 9.6.x checksum.

## Local signed release bundle

Export Gradle project properties without placing credentials in source control:

```bash
export ORG_GRADLE_PROJECT_RELEASE_STORE_FILE="$PWD/upload-keystore.jks"
export ORG_GRADLE_PROJECT_RELEASE_STORE_PASSWORD='your-keystore-password'
export ORG_GRADLE_PROJECT_RELEASE_KEY_ALIAS='your-upload-key-alias'
export ORG_GRADLE_PROJECT_RELEASE_KEY_PASSWORD='your-upload-key-password'
./gradlew --no-daemon lintRelease bundleRelease
```

The expected bundle is:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Stage 25.6 production network / optimizer contract

The release variant enables R8 minification and resource shrinking and uses `proguard-android-optimize.txt`. Native OkHttp traffic is created only through `SecureHttpClientFactory`, which enforces TLS, exact approved hosts on port 443, bounded timeouts, no automatic redirects, and no automatic connection retry for POST-heavy AI/account traffic. Android Network Security Config also rejects cleartext and trusts user-installed CAs only in debuggable builds.

Keep `mapping.txt` from every Play release so crash traces from the optimized build can be retraced.
