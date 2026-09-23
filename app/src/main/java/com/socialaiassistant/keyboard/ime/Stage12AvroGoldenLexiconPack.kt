package com.socialaiassistant.keyboard.ime

/**
 * Small exact compatibility pack derived from documented Avro examples.
 *
 * These entries intentionally preserve meaningful Avro case (for example Z/T/N/S)
 * and sit in the dictionary-first layer while the deterministic fallback grammar
 * continues to be expanded independently.
 */
object Stage12AvroGoldenLexiconPack {
    val words: Map<String, String> = linkedMapOf(
        "bybohar" to "ব্যবহার",
        "byakti" to "ব্যক্তি",
        "bishwo" to "বিশ্ব",
        "swagoto" to "স্বাগত",
        "korrmo" to "কর্ম",
        "nirrmol" to "নির্মল",
        "urrdi" to "উর্দি",
        "aZromeTik" to "অ্যারোমেটিক",
        "aZDmin" to "অ্যাডমিন",
        "rriN" to "ঋণ",
        "brritto" to "বৃত্ত",
        "shikSha" to "শিক্ষা",
        "shikkha" to "শিক্ষা",
        "brohmputro" to "ব্রহ্মপুত্র"
    )
}
