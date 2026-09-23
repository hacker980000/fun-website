# Product Entitlements + Admin Separation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep one base user identity while separating Social AI Keyboard and AI Assistant Pro access, expiry, enforcement, admin creation/management, and UI badges without losing legacy subscription data or breaking v35.3.1 clients.

**Architecture:** Add an additive `product_entitlements` table and entitlement service beside the legacy `subscriptions` table. Migrate valid legacy customers to both products, enforce `X-SocialAI-Product` on product-sensitive Worker routes with a legacy default of `ASSISTANT_PRO`, extend admin APIs to manage entitlements independently, and update Admin Portal filters/forms/cards. Preserve account-wide auth/device policy and shared AI quota.

**Tech Stack:** Cloudflare Workers, D1/SQLite migrations, JavaScript ES modules, Node test runner, static Admin Portal HTML/CSS/JS, existing auth/device-proof/quota/payment modules.

**Spec:** `docs/superpowers/specs/2026-09-19-explicit-ai-settings-product-entitlements-design.md`

## Global Constraints

- One base `users` row per person; do not create separate passwords/accounts per product.
- Product codes are exactly `KEYBOARD` and `ASSISTANT_PRO`.
- Existing `subscriptions` rows are preserved and not destructively rewritten or deleted.
- Migration 0010 is additive and idempotent.
- Every existing user with active/valid legacy subscription receives both product entitlements with the same effective expiry.
- Legacy browser clients without product identity are treated as `ASSISTANT_PRO` during this release.
- Keyboard requests identify `KEYBOARD`; updated extension requests identify `ASSISTANT_PRO`.
- Quota remains account-level/shared in this release.
- One-account/one-active-device policy remains account-wide.
- Blocking/archiving a base user blocks effective use of both products.

## Review Focus

1. Re-running migration 0010 must not duplicate entitlements or change an already-migrated entitlement expiry.
2. An active account with only one product active must be denied for the other product with `PRODUCT_ACCESS_REQUIRED`, not a generic auth or subscription error.
3. Legacy extension requests with no product header must still work as `ASSISTANT_PRO` during rollout.
4. Payment approval must continue its legacy subscription behavior and map to Assistant Pro entitlement without accidentally granting Keyboard unless explicitly designed by admin migration/backfill rules.
5. Archived/blocked users with active entitlements must still be denied even though entitlement rows remain preserved for audit/history.

---

### Task 1: Add migration 0010 and entitlement service

**Files:**
- Create: `worker/migrations/0010_product_entitlements.sql`
- Create: `worker/src/product-entitlements.js`
- Create: `worker/test/product-entitlements.test.mjs`
- Modify: `worker/test/schema.test.mjs`

**Interfaces:**
- Produces constants `PRODUCT.KEYBOARD`, `PRODUCT.ASSISTANT_PRO`.
- Produces `normalizeProductCode(value, { legacyDefault })`, `getProductEntitlement(env,userId,productCode,now)`, `listProductEntitlements(env,userId,now)`, `grantProductEntitlement`, `renewProductEntitlement`, `revokeProductEntitlement`, `effectiveProductStatus`.

- [ ] **Step 1: Write failing schema/idempotency tests**

Test that after applying all migrations:

```js
const columns = await tableColumns(db, 'product_entitlements');
assert.deepEqual(columns.map(c => c.name), [
  'id','user_id','product_code','status','cycle_started_at','cycle_expires_at','source_payment_id','updated_at'
]);
```

Seed one ACTIVE legacy subscription and verify migration creates exactly two rows, one `KEYBOARD` and one `ASSISTANT_PRO`, both with the legacy expiry. Apply 0010 again and assert row count remains two.

- [ ] **Step 2: Run focused worker tests and confirm RED**

```bash
cd worker
npm test -- --test-name-pattern='product entitlement|schema'
```
Expected: missing table/module failures.

- [ ] **Step 3: Add migration 0010**

Use exactly the table shape from the spec with `UNIQUE(user_id, product_code)` and an index on `(product_code,status,cycle_expires_at)`. Backfill from `subscriptions` with guarded `INSERT OR IGNORE`, deriving expired state from `cycle_expires_at <= CURRENT_TIMESTAMP` when legacy status is ACTIVE.

- [ ] **Step 4: Implement entitlement service**

Use the same duration validation/window semantics as `subscriptions.js`, but keyed by `(user_id, product_code)`. `renewProductEntitlement` extends from future active expiry; otherwise starts now.

- [ ] **Step 5: Run focused tests and confirm GREEN**

```bash
cd worker
npm test -- --test-name-pattern='product entitlement|schema'
```
Expected: PASS.

