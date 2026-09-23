# Stage 25.6 — Production Network & Release Hardening

## Scope

This stage hardens the native HTTP boundary and the production release build without changing product behavior or AI prompting semantics.

## Changes

### 1. Hardened production OkHttp client

`SocialAiApplication` no longer creates a raw `OkHttpClient()`. `SecureHttpClientFactory` now creates the shared client used by the managed backend and personal OpenRouter gateway.

The production client enforces:

- TLS-only `ConnectionSpec.MODERN_TLS`;
- exact approved hosts (`BackendConfig.API_BASE` host and `openrouter.ai`);
- port 443 only;
- no URL user-info credentials;
- 8 s connect, 20 s read, 15 s write, 26 s absolute OkHttp call timeout;
- `retryOnConnectionFailure(false)` so account/AI POST requests are not automatically replayed after transport failure;
- `followRedirects(false)` and `followSslRedirects(false)` so a server-controlled Location cannot move bearer/device-proof/prompt traffic to another origin.

The existing coroutine request budgets (22 s OpenRouter, 24 s managed backend) remain stricter than the client call timeout.

### 2. Android Network Security Config

`@xml/network_security_config` is attached to the application. Release traffic has `cleartextTrafficPermitted=false` and trusts platform/system roots. A user-installed CA is accepted only through `debug-overrides`, which Android ignores for non-debuggable release builds.

Certificate pinning was deliberately not added in this stage. The backend is hosted behind managed public TLS infrastructure; static leaf/intermediate pins would create an availability/rotation risk unless an operational pin-rotation process and backup pins are in place.

### 3. Request/data handling

OpenRouter and managed backend requests now explicitly advertise JSON; OpenRouter also sends `Cache-Control: no-store` like the managed backend. Managed backend exceptions no longer retain up to 2,000 characters of arbitrary raw server response data, reducing the chance that account/provider details later reach logs or crash tooling.

### 4. Production optimizer

Release builds now enable:

```text
isMinifyEnabled = true
isShrinkResources = true
isDebuggable = false
proguard-android-optimize.txt
```

R8 rules keep source/line mapping metadata for retrace while renaming the source-file attribute. No broad package keep rule, `-dontshrink`, `-dontoptimize`, or `-dontobfuscate` was added.

The signed release workflow now runs `lintRelease bundleRelease`. The packaged Unix `gradlew` launcher is also marked executable; the verified wrapper JAR bootstrap policy is unchanged.

## Dependency audit (not mixed into this patch)

The source currently pins Room 2.7.2, DataStore 1.1.7, Lifecycle 2.9.4, AppCompat 1.7.1, Core 1.17.0 and OkHttp 4.12.0. As of 2026-09-22, several newer stable versions exist (for example Room 2.8.5, DataStore 1.2.1, Lifecycle 2.11.0 and AppCompat 1.8.0; OkHttp has moved to the 5.x line). These updates are intentionally deferred to a separate dependency-modernization stage because this environment cannot execute the full Android Gradle build. A major OkHttp upgrade should not be combined with the network-policy change without Gradle unit/lint/release and device coverage.

Core 1.18.0 also raises its compile SDK requirement to API 36.1, while this project deliberately targets compileSdk 36, so it is not a drop-in version bump for the current release contract.

## Verification

- Stage 25.6 static verifier: 25/25 PASS.
- NetworkEndpointPolicy pure-Kotlin self-test: 9/9 PASS.
- Stage 25.0 memory: 13/13 PASS.
- Stage 25.1 Accessibility privacy: 11/11 PASS.
- Stage 25.2 clipboard privacy: 18/18 PASS.
- Stage 25.3 sensitive fields: 24/24 PASS.
- Stage 25.4 conversation context: 27/27 PASS.
- Stage 25.5 WebView security: 28/28 PASS.
- Stage 24.3 settings contract: 158/158 PASS.
- Stage 24.1 bubble contract: 48/48 PASS.
- Stage 24.2 optional theme bubble contract: 30/30 PASS.
- Release verifier: PASS.
- Shared Python verifier sweep: 44 PASS / 6 unchanged historical FAIL; no prior PASS became FAIL.

## Environment limitation

This container does not provide the project's full Android SDK/Gradle runtime and the source intentionally does not vendor the verified `gradle-wrapper.jar`. Therefore this stage does not claim `assembleRelease`, `lintRelease`, R8 execution, Robolectric/instrumentation, AAB generation, or physical-device validation. Those are enforced/documented in CI and the release-device checklist once the verified Gradle 9.6 wrapper is restored.
