import com.socialaiassistant.keyboard.ime.EnglishSuggestionKind
import com.socialaiassistant.keyboard.ime.InMemoryEnglishTypingLearningModel
import com.socialaiassistant.keyboard.ime.InMemoryTypingLearningModel
import com.socialaiassistant.keyboard.ime.OfflineBanglaNextWordModel
import com.socialaiassistant.keyboard.ime.OfflineBanglaSuggestionEngine
import com.socialaiassistant.keyboard.ime.OfflineEnglishNextWordModel
import com.socialaiassistant.keyboard.ime.OfflineEnglishSuggestionEngine
import com.socialaiassistant.keyboard.ime.PredictionAutocorrectPolicy
import com.socialaiassistant.keyboard.ime.SuggestionKind

private var checks = 0
private fun checkThat(value: Boolean, message: String) {
    checks++
    check(value) { message }
}

private data class EnglishFixture(
    val input: String,
    val expectedTop: String,
    val expectedAutocorrect: String?
)

fun main() {
    checkThat(
        PredictionAutocorrectPolicy.isConfidentEnglishTypo(5, 2_000, null),
        "single strong English typo candidate should be eligible"
    )
    checkThat(
        PredictionAutocorrectPolicy.isConfidentEnglishTypo(4, 2_000, 1_800),
        "English typo with a clear score margin should be eligible"
    )
    checkThat(
        !PredictionAutocorrectPolicy.isConfidentEnglishTypo(4, 2_000, 1_900),
        "English typo with an ambiguous runner-up should be blocked"
    )
    checkThat(
        !PredictionAutocorrectPolicy.isConfidentEnglishTypo(3, 2_800, null),
        "short English tokens should not be fuzzy auto-replaced"
    )
    checkThat(
        !PredictionAutocorrectPolicy.isConfidentEnglishTypo(8, 1_400, null),
        "low-confidence English typo should be blocked"
    )
    checkThat(
        PredictionAutocorrectPolicy.isConfidentBanglaTypo(5, 980, 900),
        "existing Bangla safe-typo envelope should remain eligible"
    )
    checkThat(
        !PredictionAutocorrectPolicy.isConfidentBanglaTypo(5, 980, 950),
        "existing Bangla ambiguity margin should remain enforced"
    )
    checkThat(
        PredictionAutocorrectPolicy.isConfidentTypo(5, Int.MAX_VALUE, Int.MIN_VALUE, 4, 0, 1),
        "confidence subtraction must be overflow-safe"
    )

    val english = OfflineEnglishSuggestionEngine()
    val fixtures = listOf(
        EnglishFixture("dont", "don't", "don't"),
        EnglishFixture("mesage", "message", "message"),
        EnglishFixture("thnks", "thanks", "thanks"),
        EnglishFixture("wdth", "with", "with"),
        EnglishFixture("widh", "with", "with"),
        EnglishFixture("helo", "hello", "hello"),
        EnglishFixture("acount", "account", "account"),
        EnglishFixture("widht", "width", "width"),
        EnglishFixture("shave", "save", null),
        EnglishFixture("typea", "type", null),
        EnglishFixture("teh", "the", null)
    )
    fixtures.forEach { fixture ->
        val snapshot = english.suggest(fixture.input, 5)
        checkThat(
            snapshot.candidates.firstOrNull()?.text == fixture.expectedTop,
            "${fixture.input} should keep deterministic top candidate ${fixture.expectedTop}"
        )
        checkThat(
            snapshot.autocorrectText == fixture.expectedAutocorrect,
            "${fixture.input} autocorrect decision should match confidence policy"
        )
    }

    val alias = english.suggest("Dont", 5)
    checkThat(alias.candidates.first().kind == EnglishSuggestionKind.ALIAS,
        "safe alias should still outrank the raw exact dictionary form")
    checkThat(alias.autocorrectText == "Don't", "safe alias casing contract must remain intact")
    checkThat(english.suggest("message", 5).autocorrectText == null,
        "known exact English words must never be fuzzy autocorrected")
    checkThat(english.commitText("shave", true) == "shave",
        "ambiguous English typo must remain exactly what the user typed")
    checkThat(english.commitText("wdth", true) == "with",
        "high-margin English typo should commit the confident correction")
    checkThat(english.commitText("wdth", false) == "wdth",
        "disabling autocorrect must preserve literal English input")

    val englishNext = OfflineEnglishNextWordModel()
    checkThat(englishNext.suggest("thank", 3).firstOrNull()?.text == "you",
        "Stage 21 English next-word fixture must remain stable")
    checkThat(englishNext.suggest("please", 3).isNotEmpty(),
        "English next-word prediction coverage should remain available")

    val englishLearning = InMemoryEnglishTypingLearningModel()
    repeat(6) {
        englishLearning.recordWord("update")
        englishLearning.recordTransition("project", "update")
    }
    checkThat(
        OfflineEnglishNextWordModel(emptyList(), englishLearning).suggest("project", 3).firstOrNull()?.text == "update",
        "personal English next-word re-ranking must remain intact"
    )

    val bangla = OfflineBanglaSuggestionEngine()
    val banglaExact = bangla.suggest("ami", "আমি", 5)
    checkThat(
        banglaExact.candidates.firstOrNull()?.text == "আমি" && banglaExact.candidates.first().kind == SuggestionKind.EXACT,
        "Bangla exact ranking must remain intact"
    )
    val banglaAlias = bangla.suggest("amr", "আমর", 5)
    checkThat(
        banglaAlias.candidates.firstOrNull()?.text == "আমার" && banglaAlias.candidates.first().kind == SuggestionKind.ALIAS,
        "Bangla safe alias must remain intact"
    )
    checkThat(banglaAlias.autocorrectText == "আমার", "Bangla explicit alias autocorrect contract must remain intact")

    val banglaNext = OfflineBanglaNextWordModel()
    checkThat(banglaNext.suggest("আমি", 5).any { it.text == "এখন" },
        "Stage 21 Bangla next-word fixture must remain stable")

    val banglaLearning = InMemoryTypingLearningModel()
    val learnedBangla = OfflineBanglaNextWordModel(emptyList(), banglaLearning)
    repeat(5) { learnedBangla.learnTransition("আমি", "আসছি") }
    checkThat(learnedBangla.suggest("আমি", 3).firstOrNull()?.text == "আসছি",
        "personal Bangla next-word re-ranking must remain intact")

    println("Stage22 autocorrect/prediction-quality self-test: $checks/$checks PASS")
}
