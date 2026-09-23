import com.socialaiassistant.keyboard.ime.ColdStartMilestone
import com.socialaiassistant.keyboard.ime.ColdStartProfiler
import com.socialaiassistant.keyboard.ime.EnglishSuggestionKind
import com.socialaiassistant.keyboard.ime.InMemoryEnglishTypingLearningModel
import com.socialaiassistant.keyboard.ime.InMemoryTypingLearningModel
import com.socialaiassistant.keyboard.ime.OfflineBanglaNextWordModel
import com.socialaiassistant.keyboard.ime.OfflineBanglaSuggestionEngine
import com.socialaiassistant.keyboard.ime.OfflineEnglishNextWordModel
import com.socialaiassistant.keyboard.ime.OfflineEnglishSuggestionEngine
import com.socialaiassistant.keyboard.ime.PredictionRankingPolicy
import com.socialaiassistant.keyboard.ime.SuggestionKind

private var checks = 0
private fun checkThat(value: Boolean, message: String) {
    checks++
    check(value) { message }
}

fun main() {
    checkThat(PredictionRankingPolicy.ENGLISH_ALIAS_BASE > PredictionRankingPolicy.ENGLISH_EXACT_BASE,
        "explicit safe English aliases should outrank raw dictionary spellings")
    checkThat(PredictionRankingPolicy.completionPenalty(8, 3, 7) == 35,
        "completion penalty should be deterministic")
    checkThat(PredictionRankingPolicy.completionPenalty(2, 3, 7) == 0,
        "completion penalty must never become negative")
    checkThat(PredictionRankingPolicy.englishKindPriority(EnglishSuggestionKind.EXACT) >
        PredictionRankingPolicy.englishKindPriority(EnglishSuggestionKind.TYPO),
        "exact English tie-break priority should exceed typo")
    checkThat(PredictionRankingPolicy.banglaKindPriority(SuggestionKind.EXACT) >
        PredictionRankingPolicy.banglaKindPriority(SuggestionKind.FALLBACK),
        "exact Bangla tie-break priority should exceed fallback")

    val cold = ColdStartProfiler(1_000_000L)
    val learning = cold.mark(ColdStartMilestone.LEARNING_MODELS_READY, 2_500_000L)
    checkThat(learning?.elapsedMicros == 1_500L, "cold-start elapsed time should use monotonic nanoseconds")
    checkThat(cold.mark(ColdStartMilestone.LEARNING_MODELS_READY, 3_000_000L) == null,
        "cold-start milestones must be one-shot")
    val service = cold.mark(ColdStartMilestone.SERVICE_READY, 4_000_000L)
    checkThat(service?.elapsedMicros == 3_000L, "service-ready milestone should keep process anchor")
    checkThat(cold.isMarked(ColdStartMilestone.SERVICE_READY), "profiler should expose marked milestone state")
    val clamped = ColdStartProfiler(5_000L).mark(ColdStartMilestone.FIRST_INPUT_VIEW_READY, 4_000L)
    checkThat(clamped?.elapsedMicros == 0L, "clock anomalies must clamp at zero")

    val english = OfflineEnglishSuggestionEngine()
    val alias = english.suggest("dont", 3)
    checkThat(alias.candidates.first().text == "don't", "safe alias should be the first visible candidate")
    checkThat(alias.candidates.first().kind == EnglishSuggestionKind.ALIAS, "safe alias should retain its source kind")
    checkThat(alias.candidates.map { it.text.lowercase() }.distinct().size == alias.candidates.size,
        "English candidates should be deduplicated after quality ordering")
    checkThat(english.suggest("mesage", 3).candidates.first().text == "message",
        "single-edit typo should rank the intended common word first")
    checkThat(english.suggest("tha", 3).candidates.first().text == "that",
        "short English prefix should rank the strongest completion first")
    checkThat(english.suggest("messag", 3).candidates.first().text == "message",
        "near-complete prefix should prefer the shortest strong completion")
    checkThat(english.commitText("Dont", true) == "Don't", "alias autocorrect contract must stay unchanged")

    val englishNext = OfflineEnglishNextWordModel()
    checkThat(englishNext.suggest("thank", 3).firstOrNull()?.text == "you",
        "English next-word corpus should keep 'thank -> you' at top")
    checkThat(englishNext.suggest("hello", 3).any { it.text == "how" },
        "English conversational next-word coverage should remain intact")

    val englishLearning = InMemoryEnglishTypingLearningModel()
    repeat(5) {
        englishLearning.recordWord("update")
        englishLearning.recordTransition("project", "update")
    }
    checkThat(OfflineEnglishNextWordModel(emptyList(), englishLearning).suggest("project", 3).first().text == "update",
        "personal English transition should still be able to re-rank")

    val bangla = OfflineBanglaSuggestionEngine()
    val banglaPrefix = bangla.suggest("am", "আম", 5)
    checkThat(banglaPrefix.candidates.first().text == "আমি", "Bangla common-prefix top result should remain stable")
    val banglaExact = bangla.suggest("ami", "আমি", 5)
    checkThat(banglaExact.candidates.first().text == "আমি" && banglaExact.candidates.first().kind == SuggestionKind.EXACT,
        "Bangla exact candidate should remain first")
    val banglaAlias = bangla.suggest("amr", "আমর", 5)
    checkThat(banglaAlias.candidates.first().text == "আমার" && banglaAlias.candidates.first().kind == SuggestionKind.ALIAS,
        "Bangla safe alias ranking should remain intact")
    checkThat(banglaAlias.candidates.map { it.text }.distinct().size == banglaAlias.candidates.size,
        "Bangla candidates should stay deduplicated")

    val banglaNext = OfflineBanglaNextWordModel()
    val afterAmi = banglaNext.suggest("আমি", 5)
    checkThat(afterAmi.any { it.text == "এখন" }, "Bangla next-word model should retain common 'আমি -> এখন' coverage")
    val withContext = banglaNext.suggest("আমি", "এখন", 5)
    checkThat(withContext.isNotEmpty(), "two-word Bangla context ranking should remain available")

    val banglaLearning = InMemoryTypingLearningModel()
    val learnedBangla = OfflineBanglaNextWordModel(emptyList(), banglaLearning)
    repeat(5) { learnedBangla.learnTransition("আমি", "আসছি") }
    checkThat(learnedBangla.suggest("আমি", 3).firstOrNull()?.text == "আসছি",
        "personal Bangla next-word transition should still be able to re-rank")

    println("Stage21 prediction/cold-start self-test: $checks/$checks PASS")
}
