from pathlib import Path

src = Path('app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt').read_text()
catalog = Path('app/src/main/java/com/socialaiassistant/keyboard/ime/EmojiCatalog.kt').read_text()
checks = {
    'catalog_categories_api': 'fun categories()' in catalog,
    'horizontal_category_bar': 'HorizontalScrollView' in src,
    'scrollable_emoji_grid': 'ScrollView' in src,
    'category_buttons_from_catalog': 'EmojiCatalog.categories()' in src,
    'category_switch_rendering': 'renderEmojiCategory' in src,
    'emoji_insertion_preserved': 'currentInputConnection?.commitText(emoji, 1)' in src,
}
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit('FAIL: ' + ', '.join(failed))
print('PASS: category bar, scrollable grid, category switching, insertion wiring present')
