# Explicit AI Trigger, Premium Settings, and Product Entitlements Design

**Date:** 2026-09-19  
**Status:** Approved architecture, written-spec review pending  
**Applies to:** Social AI Keyboard v35.3.1 Premium Neon baseline + Social AI Assistant Pro v35.3.1 Worker/Admin Portal

## 1. Purpose

This update fixes four user-visible problems while preserving all existing keyboard, account, subscription, device-security, privacy, theme, and AI capabilities:

1. AI generation must happen only after an explicit user action. Context/accessibility updates must never trigger generation on their own.
2. AI output must never expose raw JSON or fenced model payloads to the keyboard UI.
3. Keyboard Settings must become a simple premium Neon/Glass in-app settings hub, including a compact consent CTA instead of a large plain notice.
4. The Admin Panel must make one base login account easy to understand by separating product access for **Social AI Keyboard** and **Social AI Assistant Pro**, including separate access status and expiry per product.

## 2. Locked Behavioral Contract

### 2.1 AI is explicit-action only

The keyboard may continue to capture and cache the latest permitted conversation context after the user has accepted the existing privacy/accessibility disclosures. Context capture is passive only.

No context event may call an AI gateway. No incoming message, outgoing message, composer mutation, focus change, accessibility event, screen change, or snapshot fingerprint change may automatically generate a reply.

An AI request is allowed only from one of these explicit actions:

- tapping the toolbar **AI** button;
- tapping **Smart Reply / Smart Comment**;
- tapping **Unique Reply / Unique Comment**;
- tapping **Flirty Reply / Flirty Comment**;
- tapping **Funny Comment** where applicable;
- tapping **Rewrite**, **Translate**, or **Grammar**;
- tapping **Regenerate** after a prior explicit request;
- explicitly requesting caption/photo-caption generation.

**One explicit tap = at most one generation request.** A new context snapshot after a result is returned does not start another request. The next generation requires another user tap.

No AI result is automatically sent or posted. Insert/append/replace remains explicit and final Send/Post remains manual.

### 2.2 Toolbar AI button intent resolution

A toolbar AI tap performs an **AUTO** conversation-generation request from the latest stable allowed context and opens the AI result surface in a loading state.

The request intent is resolved at tap time:

- **REPLY**: the latest meaningful conversation message is from `RECIPIENT`; generate a reply to that message using relevant recent history.
- **CONTINUE**: the latest meaningful message is from `SENDER` and there is no newer recipient message; generate a natural next/continuation message without pretending the other person has replied.
- **START**: there is no meaningful message history; generate a natural first message using available conversation/person/surface hints. If there is not enough context to do this safely, show a friendly `NeedsContext` state instead of inventing facts.

The resolver must never infer a recipient reply that is not present in captured context.

### 2.3 Manual AI actions remain explicit

Smart/Unique/Flirty/Funny actions use the same tap-time context snapshot but retain their own style semantics. Rewrite/Translate/Grammar operate on the explicit current/selected draft and never trigger because the draft changes.

Regenerate is permitted only after a prior explicit request and reuses the previous explicit action intent with the latest safe context/draft as defined by that action.

## 3. Root-Cause Changes

### 3.1 ReplyOrchestrator

Current behavior observes context snapshots and generates when the latest-recipient fingerprint changes. That behavior is removed.

`ReplyOrchestrator` becomes request-driven:

- it may observe/cache the latest snapshot/session for availability and safety;
- it exposes an explicit request function/event;
- only the request function starts debounce/cancellation/gateway work;
- new snapshots may update cached context but do not generate;
- stale in-flight work is still cancelled/ignored if the explicit request is superseded by a newer explicit request;
- blocked/sensitive fields immediately cancel and hide results.

Tests that currently expect `changed_latest_recipient_message_regenerates` are replaced with tests proving zero gateway calls until an explicit request and no re-generation after context changes.

### 3.2 AI invocation intent

Add a small pure `ConversationAiIntentResolver` (or equivalent focused unit) that consumes the latest `ContextSnapshot`/history and returns `REPLY`, `CONTINUE`, `START`, or `NEEDS_CONTEXT`.

This unit contains no Android UI code and is unit-tested independently.

## 4. Model Output Safety and JSON Parsing

`ModelResultParser` is the single normalization boundary for conversational model responses.

It must support:

- plain JSON objects;
- fenced JSON with `json`, `JSON`, or no fence language;
- an extra standalone `JSON`/`json` label before a fenced block;
- leading/trailing whitespace and prose-free newline variation;
- the known envelope fields `reply`, optional `category`, optional `confidence`.

Only the normalized `reply` string is renderable as conversational output. Metadata fields are never shown as reply text.

