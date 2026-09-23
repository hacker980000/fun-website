# Stage 25.4 — Conversation Context Accuracy & Thread Isolation

## Scope

This stage improves the reliability of visible conversation context used for explicit AI reply/comment requests. It builds on Stage 25.0 conversation-memory preservation, Stage 25.1 Accessibility package privacy boundaries, Stage 25.2 clipboard hardening, and Stage 25.3 sensitive-field detection.

The goals are:

1. use visual chronology rather than raw Accessibility tree traversal when reliable bounds are available;
2. classify SELF/RECIPIENT conservatively instead of guessing centered/system rows;
3. avoid stale context during chat/window transitions;
4. prevent raw window ids or transient event descriptions from becoming persistent conversation identities;
5. prevent group-chat participant labels / UNKNOWN rows from being misrepresented as a single recipient;
6. keep the newest visible rows when context size is bounded.

## Problems addressed

### 1. Raw Accessibility traversal was treated as chronological order

Accessibility tree traversal order does not guarantee visual top-to-bottom chat order. A newer row can be visited before an older row, which can make `latestRecipientMessage` and reply intent incorrect.

### 2. Sender inference used a fixed 42% / 58% center rule

The previous service classified left rows as RECIPIENT and right rows as SELF using only screen position. That could misclassify centered system notices, wide rows, RTL layouts, or platform nodes with explicit incoming/outgoing semantics.

### 3. Context character budget favored older rows

`GenericConversationAdapter` walked from the oldest candidate forward and stopped when its character budget was exhausted. On long visible conversations, the newest message could be dropped even though it is the most important input for reply generation.

### 4. Window identity could contaminate conversation memory

`ConversationKeyFactory` fell back to `windowSignature` when no conversation hint was available. Android chat apps can reuse the same Activity/window across different threads, so persisted memory under that fallback could mix separate conversations.

### 5. Event `contentDescription` was not a trustworthy thread identity

The Accessibility event that triggered extraction may describe a Back button, avatar, row, or other transient UI element. Stage 25.4 therefore accepts persistent identity only from explicit conversation/thread/header resource nodes. If such identity is unavailable, current visible context remains usable for the explicit request but is not persisted into conversation memory.

### 6. UNKNOWN could be promoted to OTHER in managed payloads

The managed payload previously mapped every non-SELF row to `OTHER`, which meant an unclassified row could become fabricated recipient attribution. Stage 25.4 excludes UNKNOWN rows from that SELF/OTHER payload conversion.

### 7. Group-chat ambiguity

A generic `username` resource marker could refer to an individual message participant rather than the thread itself. It has been removed from persistent thread-identity markers. Prompt instructions now explicitly state that consecutive OTHER rows in a group chat can represent different people and must not be merged into one invented identity.

## Implementation

### Geometry-aware visible row model

`VisibleTextNode` now carries optional screen bounds:

- `screenLeft`
- `screenTop`
- `screenRight`
- `screenBottom`
- `hasScreenBounds`

`SocialAiAccessibilityService` captures those bounds while traversing visible nodes.

### Conservative sender classifier

New `ConversationSenderClassifier` applies sender evidence in this order:

1. resource/class metadata indicating explicit incoming/outgoing roles;
2. explicit sender-role phrases in content descriptions;
3. conservative side-alignment geometry.

Centered rows and rows spanning at least 82% of screen width remain `UNKNOWN`. Geometry respects RTL layout direction. Arbitrary message text is never keyword-scanned to decide sender identity.

### Visual chronology and duplicate suppression

New `ConversationNodeOrdering` sorts eligible message rows by visual top/bottom position when all eligible rows have reliable screen bounds. If geometry is incomplete, it falls back to deterministic Accessibility traversal order instead of using a mixed non-transitive comparator.

Near-identical parent/child Accessibility rows with the same normalized text, sender class, and almost identical bounds are collapsed so a single rendered bubble is not duplicated in context.

### Recent-context budget

`GenericConversationAdapter` now walks ordered candidates from newest to oldest while applying `maxMessages` and `maxTotalChars`, then reverses the selected set back to chronological order. This guarantees the newest visible rows are retained before older rows when the budget is full.

### Stable identity only

`ConversationHintResolver` now prefers top/thread/header identity nodes and does not use:

- Accessibility event `contentDescription` by itself;
- raw `windowId:className`;
- generic per-message `username` markers.

`ConversationKeyFactory.hasStableIdentity()` exposes whether a snapshot has a persistence-safe identity.

When identity is missing, `ConversationRepository.mergeSnapshot()` returns transient history made only from the current snapshot and does **not** create/update Room conversation/message rows. The ephemeral key is capture-scoped and used only for request-level identity/fingerprinting.

### Stale snapshot boundary

`TYPE_WINDOW_STATE_CHANGED` immediately clears the previous `ContextSnapshot` before the debounced extraction of the new window. This prevents a short transition window where an explicit AI tap could use the previous chat's snapshot.

### Group/UNKNOWN prompt and payload behavior

Inbox prompt rules now say:

- `OTHER` means a non-self participant, not necessarily one fixed person;
- consecutive OTHER rows in group chats may come from different people;
- the model must not invent participant names or merge identities;
- `UNKNOWN` is background context and is not evidence that a recipient replied.

Managed payload conversion filters `SenderClass.UNKNOWN` rather than converting it to `OTHER`.

## Tests added/updated

### Android-independent Stage 25.4 self-test