- [ ] **Step 7: Commit the task**

```bash
git add worker/migrations/0010_product_entitlements.sql worker/src/product-entitlements.js worker/test/product-entitlements.test.mjs worker/test/schema.test.mjs
git commit -m "feat: add product entitlement model"
```

### Task 2: Enforce product access on AI and expose entitlements to clients

**Files:**
- Modify: `worker/src/routes-ai.js`
- Modify: `worker/src/ai-prompts.js`
- Modify: `worker/src/routes-auth.js`
- Modify: `worker/src/routes-user.js`
- Modify: `worker/src/constants.js`
- Add/modify tests: `worker/test/ai-gateway.test.mjs`, `worker/test/auth.test.mjs`, `worker/test/protected-parity.test.mjs`

**Interfaces:**
- Product header: `X-SocialAI-Product`.
- Missing header on legacy extension paths resolves to `ASSISTANT_PRO`.
- `GET /api/v1/auth/me` returns `entitlements: [{ productCode, status, cycle_expires_at, effectiveStatus }]`.
- Product denial code: `PRODUCT_ACCESS_REQUIRED`; expired product may use `PRODUCT_ACCESS_EXPIRED` if kept distinct.
- Managed inbox payload accepts optional `conversationIntent` values `REPLY`, `CONTINUE`, `START`; absent keeps legacy behavior.

- [ ] **Step 1: Add failing authorization tests**

Cover:
- active `ASSISTANT_PRO`, inactive `KEYBOARD`, header `KEYBOARD` => 402/403 `PRODUCT_ACCESS_REQUIRED`;
- active `KEYBOARD`, inactive `ASSISTANT_PRO`, header `ASSISTANT_PRO` => denied;
- matching active product => authorized;
- missing product header => defaults to `ASSISTANT_PRO`;
- blocked base user remains denied regardless of entitlement;
- `conversationIntent=CONTINUE` with latest SELF generates continuation rules instead of pretending OTHER replied;
- `conversationIntent=START` adds opener rules without inventing prior familiarity;
- absent `conversationIntent` keeps legacy prompt behavior for older extension clients.

- [ ] **Step 2: Run focused tests and confirm RED**

```bash
cd worker
npm test -- --test-name-pattern='product|entitlement|AI gateway|auth me'
```

- [ ] **Step 3: Replace AI subscription authorization with product entitlement authorization**

In `authorizeAiRequest`, resolve product from `X-SocialAI-Product`; use the entitlement service. Keep the legacy subscription object only where quota-cycle compatibility still needs `cycle_started_at`, falling back to the active entitlement cycle when appropriate. Do not alter device-proof checks or rate limiting.

- [ ] **Step 4: Make managed authoritative prompts honor explicit conversation intent**

In `ai-prompts.js`, normalize the optional field:

```js
export function normalizeConversationIntent(value) {
  const intent = String(value || '').toUpperCase();
  return ['REPLY', 'CONTINUE', 'START'].includes(intent) ? intent : '';
}
```

For INBOX prompts add intent-specific hard rules. `CONTINUE` must explicitly say the latest meaningful turn is SELF and the model must continue naturally without fabricating an OTHER reply. `START` must generate a first message without false familiarity. `REPLY` targets the latest OTHER turn. Include the normalized intent in `buildUserText()` so the provider receives the same state. In `shouldWaitForRecipient`, retain legacy wait behavior only when `conversationIntent` is absent; explicit `CONTINUE` from Android must not be rejected as `WAIT_FOR_RECIPIENT`.

- [ ] **Step 5: Extend auth/me and subscription/status responses**

Return both normalized entitlements without hashes/secrets. Keep the legacy `subscription` field for v35.3.1 compatibility.

- [ ] **Step 6: Run focused and security tests**

```bash
cd worker
npm test -- --test-name-pattern='product|entitlement|AI gateway|auth|security|device proof'
```
Expected: PASS.

- [ ] **Step 6: Commit the task**

```bash
git add worker/src/routes-ai.js worker/src/routes-auth.js worker/src/routes-user.js worker/src/constants.js worker/test
git commit -m "feat: enforce product entitlements on worker routes"
```

### Task 3: Keep payments backward-compatible while mapping legacy paid access to Assistant Pro

**Files:**
- Modify: `worker/src/subscriptions.js`
- Modify: `worker/src/payments.js`
- Modify: `worker/src/product-entitlements.js`
- Modify tests: `worker/test/billing-device.test.mjs`, `worker/test/product-entitlements.test.mjs`

