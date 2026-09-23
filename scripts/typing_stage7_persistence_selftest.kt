import android.content.Context
import android.content.SharedPreferences
import com.socialaiassistant.keyboard.ime.PersistentEnglishTypingLearningModel

private class FakePreferences : SharedPreferences {
    private val values = linkedMapOf<String, Any>()
    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        (values[key] as? Set<String>)?.toSet() ?: defValues
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners += listener
    }
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners -= listener
    }
    override fun edit(): SharedPreferences.Editor = EditorImpl()

    private inner class EditorImpl : SharedPreferences.Editor {
        private val updates = linkedMapOf<String, Any?>()
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = apply { updates[key] = values?.toSet() }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { updates[key] = value }
        override fun remove(key: String): SharedPreferences.Editor = apply { updates[key] = null }
        override fun apply() {
            updates.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
                listeners.toList().forEach { it.onSharedPreferenceChanged(this@FakePreferences, key) }
            }
        }
    }
}

private class FakeContext : Context() {
    private val prefs = FakePreferences()
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = prefs
}

fun main() {
    var checks = 0
    val context = FakeContext()
    val first = PersistentEnglishTypingLearningModel(context)
    repeat(4) {
        first.recordWord("project")
        first.recordWord("update")
        first.recordTransition("project", "update")
    }
    first.flush()
    check(first.stats().learnedWords == 2)
    checks++
    check(first.stats().learnedTransitions == 1)
    checks++

    val second = PersistentEnglishTypingLearningModel(context)
    check(second.wordBoost("project") > 0)
    checks++
    check(second.transitionBoost("project", "update") > 0)
    checks++
    check(second.transitionCandidates("project", 3).first().text == "update")
    checks++

    PersistentEnglishTypingLearningModel.clearStoredLearning(context)
    val third = PersistentEnglishTypingLearningModel(context)
    check(third.stats().learnedWords == 0)
    checks++
    check(third.stats().learnedTransitions == 0)
    checks++

    println("typing_stage7_persistence_selftest: $checks checks PASS")
}
