package com.henryia.app.core

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class VoiceController(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    fun start(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Reconhecimento de voz não está disponível neste aparelho.")
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}

                override fun onResults(results: android.os.Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotBlank()) onResult(text)
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Não consegui acessar o microfone."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Problema de conexão com o reconhecimento de voz."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Não consegui entender o que foi dito."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "O reconhecimento de voz já está ocupado."
                        else -> "Não foi possível reconhecer sua voz."
                    }
                    onError(message)
                }
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale("pt", "BR"))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
