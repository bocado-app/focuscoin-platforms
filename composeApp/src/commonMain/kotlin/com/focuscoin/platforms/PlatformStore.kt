package com.focuscoin.platforms

expect object PlatformStore {
    fun isIos(): Boolean
    fun nowMillis(): Long
    fun todayKey(): String
    fun readLong(key: String, fallback: Long = 0L): Long
    fun writeLong(key: String, value: Long)
    fun readString(key: String, fallback: String = ""): String
    fun writeString(key: String, value: String)
}
