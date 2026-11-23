package com.amala.app.model

/** How Amla chooses its light/dark appearance. */
enum class ThemeMode(val label: String) {
    FOLLOW_SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED black");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.name == key } ?: FOLLOW_SYSTEM
    }
}

/** Ready-made accent colours for the theme picker. */
data class AccentOption(val name: String, val argb: Int)
