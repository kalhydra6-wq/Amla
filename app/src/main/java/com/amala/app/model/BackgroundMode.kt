package com.amala.app.model

/** How Amla decides whether its background service should run. */
enum class BackgroundMode(val label: String) {
    ALWAYS("Always on"),
    SMART("Smart"),
    MANUAL("Manual");

    companion object {
        fun fromKey(key: String?): BackgroundMode = entries.firstOrNull { it.name == key } ?: ALWAYS
    }
}
