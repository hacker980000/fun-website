package com.socialaiassistant.keyboard.ai

object ExtensionLanguageLogic {
    private val banglishWords = setOf(
        "ami", "amar", "amake", "amader",
        "tumi", "tomar", "tomake", "tui", "tor", "tore",
        "apni", "apnar", "apnake",
        "valo", "bhalo", "vhalo", "kemon", "kemne", "kivabe",
        "khobor", "khabar", "ki", "keno", "acho", "achen", "achis", "ache", "ase", "asen",
        "nai", "na", "bhai", "vai", "bhaiya", "vaiya", "apu", "bon",
        "kor", "koro", "koren", "korbo", "korben", "korchi", "korcho", "korso",
        "hobe", "hoy", "hoise", "hoilo", "hoye", "jabo", "jaben", "jabi",
        "aso", "ashben", "gelam", "gese", "geso", "bol", "bolo", "bolen", "bolsi",
        "dekhi", "dekho", "dekhen", "onek", "ektu", "aj", "kal", "ekhon",
        "mon", "shona", "jan", "khaiso", "khawa", "ghum", "alhamdulillah",
        "assalamualaikum", "salam", "walaikum"
    )

    private val strongBanglish = Regex(
        "\\b(kemon acho|kemon achen|ki khobor|valo acho|bhalo acho|ki kor|ki koro|ki koren|kmn aso|kmn achen|ki obostha|keno re|ki re|vai ki|bhai ki)\\b",
        RegexOption.IGNORE_CASE
    )

    fun cleanString(value: Any?, maxLength: Int = 5_000): String {
        val boundedLength = maxLength.coerceAtLeast(0)
        return value?.toString().orEmpty()
            .replace("\u0000", "")
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
            .take(boundedLength)
    }

    fun detectLanguageMode(value: Any?): LanguageMode? {
        val text = cleanString(value, 1_800)
        if (text.isEmpty()) return null

        if (Regex("[\\u0980-\\u09FF]").containsMatchIn(text)) return LanguageMode.BENGALI
        if (Regex("[\\u0600-\\u06FF]").containsMatchIn(text)) return LanguageMode.SOURCE_LANGUAGE
        if (Regex("[\\u0900-\\u097F]").containsMatchIn(text)) return LanguageMode.SOURCE_LANGUAGE
        if (Regex("[\\u0400-\\u04FF]").containsMatchIn(text)) return LanguageMode.SOURCE_LANGUAGE
        if (Regex("[\\u3040-\\u30FF\\u3400-\\u9FFF]").containsMatchIn(text)) return LanguageMode.SOURCE_LANGUAGE

        if (Regex("[A-Za-z]").containsMatchIn(text)) {
            val words = Regex("[a-z']+").findAll(text.lowercase()).map { it.value }.toList()
            var score = words.count(banglishWords::contains)
            if (strongBanglish.containsMatchIn(text)) score += 2
            if (score >= 2) return LanguageMode.BANGLISH
            return LanguageMode.LATIN_INFER
        }

        return null
    }

    fun resolveInboxLanguageMode(
        latestRecipientMessage: String?,
        cachedLanguageMode: LanguageMode?
    ): LanguageMode {
        detectLanguageMode(latestRecipientMessage)?.let { return it }
        cachedLanguageMode?.let { return it }
        return LanguageMode.BENGALI_DEFAULT
    }

    fun buildInboxLanguageInstruction(mode: LanguageMode): String = when (mode) {
        LanguageMode.BENGALI -> """
            LANGUAGE MODE: BENGALI.
            The recipient's latest meaningful message is Bengali.
            Reply in natural Bangla using Bengali script.
            Avoid unnecessary English words when a normal Bangla word works.
            Names, brands or unavoidable technical terms may remain unchanged.
            Do not switch to English because SELF previously used English.
        """.trimIndent()

        LanguageMode.BANGLISH -> """
            LANGUAGE MODE: BANGLISH.
            The recipient's latest meaningful message is Banglish / romanized Bengali.
            Reply naturally in Banglish using Latin letters.
            Match the recipient's casual spelling and tone.
            Do not convert Banglish into Bengali script unless the recipient switches to Bengali script.
        """.trimIndent()

        LanguageMode.LATIN_INFER -> """
            LANGUAGE MODE: LATIN-SCRIPT AUTO DETECTION.
            Read the latest OTHER/recipient message carefully and identify its actual language.
            If it is English, reply in English.
            If it is Banglish/romanized Bengali, reply in Banglish.
            If it is another language written in Latin letters, reply in that same language.
            Do not default to English merely because Latin letters are used.
        """.trimIndent()

        LanguageMode.SOURCE_LANGUAGE -> """
            LANGUAGE MODE: MATCH SOURCE LANGUAGE.
            Reply in the same language and writing system as the recipient's latest meaningful message.
            Do not translate it into Bengali or English unless the recipient changes language.
        """.trimIndent()

        LanguageMode.BENGALI_DEFAULT -> """
            LANGUAGE MODE: BANGLADESH DEFAULT.
            There is no reliable recipient message yet, so this is a new or language-ambiguous conversation.
            Use natural Bengali in Bengali script by default.
            Do NOT create the first message in English unless the recipient has already clearly established English.
            Avoid unnecessary English words.
        """.trimIndent()
    }
}
