#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

FORBIDDEN_NAMES = {
    "flirty_conversations_500.json",
    "flirty_conversations_500.md",
    "flirty-conversation-expansion-pack-0011.js",
}
FORBIDDEN_MARKERS = ("flirt-conv-0011-bl-", "flirt-conv-0011-bn-")
REQUIRED_PROMPT_MARKERS = ("soft flirt", "continuation", "normal chat")


def _read(rel: str) -> str:
    path = ROOT / rel
    return path.read_text(encoding="utf-8", errors="ignore") if path.exists() else ""


def check_training_parity(require_release_integration: bool = True) -> list[str]:
    errors: list[str] = []
    app_main = ROOT / "app/src/main"

    bundled_names = sorted(
        str(path.relative_to(ROOT))
        for path in app_main.rglob("*")
        if path.is_file() and path.name in FORBIDDEN_NAMES
    )
    if bundled_names:
        errors.append(f"500-conversation corpus files must not be bundled: {bundled_names}")

    marker_hits: list[str] = []
    for path in app_main.rglob("*"):
        if not path.is_file() or path.suffix.lower() in {".png", ".jpg", ".jpeg", ".gif", ".webp", ".jar"}:
            continue
        data = path.read_text(encoding="utf-8", errors="ignore").lower()
        for marker in FORBIDDEN_MARKERS:
            if marker in data:
                marker_hits.append(f"{path.relative_to(ROOT)}:{marker}")
    if marker_hits:
        errors.append(f"conversation corpus IDs must not be embedded in Android source: {marker_hits}")

    prompt = _read("app/src/main/java/com/socialaiassistant/keyboard/ai/ExtensionPromptBuilder.kt").lower()
    for marker in REQUIRED_PROMPT_MARKERS:
        if marker not in prompt:
            errors.append(f"personal prompt policy missing marker: {marker}")

    payload = _read("app/src/main/java/com/socialaiassistant/keyboard/backend/ManagedAiPayload.kt")
    if 'put("conversationIntent", it.name)' not in payload:
        errors.append("Managed AI payload must continue emitting conversationIntent")
    if 'sourcePlatform: String = "ANDROID_KEYBOARD"' not in payload:
        errors.append("Managed AI payload must retain ANDROID_KEYBOARD source identity")

    backend_config = _read("app/src/main/java/com/socialaiassistant/keyboard/backend/BackendConfig.kt")
    client = _read("app/src/main/java/com/socialaiassistant/keyboard/backend/SocialAiBackendClient.kt")
    if 'const val PRODUCT_CODE = "KEYBOARD"' not in backend_config or 'PRODUCT_HEADER = "X-SocialAI-Product"' not in client:
        errors.append("backend config/client must retain KEYBOARD product identity header")
    if "builder.header(PRODUCT_HEADER, BackendConfig.PRODUCT_CODE)" not in client:
        errors.append("product-sensitive backend requests must send centralized KEYBOARD identity")

    if require_release_integration:
        release = _read("scripts/verify_release_ready.py")
        if "check_training_parity" not in release:
            errors.append("verify_release_ready.py must invoke check_training_parity")

    return errors


def main() -> int:
    errors = check_training_parity(require_release_integration=True)
    if errors:
        print("TRAINING PARITY VERIFICATION: FAIL")
        for error in errors:
            print(f"- {error}")
        return 1
    print("TRAINING PARITY VERIFICATION: PASS")
    print("- compact Personal AI style/boundary markers present")
    print("- 500-conversation corpus files/IDs absent from app/src/main")
    print("- Managed AI conversationIntent and KEYBOARD product identity preserved")
    print("- release verifier includes this parity gate")
    return 0


if __name__ == "__main__":
    sys.exit(main())
