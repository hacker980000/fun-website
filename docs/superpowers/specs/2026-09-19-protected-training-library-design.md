# Protected Training Library + Deduplicated Chat Engine Design

**Date:** 2026-09-19  
**Applies to:** Social AI Assistant Pro Worker/Admin + Social AI Keyboard v35.3.1  
**Status:** Design approved in chat; written-spec review pending  

## 1. Goal

Integrate Training Pack 0010 (98 items) and Training Pack 0011 (500 conversation-derived items) into the current shared AI engine used by Social AI Assistant Pro and Social AI Keyboard, while ensuring that:

- duplicate training logic/messages are not inserted or repeatedly selected;
- Bengali/Banglish behavior from Pack 0010 remains active;
- Pack 0011 extends rather than replaces Pack 0010;
- canonical training data survives future updates and cannot be accidentally hard-deleted or silently overwritten;
- managed AI in both products reads the same protected server-side library;
- Keyboard Personal OpenRouter mode receives only a compact distilled style policy, not the 500-conversation corpus;
- all migrations are additive, idempotent, and safe for existing production data.

## 2. Current-State Facts

The current production-source snapshot already contains migration `0010_product_entitlements.sql`; therefore the uploaded training migration named `0010_bengali_banglish_flirty_video_style.sql` cannot retain database migration number 0010.

The canonical training sources are:

| Source | Canonical items | Role |
| --- | ---: | --- |
| Existing default library | 420 | Base training already shipped in migration 0005 |
| Logical Training Pack 0010 | 98 | 6 rules, 50 Banglish FLIRT_MSG, 30 Bengali FLIRT_MSG, 12 FLIRT_CMT examples |
| Logical Training Pack 0011 | 500 | 500 full-conversation-derived FLIRT_MSG examples, including high-priority boundary examples |
| **Total canonical protected library** | **1018** | Shared managed-AI library |

Fresh normalized inspection found no exact duplicate canonical items inside Pack 0010, inside Pack 0011, or across the three sources. The existing 420-item base contains 5 repeated-response groups (9 extra repeated responses). Those base rows will not be deleted; runtime selection will suppress repeated responses.

## 3. Non-Goals

- Do not replace or downgrade the current Worker, Extension, Admin Portal, or Keyboard source with files from an older training patch.
- Do not embed all 500 conversations in the Android APK prompt.
- Do not introduce vector search, embeddings, or a paid semantic-deduplication service.
- Do not delete, rewrite, or renumber existing migrations 0001-0010.
- Do not auto-send AI output. Existing explicit-user-action behavior remains authoritative.

## 4. Migration Layout

The logical pack numbers remain 0010 and 0011 in metadata, while database migration numbers avoid collision with the existing product-entitlement migration:

1. `0011_training_pack_0010_flirty_video_style.sql`
   - inserts the 98 logical Pack 0010 items;
   - `INSERT OR IGNORE` only;
   - no DELETE, UPDATE, DROP, table rewrite, or replacement of existing rows.

2. `0012_training_pack_0011_500_conversations.sql`
   - inserts the 500 logical Pack 0011 items;
   - `INSERT OR IGNORE` only;
   - no destructive SQL.

3. `0013_training_library_integrity.sql`
   - creates the pack registry and item-integrity metadata tables;
   - registers the existing 420-item base as a protected canonical pack and registers logical Packs 0010 and 0011;
   - inserts SHA-256 content fingerprints for all 1018 canonical items;
   - installs hard-delete and protected-content-mutation guards;
   - remains idempotent.

Migration ordering is important: pack rows are inserted first, then migration 0013 registers/protects the complete canonical library.

## 5. Canonical Source Storage

The repository becomes the permanent reconstructable source of truth. The final Owner/Admin source must contain:

- `worker/data/default-training-items.js` — existing 420-item canonical base;
- `worker/data/flirty-video-style-pack-0010.js` — canonical 98-item logical Pack 0010;
- `worker/data/flirty-conversation-expansion-pack-0011.js` — canonical 500 training items;
- `worker/data/training-library-manifest.json` — pack IDs, logical versions, item counts, source filenames, and SHA-256 values;
- `worker/data/archive/0011/flirty_conversations_500.json` — original 500 full 7-turn conversations so derived examples can always be rebuilt;
- training handoff/summary/checksum documentation under `docs/training/`.

