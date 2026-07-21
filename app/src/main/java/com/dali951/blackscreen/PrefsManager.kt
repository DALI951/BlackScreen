package com.dali951.blackscreen

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("blackscreen_prefs", Context.MODE_PRIVATE)

    var showClock: Boolean
        get() = prefs.getBoolean(KEY_SHOW_CLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_CLOCK, value).apply()

    var showDate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DATE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DATE, value).apply()

    var showBattery: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BATTERY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BATTERY, value).apply()

    var showMedia: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MEDIA, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_MEDIA, value).apply()

    var aodTimeout: Long
        get() = prefs.getLong(KEY_AOD_TIMEOUT, 10_000L)
        set(value) = prefs.edit().putLong(KEY_AOD_TIMEOUT, value).apply()

    companion object {
        private const val KEY_SHOW_CLOCK = "show_clock"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_SHOW_BATTERY = "show_battery"
        private const val KEY_SHOW_MEDIA = "show_media"
        private const val KEY_AOD_TIMEOUT = "aod_timeout"
    }
}
