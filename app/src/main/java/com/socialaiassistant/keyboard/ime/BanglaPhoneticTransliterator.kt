package com.socialaiassistant.keyboard.ime

/**
 * Pure Kotlin phonetic transliteration boundary.
 *
 * Keeping the transliterator Android-free makes it fast to unit-test and lets us
 * replace/extend the rule-set without touching the IME service or AI stack.
 */
fun interface BanglaPhoneticTransliterator {
    fun transliterate(roman: String): String
}

/**
 * Stage-10 Avro-compatible classic transliteration core.
 *
 * This implements the high-value documented Avro case-sensitive character rules
 * and context-sensitive vowel forms used by the keyboard's fallback path. It is
 * deliberately kept separate from the conversational dictionary/autocorrect layer:
 * the dictionary may prefer a modern/common spelling while this class remains a
 * deterministic classic phonetic renderer.
 *
 * Stage 10 closes more high-value grammar gaps, but still does not claim exhaustive
 * byte-for-byte parity with every historical Avro rule. The stable interface keeps
 * remaining grammar expansion isolated from the IME and AI stack.
 */
class AvroCompatibleBanglaPhoneticTransliterator : BanglaPhoneticTransliterator {
    override fun transliterate(roman: String): String {
        if (roman.isBlank()) return roman
        return renderStream(AvroRomanNormalizer.normalizeCase(roman))
    }

    /**
     * Avro is a stream grammar, not a whitespace-only word converter. Stage 10
     * therefore renders Roman runs while preserving/mapping punctuation, digits
     * and backtick escapes instead of abandoning a whole token when punctuation
     * is attached (for example: `ami, tumi?`).
     */
    private fun renderStream(raw: String): String {
        val out = StringBuilder(raw.length)
        val run = StringBuilder()

        fun flushRun() {
            if (run.isNotEmpty()) {
                out.append(renderWord(run.toString()))
                run.clear()
            }
        }

        var index = 0
        while (index < raw.length) {
            val char = raw[index]
            if (char.isLetter() || char == '`') {
                run.append(char)
                index++
                continue
            }

            flushRun()
            val symbol = orderedSymbolTokens.firstOrNull { raw.startsWith(it, index) }
            if (symbol != null) {
                out.append(symbolMap.getValue(symbol))
                index += symbol.length
                continue
            }

            if (char in '0'..'9') {
                out.append(BENGALI_DIGITS[char] ?: char)
            } else {
                out.append(char)
            }
            index++
        }
        flushRun()
        return out.toString()
    }

    private fun renderWord(raw: String): String {
        if (raw.isEmpty()) return raw
        val units = tokenize(raw) ?: return raw
        val out = StringBuilder()
        var canJoinConsonant = false

        units.forEachIndexed { index, unit ->
            val next = units.getOrNull(index + 1)
            when (unit.kind) {
                UnitKind.CONSONANT -> {
                    if (canJoinConsonant) out.append(HASANTA)
                    out.append(unit.output)
                    canJoinConsonant = true
                }

                UnitKind.DIRECT -> {
                    out.append(unit.output)
                    canJoinConsonant = unit.leavesConsonantOpen
                }

                UnitKind.CONTEXT_Y -> {
                    when {
                        index == 0 -> out.append("ইয়")
                        canJoinConsonant -> out.append("্য")
                        else -> out.append("য়")
                    }
                    canJoinConsonant = true
                }

                UnitKind.FORCED_Y_PHALA -> {
                    out.append("্য")
                    canJoinConsonant = true
                }

                UnitKind.CONTEXT_W -> {
                    when {
                        canJoinConsonant -> {
                            out.append("্ব")
                            canJoinConsonant = true
                        }
                        index == 0 && next?.kind in VOWEL_LIKE_KINDS -> {
                            out.append("ওয়")
                            canJoinConsonant = true
                        }
                        else -> {
                            out.append("ও")
                            canJoinConsonant = false
                        }
                    }
                }

                UnitKind.CONTEXT_X -> {
                    // Avro renders x as এক্স at a word boundary, otherwise ক্স.
                    out.append(if (index == 0) "এক্স" else "ক্স")
                    canJoinConsonant = true
                }

                UnitKind.VOWEL -> {
                    if (canJoinConsonant) {
                        out.append(dependentVowel(unit.token, next))
                    } else {
                        out.append(unit.independentOutput)
                    }
                    canJoinConsonant = false
                }

                UnitKind.FORCED_KAR -> {
                    out.append(unit.output)
                    canJoinConsonant = false
                }

                UnitKind.MODIFIER -> {
                    out.append(unit.output)
                    canJoinConsonant = false
                }
            }
        }
        return out.toString()
    }