The final delivery ZIP also includes these canonical files. A future application update must treat them as additive project assets, not disposable generated output.

## 6. Integrity Data Model

Migration 0013 adds two companion tables without changing the existing `ai_training_items` schema contract.

### 6.1 `ai_training_packs`

Fields:

- `pack_id TEXT PRIMARY KEY`
- `logical_version TEXT NOT NULL`
- `title TEXT NOT NULL`
- `expected_item_count INTEGER NOT NULL`
- `manifest_sha256 TEXT NOT NULL`
- `data_sha256 TEXT NOT NULL`
- `is_protected INTEGER NOT NULL DEFAULT 1`
- `installed_at TEXT NOT NULL`
- `updated_at TEXT NOT NULL`

Canonical pack IDs:

- `base-default-0005`
- `flirty-video-style-0010`
- `flirty-conversation-expansion-0011`

### 6.2 `ai_training_item_integrity`

Fields:

- `item_id TEXT PRIMARY KEY` referencing `ai_training_items(id)`;
- `pack_id TEXT` referencing `ai_training_packs(pack_id)`;
- `content_fingerprint TEXT NOT NULL`;
- `response_fingerprint TEXT NOT NULL DEFAULT ''`;
- `is_protected INTEGER NOT NULL DEFAULT 0`;
- `created_at TEXT NOT NULL`.

A separate registry table, `ai_training_fingerprint_registry`, stores one preferred item per strict fingerprint:

- `content_fingerprint TEXT PRIMARY KEY`;
- `preferred_item_id TEXT NOT NULL`;
- `first_seen_at TEXT NOT NULL`.

This split is deliberate: canonical data must be unique, but a production database may already contain legacy Admin duplicates that must not be deleted. Every legacy row can still receive integrity metadata while the registry preserves one unique preferred fingerprint for future duplicate rejection.

`content_fingerprint` is SHA-256 over a strict canonical representation of:

`kind | channel | mode | language | trigger_text | response_text | instruction_text`

Strict normalization performs Unicode NFKC, line-ending normalization, trim, whitespace collapse, Unicode quote/dash normalization, and case normalization without dropping semantic characters.

`response_fingerprint` uses a looser response-only normalization and is used for runtime repeat suppression, not for destructive database uniqueness decisions.

Tags, priority, source labels, timestamps, and IDs are intentionally excluded from the content fingerprint so the same training logic cannot be re-added merely with different metadata.

## 7. Protected-Row Rules

All 1018 canonical rows are protected.

Database triggers enforce:

- `DELETE` of a protected canonical `ai_training_items` row -> abort with `PROTECTED_TRAINING_ITEM_DELETE`;
- updates to canonical content fields (`kind`, `channel`, `mode`, `language`, `trigger_text`, `response_text`, `instruction_text`, `tags`, `priority`, `source`) -> abort with `PROTECTED_TRAINING_ITEM_MUTATION`;
- `is_active` may still be changed, so Admin can disable/re-enable a problematic canonical item without losing it.

The current Admin API has no hard-delete route; the database trigger exists as defense-in-depth against future code or manual SQL.

Admin-created items remain editable and are not marked protected.

## 8. Duplicate Prevention

Deduplication is layered so a single failure does not create repetitive behavior.

### 8.1 Canonical build/import dedupe

Builders validate:

- unique item IDs;
- unique strict `content_fingerprint` values;
- expected pack counts (420, 98, 500, total 1018);
- no destructive SQL in generated pack migrations.

A duplicate causes the build/verification script to fail rather than silently choosing one row.

### 8.2 Database/admin dedupe

For Admin create/update operations:

- the Worker calculates the same strict content fingerprint;
- if `ai_training_fingerprint_registry` already owns that fingerprint, return HTTP 409 with error code `TRAINING_DUPLICATE` and the preferred existing item ID;
- protected canonical items cannot be edited through the generic update route; the response uses `TRAINING_ITEM_PROTECTED`;
- enable/disable remains allowed for protected items.

