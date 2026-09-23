from pathlib import Path
import sys

src = Path('app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt')
text = src.read_text(encoding='utf-8')
errors = []

if 'HorizontalScrollView.LayoutParams' in text:
    errors.append('HorizontalScrollView.LayoutParams must not be used; it does not resolve in Kotlin here')
if 'import android.widget.FrameLayout' not in text:
    errors.append('FrameLayout import is required for HorizontalScrollView child layout params')
if 'FrameLayout.LayoutParams(' not in text:
    errors.append('categoryRow must use FrameLayout.LayoutParams')

if errors:
    for e in errors:
        print('FAIL:', e)
    sys.exit(1)

print('PASS: emoji category row uses FrameLayout.LayoutParams')
