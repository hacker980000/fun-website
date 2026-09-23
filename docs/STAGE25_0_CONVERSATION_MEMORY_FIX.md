# Stage 25.0 - Conversation Memory Persistence Fix

## Problem fixed

`ConversationDao.upsertConversation()` previously used `@Insert(onConflict = OnConflictStrategy.REPLACE)` while `MessageEntity` referenced `ConversationEntity` with `ON DELETE CASCADE`.

SQLite `REPLACE` semantics delete the conflicting parent row before inserting its replacement. That deletion can cascade into `messages`, wiping previously persisted conversation history every time the same conversation metadata is refreshed.

## Changes

1. `ConversationDao.upsertConversation()` now uses Room `@Upsert` so parent metadata is updated without destructive REPLACE semantics.
2. `ConversationRepository.mergeSnapshot()` now compares the stored history tail with the incoming accessibility snapshot prefix and appends only the new tail.
3. The old five-minute coarse-time dedupe bucket was removed. New-row dedupe identity now includes snapshot capture time and message position, allowing repeated identical text messages inside the same snapshot to remain distinct.
4. Regression coverage was added for:
   - same snapshot merged twice;
   - same visible snapshot after more than five minutes;
   - rolling snapshots preserving old history while appending only new messages;
   - legitimate identical repeated messages;
   - clear-all behavior.
5. Added Android-free verifier: `scripts/verify_conversation_memory_stage1.py`.

## Verification performed in this environment

- `python3 scripts/verify_conversation_memory_stage1.py` -> 13/13 PASS
- `python3 scripts/verify_release_ready.py` -> PASS
- `python3 scripts/verify_settings_navigation_stage24_3.py` -> 158/158 PASS
- Stage 24.3 settings navigation self-test -> PASS
- Stage 24.1 Bubble Flight self-tests -> PASS
- Stage 24.2 Theme Bubble self-test -> PASS

## Build limitation

A full Android Gradle/Robolectric build was not claimed in this environment because the source intentionally does not vendor the verified `gradle-wrapper.jar` and no Android SDK/system Gradle is installed here. The existing verified wrapper bootstrap policy remains unchanged.

## Remaining edge case

When an accessibility snapshot contains only one message with no timestamp hint, a newly received message that is textually identical to the stored tail is inherently ambiguous. The overlap rule favors avoiding duplicate history. A later context-capture stage can improve this further by supplying stronger source message identity/timestamp metadata.
