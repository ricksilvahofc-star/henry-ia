package com.henryia.app.core

import android.content.Context

class MemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("henry_memory", 0)

    fun load(): List<String> =
        prefs.getStringSet("facts", emptySet())?.toList().orEmpty()

    fun add(fact: String) {
        val clean = fact.trim()
        if (clean.isBlank()) return
        val current = load().toMutableSet()
        current.add(clean)
        prefs.edit().putStringSet("facts", current.takeLast(100).toSet()).apply()
    }

    fun clear() {
        prefs.edit().remove("facts").apply()
    }
}