Existing production Admin rows that predate the fingerprint system are fingerprinted lazily during the first Admin Training Center write/maintenance pass; canonical rows are fully backfilled by migration 0013. Legacy duplicate Admin rows are preserved, mapped to the same fingerprint, and runtime-deduped; the maintenance pass never deletes or silently disables them.

### 8.3 Retrieval dedupe

`selectTrainingItems()` continues ID dedupe and adds:

- strict content-fingerprint dedupe;
- normalized response-fingerprint dedupe for examples;
- normalized instruction-fingerprint dedupe for rules/knowledge;
- skip an example whose response is already present verbatim/normalized in the supplied recent conversation;
- retain ranking/priority order, so the highest-ranked representative wins.

This suppresses the 5 response-repeat groups already present in the original 420 rows without deleting legacy data.

### 8.4 Generation anti-repeat

The authoritative prompt explicitly instructs the model to:

- treat examples as style demonstrations, never templates to copy verbatim;
- avoid repeating an opener, metaphor, emoji pattern, punchline, or recent response already used in the active conversation;
- prefer context-specific variation over memorized training wording.

## 9. Language and Flirty-Style Engine Update

Pack 0010 language/style behavior is merged into the current Worker rather than replacing `ai-prompts.js` with an older file.

For inbox generation:

- detected Bengali context -> natural Bengali script;
- detected Banglish context -> natural Bangladeshi Banglish in Latin script;
- detected English context -> existing current English behavior where the active product/context calls for English;
- explicit authoritative conversation-intent rules remain intact.

For comments:

- explicit English comment mode remains English;
- detected Banglish may produce natural Banglish where the active comment mode allows it;
- Bengali remains Bengali;
- public flirty comments remain lighter and less intimate than private inbox replies.

`FLIRT_MSG` follows:

`active context -> playful observation/twist -> soft flirt or wordplay -> natural continuation hook`

Boundary handling is higher priority than flirting. If the recipient rejects flirting, asks to stop, asks for normal/friend-only conversation, says they are uncomfortable, is busy, or closes the conversation, the output immediately de-escalates and does not continue romantic escalation.

## 10. Shared Managed-AI Architecture

Both products continue to use the same server-authoritative training library:

`Assistant Pro Managed AI -> Shared Worker -> D1 Training Center -> Protected 1018-item library`

`Keyboard Managed AI -> Shared Worker -> D1 Training Center -> Protected 1018-item library`

No customer Extension rebuild and no Keyboard APK rebuild is required solely for server-side training rows if the shared `/api/v1/ai/generate` contract remains unchanged. A new Keyboard source package is still produced because Personal OpenRouter prompt parity is part of this update.

## 11. Keyboard Personal OpenRouter Parity

The 500-conversation corpus is not embedded in the APK.

Instead, the current Keyboard `ExtensionPromptBuilder` receives a compact FLIRT_MSG policy derived from Packs 0010/0011:

- context first;
- Bengali stays Bengali;
- Banglish stays Banglish;
- use playful observation/wordplay before a soft flirt;
- use a continuation hook only when natural;
- do not copy memorized sample lines;
- avoid repeated openers/metaphors/punchlines in the same conversation;
- immediately respect disinterest, discomfort, stop requests, normal-chat requests, busy/reply-later signals, and friend-only boundaries.

Existing explicit AI-button generation, REPLY/CONTINUE/START intent, privacy, product-entitlement, and manual-send rules remain unchanged.

## 12. Verification and Repair Tooling

The source includes deterministic scripts/tests that can run without production mutation:

### `verify-training-library.mjs`

Checks:

- 420 + 98 + 500 = 1018 canonical items;
- expected per-pack subtype/language counts;
- no duplicate IDs;
- no duplicate strict content fingerprints;
- manifest/data SHA-256 matches;
- generated migrations contain no destructive SQL;
- migration filenames do not collide with existing migrations;
- all canonical IDs have integrity metadata.

### `build-training-repair-sql.mjs`

Produces an additive repair SQL file containing only:

- `INSERT OR IGNORE` canonical training rows;
- `INSERT OR IGNORE` pack registry rows;
- `INSERT OR IGNORE` integrity rows.