    private fun dependentVowel(token: String, next: RomanUnit?): String = when (token) {
        // Small o is Avro's inherent-vowel control. Before a following consonant
        // it has no explicit mark (kor -> কর); at a syllable ending we retain the
        // Stage-9 compatibility behavior used by the keyboard (kalo -> কালো).
        "o" -> if (next == null || next.kind in VOWEL_LIKE_KINDS) "ো" else ""
        else -> dependentVowels[token] ?: ""
    }

    private fun tokenize(raw: String): List<RomanUnit>? {
        val result = mutableListOf<RomanUnit>()
        var index = 0
        while (index < raw.length) {
            val token = orderedTokens.firstOrNull { candidate -> raw.startsWith(candidate, index) }
                ?: return null
            result += tokenMap.getValue(token)
            index += token.length
        }
        return result
    }

    private enum class UnitKind {
        CONSONANT,
        DIRECT,
        CONTEXT_Y,
        FORCED_Y_PHALA,
        CONTEXT_W,
        CONTEXT_X,
        VOWEL,
        FORCED_KAR,
        MODIFIER
    }

    private data class RomanUnit(
        val token: String,
        val kind: UnitKind,
        val output: String,
        val independentOutput: String = output,
        val leavesConsonantOpen: Boolean = false
    )

    private companion object {
        const val HASANTA = "্"
        val VOWEL_LIKE_KINDS = setOf(UnitKind.VOWEL, UnitKind.FORCED_KAR)

        val dependentVowels = mapOf(
            "a" to "া",
            "i" to "ি",
            "I" to "ী",
            "ee" to "ী",
            "u" to "ু",
            "oo" to "ু",
            "U" to "ূ",
            "e" to "ে",
            "OI" to "ৈ",
            "oi" to "ৈ",
            "O" to "ো",
            "OU" to "ৌ",
            "ou" to "ৌ",
            "rri" to "ৃ"
        )

        // Stream-level Avro punctuation / escape mappings. Longest tokens win.
        val symbolMap = mapOf(
            "..." to "...",
            ".`" to ".",
            ".." to "।।",
            "." to "।",
            ":`" to ":",
            ":" to "ঃ",
            "^`" to "^",
            "^" to "ঁ",
            ",," to "্‌",
            "," to ",",
            "$" to "৳"
        )
        val orderedSymbolTokens = symbolMap.keys.sortedByDescending(String::length)
        val BENGALI_DIGITS = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )

