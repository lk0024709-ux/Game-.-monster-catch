package com.example.rann.profile

import android.content.Context

/**
 * Thin Android storage layer for [PlayerProfile].
 * Keeps the profile logic itself pure Kotlin (and unit-testable).
 */
class ProfileStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): PlayerProfile = ProfileCodec.decode(prefs.getString(KEY_PROFILE, null))

    fun save(profile: PlayerProfile) {
        prefs.edit().putString(KEY_PROFILE, ProfileCodec.encode(profile)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PROFILE).apply()
    }

    companion object {
        private const val PREFS = "rannbhoomi_save"
        private const val KEY_PROFILE = "profile_v1"
    }
}
