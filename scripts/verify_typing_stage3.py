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

lexicon = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ProductionBanglaLexicon.kt")
transliterator = read("app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaPhoneticTransliterator.kt")
suggestion = read("app/src/main/java/com/socialaiassistant/keyboard/ime/OfflineBanglaSuggestionEngine.kt")
next_word = read("app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaNextWordModel.kt")
typing = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ImeTypingEngine.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")

require("object ProductionBanglaLexicon" in lexicon, "expanded production lexicon missing")
require("class BanglaLexiconIndex" in lexicon, "indexed lexicon lookup missing")
require("prefixBuckets" in lexicon and "typoBuckets" in lexicon, "prefix/typo indexes missing")
require("ProductionBanglaLexicon.words" in transliterator, "phonetic transliterator must use Stage-3 lexicon")
require("SuggestionKind.NEXT_WORD" in typing, "typing engine must expose next-word candidates")
require("class OfflineBanglaNextWordModel" in next_word, "offline next-word model missing")
require("object BanglaConversationCorpus" in next_word, "built-in local conversation corpus missing")
require("interface LocalTypingLearningModel" in next_word, "personal learning seam missing")
require("class InMemoryTypingLearningModel" in next_word, "session-local learning foundation missing")
require("transitionBoost" in next_word and "wordBoost" in next_word, "personal ranking boosts missing")
require("index.prefix" in suggestion and "index.typoCandidates" in suggestion, "suggestion engine must use indexed lookup")
require("lexicon.asSequence()" not in suggestion, "Stage-3 suggestion engine must not full-scan the lexicon")
require("fun isShowingNextWordSuggestions" in typing, "next-word visibility state missing")
require("learnCommittedText" in typing, "committed text must feed local learning/context")
require("typingEngine.isShowingNextWordSuggestions()" in service, "suggestion bar must support next-word state")
require(('connection.commitText("$accepted ", 1)' in service) or ('commitText("$accepted ", 1)' in service and "safeConnectionOperation" in service), "next-word tap must insert candidate with a word boundary")
require("val hadComposition = isActiveComposing()" in service or "val hadComposition = typingEngine.isComposing()" in service, "Space handling must distinguish word commit vs extra space")
require("if (!hadComposition) dismissActiveNextWordSuggestions()" in service or "if (!hadComposition) typingEngine.dismissNextWordSuggestions()" in service, "extra Space must dismiss stale next-word candidates")
require("typingEngine.setSuggestionsEnabled(allowSuggestions)" in service and "currentFieldPolicy.showSuggestions" in service, "sensitive/editor-policy fields must disable typing suggestions")
require("currentFieldPolicy.allowLearning" in service and "currentSettings.personalTypingLearning" in service, "sensitive/editor-policy fields or disabled preference must block local typing learning")
require("FieldSafety.BLOCK_AI" in service and "currentFieldPolicy.showSuggestions" in service, "sensitive/editor-policy fields must hide suggestion predictions")

# Static scale guard: Stage-3 source should contain a substantial number of roman->Bangla additions.
extra_entry_count = len(re.findall(r'"[^"\n]+"\s+to\s+"[^"\n]+"', lexicon))
base_entry_count = len(re.findall(r'"[^"\n]+"\s+to\s+"[^"\n]+"', transliterator.split("object CommonBanglaPhoneticLexicon", 1)[-1]))
entry_count = extra_entry_count + base_entry_count
require(entry_count >= 650, f"merged Stage-3 lexicon source is unexpectedly small: {entry_count} entries")
corpus_count = len(re.findall(r'"[^"\n]+"', next_word.split("val sentences: List<String> = listOf(", 1)[-1].split(")\n}", 1)[0]))
require(corpus_count >= 120, f"next-word corpus is unexpectedly small: {corpus_count} sentences")

if errors:
    print("TYPING STAGE 3 VERIFICATION: FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("TYPING STAGE 3 VERIFICATION: PASS")
print(f"- lexicon source entries: {entry_count}")
print(f"- local corpus sentences: {corpus_count}")
print("- indexed prefix/typo lookup present")
print("- offline next-word + session learning foundation present")
