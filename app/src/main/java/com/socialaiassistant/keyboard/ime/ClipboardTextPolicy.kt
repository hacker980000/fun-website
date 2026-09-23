package com.socialaiassistant.keyboard.ime

object ClipboardTextPolicy {
    private const val MAX_ITEMS = 20
    private const val MAX_LABEL = 42

    fun sanitize(values: List<String>): List<String> = values
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .take(MAX_ITEMS)

    fun label(value: String): String =
        if (value.length <= MAX_LABEL) value else value.take(MAX_LABEL) + "…"

    fun shouldExposeContent(fieldSensitive: Boolean, clipMarkedSensitive: Boolean): Boolean =
        !fieldSensitive && !clipMarkedSensitive
}
