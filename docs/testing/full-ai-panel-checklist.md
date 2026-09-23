# Full AI Panel Device Test Checklist

This checklist validates Phase 2B on a real Android device/emulator after the project can be built with Android SDK 36 and Gradle 9.6.

## Prerequisites

- Social AI Keyboard enabled as an IME.
- AI Context Access disclosure accepted and Accessibility service enabled.
- Valid Personal OpenRouter API key saved and tested.
- Managed AI remains unavailable in this phase.

## Conversation actions

- [ ] Open Messenger/WhatsApp/Telegram/Instagram DM or another supported chat.
- [ ] Tap the AI toolbar button.
- [ ] Confirm the panel labels show **Smart Reply**, **Unique Reply**, and **Flirty Reply** for inbox/message surfaces.
- [ ] Tap **Smart Reply** and confirm one result appears without blocking normal typing.
- [ ] Tap **Unique Reply** and confirm the result is witty/context-aware but not insulting.
- [ ] Tap **Flirty Reply** and confirm the result is playful/respectful/non-explicit.
- [ ] Confirm the AI never inserts a result until **Insert** is tapped.
- [ ] Confirm the host app's Send button is never triggered automatically.

## Comment actions

- [ ] Open a supported comment composer whose context adapter exposes a comment hint.
- [ ] Confirm the panel labels show **Smart Comment**, **Unique Comment**, and **Flirty Comment**.
- [ ] Verify Flirty Comment falls back to respectful normal behavior on sensitive/professional/tragedy context.

## Draft actions

- [ ] In any non-sensitive text field, type a draft and open AI.
- [ ] Tap **Rewrite** and confirm meaning is preserved while wording improves.
- [ ] Switch keyboard to English, tap **Translate**, and confirm the target is English.
- [ ] Switch keyboard to Bangla, tap **Translate**, and confirm the target is Bengali.
- [ ] Tap **Grammar** and confirm grammar/spelling/punctuation improve without changing meaning or language.
- [ ] With an empty draft, confirm Rewrite/Translate/Grammar shows a clear “type or select text first” state.

## Draft preservation and insertion

- [ ] Generate a social reply while a manual draft already exists.
- [ ] Tap **Insert** and confirm the draft is not silently overwritten.
- [ ] Confirm explicit **Append** and **Replace Draft** choices appear.
- [ ] Verify **Append** keeps existing text and adds the AI result.
- [ ] Verify **Replace Draft** replaces only after the explicit tap.
- [ ] Confirm no path auto-sends the result.

## Regenerate and cancellation

- [ ] Generate any manual AI action.
- [ ] Tap **Regenerate** and confirm a new request starts.
- [ ] Tap a different AI action while one is still loading; confirm the newest request wins and stale output does not replace it.

## Safety and error states

- [ ] Open a password/PIN/OTP/payment field and confirm the AI toolbar button is disabled.
- [ ] Confirm no manual AI request can run in a sensitive field.
- [ ] Disable network and confirm the panel reports AI offline while normal typing continues.
- [ ] Remove the OpenRouter key and confirm the panel asks for an API key without attempting generation.
- [ ] Disable Accessibility/context consent and confirm social Smart/Unique/Flirty requests report that conversation context is unavailable.
- [ ] Confirm Rewrite/Translate/Grammar can still operate on a local draft in a normal non-sensitive field even when conversation context is unavailable.

## Language behavior

- [ ] Bengali recipient latest message -> Bengali-script reply.
- [ ] Banglish recipient latest message -> Banglish reply.
- [ ] English recipient latest message -> English reply.
- [ ] Other-language recipient latest message -> same language/script where supported by the model.

## Performance

- [ ] Type continuously while an AI request is loading; keystrokes remain responsive.
- [ ] Switch apps/fields during generation; stale manual result does not auto-insert anywhere.
- [ ] Reopen AI panel; no duplicate network request occurs unless an action is tapped or Regenerate is chosen.
