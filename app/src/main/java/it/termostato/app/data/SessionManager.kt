package it.termostato.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestione sessione: token, profilo, stagione e credenziali (Remember me).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("termostato_session", Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) = prefs.edit().putString("token", value).apply()

    var profile: String
        get() = prefs.getString("profile", "BASE") ?: "BASE"
        set(value) = prefs.edit().putString("profile", value).apply()

    var season: String
        get() = prefs.getString("season", "") ?: ""
        set(value) = prefs.edit().putString("season", value).apply()

    // Remember me
    var savedApartmentId: String
        get() = prefs.getString("savedApartmentId", "") ?: ""
        set(value) = prefs.edit().putString("savedApartmentId", value).apply()

    var savedPin: String
        get() = prefs.getString("savedPin", "") ?: ""
        set(value) = prefs.edit().putString("savedPin", value).apply()

    var rememberMe: Boolean
        get() = prefs.getBoolean("rememberMe", false)
        set(value) = prefs.edit().putBoolean("rememberMe", value).apply()

    fun saveLogin(token: String, profile: String, season: String, apartmentId: String, pin: String, remember: Boolean) {
        this.token = token
        this.profile = profile
        this.season = season
        if (remember) {
            savedApartmentId = apartmentId
            savedPin = pin
            rememberMe = true
        } else {
            clearSavedCredentials()
        }
    }

    fun clearSavedCredentials() {
        prefs.edit()
            .remove("savedApartmentId")
            .remove("savedPin")
            .putBoolean("rememberMe", false)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove("token")
            .remove("profile")
            .remove("season")
            .apply()
    }

    val isLoggedIn: Boolean get() = token.isNotBlank()
}