        val tokenMap: Map<String, RomanUnit> = buildMap {
            fun consonant(token: String, output: String) {
                put(token, RomanUnit(token, UnitKind.CONSONANT, output))
            }
            fun direct(token: String, output: String, leavesConsonantOpen: Boolean = true) {
                put(token, RomanUnit(token, UnitKind.DIRECT, output, leavesConsonantOpen = leavesConsonantOpen))
            }
            fun contextualY(token: String) {
                put(token, RomanUnit(token, UnitKind.CONTEXT_Y, "য়"))
            }
            fun forcedYPhala(token: String) {
                put(token, RomanUnit(token, UnitKind.FORCED_Y_PHALA, "্য", leavesConsonantOpen = true))
            }
            fun contextualW(token: String) {
                put(token, RomanUnit(token, UnitKind.CONTEXT_W, "ও"))
            }
            fun contextualX(token: String) {
                put(token, RomanUnit(token, UnitKind.CONTEXT_X, "ক্স"))
            }
            fun vowel(token: String, independent: String) {
                put(token, RomanUnit(token, UnitKind.VOWEL, independent, independent))
            }
            fun forcedKar(token: String, output: String) {
                put(token, RomanUnit(token, UnitKind.FORCED_KAR, output))
            }
            fun modifier(token: String, output: String) {
                put(token, RomanUnit(token, UnitKind.MODIFIER, output))
            }

            // High-value exact Avro conjunct patterns whose Unicode output cannot
            // be derived safely by simply joining two independent consonants.
            direct("NgkSh", "ঙ্ক্ষ")
            direct("Ngkkh", "ঙ্ক্ষ")
            direct("Ngkx", "ঙ্ক্ষ")
            direct("NGch", "ঞ্ছ")
            direct("Nggh", "ঙ্ঘ")
            direct("Ngkh", "ঙ্খ")
            direct("NGjh", "ঞ্ঝ")
            direct("ngOU", "ঙ্গৌ", leavesConsonantOpen = false)
            direct("ngOI", "ঙ্গৈ", leavesConsonantOpen = false)
            direct("kkh", "ক্ষ")
            direct("kSh", "ক্ষ")
            direct("kx", "ক্ষ")
            direct("ksh", "কশ")
            direct("jNG", "জ্ঞ")
            direct("nch", "ঞ্ছ")
            direct("njh", "ঞ্ঝ")
            direct("ngh", "ঙ্ঘ")
            direct("Ngk", "ঙ্ক")
            direct("Ngg", "ঙ্গ")
            direct("NGj", "ঞ্জ")
            direct("NGc", "ঞ্চ")
            direct("ndh", "ন্ধ")
            direct("nTh", "ন্ঠ")
            direct("NTh", "ণ্ঠ")
            direct("nth", "ন্থ")
            direct("NDh", "ণ্ঢ")
            direct("nga", "ঙ্গা", leavesConsonantOpen = false)
            direct("ngi", "ঙ্গি", leavesConsonantOpen = false)
            direct("ngI", "ঙ্গী", leavesConsonantOpen = false)
            direct("ngu", "ঙ্গু", leavesConsonantOpen = false)
            direct("ngU", "ঙ্গূ", leavesConsonantOpen = false)
            direct("nge", "ঙ্গে", leavesConsonantOpen = false)
            direct("ngO", "ঙ্গো", leavesConsonantOpen = false)
            direct("ngo", "ঙ্গ", leavesConsonantOpen = true)
            direct("gg", "জ্ঞ")
            direct("GG", "জ্ঞ")
            direct("Gg", "জ্ঞ")
            direct("gG", "জ্ঞ")
            direct("nk", "ঙ্ক")
            direct("nc", "ঞ্চ")
            direct("nj", "ঞ্জ")
            direct("nn", "ন্ন")
            direct("NN", "ণ্ণ")
            direct("Nn", "ণ্ন")
            direct("nm", "ন্ম")
            direct("Nm", "ণ্ম")
            direct("nd", "ন্দ")
            direct("nT", "ন্ট")
            direct("NT", "ণ্ট")
            direct("nD", "ন্ড")
            direct("ND", "ণ্ড")
            direct("nt", "ন্ত")
            direct("ns", "ন্স")
            direct("aZ", "অ্যা", leavesConsonantOpen = false)
            direct("oZ", "অ্য", leavesConsonantOpen = true)

            // Explicit-kar backtick forms. These force the dependent vowel even
            // at a word boundary and the trailing backtick is consumed.
            forcedKar("rri`", "ৃ")
            forcedKar("OI`", "ৈ")
            forcedKar("OU`", "ৌ")
            forcedKar("ee`", "ী")
            forcedKar("a`", "া")
            forcedKar("i`", "ি")
            forcedKar("I`", "ী")
            forcedKar("u`", "ু")
            forcedKar("U`", "ূ")
            forcedKar("e`", "ে")
            forcedKar("O`", "ো")
            forcedKar("o`", "")

            consonant("t``", "ৎ")
            consonant("Rh", "ঢ়")
            consonant("kh", "খ")
            consonant("gh", "ঘ")
            consonant("ch", "ছ")
            consonant("jh", "ঝ")
            consonant("Th", "ঠ")
            consonant("Dh", "ঢ")
            consonant("th", "থ")
            consonant("dh", "ধ")
            consonant("ph", "ফ")
            consonant("bh", "ভ")
            consonant("sh", "শ")
            consonant("Sh", "ষ")
            consonant("Ng", "ঙ")
            consonant("NG", "ঞ")
            consonant("rr", "র")

            consonant("k", "ক")
            consonant("q", "ক")
            consonant("g", "গ")
            consonant("G", "গ")
            consonant("c", "চ")
            consonant("C", "ছ")
            consonant("j", "জ")
            consonant("J", "জ")
            consonant("z", "য")
            // Official Avro Y is য়; Z is the explicit য-ফলা form.
            consonant("Y", "য়")
            forcedYPhala("Z")
            consonant("T", "ট")
            consonant("D", "ড")
            consonant("N", "ণ")
            consonant("t", "ত")
            consonant("d", "দ")
            consonant("n", "ন")
            consonant("p", "প")
            consonant("f", "ফ")
            consonant("b", "ব")
            consonant("v", "ভ")
            consonant("m", "ম")
            consonant("r", "র")
            consonant("l", "ল")
            consonant("S", "শ")
            consonant("s", "স")
            consonant("h", "হ")
            consonant("R", "ড়")
            contextualW("w")
            contextualX("x")
            contextualY("y")

            // Bare ng is anusvara; vowelized ng* forms above are direct clusters.
            modifier("ng", "ং")

            vowel("rri", "ঋ")
            vowel("OI", "ঐ")
            vowel("oi", "ঐ")
            vowel("OU", "ঔ")
            vowel("ou", "ঔ")
            vowel("ee", "ঈ")
            vowel("oo", "উ")
            vowel("a", "আ")
            vowel("I", "ঈ")
            vowel("i", "ই")
            vowel("U", "ঊ")
            vowel("u", "উ")
            vowel("e", "এ")
            vowel("O", "ও")
            vowel("o", "অ")
        }

