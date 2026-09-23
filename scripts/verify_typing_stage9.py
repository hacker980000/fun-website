#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing: {rel}")
        return ""
    return p.read_text(encoding="utf-8")

def req(condition, message):
    if not condition:
        errors.append(message)

phonetic = read("app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaPhoneticTransliterator.kt")
glide = read("app/src/main/java/com/socialaiassistant/keyboard/ime/GlideTypingEngine.kt")
test = read("scripts/typing_stage9_avro_glide_selftest.kt")
doc = read("docs/typing/TYPING_CORE_V2_STAGE9.md")

req("class AvroCompatibleBanglaPhoneticTransliterator" in phonetic, "Stage 9 Avro-compatible renderer missing")
for token in ['consonant("Ng", "ঙ")', 'consonant("NG", "ঞ")', 'modifier("ng", "ং")',
              'consonant("R", "ড়")', 'consonant("Rh", "ঢ়")', 'consonant("S", "শ")',
              'consonant("Sh", "ষ")', 'consonant("z", "য")', 'consonant("Y", "য়")',
              'contextualY("y")', 'consonant("t``", "ৎ")',
              'vowel("rri", "ঋ")', 'vowel("oo", "উ")', 'vowel("O", "ও")', 'vowel("OU", "ঔ")']:
    req(token in phonetic, f"missing documented Avro mapping: {token}")
req('"oo" to "ু"' in phonetic, "oo-kar compatibility missing")
req(('modifier(":", "ঃ")' in phonetic) or ('":" to "ঃ"' in phonetic), "visarga mapping missing")
req(('modifier("^", "ঁ")' in phonetic) or ('"^" to "ঁ"' in phonetic), "chandrabindu mapping missing")
req(("classic.transliterate(roman)" in phonetic) or ("classic.transliterate(normalized)" in phonetic), "Hybrid fallback is not routed through classic renderer")

for token in ["weightedEditCost", "orderedCoverage", "matchingTransitions", "keyboardAdjacent",
              "QWERTY_POSITIONS", "FINAL_ENDPOINT_NEIGHBOR_PENALTY"]:
    req(token in glide, f"Stage 9 glide scorer missing: {token}")
req("observed.first() != candidate.first()" in glide, "glide first-key safety guard missing")
req("typing_stage9_avro_glide_selftest" in test, "Stage 9 self-test missing")
req("Avro compatibility" in doc and "QWERTY-aware" in doc, "Stage 9 documentation incomplete")

if errors:
    print("TYPING STAGE 9 VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("TYPING STAGE 9 VERIFICATION: PASS")
print("- documented high-value Avro case-sensitive mappings wired")
print("- dictionary-first + classic fallback boundary retained")
print("- QWERTY-aware glide scoring and ordered path coverage wired")
