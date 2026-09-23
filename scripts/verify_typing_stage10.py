#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing: {rel}")
        return ""
    return path.read_text(encoding="utf-8")

def req(condition, message):
    if not condition:
        errors.append(message)

phonetic = read("app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaPhoneticTransliterator.kt")
test = read("scripts/typing_stage10_avro_quality_selftest.kt")
bench = read("scripts/typing_stage10_dictionary_benchmark.kt")
doc = read("docs/typing/TYPING_CORE_V2_STAGE10.md")

for token in [
    "object AvroRomanNormalizer", 'consonant("Y", "য়")', 'forcedYPhala("Z")',
    'contextualW("w")', 'contextualX("x")', 'direct("gg", "জ্ঞ")',
    'direct("jNG", "জ্ঞ")', 'direct("kSh", "ক্ষ")', 'direct("ksh", "কশ")',
    'direct("nk", "ঙ্ক")', 'direct("nga", "ঙ্গা"', 'forcedKar("OI`", "ৈ")',
    '".." to "।।"', '"$" to "৳"', "BENGALI_DIGITS"
]:
    req(token in phonetic, f"Stage 10 phonetic hardening missing: {token}")

req("containsSensitiveUppercase" in phonetic, "case-sensitive dictionary guard missing")
req("typing_stage10_avro_quality_selftest" in test, "Stage 10 self-test missing")
req("normalizationConflicts" in bench and "dictionaryExactMismatches" in bench, "Stage 10 dictionary benchmark incomplete")
req("Avro grammar hardening" in doc and "quality benchmark" in doc.lower(), "Stage 10 documentation incomplete")

if errors:
    print("TYPING STAGE 10 VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("TYPING STAGE 10 VERIFICATION: PASS")
print("- Avro case/y-phala/w/x/punctuation/digit hardening wired")
print("- high-value conjunct and forced-kar mappings wired")
print("- dictionary normalization quality benchmark wired")
