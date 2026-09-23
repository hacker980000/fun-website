package com.socialaiassistant.keyboard.ime

/** Thread-safe lazy resource that can be released under memory pressure and recreated later. */
class ResettableLazyResource<T>(private val factory: () -> T) {
    @Volatile private var value: T? = null

    fun get(): T = value ?: synchronized(this) {
        value ?: factory().also { value = it }
    }

    @Synchronized
    fun release(): Boolean {
        if (value == null) return false
        value = null
        return true
    }

    fun isInitialized(): Boolean = value != null
}
