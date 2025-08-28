package com.wang.phoneaiassistant.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.os.Build

@Singleton
class VoiceInputManager @Inject constructor() : IVoiceInputManager {
    
    private val _voiceInputState = MutableStateFlow(VoiceInputState())
    override val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState
    
    private var speechRecognizer: SpeechRecognizer? = null
    
    override fun initializeSpeechRecognizer(context: Context) {
        Log.d("VoiceInputManager", "Initializing speech recognizer")
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("VoiceInputManager", "Speech recognition not available")
            val errorMsg = if (isEmulator()) {
                "语音识别在模拟器上不可用，请使用真实设备测试"
            } else {
                "语音识别不可用，请检查是否安装了Google应用和语音搜索服务"
            }
            _voiceInputState.value = _voiceInputState.value.copy(
                error = errorMsg
            )
            return
        }
        
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                initializeSpeechRecognizer(context)
            }
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("VoiceInputManager", "Ready for speech")
                    _voiceInputState.value = _voiceInputState.value.copy(
                        isListening = true,
                        error = null
                    )
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d("VoiceInputManager", "Beginning of speech")
                    _voiceInputState.value = _voiceInputState.value.copy(
                        isProcessing = true
                    )
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    _voiceInputState.value = _voiceInputState.value.copy(
                        soundLevel = rmsdB
                    )
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                
                override fun onEndOfSpeech() {
                    _voiceInputState.value = _voiceInputState.value.copy(
                        isListening = false
                    )
                }
                
                override fun onError(error: Int) {
                    Log.e("VoiceInputManager", "Speech recognition error: $error")
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    
                    _voiceInputState.value = _voiceInputState.value.copy(
                        isListening = false,
                        isProcessing = false,
                        error = errorMessage
                    )
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    Log.d("VoiceInputManager", "Speech results: $text")
                    
                    _voiceInputState.value = _voiceInputState.value.copy(
                        isListening = false,
                        isProcessing = false,
                        transcribedText = text,
                        error = null
                    )
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = matches?.firstOrNull() ?: ""
                    
                    _voiceInputState.value = _voiceInputState.value.copy(
                        partialTranscript = partialText
                    )
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        Log.d("VoiceInputManager", "Speech recognizer initialized successfully")
    }
    
    override fun startListening(context: Context, language: String) {
        Log.d("VoiceInputManager", "Starting to listen, language: $language")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            if (speechRecognizer == null) {
                Log.e("VoiceInputManager", "Speech recognizer is null, initializing first")
                initializeSpeechRecognizer(context)
            }
            speechRecognizer?.startListening(intent)
            Log.d("VoiceInputManager", "Started listening successfully")
        } catch (e: Exception) {
            Log.e("VoiceInputManager", "Error starting listening", e)
            _voiceInputState.value = _voiceInputState.value.copy(
                isListening = false,
                error = "Failed to start voice recognition: ${e.message}"
            )
        }
    }
    
    override fun stopListening() {
        Log.d("VoiceInputManager", "Stopping listening")
        speechRecognizer?.stopListening()
        _voiceInputState.value = _voiceInputState.value.copy(
            isListening = false,
            isProcessing = false
        )
    }
    
    override fun clearTranscription() {
        _voiceInputState.value = _voiceInputState.value.copy(
            transcribedText = "",
            partialTranscript = "",
            error = null
        )
    }
    
    override fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }
}
