# Typing Core v2 — Stage 18

Stage 18 reduces repeated main-thread work without changing typing semantics.

## Runtime changes

- Keyboard rows use a render signature. Identical populated layouts are retained instead of removing/recreating every key View.
- The signature includes language/layer/shift state, number row, editor quick keys, key height, one-handed mode, Glide state, key gap and Enter action label.
- Suggestion computation uses a one-entry in-memory memo keyed by language + typing-engine suggestion revision + hint settings.
- Bangla and English typing engines increment a revision whenever visible suggestion state can change.
- Suggestion buttons use a render signature, so identical candidate rows are not recreated.
- Theme updates restyle existing dynamic Views and only rebuild keys when the layout signature actually changes.
- Memory-pressure cleanup clears the suggestion memo; no typed text is persisted by Stage 18 caches.

## Safety

All caches are process-memory only and bounded to one current result/signature. No typed content is written to disk, logs, analytics, backend, AI training, or context storage by this stage.
