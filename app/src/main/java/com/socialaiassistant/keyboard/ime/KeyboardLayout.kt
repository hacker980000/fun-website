package com.socialaiassistant.keyboard.ime

enum class KeyVisualRole { STANDARD, ALPHABETIC }

data class KeySpec(
    val label: String,
    val output: String? = null,
    val action: KeyboardAction? = null,
    val weight: Float = 1f,
    val visualRole: KeyVisualRole = KeyVisualRole.STANDARD
)

/**
 * Calculator-style numeric pad used by the dedicated NUMBER surface.
 *
 * The rails are intentionally modeled separately from the digit grid so the renderer can
 * reproduce the reference layout: four operators are stacked in the left rail while the
 * three digit rows occupy the center and %, space and backspace occupy the right rail.
 * The bottom row spans the full keyboard width.
 */
data class NumericPadLayout(
    val leftRail: List<KeySpec>,
    val digitRows: List<List<KeySpec>>,
    val rightRail: List<KeySpec>,
    val bottomRow: List<KeySpec>
)

data class KeyboardLayout(val rows: List<List<KeySpec>>) {
    companion object {
        fun forMode(
            mode: KeyboardUiMode,
            showNumberRow: Boolean = false,
            quickKeys: List<String> = emptyList()
        ): KeyboardLayout = when (mode.layer) {
            KeyboardLayer.NUMBERS -> numberLayer(mode)
            KeyboardLayer.SYMBOLS -> symbolLayer(mode)
            KeyboardLayer.LETTERS -> {
                val showAlphabeticNumberRow = AlphabeticNumberRowPolicy.shouldShow(
                    mode = mode,
                    enabledInSettings = showNumberRow
                )
                when {
                    mode.language == KeyboardLanguage.ENGLISH -> englishQwerty(mode.shifted, mode, showAlphabeticNumberRow, quickKeys)
                    mode.banglaMode == BanglaInputMode.PHONETIC -> phoneticQwerty(mode.shifted, mode, showAlphabeticNumberRow, quickKeys)
                    else -> bijoyBangla(mode.shifted, mode, showAlphabeticNumberRow, quickKeys)
                }
            }
        }

        fun englishQwerty(shifted: Boolean = false): KeyboardLayout =
            englishQwerty(shifted, KeyboardUiMode(shifted = shifted), false, emptyList())

        private fun englishQwerty(shifted: Boolean, mode: KeyboardUiMode, showNumberRow: Boolean, quickKeys: List<String>): KeyboardLayout {
            fun letter(value: String): KeySpec {
                val text = if (shifted) value.uppercase() else value
                return KeySpec(label = text, output = text, action = KeyboardAction.Text(text), visualRole = KeyVisualRole.ALPHABETIC)
            }

            val rows = mutableListOf<List<KeySpec>>()
            if (showNumberRow) rows += numberRow()
            if (quickKeys.isNotEmpty()) rows += quickKeyRow(quickKeys)
            rows += listOf(
                    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map(::letter),
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map(::letter),
                    listOf(KeySpec("⇧", action = KeyboardAction.Shift, weight = 1.25f)) +
                        listOf("z", "x", "c", "v", "b", "n", "m").map(::letter) +
                        KeySpec("⌫", action = KeyboardAction.Backspace, weight = 1.25f),
                    bottomRow(mode)
                )
            return KeyboardLayout(rows = rows)
        }

        private fun phoneticQwerty(shifted: Boolean, mode: KeyboardUiMode, showNumberRow: Boolean, quickKeys: List<String>): KeyboardLayout {
            fun letter(value: String): KeySpec {
                val text = if (shifted) value.uppercase() else value
                return KeySpec(label = text, output = text, action = KeyboardAction.Text(text), visualRole = KeyVisualRole.ALPHABETIC)
            }
            val rows = mutableListOf<List<KeySpec>>()
            if (showNumberRow) rows += numberRow()
            if (quickKeys.isNotEmpty()) rows += quickKeyRow(quickKeys)
            rows += listOf(
                    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map(::letter),
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map(::letter),
                    listOf(KeySpec("⇧", action = KeyboardAction.Shift, weight = 1.25f)) +
                        listOf("z", "x", "c", "v", "b", "n", "m").map(::letter) +
                        KeySpec("⌫", action = KeyboardAction.Backspace, weight = 1.25f),
                    bottomRow(mode)
                )
            return KeyboardLayout(rows = rows)
        }

        private fun bijoyBangla(shifted: Boolean, mode: KeyboardUiMode, showNumberRow: Boolean, quickKeys: List<String>): KeyboardLayout {
            val normalRows = listOf(
                listOf("ৌ", "ৈ", "া", "ী", "ূ", "ব", "হ", "গ", "দ", "জ", "ড"),
                listOf("ো", "ে", "্", "ি", "ু", "প", "র", "ক", "ত", "চ", "ট"),
                listOf("ং", "ঁ", "ম", "ু", "ন", "ল", "স", "য়", "ষ")
            )
            val shiftedRows = listOf(
                listOf("ঔ", "ঐ", "আ", "ঈ", "ঊ", "ভ", "ঙ", "ঘ", "ধ", "ঝ", "ঢ"),
                listOf("ও", "এ", "অ", "ই", "উ", "ফ", "ড়", "খ", "থ", "ছ", "ঠ"),
                listOf("ঃ", "ৎ", "ণ", "ৃ", "ঞ", "ং", "শ", "য", "ক্ষ")
            )
            val rows = if (shifted) shiftedRows else normalRows
            val layoutRows = mutableListOf<List<KeySpec>>()
            if (showNumberRow) layoutRows += numberRow()
            if (quickKeys.isNotEmpty()) layoutRows += quickKeyRow(quickKeys)
            layoutRows += listOf(
                    rows[0].map(::alphabeticKey),
                    rows[1].map(::alphabeticKey),
                    listOf(KeySpec("⇧", action = KeyboardAction.Shift, weight = 1.25f)) +
                        rows[2].map(::alphabeticKey) + KeySpec("⌫", action = KeyboardAction.Backspace, weight = 1.25f),
                    bottomRow(mode)
                )
            return KeyboardLayout(rows = layoutRows)
        }

        private fun numberRow(): List<KeySpec> =
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map(::textKey)

        private fun quickKeyRow(values: List<String>): List<KeySpec> =
            values.take(6).map { value ->
                KeySpec(label = value, output = value, action = KeyboardAction.Text(value), weight = if (value.length > 3) 1.6f else 1f)
            }

        /**
         * Shared numeric-pad specification. All theme packages render this exact structure;
         * theme selection only changes colors, fills, radii, typography and effects.
         */
        fun numericPad(mode: KeyboardUiMode): NumericPadLayout = NumericPadLayout(
            leftRail = listOf("+", "-", "*", "/").map { value ->
                KeySpec(value, output = value, action = KeyboardAction.Text(value), weight = 1.5f)
            },
            digitRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            ).map { row ->
                row.map { value -> KeySpec(value, output = value, action = KeyboardAction.Text(value), weight = 2f) }
            },
            rightRail = listOf(
                KeySpec("%", output = "%", action = KeyboardAction.Text("%"), weight = 1.5f),
                KeySpec("␣", output = " ", action = KeyboardAction.Space, weight = 1.5f),
                KeySpec("⌫", action = KeyboardAction.Backspace, weight = 1.5f)
            ),
            bottomRow = listOf(
                KeySpec("ABC", action = KeyboardAction.ShowLetters, weight = 1.5f),
                KeySpec(",", output = ",", action = KeyboardAction.Text(","), weight = 1f),
                KeySpec("!?#", action = KeyboardAction.ShowSymbols, weight = 1f),
                KeySpec("0", output = "0", action = KeyboardAction.Text("0"), weight = 2f),
                KeySpec("=", output = "=", action = KeyboardAction.Text("="), weight = 1f),
                KeySpec(".", output = ".", action = KeyboardAction.Text("."), weight = 1f),
                KeySpec("↵", action = KeyboardAction.Enter, weight = 1.5f)
            )
        )

