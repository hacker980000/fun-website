# Multilingual Keyboard + Toolbar Device Checklist

Use this checklist on an Android 8+ device/emulator after `assembleDebug` succeeds.

## Core typing

- [ ] English QWERTY inserts lowercase letters.
- [ ] Shift produces uppercase English letters for the next key and returns to unshifted.
- [ ] Backspace deletes one character when no Bangla phonetic composition is active.
- [ ] Space and Enter work in multiline editors and action editors.
- [ ] `123` opens the number layer, `#+=` opens symbols, and `ABC` returns to letters.

## Bangla

- [ ] `EN/বাংলা` toggles the active language without losing existing draft text.
- [ ] In Bangla Phonetic mode, typing `ami` then Space produces `আমি `.
- [ ] In Bangla Phonetic mode, typing `bangla` then Space produces `বাংলা `.
- [ ] Phonetic backspace edits the current composition before deleting previous committed text.
- [ ] `Phonetic/Bijoy` toggles Bangla input mode.
- [ ] Bijoy-style mode exposes Bengali glyph keys including `ক` and `া`.
- [ ] Bijoy Shift exposes alternate Bengali glyphs.

## Toolbar

- [ ] Emoji panel opens only after tapping the emoji button.
- [ ] Tapping an emoji inserts it at the current cursor.
- [ ] Clipboard panel reads clipboard only after explicit tap.
- [ ] Clipboard history is OFF by default; when enabled, persisted history is encrypted on-device, bounded to 20 items, and expires after 24 hours.
- [ ] Password/PIN/OTP/protected fields and Android-sensitive clips do not expose clipboard payloads in the keyboard panel.
- [ ] Disabling clipboard history clears stored encrypted history and legacy plaintext history.
- [ ] Selecting a clipboard item inserts the full text and returns to keys.
- [ ] Voice button launches the system speech recognizer.
- [ ] Voice result is inserted only if the user returns to the same package/input session.
- [ ] Settings button opens the Social AI Keyboard setup/settings screen.
- [ ] AI toolbar button opens the AI-actions panel placeholder and `ABC` returns to keys.

## Existing AI / safety regression

- [ ] Smart Reply bar can still show one Best Reply for allowed conversation fields.
- [ ] Smart Reply inserts only after user tap.
- [ ] Existing manual draft is not silently overwritten by Smart Reply.
- [ ] Password/PIN/OTP/payment-sensitive fields hide AI suggestions.
- [ ] Voice and AI toolbar actions are disabled in sensitive fields.
- [ ] Normal typing remains usable with network disabled.