        val orderedTokens = tokenMap.keys.sortedByDescending(String::length)
    }
}

/** Avro-compatible case normalization kept separate for tests and lexicon lookup. */
object AvroRomanNormalizer {
    private const val CASE_SENSITIVE = "oiudgjnrstyz"
    private val sensitiveUppercase = CASE_SENSITIVE.uppercase().toSet()

    fun normalizeCase(text: String): String = buildString(text.length) {
        text.forEach { char ->
            if (char in 'A'..'Z' && char.lowercaseChar() !in CASE_SENSITIVE) {
                append(char.lowercaseChar())
            } else {
                append(char)
            }
        }
    }

    fun containsSensitiveUppercase(text: String): Boolean = text.any { it in sensitiveUppercase }
}

/**
 * Hybrid production engine: conversational dictionary/autocorrect compatible
 * lexical entries are preferred, then the deterministic Avro-compatible classic
 * renderer is used as fallback.
 */
class HybridBanglaPhoneticTransliterator(
    private val lexicon: Map<String, String> = ProductionBanglaLexicon.words,
    private val classic: BanglaPhoneticTransliterator = AvroCompatibleBanglaPhoneticTransliterator()
) : BanglaPhoneticTransliterator {
    override fun transliterate(roman: String): String {
        if (roman.isBlank()) return roman
        lexicon[roman]?.let { return it }

        val normalized = AvroRomanNormalizer.normalizeCase(roman)
        lexicon[normalized]?.let { return it }

        // Do not erase meaningful Avro case (I/U/O/D/G/J/N/R/S/T/Y/Z) just to
        // hit a conversational lowercase dictionary entry.
        if (!AvroRomanNormalizer.containsSensitiveUppercase(normalized)) {
            lexicon[normalized.lowercase()]?.let { return it }
        }
        return classic.transliterate(normalized)
    }
}

