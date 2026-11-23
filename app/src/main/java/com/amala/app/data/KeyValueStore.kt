package com.amala.app.data

import android.content.Context
import android.content.SharedPreferences

/** Minimal key/value abstraction so profile logic can be tested on the JVM. */
interface KeyValueStore {
    fun getString(key: String, fallback: String?): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, fallback: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, fallback: Int): Int
    fun putInt(key: String, value: Int)
    fun remove(key: String)
    fun keys(): Set<String>
}

class SharedPreferencesStore(context: Context) : KeyValueStore {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("amla_preferences", Context.MODE_PRIVATE)

    override fun getString(key: String, fallback: String?): String? =
        preferences.getString(key, fallback)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, fallback: Boolean): Boolean =
        preferences.getBoolean(key, fallback)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String, fallback: Int): Int = preferences.getInt(key, fallback)

    override fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    override fun keys(): Set<String> = preferences.all.keys
}
