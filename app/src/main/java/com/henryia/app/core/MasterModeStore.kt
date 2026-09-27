package com.henryia.app.core

import android.content.Context
import java.security.MessageDigest

class MasterModeStore(context: Context) {
    private val prefs = context.getSharedPreferences("henry_master_mode", 0)

    fun hasPassword(): Boolean = prefs.getString("password_hash", "").orEmpty().isNotBlank()

    fun setPassword(password: String) {
        val clean = password.trim()
        if (clean.isBlank()) return
        prefs.edit().putString("password_hash", hash(clean)).apply()
    }

    fun verify(password: String): Boolean =
        hasPassword() && hash(password.trim()) == prefs.getString("password_hash", "")

    fun clearPassword() {
        prefs.edit().remove("password_hash").apply()
    }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