/** High-frequency ambiguous words that are safer to resolve lexically than heuristically. */
object CommonBanglaPhoneticLexicon {
    val words: Map<String, String> = mapOf(
        "a" to "আ",
        "ami" to "আমি",
        "amra" to "আমরা",
        "amar" to "আমার",
        "amake" to "আমাকে",
        "amader" to "আমাদের",
        "apni" to "আপনি",
        "apnar" to "আপনার",
        "apnake" to "আপনাকে",
        "apnara" to "আপনারা",
        "tumi" to "তুমি",
        "tomar" to "তোমার",
        "tomake" to "তোমাকে",
        "tomra" to "তোমরা",
        "tader" to "তাদের",
        "tara" to "তারা",
        "se" to "সে",
        "she" to "সে",
        "uni" to "উনি",
        "ora" to "ওরা",
        "eta" to "এটা",
        "eita" to "এইটা",
        "oti" to "অতি",
        "oi" to "ওই",
        "oita" to "ওটা",
        "ki" to "কি",
        "kivabe" to "কিভাবে",
        "kibhabe" to "কীভাবে",
        "keno" to "কেন",
        "kothay" to "কোথায়",
        "kothae" to "কোথায়",
        "kobe" to "কবে",
        "ke" to "কে",
        "kar" to "কার",
        "kemon" to "কেমন",
        "kemonacho" to "কেমন আছো",
        "acho" to "আছো",
        "achi" to "আছি",
        "acchi" to "আছি",
        "achen" to "আছেন",
        "achhen" to "আছেন",
        "valo" to "ভালো",
        "bhalo" to "ভালো",
        "bhalobashi" to "ভালোবাসি",
        "bhalobasa" to "ভালোবাসা",
        "bhalobasha" to "ভালোবাসা",
        "sundor" to "সুন্দর",
        "khub" to "খুব",
        "onek" to "অনেক",
        "ektu" to "একটু",
        "ekhon" to "এখন",
        "tokhon" to "তখন",
        "aj" to "আজ",
        "aaj" to "আজ",
        "kal" to "কাল",
        "agami" to "আগামী",
        "sokal" to "সকাল",
        "dupur" to "দুপুর",
        "bikal" to "বিকাল",
        "rat" to "রাত",
        "raat" to "রাত",
        "din" to "দিন",
        "somoy" to "সময়",
        "shomoy" to "সময়",
        "kaj" to "কাজ",
        "kaaj" to "কাজ",
        "korbo" to "করবো",
        "korchi" to "করছি",
        "kori" to "করি",
        "koro" to "করো",
        "koren" to "করেন",
        "korben" to "করবেন",
        "korte" to "করতে",
        "kore" to "করে",
        "kora" to "করা",
        "hobe" to "হবে",
        "hoy" to "হয়",
        "hocche" to "হচ্ছে",
        "hoyeche" to "হয়েছে",
        "holo" to "হলো",
        "jabo" to "যাবো",
        "jachi" to "যাচ্ছি",
        "jacchi" to "যাচ্ছি",
        "jai" to "যাই",
        "jan" to "জান",
        "jani" to "জানি",
        "janina" to "জানি না",
        "bolbo" to "বলবো",
        "bolchi" to "বলছি",
        "bolo" to "বলো",
        "bolen" to "বলেন",
        "bole" to "বলে",
        "dekhbo" to "দেখবো",
        "dekhi" to "দেখি",
        "dekho" to "দেখো",
        "dekhen" to "দেখেন",
        "dibo" to "দিবো",
        "dao" to "দাও",
        "den" to "দেন",
        "nibo" to "নিবো",
        "nio" to "নিও",
        "nen" to "নেন",
        "asbo" to "আসবো",
        "ashbo" to "আসবো",
        "asi" to "আসি",
        "ashi" to "আসি",
        "ashen" to "আসেন",
        "ashe" to "আসে",
        "gechi" to "গেছি",
        "gese" to "গেছে",
        "geche" to "গেছে",
        "gelam" to "গেলাম",
        "jodi" to "যদি",
        "tahole" to "তাহলে",
        "kintu" to "কিন্তু",
        "ebong" to "এবং",
        "ar" to "আর",
        "ba" to "বা",
        "na" to "না",
        "nai" to "নাই",
        "nei" to "নেই",
        "hya" to "হ্যাঁ",
        "ha" to "হ্যাঁ",
        "ji" to "জি",
        "thik" to "ঠিক",
        "thikache" to "ঠিক আছে",
        "accha" to "আচ্ছা",
        "acha" to "আচ্ছা",
        "dhonnobad" to "ধন্যবাদ",
        "dhonyobad" to "ধন্যবাদ",
        "donnobad" to "ধন্যবাদ",
        "please" to "প্লিজ",
        "plz" to "প্লিজ",
        "sorry" to "সরি",
        "sry" to "সরি",
        "bangla" to "বাংলা",
        "banglay" to "বাংলায়",
        "bangladesh" to "বাংলাদেশ",
        "dhaka" to "ঢাকা",
        "chattogram" to "চট্টগ্রাম",
        "sylhet" to "সিলেট",
        "rajshahi" to "রাজশাহী",
        "khulna" to "খুলনা",
        "barishal" to "বরিশাল",
        "rongpur" to "রংপুর",
        "rangpur" to "রংপুর",
        "mymensingh" to "ময়মনসিংহ",
        "facebook" to "ফেসবুক",
        "messenger" to "মেসেঞ্জার",
        "whatsapp" to "হোয়াটসঅ্যাপ",
        "youtube" to "ইউটিউব",
        "video" to "ভিডিও",
        "photo" to "ফটো",
        "mobile" to "মোবাইল",
        "phone" to "ফোন",
        "internet" to "ইন্টারনেট",
        "message" to "মেসেজ",
        "reply" to "রিপ্লাই",
        "office" to "অফিস",
        "school" to "স্কুল",
        "college" to "কলেজ",
        "university" to "ইউনিভার্সিটি",
        "doctor" to "ডাক্তার",
        "engineer" to "ইঞ্জিনিয়ার",
        "sir" to "স্যার",
        "madam" to "ম্যাডাম",
        "vai" to "ভাই",
        "bhai" to "ভাই",
        "apu" to "আপু",
        "bondhu" to "বন্ধু",
        "dost" to "দোস্ত",
        "priyo" to "প্রিয়",
        "shuvo" to "শুভ",
        "subho" to "শুভ",
        "prothom" to "প্রথম",
        "notun" to "নতুন",
        "purono" to "পুরোনো",
        "choto" to "ছোট",
        "boro" to "বড়",
        "bari" to "বাড়ি",
        "bashay" to "বাসায়",
        "basay" to "বাসায়",
        "rastay" to "রাস্তায়",
        "taka" to "টাকা",
        "tk" to "টাকা"
    )
}
