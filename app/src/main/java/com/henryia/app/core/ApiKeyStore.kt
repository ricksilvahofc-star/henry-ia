package com.henryia.app.core

import android.content.Context

class ApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("henry_settings", Context.MODE_PRIVATE)
    fun getOpenRouterKey(): String = prefs.getString("openrouter_key", "") ?: ""
    fun setOpenRouterKey(value: String) { prefs.edit().putString("openrouter_key", value.trim()).apply() }
}
