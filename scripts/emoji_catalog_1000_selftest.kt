import com.socialaiassistant.keyboard.ime.EmojiCatalog

private fun fail(message: String): Nothing = error(message)

fun main() {
    val categoriesMethod = EmojiCatalog.javaClass.methods.firstOrNull {
        it.name == "categories" && it.parameterCount == 0
    } ?: fail("EmojiCatalog.categories() is missing")

    val categories = categoriesMethod.invoke(EmojiCatalog) as? List<*>
        ?: fail("EmojiCatalog.categories() must return a List")

    check(categories.size == 20) { "Expected 20 categories, got ${categories.size}" }

    val all = mutableListOf<String>()
    val ids = mutableSetOf<String>()
    val labels = mutableSetOf<String>()
    categories.forEachIndexed { index, category ->
        category ?: fail("Category $index is null")
        val clazz = category.javaClass
        val id = clazz.getMethod("getId").invoke(category) as String
        val label = clazz.getMethod("getLabel").invoke(category) as String
        val icon = clazz.getMethod("getIcon").invoke(category) as String
        val emojis = clazz.getMethod("getEmojis").invoke(category) as List<*>
        check(id.isNotBlank()) { "Category $index id is blank" }
        check(label.isNotBlank()) { "Category $index label is blank" }
        check(icon.isNotBlank()) { "Category $label icon is blank" }
        check(ids.add(id)) { "Duplicate category id: $id" }
        check(labels.add(label)) { "Duplicate category label: $label" }
        check(emojis.size == 50) { "$label expected 50 emojis, got ${emojis.size}" }
        emojis.forEach { value ->
            check(value is String && value.isNotBlank()) { "$label contains invalid emoji value" }
            all += value
        }
    }

    check(all.size == 1000) { "Expected 1000 emoji entries, got ${all.size}" }
    check(all.distinct().size == 1000) {
        "Expected 1000 globally unique emoji entries, got ${all.distinct().size}"
    }


    val byId = categories.associateBy { category ->
        val clazz = category!!.javaClass
        clazz.getMethod("getId").invoke(category) as String
    }
    fun emojis(id: String): List<String> {
        val category = byId[id] ?: fail("Missing category: $id")
        @Suppress("UNCHECKED_CAST")
        return category.javaClass.getMethod("getEmojis").invoke(category) as List<String>
    }
    val representatives = mapOf(
        "smileys" to "😀", "people" to "🧑", "love" to "❤️", "gestures" to "👍",
        "body" to "👀", "animals" to "🐶", "nature" to "☀️", "food_drink" to "🍔",
        "activities" to "🎮", "sports" to "⚽", "travel" to "🚗", "places" to "🏠",
        "objects" to "🔑", "technology" to "💻", "fashion" to "👕", "work_study" to "📚",
        "health_fitness" to "🩺", "celebration" to "🎉", "symbols" to "✅", "flags" to "🇧🇩"
    )
    representatives.forEach { (id, emoji) ->
        check(emoji in emojis(id)) { "$id is missing representative emoji $emoji" }
    }
    val unrelatedNature = setOf("🥄", "🥅", "🥇", "🥈", "🥉", "🥊")
    check(emojis("nature").none { it in unrelatedNature }) { "Nature contains unrelated sports/utensil emoji" }

    val requiredOldCommon = listOf(
        "🙂", "😊", "😂", "❤️", "👍", "🙏", "😍", "🥰", "😅", "😢",
        "😮", "🔥", "🎉", "✅", "💯", "🤝", "👏", "👌", "🤔", "🙌"
    )
    requiredOldCommon.forEach { emoji ->
        check(emoji in all) { "Old common emoji missing from expanded catalog: $emoji" }
    }

    println("PASS: 20 categories, 50 each, 1000 globally unique emojis, old common set preserved")
}
