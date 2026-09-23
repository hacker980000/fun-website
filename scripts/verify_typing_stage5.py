#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing: {rel}")
        return ""
    return p.read_text(encoding="utf-8")

def require(condition, message):
    if not condition:
        errors.append(message)

lex = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ProductionBanglaLexicon.kt")
pack = read("app/src/main/java/com/socialaiassistant/keyboard/ime/Stage5BanglaLexiconPack.kt")
aliases = read("app/src/main/java/com/socialaiassistant/keyboard/ime/Stage5BanglaTypingAliases.kt")
corpus = read("app/src/main/java/com/socialaiassistant/keyboard/ime/Stage5ConversationCorpus.kt")
next_word = read("app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaNextWordModel.kt")
persistent = read("app/src/main/java/com/socialaiassistant/keyboard/ime/PersistentTypingLearningModel.kt")
suggestion = read("app/src/main/java/com/socialaiassistant/keyboard/ime/OfflineBanglaSuggestionEngine.kt")
typing = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ImeTypingEngine.kt")
stage22_policy = read("app/src/main/java/com/socialaiassistant/keyboard/ime/Stage22PredictionConfidence.kt")

require("Stage5BanglaLexiconPack.words" in lex, "Stage-5 lexicon pack is not merged")
require(pack.count(' to "') >= 500, "Stage-5 curated lexicon expansion is unexpectedly small")
require("Stage5BanglaTypingAliases.aliasToCanonical" in suggestion and aliases.count(' to "') >= 80, "Stage-5 conservative alias pack missing")
require("oneEditBuckets" in lex and "oneEditSignatures" in lex, "one-edit typo index missing")
require("boundedDamerauEditDistance" in suggestion and "isSingleAdjacentTransposition" in suggestion, "transposition-aware typo ranking missing")
require(
    "SAFE_TYPO_AUTOCORRECT" in suggestion or (
        "PredictionAutocorrectPolicy.isConfidentBanglaTypo" in suggestion and
        "BANGLA_TYPO_MIN_LENGTH = 5" in stage22_policy and
        "BANGLA_TYPO_MIN_SCORE = 935" in stage22_policy and
        "BANGLA_TYPO_MIN_MARGIN = 45" in stage22_policy
    ),
    "safe typo autocorrect gate missing"
)
require("Stage5ConversationCorpus.sentences" in next_word, "expanded Stage-5 context corpus not wired")
require(corpus.count('"') // 2 >= 180, "Stage-5 conversation corpus is unexpectedly small")
require("contextTransitionCounts" in next_word and "contextKeyCount" in next_word, "two-word corpus context model missing")
require("recordContextTransition" in next_word and "contextTransitionBoost" in next_word, "personal context-learning contract missing")
require("KEY_CONTEXTS" in persistent and "MAX_CONTEXT_TRANSITIONS = 1_024" in persistent, "bounded persistent context learning missing")
require("secondLastCommittedWord" in typing and "learnCommittedTextWithContext" in typing, "IME two-word context tracking missing")

if errors:
    print("TYPING STAGE 5 VERIFICATION: FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("TYPING STAGE 5 VERIFICATION: PASS")
print("- curated production lexicon expansion and alias pack present")
print("- one-edit/transposition typo ranking and conservative autocorrect present")
print("- two-word corpus context and persistent personal context learning present")
