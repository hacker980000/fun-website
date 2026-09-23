# Stage 24.2 - Optional Bubble Effect per Keyboard Theme

Stage 24.2 makes the Stage 24.1 Bubble Flight to Caret effect optional for every premium keyboard theme package.

## User behavior

Each premium keyboard theme remembers its own Bubble Effect preferences:

- Classic Dark
- Glass Modern
- Clean Light
- Gradient Pro

Each package stores:

- **Bubble Effect**: On / Off
- **Bubble Style**: Soft / Normal / Playful

Fresh installs start with Bubble Effect **Off** and style **Normal**. Existing installs are migrated safely: the previous global Bubble Key On/Off and intensity values seed only theme-pack values that do not already exist.

## Settings surfaces

`ThemeSettingsActivity` shows Bubble Effect controls inside every premium Theme Package card, next to that package's Key Boundary control. The existing Keyboard Preferences Bubble controls remain as a backward-compatible shortcut for the currently selected global premium theme. Legacy/custom themes continue to use the previous global Bubble settings.

## IME resolution

The IME resolves the actual theme pack used by the current keyboard surface, including per-surface theme overrides. Bubble flight policy receives the selected pack's `enabled` and `intensity` values.

When Bubble Effect is Off for the active pack:

- no new Bubble Flight request is created;
- the local bubble renderer is released;
- active flights for the current editor are cancelled;
- pending retarget state is cleared;
- normal text commit/typing is unchanged.

When Bubble Effect is On, Stage 24.1 behavior is unchanged: the bubble starts at the exact pressed key, text commits immediately, and the bubble flies toward the caret/textbox target.

## Safety and performance

Existing Stage 24.1 policy remains authoritative. Bubble Effect remains blocked for sensitive fields, glide drags, non-letter layers, disabled system animations, and non-alphabetic labels. The maximum simultaneous bubble count remains eight.
