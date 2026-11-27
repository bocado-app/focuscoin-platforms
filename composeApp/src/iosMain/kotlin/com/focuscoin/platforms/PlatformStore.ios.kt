package com.focuscoin.platforms

import platform.Foundation.NSNumber
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUserDefaults

actual object ForegroundProcess {
    actual fun executableName(): String? = null
}

actual object PlatformStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    actual fun isIos(): Boolean = true
    actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
    actual fun todayKey(): String = NSDateFormatter().apply { dateFormat = "yyyy-MM-dd" }.stringFromDate(NSDate())
    actual fun readLong(key: String, fallback: Long): Long =
        (defaults.objectForKey(key) as? NSNumber)?.longLongValue ?: fallback

    actual fun writeLong(key: String, value: Long) {
        defaults.setObject(NSNumber(value.toDouble()), forKey = key)
    }

    actual fun readString(key: String, fallback: String): String =
        defaults.stringForKey(key) ?: fallback

    actual fun writeString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
