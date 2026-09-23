#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]


def check_shared_backend_config(require_release_integration: bool = True) -> list[str]:
    errors: list[str] = []
    backend = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/backend/BackendConfig.kt').read_text()
    client = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/backend/SocialAiBackendClient.kt').read_text()
    settings = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt').read_text()
    ai_settings = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ai/AiSettingsRepository.kt').read_text()
    manual = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiActionEngine.kt').read_text()
    settings_category = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt').read_text()

    def require(cond: bool, msg: str):
        if not cond:
            errors.append(msg)

    require('const val API_BASE = "https://super-boat-4aba.madigitalstudio2018.workers.dev"' in backend,
            'Keyboard must use the existing shared Cloudflare Worker URL.')
    require('const val PRODUCT_CODE = "KEYBOARD"' in backend,
            'Keyboard product identity must be centralized as KEYBOARD.')
    require('val gatewayMode: GatewayMode = GatewayMode.MANAGED' in settings,
            'Fresh installs must default to Managed AI/shared backend.')
    require('if (value.equals("personal", ignoreCase = true)) PERSONAL else MANAGED' in ai_settings,
            'Stored mode decoder must default missing/unknown values to MANAGED while preserving explicit PERSONAL.')
    require('val gatewayMode: GatewayMode = GatewayMode.MANAGED' in manual,
            'Manual AI engine defaults must match the Managed AI product default.')
    require('builder.header(PRODUCT_HEADER, BackendConfig.PRODUCT_CODE)' in client,
            'Product-sensitive backend requests must use centralized KEYBOARD identity.')
    require('GatewayMode.PERSONAL' in settings_category and 'button_use_personal' in settings_category,
            'Personal OpenRouter must remain an explicit optional mode.')

    if require_release_integration:
        release = (ROOT / 'scripts/verify_release_ready.py').read_text()
        require('check_shared_backend_config' in release,
                'Release verifier must invoke the shared-backend configuration gate.')

    for path in (ROOT / 'app/src/main').rglob('*'):
        if not path.is_file() or path.suffix.lower() not in {'.kt','.xml','.json','.properties'}:
            continue
        text = path.read_text(errors='ignore')
        if re.search(r'\bsk-or-v1-[A-Za-z0-9_-]{16,}', text) or re.search(r'\bsk-[A-Za-z0-9_-]{24,}', text):
            errors.append(f'Likely provider secret embedded in {path.relative_to(ROOT)}')

    return errors


def main() -> int:
    errors = check_shared_backend_config(require_release_integration=True)
    if errors:
        print('SHARED BACKEND CONFIG VERIFICATION: FAIL')
        for e in errors:
            print('-', e)
        return 1
    print('SHARED BACKEND CONFIG VERIFICATION: PASS')
    print('- existing Worker URL reused')
    print('- KEYBOARD product identity centralized')
    print('- Managed AI is fresh-install default')
    print('- Personal OpenRouter remains optional')
    print('- no likely provider secret embedded')
    print('- release verifier includes this gate')
    return 0


if __name__ == '__main__':
    sys.exit(main())
