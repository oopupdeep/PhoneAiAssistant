package com.wang.phoneaiassistant.data.voice

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface IVoiceInputManager {
    val voiceInputState: StateFlow<VoiceInputState>
    
    fun initializeSpeechRecognizer(context: Context)
    fun startListening(context: Context, language: String = "zh-CN")
    fun stopListening()
    fun clearTranscription()
    fun destroy()
}