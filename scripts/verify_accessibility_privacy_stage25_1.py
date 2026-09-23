#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
service = (root / 'app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt').read_text()
registry = (root / 'app/src/main/java/com/socialaiassistant/keyboard/context/PlatformContextAdapterRegistry.kt').read_text()
test = (root / 'app/src/test/java/com/socialaiassistant/keyboard/context/PlatformContextAdapterRegistryTest.kt').read_text()
privacy = (root / 'docs/privacy/context-access-disclosure.md').read_text()
checklist = (root / 'docs/testing/multi-app-context-adapters-checklist.md').read_text()

checks = []
def require(name, condition):
    checks.append((name, bool(condition)))
    if not condition:
        raise SystemExit(f'FAIL: {name}')

require('registry exposes explicit package support check', 'fun supportsPackage(packageName: String): Boolean' in registry)
require('unknown registry inputs return no nodes', 'nodes = emptyList()' in registry)
require('unknown registry inputs discard conversation hint', 'conversationHint = null' in registry)
require('service gates unsupported packages before debounce extraction', service.find('if (!platformAdapters.supportsPackage(packageName))') < service.find('debounceExtract(packageName'))
extract_start = service.find('private fun extractCurrentWindow')
root_read = service.find('val root = rootInActiveWindow', extract_start)
guard = service.find('if (!platformAdapters.supportsPackage(packageName))', extract_start)
require('extractCurrentWindow rechecks package support before root traversal', extract_start >= 0 and guard >= 0 and root_read >= 0 and guard < root_read)
require('unsupported-package path clears snapshot', 'ContextSnapshotBus.clear()' in service[guard:root_read])
require('pending context extractions can be cancelled', 'cancelPendingContextExtractions()' in service)
require('registry regression test rejects unknown app', 'rejectsUnknownAppsFailClosed()' in test)
require('generic snapshot regression test proves null for unknown app', 'unknownAppsCannotProduceGenericSnapshot()' in test and 'check(snapshot == null)' in test)
require('privacy disclosure documents fail-closed boundary', 'Unsupported app packages do not fall back to generic Accessibility conversation-text capture.' in privacy)
require('device checklist covers unsupported app boundary', 'Unsupported App Privacy Boundary' in checklist)

print(f'Stage 25.1 accessibility privacy verifier PASS ({len(checks)}/{len(checks)})')
for name, _ in checks:
    print(f'  PASS: {name}')
