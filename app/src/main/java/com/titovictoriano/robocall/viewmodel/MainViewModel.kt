package com.titovictoriano.robocall.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.titovictoriano.robocall.model.VoiceState
import com.titovictoriano.robocall.util.CommandExecutor
import com.titovictoriano.robocall.util.CommandParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val commandExecutor = CommandExecutor(application)
    
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    init {
        if (SpeechRecognizer.isRecognitionAvailable(application)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
            setupSpeechRecognizer()
        } else {
            _voiceState.value = VoiceState.Error("Speech recognition is not available on this device")
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.Listening
            }

            override fun onBeginningOfSpeech() {
                _voiceState.value = VoiceState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Update UI to show that we're receiving audio
                if (_voiceState.value is VoiceState.Listening) {
                    _voiceState.value = VoiceState.Listening
                }
            }

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.Processing("Processing your speech...")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches.isNullOrEmpty()) {
                    _voiceState.value = VoiceState.Error("No speech detected")
                    return
                }

                val spokenText = matches[0]
                _voiceState.value = VoiceState.Processing(spokenText)
                
                viewModelScope.launch {
                    processCommand(spokenText)
                }
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Please try speaking again"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Please speak after tapping the mic"
                    else -> "Unknown error"
                }
                _voiceState.value = VoiceState.Error(errorMessage)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
        })
    }

    fun startListening() {
        if (speechRecognizer == null) {
            _voiceState.value = VoiceState.Error("Speech recognition is not available")
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Add timing parameters
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            }
            
            _voiceState.value = VoiceState.Preparing
            speechRecognizer?.cancel() // Cancel any ongoing recognition
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("Failed to start speech recognition: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun processCommand(spokenText: String) {
        val command = CommandParser.parseCommand(spokenText)
        val success = commandExecutor.executeCommand(command)
        
        _voiceState.value = if (success) {
            VoiceState.Success("Command executed successfully")
        } else {
            VoiceState.Error("Failed to execute command")
        }
    }
} 