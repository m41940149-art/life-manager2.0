package com.example.auth

import android.content.Context
import android.content.SharedPreferences

object AuthPreferences {
    private const val PREFS_NAME = "life_manager_auth_prefs"
    private const val KEY_CUSTOM_WEB_CLIENT_ID = "custom_web_client_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCustomWebClientId(context: Context): String? {
        val id = getPrefs(context).getString(KEY_CUSTOM_WEB_CLIENT_ID, null)
        return if (!id.isNullOrBlank()) id else null
    }

    fun setCustomWebClientId(context: Context, webClientId: String?) {
        getPrefs(context).edit().putString(KEY_CUSTOM_WEB_CLIENT_ID, webClientId?.trim()).apply()
    }

    fun resolveServerClientId(context: Context): String? {
        // 1. Try reading generated default_web_client_id from R.string
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            try {
                val id = context.getString(resId)
                if (id.isNotBlank()) return id
            } catch (_: Exception) {}
        }

        // 2. Try custom Web Client ID saved by user
        return getCustomWebClientId(context)
    }
}
