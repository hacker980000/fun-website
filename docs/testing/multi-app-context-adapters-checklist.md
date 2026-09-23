# Phase 2C Multi-App Context Adapters Device Checklist

Use a real Android device with Social AI Keyboard enabled as the active IME and AI Context Access enabled. Keep final Send manual in every test.

## Setup

- [ ] Install the current build and select Social AI Keyboard as the active keyboard.
- [ ] Accept the in-app AI Context Access disclosure and enable the Accessibility service.
- [ ] Save and successfully test a Personal OpenRouter API key.
- [ ] Confirm a password/PIN/OTP/payment field hides AI suggestions and does not expose conversation context.

## Facebook

- [ ] Open a feed post comment box. AI panel shows **Smart Comment / Unique Comment / Flirty Comment**.
- [ ] Verify visible post/comment text is captured without toolbar noise such as Like/Reply/Share/Send.
- [ ] Open a Messenger-style chat surface inside Facebook. AI panel switches to **Smart Reply / Unique Reply / Flirty Reply**.
- [ ] Switch between two people/threads and confirm stored history does not mix across conversation keys.

## Messenger

- [ ] Open a one-to-one conversation. Context is classified as Inbox.
- [ ] Confirm `Active now`, `Seen`, `Delivered`, call buttons, GIF/sticker labels are not treated as messages.
- [ ] Confirm left/right sender inference still identifies recipient vs self for visible bubbles.
- [ ] Tap a generated reply: it inserts only after the tap and never presses Send.

## WhatsApp

- [ ] Open a chat. Context is classified as Inbox.
- [ ] Confirm the visible contact/thread name becomes the conversation identity when available.
- [ ] Confirm message text remains while `online`, time-only rows, `Delivered`-style UI noise are excluded.
- [ ] Open Search and verify the adapter does not classify Search as an Inbox composer.

## Instagram

- [ ] Open a post comment composer containing an Add/Write Comment hint. AI panel shows Comment labels.
- [ ] Open a Direct Message conversation. AI panel shows Reply labels.
- [ ] Confirm generic Instagram navigation labels are not inserted as conversation messages.

## Telegram

- [ ] Open a chat. Context is classified as Inbox.
- [ ] Confirm contact/thread identity is stable when the toolbar title resource is exposed.
- [ ] Open Search and verify the surface is not treated as a normal chat composer.

## Unsupported App Privacy Boundary

- [ ] Open a chat/SMS/email app whose package is not in the supported adapter allowlist.
- [ ] Confirm normal keyboard typing still works.
- [ ] Confirm AI conversation context is not captured and no ContextSnapshot is produced for the unsupported package.
- [ ] Switch from a supported chat to the unsupported app and confirm any previous ContextSnapshot is cleared rather than reused.
- [ ] If Bubble Key is enabled, confirm its transient editable-bounds animation can still fall back safely without reading or persisting conversation text.

## Tone Presets

- [ ] Open the AI panel and tap the Tone button repeatedly.
- [ ] Confirm sequence: Auto → Friendly → Professional → Casual → Concise → Warm → Auto.
- [ ] Close/reopen the keyboard and confirm the selected tone persists.
- [ ] Generate a reply and confirm tone changes do not alter recipient-language matching rules.

## Custom AI Instruction

- [ ] Open Social AI Keyboard settings.
- [ ] Save a custom instruction such as `Keep replies practical and end with one short question.`
- [ ] Reopen AI panel and confirm **Custom Prompt: On**.
- [ ] Generate Smart/Unique/Flirty and Rewrite results; verify the instruction influences style without exposing hidden prompts or breaking language matching.
- [ ] Save an instruction longer than 1200 characters and confirm it is bounded.
- [ ] Clear the instruction and confirm AI panel shows **Custom Prompt: Off**.

## Draft and Send Boundary

- [ ] Type a manual draft before requesting AI.
- [ ] Confirm generated text never overwrites the draft automatically.
- [ ] Confirm Insert/Append/Replace requires an explicit user tap.
- [ ] Confirm the app never triggers Accessibility click, gesture, global action, or autonomous Send.

## Regression

- [ ] English typing works offline.
- [ ] Bangla Phonetic typing works offline.
- [ ] Bijoy-style layout still switches correctly.
- [ ] Emoji, clipboard, voice shortcut, and settings shortcut still open.
- [ ] Smart Reply auto-generation remains non-blocking while the user types.

## Stage 25.4 Context Accuracy / Thread Isolation

- [ ] In each supported app, open a conversation with at least three visible alternating messages and confirm AI context follows visual top-to-bottom chronology even when Accessibility traversal order differs.
- [ ] Confirm a clearly leading-side incoming bubble and trailing-side outgoing bubble are attributed correctly; centered date/system rows must not become SELF/RECIPIENT.
- [ ] Repeat sender checks on an RTL device/layout where the host app mirrors chat alignment.
- [ ] Switch rapidly from thread A to thread B while the keyboard remains open; before the new extraction finishes, the previous ContextSnapshot must be cleared and must not be used for an AI request.
- [ ] Use a supported chat surface where no stable toolbar/contact/group title is exposed. Current visible context may be used, but reopening/switching chats must not recover or merge disk-persisted history under the raw window id.
- [ ] Open a group chat with messages from multiple other participants. Generated output must not invent participant names or assume all `OTHER` rows came from the same person unless the visible text itself establishes that fact.
- [ ] Verify duplicate parent/child Accessibility text nodes representing the same rendered bubble do not duplicate that message in the AI context.
- [ ] With a long visible conversation, verify the bounded context retains the newest rows rather than spending the character budget on the oldest visible rows.
