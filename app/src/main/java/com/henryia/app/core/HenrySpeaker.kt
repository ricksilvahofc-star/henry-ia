package com.henryia.app.core

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class HenrySpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale("pt", "BR")
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.0f)
            }
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        val clean = text
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("[*_#]"), "")
            .trim()
        if (clean.isNotBlank()) {
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "henry-response")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