        /**
         * Row-based fallback retained for tests/non-IME consumers. The production IME uses
         * [numericPad] with the dedicated rail renderer so the operator strip can contain
         * four vertically stacked keys beside only three digit rows.
         */
        private fun numberLayer(mode: KeyboardUiMode): KeyboardLayout {
            val pad = numericPad(mode)
            return KeyboardLayout(
                rows = listOf(
                    listOf(pad.leftRail[0]) + pad.digitRows[0] + pad.rightRail[0],
                    listOf(pad.leftRail[1]) + pad.digitRows[1] + pad.rightRail[1],
                    listOf(
                        pad.leftRail[2].copy(weight = 0.75f),
                        pad.leftRail[3].copy(weight = 0.75f)
                    ) + pad.digitRows[2] + pad.rightRail[2],
                    pad.bottomRow
                )
            )
        }

        private fun symbolLayer(mode: KeyboardUiMode): KeyboardLayout = KeyboardLayout(
            rows = listOf(
                listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=").map(::textKey),
                listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•").map(::textKey),
                listOf(
                    KeySpec("123", action = KeyboardAction.ShowNumbers, weight = 1.5f),
                    KeySpec("…", output = "…", action = KeyboardAction.Text("…")),
                    KeySpec(":", output = ":", action = KeyboardAction.Text(":")),
                    KeySpec(";", output = ";", action = KeyboardAction.Text(";")),
                    KeySpec("⌫", action = KeyboardAction.Backspace, weight = 1.5f)
                ),
                bottomRow(mode, lettersLabel = "ABC")
            )
        )

        private fun bottomRow(mode: KeyboardUiMode, lettersLabel: String? = null): List<KeySpec> {
            val languageLabel = if (mode.language == KeyboardLanguage.ENGLISH) "EN" else "বাংলা"
            val primaryAction = if (lettersLabel == null) KeyboardAction.ShowNumbers else KeyboardAction.ShowLetters
            val primaryLabel = lettersLabel ?: "123"
            return listOf(
                KeySpec(primaryLabel, action = primaryAction, weight = 1.15f),
                KeySpec(languageLabel, action = KeyboardAction.ToggleLanguage, weight = 1.2f),
                KeySpec("space", output = " ", action = KeyboardAction.Space, weight = 4.2f),
                KeySpec("↵", action = KeyboardAction.Enter, weight = 1.3f)
            )
        }

        private fun alphabeticKey(value: String): KeySpec =
            KeySpec(
                label = value,
                output = value,
                action = KeyboardAction.Text(value),
                visualRole = KeyVisualRole.ALPHABETIC
            )

        private fun textKey(value: String): KeySpec =
            KeySpec(label = value, output = value, action = KeyboardAction.Text(value))
    }
}
