package com.socialaiassistant.keyboard.ime

object ProductionEnglishLexicon {
    val words: Map<String, Int> = linkedMapOf(
        "hello" to 1400,
        "help" to 1200,
        "message" to 1350,
        "thanks" to 1300,
        "there" to 1250,
        "keyboard" to 1100,
        "typing" to 1050
    )
}

object ProductionBanglaLexicon {
    val words: Map<String, String> = linkedMapOf(
        "ami" to "আমি",
        "amar" to "আমার",
        "valo" to "ভালো",
        "kemon" to "কেমন",
        "kmn" to "কেমন",
        "bangla" to "বাংলা",
        "message" to "মেসেজ"
    )
    val frequencyBonus: Map<String, Int> = mapOf(
        "ami" to 120,
        "amar" to 110,
        "valo" to 105,
        "kemon" to 100,
        "kmn" to 90,
        "bangla" to 100,
        "message" to 80
    )
}