If a payload still looks like a JSON/object/code envelope after normalization but cannot be parsed safely, the parser returns a structured parse failure. The UI shows a friendly retry/error state. **Raw JSON, braces, code fences, category/confidence metadata, or backend envelopes must never be rendered as the reply fallback.**

Plain natural-language responses remain supported, including the existing `Reply: ...` cleanup path.

## 5. Consent Banner UX

The large plain Bengali consent notice in the AI panel is replaced by a compact premium Neon/Glass banner shown only when AI privacy/data consent is required.

Banner content:

- title: `AI Features Locked` (localized Bengali/English copy as appropriate);
- one short explanation;
- primary action: `Enable AI` / `Enable Now`;
- action opens the in-app Settings Hub directly to **AI & Privacy**;
- once consent is accepted, the banner disappears immediately without reopening the keyboard.

The banner does not bypass or weaken the existing disclosure/consent requirement.

## 6. Premium In-App Settings Hub

The settings experience is redesigned around the already locked Premium Neon visual system. It must be simple to use on a phone and must not resemble a raw developer/settings screen.

### 6.1 Main sections

1. **AI & Privacy**
   - Privacy/Data Consent
   - Accessibility disclosure/status
   - Preserve Draft
   - Custom Prompt
   - Tone preset
   - Sensitive-field protection summary

2. **Theme & Appearance**
   - current theme preview
   - existing eight premium presets
   - Custom Theme
   - manual background photo
   - Full Keyboard / Keys Only / AI Panel Only
   - blur, opacity, dim/overlay, fit mode

3. **Keyboard Preferences**
   - key/font sizing where already supported or added by this update
   - vibration/sound toggles only if implemented without new risky permissions
   - shortcut visibility/preferences

4. **Language & Input**
   - English / বাংলা
   - Phonetic / Bijoy
   - default language and quick switch behavior

5. **Account & Subscription**
   - signed-in email/mobile
   - product access badges/status
   - Keyboard expiry
   - Assistant Pro expiry
   - User Portal
   - Logout

6. **Help & Support**
   - WhatsApp Support
   - privacy/help links
   - app/version information

### 6.2 UX rules

- card-based Neon/Glass layout matching the locked keyboard design;
- large touch targets and short labels;
- destructive/account actions clearly separated;
- no hidden technical configuration required for ordinary users;
- theme changes continue to preview/apply without breaking the existing Theme Engine;
- no existing user setting is silently reset during migration.

## 7. Admin Product Separation

### 7.1 Identity model

A person has one base identity in `users`: email, mobile, password, account status, device/security identity.

Product access is separate from identity. Supported product codes:

- `KEYBOARD`
- `ASSISTANT_PRO`

An account may have one or both products active.

### 7.2 Data model

Add a new migration `0010_product_entitlements.sql` with a dedicated entitlement table rather than altering the legacy `subscriptions` unique-user schema in place.

Proposed logical shape:

```sql
product_entitlements(
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  product_code TEXT NOT NULL CHECK(product_code IN ('KEYBOARD','ASSISTANT_PRO')),
  status TEXT NOT NULL CHECK(status IN ('INACTIVE','ACTIVE','EXPIRED','REVOKED')),
  cycle_started_at TEXT,
  cycle_expires_at TEXT,
  source_payment_id TEXT,
  updated_at TEXT NOT NULL,
  UNIQUE(user_id, product_code),
  FOREIGN KEY(user_id) REFERENCES users(id)
)
```

The existing `subscriptions` table is preserved for backward compatibility during this release. Existing subscription rows are not deleted or destructively rewritten.

### 7.3 Existing-user migration rule

To avoid accidentally locking out current customers, every existing user with an active/valid legacy subscription is migrated to **both** product entitlements with the same effective expiry. Existing inactive/expired/revoked state is copied consistently.

This migration is idempotent (`INSERT OR IGNORE`/equivalent guarded behavior) and safe to rerun.

### 7.4 Product enforcement

Clients identify the product on authenticated product-sensitive requests:

- Android Keyboard sends `KEYBOARD`.
- Browser Extension sends `ASSISTANT_PRO`.
- Legacy browser clients that do not yet send a product identifier are treated as `ASSISTANT_PRO` for backward compatibility during this release.

Before AI generation and other paid product-sensitive operations, the Worker verifies that the requested product entitlement is active and not expired.

Base login may authenticate identity, but a client without entitlement for its product must receive a clear `PRODUCT_ACCESS_REQUIRED`/equivalent response and UI status rather than silently using the other product's entitlement.

The existing one-account/one-active-device security policy remains account-wide unless explicitly changed in a future spec.

### 7.5 Subscription and quota semantics