It never emits DELETE, DROP, destructive UPDATE, or table replacement. If a protected row is missing due to an external database restore/manual intervention, the generated repair SQL can reconstruct it from repository canonical data.

### Database verification queries

Deployment documentation includes commands to verify:

- expected pack row counts;
- protected metadata count = 1018;
- canonical duplicate fingerprint count = 0 and fingerprint-registry collisions are reported;
- logical Pack 0010 row count = 98;
- logical Pack 0011 row count = 500;
- re-applying migrations/repair remains idempotent.

## 13. Test Requirements

### Worker tests

Must cover:

- Pack 0010 item counts and language/style rules;
- Pack 0011 exact 500 count and 250 Banglish / 250 Bengali distribution;
- boundary examples remain priority 100;
- migration numbering follows existing `0010_product_entitlements.sql`;
- content fingerprint normalization is deterministic;
- duplicate Admin create/update returns 409;
- protected content mutation is rejected;
- protected enable/disable remains allowed;
- runtime examples do not include duplicate response fingerprints;
- recent-conversation response reuse is suppressed;
- Banglish input selects Banglish training and produces a Banglish language instruction;
- Bengali input selects Bengali training and Bengali instruction;
- current product-entitlement and conversation-intent tests stay green.

### Keyboard tests

Must cover Personal OpenRouter prompt text for:

- Banglish FLIRT_MSG style preservation;
- Bengali FLIRT_MSG style preservation;
- anti-copy/anti-repeat rule;
- boundary/de-escalation rule;
- REPLY/CONTINUE/START rules remain present;
- no 500-conversation corpus is bundled into production prompt/resources.

### Migration tests

A clean SQLite/D1-compatible database applies migrations 0001 through 0013 in order, then re-applies the three new migrations/repair logic without increasing canonical row counts or losing any legacy row.

## 14. Deployment Safety

Deployment order:

1. Back up/export the current D1 database before migration.
2. Build/verify canonical packs and checksums locally.
3. Run full Worker and Extension test suites.
4. Run Keyboard tests/static verifier; Android Gradle build runs where network/SDK access is available.
5. Apply D1 migrations 0011-0013 remotely.
6. Run post-migration count/fingerprint verification.
7. Deploy the **current** Worker source.
8. Smoke-test Assistant Pro Managed AI and Keyboard Managed AI with Bengali, Banglish, boundary, and repeated-context cases.
9. Keep canonical source files and repair tooling in the final delivery ZIP.

No production migration or deployment is performed automatically as part of source packaging unless the user explicitly requests it.

## 15. Final Deliverables

After implementation and verification, provide:

1. Updated Social AI Keyboard source ZIP.
2. Updated Social AI Assistant Pro Owner/Admin source ZIP.
3. Updated Social AI Assistant Pro Customer package ZIP if current release packaging changes or is regenerated for parity; otherwise include the verified unchanged customer package with clear status.
4. A combined Training Library Backup/Handoff ZIP containing canonical Packs 0010/0011, full 500-conversation JSON, manifest/checksums, migrations, integrity verifier, and repair builder.
5. Verification report with exact test counts, migration counts, checksums, and any environment-limited checks explicitly identified.

## 16. Acceptance Criteria

The update is accepted only when all of the following are true:

- the canonical protected library contains exactly 1018 rows after clean migration;
- logical Pack 0010 contributes exactly 98 rows;
- logical Pack 0011 contributes exactly 500 rows;
- no canonical strict content fingerprint is duplicated;
- protected canonical rows cannot be hard-deleted or content-mutated through normal DB operations;
- Admin-created duplicate training logic is rejected;
- retrieval does not present duplicate normalized responses to the model;
- Bengali/Banglish language behavior follows active conversation style;
- boundary requests immediately de-escalate flirt behavior;
- both Managed AI products consume the same protected D1 library;
- Keyboard Personal OpenRouter receives compact style parity without embedding the full corpus;
- repair tooling can reconstruct missing canonical rows additively;
- existing user, subscription, entitlement, quota, device-proof, privacy, and explicit-AI-trigger behavior remains intact.
