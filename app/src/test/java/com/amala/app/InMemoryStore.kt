package com.amala.app

import com.amala.app.data.KeyValueStore

/** Test double for [KeyValueStore] — no Android runtime required. */
class InMemoryStore : KeyValueStore {
    private val values = mutableMapOf<String, Any>()

    override fun getString(key: String, fallback: String?): String? = values[key] as? String ?: fallback

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getBoolean(key: String, fallback: Boolean): Boolean = values[key] as? Boolean ?: fallback

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun keys(): Set<String> = values.keys.toSet()
}
