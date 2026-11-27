package com.focuscoin.platforms

import java.util.prefs.Preferences

actual object PlatformStore {
    private val prefs = Preferences.userRoot().node("focuscoin/platforms")
    actual fun isIos(): Boolean = false
    actual fun readLong(key: String, fallback: Long): Long = prefs.getLong(key, fallback)
    actual fun writeLong(key: String, value: Long) { prefs.putLong(key, value) }
    actual fun readString(key: String, fallback: String): String = prefs.get(key, fallback)
    actual fun writeString(key: String, value: String) { prefs.put(key, value) }
}