- Keyboard and Assistant Pro have separate entitlement status and expiry.
- Existing account-level AI request quota remains shared in this update; product-specific quotas are out of scope.
- Existing payment flow remains backward-compatible and maps legacy/self-service subscription operations to `ASSISTANT_PRO` unless a product is explicitly supplied by an authorized admin operation.
- Admin manual controls may grant/renew/revoke either product independently.

## 8. Admin Panel UX

### 8.1 Add User

The Add Customer dialog gains **Product Access** selection:

- Social AI Keyboard
- AI Assistant Pro
- Both

The admin can optionally grant an initial duration per selected product. If both are selected, each product gets its own duration field/preset, defaulting to the same value for convenience but saved independently.

### 8.2 User list

Each row displays a clear product badge:

- `KEYBOARD`
- `ASSISTANT PRO`
- `BOTH`
- `NO PRODUCT ACCESS` where appropriate

Filters/tabs:

- All Users
- Keyboard Users
- Assistant Pro Users
- Both

### 8.3 User/product controls

User details/subscription actions show separate cards/controls:

- Keyboard: status, expiry, Grant/Renew/Revoke
- Assistant Pro: status, expiry, Grant/Renew/Revoke

Blocking/archiving the base user still blocks both products and preserves existing audit/security behavior.

## 9. API Compatibility

New/extended admin APIs must accept/return product code explicitly for entitlement operations. Existing route shapes remain supported where necessary for v35.3.1 compatibility.

User/account responses consumed by the Keyboard should expose normalized product entitlement information sufficient to render:

- product code;
- status;
- effective expiry.

No secret, password hash, admin credential, or unnecessary cross-product data is returned to clients.

## 10. Error Handling

Required user-facing states include:

- consent required;
- no safe conversation context;
- product access required/expired;
- offline/backend unavailable;
- model output invalid (without exposing raw payload);
- request already in flight for the same explicit action (button disabled/loading rather than duplicate request).

All errors remain non-destructive and must not auto-send text.

## 11. Testing Requirements

### Keyboard unit/integration tests

- context changes produce **0** AI gateway calls before explicit request;
- one toolbar AI tap produces exactly **1** request;
- new incoming/outgoing snapshots after a result produce **0** additional requests;
- second explicit tap produces the next request;
- intent resolver: recipient-last => REPLY; sender-last => CONTINUE; empty => START/NEEDS_CONTEXT;
- sensitive field cancels/hides explicit generation;
- raw/fenced/uppercase/prefixed JSON returns only `reply`;
- malformed JSON-looking payload never renders raw JSON;
- consent banner visibility/action behavior;
- Settings values persist and Theme Engine remains intact.

### Worker/Admin tests

- migration 0010 is idempotent and preserves legacy rows;
- existing valid legacy users receive both entitlements with preserved expiry;
- KEYBOARD request denied when only ASSISTANT_PRO active and vice versa;
- legacy extension request without product remains compatible as ASSISTANT_PRO;
- admin create user supports Keyboard / Assistant Pro / Both;
- grant/renew/revoke works independently per product;
- filters/badges/rendering are covered;
- account block/archive revokes effective use of both products;
- no regression in device proof, device transfer, auth, training center, quota, or security-sensitive routes.

### Release verification

Keyboard: `testDebugUnitTest`, `lintDebug`, `assembleDebug` and existing static/release checks.  
Worker/Admin: existing Node test suite plus schema/admin portal/release audit tests.

## 12. Migration and Rollback Safety

- no existing user row is deleted;
- no existing subscription row is deleted;
- no password/device binding/session history is reset by migration;
- migration is additive and idempotent;
- old Extension clients continue to function during rollout;
- Keyboard client is updated to send explicit product identity before production rollout;
- rollback can stop using `product_entitlements` while legacy `subscriptions` remains intact.

## 13. Non-Goals

This release does not:

- add autonomous sending/posting;
- add background AI generation;
- split one-device policy by product;
- create separate passwords/accounts per product;
- create product-specific AI quotas;
- redesign the Extension UI beyond changes required to identify/enforce Assistant Pro entitlement;
- delete or rewrite existing customer data.

## 14. Success Criteria

The update is successful when:

1. Leaving a chat open, receiving messages, or sending messages never causes a new AI request by itself.
2. The next AI generation starts only after the user taps AI or another explicit AI action.
3. The toolbar AI action correctly chooses reply/continue/start behavior from the latest captured sender state.
4. Users never see raw JSON/code-fence/model metadata as a reply.
5. Settings and consent flows are simple, premium, and consistent with the locked Neon design.
6. Admin can immediately distinguish Keyboard, Assistant Pro, and Both users and manage each product expiry independently.
7. Existing users remain usable after migration without data loss or accidental lockout.
