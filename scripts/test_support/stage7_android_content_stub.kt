package android.content

abstract class Context {
    open val applicationContext: Context get() = this
    abstract fun getSharedPreferences(name: String, mode: Int): SharedPreferences
    companion object { const val MODE_PRIVATE: Int = 0 }
}

interface SharedPreferences {
    fun getLong(key: String, defValue: Long): Long
    fun getStringSet(key: String, defValues: Set<String>?): Set<String>?
    fun edit(): Editor
    fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener)

    fun interface OnSharedPreferenceChangeListener {
        fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
    }

    interface Editor {
        fun putStringSet(key: String, values: Set<String>?): Editor
        fun putLong(key: String, value: Long): Editor
        fun remove(key: String): Editor
        fun apply()
    }
}
