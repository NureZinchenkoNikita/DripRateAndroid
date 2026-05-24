package com.example.driprate.data.api.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TokenManager {
    private const val PREF_NAME = "DripRatePrefs"
    private const val KEY_TOKEN = "jwt_token"
    private var prefs: SharedPreferences? = null

    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _tokenFlow.value = token
    }

    var token: String?
        get() = prefs?.getString(KEY_TOKEN, null)?.replace("\"", "")?.trim()
        set(value) {
            val cleaned = value?.replace("\"", "")?.trim()
            if (cleaned.isNullOrEmpty()) {
                if (prefs?.contains(KEY_TOKEN) == true) {
                    prefs?.edit()?.remove(KEY_TOKEN)?.apply()
                }
                if (_tokenFlow.value != null) {
                    _tokenFlow.value = null
                }
            } else {
                if (cleaned != prefs?.getString(KEY_TOKEN, null)) {
                    prefs?.edit()?.putString(KEY_TOKEN, cleaned)?.apply()
                    _tokenFlow.value = cleaned
                }
            }
        }

    fun clear() {
        token = null
    }
}
