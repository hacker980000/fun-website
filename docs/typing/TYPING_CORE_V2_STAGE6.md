# Typing Core v2 — Stage 6

Daily-use keyboard parity improvements:

- Optional number row on letter layouts.
- Spacebar horizontal cursor control with no accidental space commit after a drag.
- Compact / Normal / Tall key height presets (44 / 50 / 56dp).
- User-controlled haptic feedback and key sound.
- Optional bounded on-device recent clipboard history (20 items, 4,000 chars/item), OFF by default.
- When enabled, the history payload is encrypted with AES-256-GCM using a dedicated Android Keystore key and automatically expires after 24 hours.
- Clipboard content/history is neither read into the panel nor shown in sensitive/password/PIN/OTP fields.
- Clipboard items explicitly marked sensitive by Android are hidden from the panel and never added to persistent history.
- Disabling clipboard history immediately clears encrypted history; Stage 25.2 also purges the legacy plaintext v1 store instead of migrating it.
- Existing AI/backend/context/safety behavior remains unchanged.