**Interfaces:**
- Existing payment approval still writes/updates legacy `subscriptions`.
- After successful approval, mirror the same window to `ASSISTANT_PRO` entitlement only for new payment actions.
- Rollback/revoke mirrors `ASSISTANT_PRO` state for the payment that owns the active cycle.
- Migration backfill is the only operation that grants both products from old legacy rows.

- [ ] **Step 1: Add failing payment compatibility tests**

Prove new payment approval yields active legacy subscription + active `ASSISTANT_PRO` entitlement + no new `KEYBOARD` grant. Prove rollback revokes the matching Assistant Pro entitlement and leaves unrelated Keyboard entitlement unchanged.

- [ ] **Step 2: Run focused tests and confirm RED**

```bash
cd worker
npm test -- --test-name-pattern='billing|payment|entitlement'
```

- [ ] **Step 3: Implement mirroring after successful payment state transitions**

Call entitlement helpers from `approvePayment` / rollback path only after legacy subscription operation succeeds. Preserve transaction/audit behavior.

- [ ] **Step 4: Run focused tests and confirm GREEN**

```bash
cd worker
npm test -- --test-name-pattern='billing|payment|entitlement'
```

- [ ] **Step 5: Commit the task**

```bash
git add worker/src/subscriptions.js worker/src/payments.js worker/src/product-entitlements.js worker/test
git commit -m "feat: map legacy payments to assistant pro entitlement"
```

### Task 4: Extend admin APIs for product-aware user creation and independent grant/renew/revoke

**Files:**
- Modify: `worker/src/routes-admin.js`
- Modify: `worker/src/product-entitlements.js`
- Add: `worker/test/admin-product-entitlements.test.mjs`
- Modify: `worker/test/admin-security.test.mjs`

**Interfaces:**
- `POST /api/v1/admin/users` accepts:

```json
{
  "email":"customer@example.com",
  "mobile":"01XXXXXXXXX",
  "password":"password123",
  "products":[
    {"productCode":"KEYBOARD","days":30},
    {"productCode":"ASSISTANT_PRO","days":90}
  ]
}
```

- Product action routes:
  - `POST /api/v1/admin/users/:id/products/:productCode/grant`
  - `POST /api/v1/admin/users/:id/products/:productCode/renew`
  - `POST /api/v1/admin/users/:id/products/:productCode/revoke`
- `GET /api/v1/admin/users` and user detail return normalized entitlement arrays and product summary badge value.
- Legacy `/subscription/grant|renew` routes remain available and operate on `ASSISTANT_PRO` for compatibility.

- [ ] **Step 1: Write failing admin API tests**

Cover create Keyboard-only, Assistant-only, Both with different days, duplicate/invalid product rejection, independent renew/revoke, and archived/blocked user action rejection.

- [ ] **Step 2: Run focused tests and confirm RED**

```bash
cd worker
npm test -- --test-name-pattern='admin product|manual subscription|admin security'
```

- [ ] **Step 3: Update createUserManual transaction and audit metadata**

Always create the base `users` row and legacy inactive `subscriptions` row for compatibility. Then grant only the selected product entitlements. Audit `ADMIN_USER_CREATED` with `products` array and emit product-specific grant audit actions.

- [ ] **Step 4: Add product action routes**

Validate product codes centrally; grant/renew/revoke only the selected product. Return `{ ok:true, entitlement }`.

- [ ] **Step 5: Update users/userDetail query output**

Fetch entitlements for listed users in a bounded query and attach:

```js
{
  entitlements: [...],
  productAccess: 'KEYBOARD' | 'ASSISTANT_PRO' | 'BOTH' | 'NONE'
}
```

Avoid N+1 queries by fetching entitlements for visible user IDs in one query where practical.

- [ ] **Step 6: Run focused tests and confirm GREEN**

```bash
cd worker
npm test -- --test-name-pattern='admin product|manual subscription|admin security'
```

- [ ] **Step 7: Commit the task**

```bash
git add worker/src/routes-admin.js worker/src/product-entitlements.js worker/test/admin-product-entitlements.test.mjs worker/test/admin-security.test.mjs
git commit -m "feat: manage products independently in admin API"
```

### Task 5: Redesign Admin Portal user/product controls

**Files:**
- Modify: `portal-admin/index.html`
- Modify: `portal-admin/app.js`
- Modify: `portal-admin/app.css`
- Modify: `worker/test/admin-portal-rendering.test.mjs`

**Interfaces:**
- Add-user product choices: Keyboard, Assistant Pro, Both.
- User list filters: All, Keyboard, Assistant Pro, Both.
- User rows show badge `KEYBOARD`, `ASSISTANT PRO`, `BOTH`, or `NO PRODUCT ACCESS`.
- Product cards/actions manage each product separately.

