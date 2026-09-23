# Social AI Assistant Pro 35.3.1 -> Social AI Keyboard Port Map

| v35.3.1 capability | Android keyboard status | Implementation / note |
|---|---|---|
| Protected Cloudflare Worker AI gateway | PORTED | `backend/SocialAiBackendClient.kt`, `ai/GatewayRouter.kt` |
| No OpenRouter secret in customer client | PORTED for Managed AI | Managed requests go through Worker; personal key remains optional user-supplied legacy mode |
| Privacy/Data consent gate | PORTED | separate saved consent; AI generation is blocked until accepted |
| Preserve Draft setting | PORTED | default ON; explicit Insert appends after existing draft, OFF replaces draft |
| Monthly plan / WhatsApp Support | PORTED | settings screen mirrors plan/support entry point and User Portal remains available |
| Hosted Login/Register + Turnstile | PORTED | `AuthActivity.kt` hosts the existing same-origin auth page and exchanges one-time code |
| Single-flight login / duplicate auth protection | PORTED | process-level `AtomicBoolean` guard in `AuthActivity` |
| Same-installation multi-account after logout/login | PORTED | stable installation ID + replaceable encrypted account session; backend migration 0009 remains authoritative |
| One Account -> One Active Device | PORTED via server | Worker binding policy; Android sends stable installation ID |
| 24-hour device-transfer cooldown / old-session revocation | PORTED via server | unchanged server behavior |
| Cryptographic Device Proof | PORTED | Android Keystore ECDSA P-256, public JWK binding, signed requests, DER -> P1363 conversion |
| Subscription enforcement | PORTED | Worker-authoritative; account screen shows state |
| Daily/cycle quota | PORTED | Worker-authoritative; account screen shows usage |
| Abuse/replay/security gates | PORTED via server | UUID request IDs + signed body/timestamp; backend security remains authoritative |
| Version gate | PORTED for backend compatibility | Android reports runtime `35.3.1` using existing Chromium release channel contract |
| User Portal | PORTED | `PortalActivity.kt` opens existing same-origin portal |
| Payment/account/device/password/activity/notification controls | AVAILABLE through User Portal / backend | no duplicate client-side security logic |
| Global AI Training Center | PORTED via server | server selects relevant examples/rules/knowledge; keyboard does not embed global dataset |
| Personal AI Training | PORTED | encrypted local storage, separate per managed account, max 1800 chars, sent only during AI requests |
| Smart Reply / Smart Comment | PORTED | explicit managed action; personal legacy behavior retained |
| Unique / Witty Reply / Comment | PORTED | existing AI panel + managed payload |
| Flirty Reply / Comment | PORTED | existing AI panel + managed payload; server flirty guard remains authoritative |
| Funny Comment | PORTED | new `FUNNY_CMT` mode with sensitive/serious-context prompt safeguards |
| Bengali/Banglish/English comment language mapping | PORTED | English -> English; Bengali/Banglish/Latin-infer -> Bengali default |
| Write Caption - Romantic | PORTED | `CaptionActivity.kt` |
| Write Caption - Funny | PORTED | `CaptionActivity.kt` |
| Write Caption - Emotional | PORTED | `CaptionActivity.kt` |
| Photo Caption | PORTED | up to 4 explicit user-selected photos, locally resized/re-encoded |
| Photo metadata protection | PORTED | decoded pixels are re-encoded to JPEG; original EXIF/GPS is not forwarded |
| Caption max 4 media items | PORTED | client limit + server validation |
| Caption manual insert / no auto-post | PORTED | `CaptionDraftBus.kt` + explicit Insert |
| Draft preservation / manual send | PRESERVED | keyboard only inserts/replaces/appends after explicit action; no send/post API |
| Rewrite / Translate / Grammar Fix / Regenerate | PRESERVED | Personal OpenRouter fallback for draft transforms where current Worker lacks equivalent endpoint |
| Browser DOM media preview analysis | NOT DIRECTLY PORTABLE | Android IME cannot safely inspect arbitrary host-app images; explicit Photo Caption is the privacy-preserving equivalent |
| Browser store update links / Chrome-Firefox target packaging | NOT APPLICABLE | Android has a different distribution/update channel; Worker version enforcement remains active |
| Admin Portal owner controls | SERVER/PORTAL FEATURE, NOT CUSTOMER IME UI | Add/delete/restore users, manual subscription grant, security events, system health and Training Center remain in protected Admin Portal |

## Security invariants

1. No autonomous send or post.
2. No Accessibility clicks/gestures/global actions.
3. Sensitive fields block AI/context storage.
4. Managed AI generation requires an explicit user action.
5. Account training is encrypted and account-scoped.
6. Photo transmission requires explicit selection/action and uses sanitized re-encoded copies.
7. Server remains the source of truth for auth, subscription, quota, device binding, transfer cooldown, security and global training.
