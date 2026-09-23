# Google Play AccessibilityService Declaration Draft

## App classification

Social AI Keyboard is **not an accessibility tool**. Its primary purpose is an Android keyboard with optional AI-assisted writing and reply suggestions.

The manifest metadata therefore keeps:

```text
android:isAccessibilityTool="false"
android:canPerformGestures="false"
```

## Why the app uses AccessibilityService

The optional **AI Context Access** feature needs read-only access to visible conversation text in supported apps so the keyboard can understand the current conversation and prepare a relevant reply suggestion.

For the declaration category, the closest data type is:

- Messages → **Other in-app messages**

The service also observes enough window/app context locally to distinguish supported conversational surfaces from unrelated fields. Conversation-text extraction is fail-closed to the built-in Facebook, Messenger, WhatsApp, Instagram and Telegram package allowlist; unsupported packages do not receive a generic conversation-text fallback. It does not use Accessibility to perform taps, clicks, gestures, global actions, scrolling, autonomous navigation, or autonomous Send.

## User control and prominent disclosure

Before Android Accessibility settings are opened, the app shows a separate prominent disclosure in normal app usage. The disclosure explains that:

- visible conversation text may be read after consent;
- local conversation history is encrypted and stays on the device;
- relevant text may be sent to the selected AI provider when the user requests AI assistance;
- sensitive fields are blocked;
- the Accessibility service does not perform gestures and **never presses Send**;
- context access can be disabled again from the app.

The user must affirmatively check the consent box before the app opens Android Accessibility settings. Declining consent leaves the normal keyboard usable without context reading.

## Core feature example

1. User opens Messenger or WhatsApp and focuses a reply field.
2. Social AI Keyboard is already selected as the current keyboard.
3. Because the user previously granted AI Context Access, the read-only Accessibility service extracts visible conversation text.
4. The AI feature prepares a suggested reply.
5. The user taps the suggestion to insert it.
6. The user reviews the text and manually presses Send in the host app.

## Review video sequence

Record one unedited demonstration that shows:

1. Opening Social AI Keyboard from the launcher.
2. Scrolling through the complete prominent disclosure so all text is visible.
3. Showing the consent checkbox unchecked.
4. Tapping **Enable AI Context Access** without consent and showing that the app refuses to proceed.
5. Checking the consent box and tapping **Enable AI Context Access**.
6. Enabling the Social AI Keyboard Accessibility service in Android settings.
7. Returning to the app, then opening a supported chat.
8. Demonstrating a visible conversation being used to create a suggestion.
9. Tapping Insert/Append/Replace to place generated text into the field.
10. Manually pressing Send in the host app.
11. Returning to Social AI Keyboard and disabling AI Context Access.
12. Reopening the disclosure flow to show how a user can consent again later.

The video should make it obvious that the Accessibility service is read-only and that user action controls insertion and sending.

## Bubble Key visual positioning

When **Bubble Key** is enabled and the Accessibility service is available, Social AI Keyboard may use only the active editable field bounds to position the visual key bubble near the destination text. This geometry is transient: Bubble Flight does not persist editable field text or coordinates. Exact caret positioning comes from the keyboard's IME cursor-anchor channel; Accessibility supplies only a bounds-based fallback. Conversation text access remains controlled by the separate AI Context Access consent described above. Bubble Flight uses an accessibility overlay and does **not** add a draw-over-other-apps permission.