- [ ] **Step 1: Add failing rendering/static tests**

Assert the built admin HTML/JS includes product selector IDs, filter IDs, product route patterns, and separate duration inputs for Keyboard and Assistant Pro.

- [ ] **Step 2: Run portal tests and confirm RED**

```bash
cd worker
npm test -- --test-name-pattern='admin portal'
```

- [ ] **Step 3: Update Add User modal**

Use radio/segmented selection for `KEYBOARD`, `ASSISTANT_PRO`, `BOTH`. For selected products show separate day controls; when Both is selected, copy the first duration to the second by default but allow independent edit before submit.

- [ ] **Step 4: Add filter tabs and product badges**

Filter `usersCache` client-side after the server query. Preserve search by email/mobile. Product badges must be visually distinct and readable without relying only on color.

- [ ] **Step 5: Replace generic subscription list with product cards**

Each user shows independent Keyboard and Assistant Pro status/expiry and Grant/Renew/Revoke controls using the new product routes. Keep legacy subscription data hidden from ordinary admin workflow except diagnostics/audit if needed.

- [ ] **Step 6: Run portal tests and build portals**

```bash
cd worker
npm test -- --test-name-pattern='admin portal'
node scripts/build-portals.mjs
```
Expected: PASS; generated `worker/public/admin` assets contain the new product UI.

- [ ] **Step 7: Commit the task**

```bash
git add portal-admin worker/public worker/test/admin-portal-rendering.test.mjs
git commit -m "feat: separate keyboard and assistant access in admin portal"
```

### Task 6: Update Assistant Pro extension product identity while preserving legacy fallback

**Files:**
- Modify: `extension/backend-client.js`
- Modify: `extension/test/protected-migration.test.mjs`
- Modify or add: `extension/test/product-entitlement.test.mjs`

**Interfaces:**
- Updated browser extension sends `X-SocialAI-Product: ASSISTANT_PRO` for product-sensitive authenticated requests.
- Worker still accepts missing header as Assistant Pro for older deployed v35.3.1 builds.

- [ ] **Step 1: Add failing extension-header tests**

Assert `generate()` and `me()` include the product header after update.

- [ ] **Step 2: Run focused extension tests and confirm RED**

```bash
node --test extension/test/product-entitlement.test.mjs
```

- [ ] **Step 3: Add one constant and inject the header**

```js
const PRODUCT_CODE = 'ASSISTANT_PRO';
```

Add `X-SocialAI-Product: PRODUCT_CODE` to product-sensitive requests without changing device-proof body/signature inputs.

- [ ] **Step 4: Run extension tests**

```bash
node --test extension/test/*.test.mjs
```
Expected: PASS.

- [ ] **Step 5: Commit the task**

```bash
git add extension/backend-client.js extension/test
git commit -m "feat: identify assistant pro product requests"
```

### Task 7: Full Worker/Admin release verification and rollout safety

**Files:**
- Modify documentation only if verification results require it: `FINAL_RELEASE_NOTES.txt`, `docs/deployment/PRODUCTION_CHECKLIST.md`

**Interfaces:**
- Validates schema, security, device proof, auth, quota, admin portal, extension compatibility, and migration rollback assumptions.

- [ ] **Step 1: Run the complete Worker test suite**

```bash
cd worker
npm test
```
Expected: PASS.

- [ ] **Step 2: Run extension test suite**

```bash
node --test extension/test/*.test.mjs
```
Expected: PASS.

- [ ] **Step 3: Run release audit/build verification**

```bash
node scripts/release-audit.mjs
bash scripts/verify-release.sh
```
Expected: PASS.

- [ ] **Step 4: Verify migration against a disposable D1 database**

Apply migrations 0001 through 0010 twice. Query `subscriptions` and `product_entitlements`; confirm legacy rows remain and entitlements are not duplicated.

- [ ] **Step 5: Verify rollout order**

Deploy in this order:
1. Worker code that accepts both new header and legacy missing header;
2. migration 0010;
3. Admin Portal assets;
4. updated Assistant Pro extension;
5. updated Keyboard client.

Rollback safety check: old clients continue through legacy Assistant Pro default, and ignoring `product_entitlements` does not delete/alter `subscriptions`.

- [ ] **Step 6: Commit verification documentation**

```bash
git add FINAL_RELEASE_NOTES.txt docs/deployment/PRODUCTION_CHECKLIST.md
git commit -m "docs: verify product entitlement rollout"
```
