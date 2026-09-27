package com.henryia.app.ai

import java.util.Locale

object LocalTools {
    fun tryCalculate(text: String): String? {
        val normalized = text.lowercase(Locale.ROOT)
            .replace("quanto é", "")
            .replace("calcule", "")
            .replace("calcular", "")
            .replace("resultado de", "")
            .trim()
        if (!normalized.matches(Regex("[0-9+\\-*/().,% x÷]+"))) return null
        if (!normalized.any { it.isDigit() }) return null
        val expression = normalized.replace("%", "/100").replace("x", "*").replace("÷", "/")
        return runCatching {
            val value = Parser(expression).parse()
            if (!value.isFinite()) return null
            "🧮 Resultado: " + if (value % 1.0 == 0.0) value.toLong() else value
        }.getOrNull()
    }

    private class Parser(private val s: String) {
        private var p = 0
        fun parse(): Double { val v = expr(); skip(); if (p != s.length) error("syntax"); return v }
        private fun expr(): Double {
            var v = term()
            while (true) {
                skip()
                v = when {
                    take('+') -> v + term()
                    take('-') -> v - term()
                    else -> return v
                }
            }
        }
        private fun term(): Double {
            var v = factor()
            while (true) {
                skip()
                v = when {
                    take('*') -> v * factor()
                    take('/') -> v / factor()
                    else -> return v
                }
            }
        }
        private fun factor(): Double {
            skip()
            if (take('+')) return factor()
            if (take('-')) return -factor()
            if (take('(')) { val v = expr(); if (!take(')')) error("paren"); return v }
            val start = p
            while (p < s.length && (s[p].isDigit() || s[p] == '.')) p++
            if (start == p) error("number")
            return s.substring(start, p).toDouble()
        }
        private fun take(c: Char): Boolean {
            skip()
            if (p < s.length && s[p] == c) { p++; return true }
            return false
        }
        private fun skip() { while (p < s.length && s[p].isWhitespace()) p++ }
    }
}
