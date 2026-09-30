package com.socialaiassistant.keyboard.context

enum class PostGenre {
    POLITICAL,
    HUMANITARIAN_SAD,
    EMOTIONAL,
    ROMANTIC,
    FUNNY,
    CELEBRATORY,
    MOTIVATIONAL,
    RELIGIOUS,
    INFORMATIONAL,
    CASUAL
}

/** Lightweight local pre-classifier. The model still receives the source text and must verify context. */
object PostSentimentClassifier {
    fun classify(postText: String): PostGenre {
        val clean = postText.lowercase().replace(Regex("\\s+"), " ").trim()
        if (clean.isBlank()) return PostGenre.CASUAL

        val scored = linkedMapOf(
            PostGenre.HUMANITARIAN_SAD to score(clean, SAD_TERMS),
            PostGenre.POLITICAL to score(clean, POLITICAL_TERMS),
            PostGenre.RELIGIOUS to score(clean, RELIGIOUS_TERMS),
            PostGenre.CELEBRATORY to score(clean, CELEBRATION_TERMS),
            PostGenre.FUNNY to score(clean, FUNNY_TERMS),
            PostGenre.ROMANTIC to score(clean, ROMANTIC_TERMS),
            PostGenre.EMOTIONAL to score(clean, EMOTIONAL_TERMS),
            PostGenre.MOTIVATIONAL to score(clean, MOTIVATIONAL_TERMS),
            PostGenre.INFORMATIONAL to score(clean, INFORMATION_TERMS)
        )
        val best = scored.maxByOrNull { it.value }
        return if (best == null || best.value <= 0) PostGenre.CASUAL else best.key
    }

    private fun score(text: String, terms: List<Pair<String, Int>>): Int =
        terms.sumOf { (term, weight) -> if (text.contains(term)) weight else 0 }

    private val SAD_TERMS = listOf(
        "💔" to 3, "😭" to 3, "মৃত্যু" to 4, "দুর্ঘটনা" to 4, "অসুস্থ" to 3, "হাসপাতাল" to 2,
        "অসহায়" to 3, "অসহায়" to 3, "কষ্ট" to 2, "কান্না" to 2, "শোক" to 4, "লাশ" to 4,
        "সাহায্য" to 1, "tragedy" to 4, "death" to 4, "accident" to 4, "sick" to 2,
        "helpless" to 3, "grief" to 3, "condolence" to 3
    )
    private val POLITICAL_TERMS = listOf(
        "রাজনীতি" to 4, "নির্বাচন" to 4, "ভোট" to 3, "সরকার" to 2, "মন্ত্রী" to 2, "সংসদ" to 3,
        "প্রার্থী" to 3, "politics" to 4, "election" to 4, "vote" to 3, "government" to 2,
        "minister" to 2, "parliament" to 3, "candidate" to 3, "campaign" to 3
    )
    private val RELIGIOUS_TERMS = listOf(
        "আল্লাহ" to 2, "ইনশাআল্লাহ" to 2, "আলহামদুলিল্লাহ" to 2, "দোয়া" to 2, "দোয়া" to 2,
        "মসজিদ" to 2, "ইসলাম" to 2, "prayer" to 2, "dua" to 2, "religious" to 2
    )
    private val CELEBRATION_TERMS = listOf(
        "অভিনন্দন" to 3, "শুভেচ্ছা" to 2, "জন্মদিন" to 3, "বিয়ে" to 3, "বিবাহ" to 3, "সাফল্য" to 2,
        "congratulations" to 3, "birthday" to 3, "wedding" to 3, "anniversary" to 3, "graduation" to 3, "🎉" to 3
    )
    private val FUNNY_TERMS = listOf(
        "😂" to 4, "🤣" to 4, "হাসি" to 2, "মজা" to 2, "ফানি" to 3, "মিম" to 3,
        "funny" to 3, "lol" to 2, "meme" to 3, "joke" to 3
    )
    private val ROMANTIC_TERMS = listOf(
        "❤️" to 3, "💕" to 3, "ভালোবাসা" to 3, "প্রেম" to 3, "প্রিয়" to 2, "প্রিয়" to 2,
        "love" to 3, "romantic" to 3, "valentine" to 3
    )
    private val EMOTIONAL_TERMS = listOf(
        "মনে পড়ে" to 2, "মনে পড়ে" to 2, "একাকী" to 2, "স্মৃতি" to 2, "মন খারাপ" to 3,
        "emotional" to 3, "miss you" to 2, "memory" to 1, "lonely" to 2
    )
    private val MOTIVATIONAL_TERMS = listOf(
        "অনুপ্রেরণা" to 3, "হাল ছাড়" to 2, "স্বপ্ন" to 1, "সফল" to 2, "পরিশ্রম" to 2,
        "motivation" to 3, "never give up" to 3, "dream" to 1, "success" to 2, "hard work" to 2
    )
    private val INFORMATION_TERMS = listOf(
        "সংবাদ" to 3, "খবর" to 2, "তথ্য" to 2, "নোটিশ" to 3, "আপডেট" to 2,
        "news" to 3, "update" to 2, "notice" to 3, "information" to 2, "announcement" to 2
    )
}
