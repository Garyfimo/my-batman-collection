package com.garyfimo.batmancollection.login.data

import android.content.SharedPreferences
import com.garyfimo.batmancollection.login.domain.SessionStore

class SharedPreferencesSessionStore(
    private val prefs: SharedPreferences,
) : SessionStore {

    override fun signInAt(): Long? =
        if (prefs.contains(KEY_SIGN_IN_AT)) prefs.getLong(KEY_SIGN_IN_AT, 0L) else null

    override fun recordSignIn(epochMillis: Long) {
        prefs.edit().putLong(KEY_SIGN_IN_AT, epochMillis).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_SIGN_IN_AT).apply()
    }

    companion object {
        const val PREFS_NAME = "auth_session"
        private const val KEY_SIGN_IN_AT = "sign_in_at_millis"
    }
}