`scripts/run_conversation_context_stage25_4_selftest.sh` compiles the context core with `kotlinc` and runs 18 assertions covering:

- semantic outgoing/incoming sender markers;
- LTR and RTL geometry;
- centered/full-width UNKNOWN behavior;
- geometry-based chronology;
- duplicate-node collapse;
- newest-message character-budget priority;
- toolbar identity priority;
- rejection of window/event-only identity;
- rejection of generic per-message username identity;
- stable vs capture-scoped conversation keys.

Result: **18/18 PASS**.

### Stage 25.4 source verifier

`scripts/verify_conversation_context_stage25_4.py` checks the production implementation, tests, privacy disclosure, and device-test coverage.

Result: **27/27 PASS**.

### Existing tests extended

- `ConversationSenderClassifierTest.kt` — semantic sender evidence, geometry, RTL and full-width rows.
- `GenericConversationAdapterTest.kt` — visual ordering, newest-first budget, duplicate suppression.
- `ConversationKeyFactoryTest.kt` — stable hints remain stable; missing hints are capture-scoped and non-persistent.
- `ConversationRepositoryTest.kt` — snapshots without stable identity return current context only and create no persisted message rows.
- `Phase2CContextMetadataTest.kt` — metadata identity wins; raw event/window fallback is rejected.

## Regression verification

Current-stage targeted checks after Stage 25.4:

- Stage 25.4 conversation-context verifier: **27/27 PASS**
- Stage 25.4 context self-test: **18/18 PASS**
- Stage 25.0 Conversation Memory: **13/13 PASS**
- Stage 25.1 Accessibility Privacy Boundary: **11/11 PASS**
- Stage 25.2 Clipboard Privacy & Security: **18/18 PASS**
- Stage 25.3 Sensitive Field Detection: **24/24 PASS**
- Release verifier: **PASS**
- Stage 24.3 Settings Navigation: **158/158 PASS**
- Stage 24.1 Bubble Flight contracts: **48/48 PASS**
- Stage 24.2 Optional Theme Bubble: **30/30 PASS**
- Stage 24.1 Bubble Flight policy self-test: **PASS**
- Stage 24.2 Theme Bubble policy self-test: **PASS**
- Stage 25.3 Sensitive Field self-test: **PASS**
- Phase 2C context metadata Android-independent compile/run: **PASS**
- Stage 25.4 context core `kotlinc` compile: **PASS**

### Full Python verifier comparison

The 47 verifier scripts shared with the Stage 25.3 baseline were executed for both trees:

- Stage 25.3 baseline: **41 PASS / 6 FAIL**
- Stage 25.4 shared set: **41 PASS / 6 FAIL**
- PASS -> FAIL regressions: **0**
- FAIL -> PASS changes: **0**

The six unchanged historical failures are:

- `verify_keyboard_bounded_update.py`
- `verify_numberpad_stage23_6.py`
- `verify_typing_stage23_3.py`
- `verify_typing_stage4.py`
- `verify_typing_stage7.py`
- `verify_typing_stage8.py`

Stage 25.4 adds one new verifier, so the current complete Python verifier set is **42 PASS / 6 unchanged historical FAIL**.

## Files added

- `app/src/main/java/com/socialaiassistant/keyboard/context/ConversationSenderClassifier.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/context/ConversationNodeOrdering.kt`
- `app/src/test/java/com/socialaiassistant/keyboard/context/ConversationSenderClassifierTest.kt`
- `scripts/conversation_context_stage25_4_selftest.kt`
- `scripts/run_conversation_context_stage25_4_selftest.sh`
- `scripts/verify_conversation_context_stage25_4.py`
- `docs/STAGE25_4_CONVERSATION_CONTEXT_ACCURACY.md`

## Files materially changed

- `ContextModels.kt`
- `ConversationHintResolver.kt`
- `ConversationKeyFactory.kt`
- `GenericConversationAdapter.kt`
- `SocialAiAccessibilityService.kt`
- `ConversationRepository.kt`
- `ManagedAiPayload.kt`
- `ExtensionPromptBuilder.kt`
- relevant context/memory unit tests
- privacy and real-device testing documentation.

## Build limitation in this environment

A complete Android Gradle `assembleDebug`, Android Lint, Robolectric suite, instrumentation suite, and physical-device IME run were not executed here because this source package intentionally does not include the verified Gradle wrapper JAR and the current runtime has no Android SDK/system Gradle installation.

The Android-independent context core and Stage 25.4 self-test were compiled and executed with `kotlinc`; current-stage source verifiers and shared regression verifiers were executed separately.

## Required physical-device checks before release

1. Messenger, WhatsApp and Telegram one-to-one chats: verify visual chronology and correct latest recipient message.
2. Centered date/system/status rows remain UNKNOWN.
3. RTL layout/app: verify mirrored sender alignment on apps that mirror chat bubbles.
4. Rapidly switch thread A -> thread B and immediately open the AI panel: old snapshot must not be reused.
5. A supported chat where no stable title resource is exposed must use only current visible context and must not recover disk history under the raw window id.
6. Group chat: multiple OTHER participants must not be merged into one invented identity.
7. Long visible history: newest messages remain inside the bounded AI context.
8. Parent/child duplicate Accessibility nodes must not duplicate the same rendered message.
9. All Stage 25.1 supported-package privacy boundaries and Stage 25.3 sensitive-field protections remain intact on device.
