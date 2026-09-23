# Stage 23 Android Studio Build Hotfix

Date: 21 September 2026
Baseline: Stage 23 Real Device Evidence & Baseline

## Issue found on Android Studio

`SocialAiInputMethodService.kt` contained two private helpers with the same Kotlin signature:

- `recordDebugRuntime(ImeRuntimeMetric, Long, Double)` for a start timestamp
- `recordDebugRuntime(ImeRuntimeMetric, Long, Double)` for an elapsed duration

Kotlin does not use parameter names to distinguish overloads, so Android Studio correctly reported `Conflicting overloads` / `Overload resolution ambiguity` during `compileDebugKotlin`.

## Fix

The elapsed-duration helper was renamed to `recordDebugRuntimeElapsed(...)`.

- Start-timestamp call sites keep using `recordDebugRuntime(...)`.
- The explicit elapsed-duration InputConnection call uses `recordDebugRuntimeElapsed(...)`.
- No timing formula, budget, logging payload, prediction logic, AI path, backend contract, privacy policy, or keyboard behavior was changed.

## Verification

- Stage 23 static verification: 36/36 PASS
- Stage 22 static verification: 31/31 PASS
- Stage 21 static verification: 25/25 PASS
- Stage 20 static verification: 20/20 PASS
- Release verification: PASS
- Shared backend config verification: PASS
- Training parity verification: PASS
- Bounded update verification: PASS
- `ai`, `backend`, `context`, `safety` directories are byte-identical to the Stage 23 baseline.

A full Android Gradle compile is intended to be performed in Android Studio because this execution environment does not have the complete Android/Gradle runtime used by the local IDE.
