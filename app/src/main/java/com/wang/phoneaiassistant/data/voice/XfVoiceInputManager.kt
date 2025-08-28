package com.wang.phoneaiassistant.data.voice

import android.content.Context
import android.os.Bundle
import android.util.Log
// TODO: 下载讯飞SDK后取消注释
// import com.iflytek.cloud.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.wang.phoneaiassistant.data.preferences.AppPreference

@Singleton
class XfVoiceInputManager @Inject constructor(
    private val appPreferences: AppPreference
) : IVoiceInputManager {
    
    private val _voiceInputState = MutableStateFlow(VoiceInputState())
    override val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState
    
    companion object {
        private const val TAG = "XfVoiceInputManager"
    }
    
    override fun initializeSpeechRecognizer(context: Context) {
        Log.d(TAG, "XunFei SDK not available - please download and install the SDK")
        _voiceInputState.value = _voiceInputState.value.copy(
            error = "讯飞SDK未安装，请下载SDK并按照app/libs/README.md说明配置"
        )
    }
    
    override fun startListening(context: Context, language: String) {
        Log.d(TAG, "XunFei SDK not available")
        _voiceInputState.value = _voiceInputState.value.copy(
            error = "讯飞SDK未安装，请先配置SDK"
        )
    }
    
    override fun stopListening() {
        Log.d(TAG, "Stopping listening")
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
        // 清理资源
    }
}

/* 
// TODO: 下载讯飞SDK后，删除上面的临时实现，使用下面的完整实现

package com.wang.phoneaiassistant.data.voice

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.iflytek.cloud.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.wang.phoneaiassistant.data.preferences.AppPreference

@Singleton
class XfVoiceInputManager @Inject constructor(
    private val appPreferences: AppPreference
) : IVoiceInputManager {
    
    private val _voiceInputState = MutableStateFlow(VoiceInputState())
    override val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState
    
    private var mAsr: SpeechRecognizer? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "XfVoiceInputManager"
        // 讯飞语音识别配置
        private const val LANGUAGE = "zh_cn"
        private const val ACCENT = "mandarin"
        private const val DOMAIN = "iat"
        private const val RESULT_TYPE = "json"
        private const val AUDIO_FORMAT = "wav"
        private const val ASR_PTT = "0" // 标点符号：0=无标点，1=有标点
        private const val VAD_BOS = "4000" // 前端点检测
        private const val VAD_EOS = "1000" // 后端点检测
    }
    
    override fun initializeSpeechRecognizer(context: Context) {
        // 从preferences中获取appId
        val appId = appPreferences.xfAppId
        if (appId.isEmpty()) {
            Log.e(TAG, "XunFei AppId is empty")
            _voiceInputState.value = _voiceInputState.value.copy(
                error = "请先在设置中配置讯飞AppId"
            )
            return
        }
        Log.d(TAG, "Initializing XunFei SDK with appId: $appId")
        
        try {
            // 初始化讯飞SDK
            SpeechUtility.createUtility(context, SpeechConstant.APPID + "=" + appId)
            
            // 创建语音识别对象
            mAsr = SpeechRecognizer.createRecognizer(context) { errorCode ->
                if (errorCode != ErrorCode.SUCCESS) {
                    Log.e(TAG, "SpeechRecognizer init failed, error code: $errorCode")
                    _voiceInputState.value = _voiceInputState.value.copy(
                        error = "语音识别初始化失败，错误码：$errorCode"
                    )
                } else {
                    Log.d(TAG, "SpeechRecognizer init success")
                    isInitialized = true
                }
            }
            
            // 设置语音识别监听器
            mAsr?.setRecognizerListener(recognizerListener)
            
            // 设置参数
            setParams()
            
        } catch (e: Exception) {
            Log.e(TAG, "Initialize error", e)
            _voiceInputState.value = _voiceInputState.value.copy(
                error = "初始化失败：${e.message}"
            )
        }
    }
    
    private fun setParams() {
        mAsr?.apply {
            // 清空参数
            setParameter(SpeechConstant.PARAMS, null)
            // 设置听写引擎
            setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
            // 设置返回结果格式
            setParameter(SpeechConstant.RESULT_TYPE, RESULT_TYPE)
            // 设置语言
            setParameter(SpeechConstant.LANGUAGE, LANGUAGE)
            // 设置语言区域
            setParameter(SpeechConstant.ACCENT, ACCENT)
            // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
            setParameter(SpeechConstant.VAD_BOS, VAD_BOS)
            // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
            setParameter(SpeechConstant.VAD_EOS, VAD_EOS)
            // 设置标点符号，0表示返回结果无标点，1表示返回结果有标点
            setParameter(SpeechConstant.ASR_PTT, ASR_PTT)
            // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
            setParameter(SpeechConstant.AUDIO_FORMAT, AUDIO_FORMAT)
        }
    }
    
    override fun startListening(context: Context, language: String) {
        Log.d(TAG, "Starting to listen")
        
        if (!isInitialized) {
            Log.e(TAG, "XunFei SDK not initialized")
            _voiceInputState.value = _voiceInputState.value.copy(
                error = "请先初始化讯飞SDK"
            )
            return
        }
        
        _voiceInputState.value = _voiceInputState.value.copy(
            isListening = true,
            error = null,
            transcribedText = "",
            partialTranscript = ""
        )
        
        val ret = mAsr?.startListening(recognizerListener)
        if (ret != ErrorCode.SUCCESS) {
            Log.e(TAG, "Start listening failed, error code: $ret")
            _voiceInputState.value = _voiceInputState.value.copy(
                isListening = false,
                error = "启动识别失败，错误码：$ret"
            )
        }
    }
    
    override fun stopListening() {
        Log.d(TAG, "Stopping listening")
        mAsr?.stopListening()
        _voiceInputState.value = _voiceInputState.value.copy(
            isListening = false,
            isProcessing = false
        )
    }
    
    fun cancelListening() {
        Log.d(TAG, "Canceling listening")
        mAsr?.cancel()
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
        mAsr?.cancel()
        mAsr?.destroy()
        mAsr = null
        isInitialized = false
    }
    
    private val recognizerListener = object : RecognizerListener {
        override fun onBeginOfSpeech() {
            Log.d(TAG, "onBeginOfSpeech")
            _voiceInputState.value = _voiceInputState.value.copy(
                isProcessing = true
            )
        }
        
        override fun onError(error: SpeechError?) {
            Log.e(TAG, "onError: ${error?.errorCode}, ${error?.errorDescription}")
            _voiceInputState.value = _voiceInputState.value.copy(
                isListening = false,
                isProcessing = false,
                error = error?.errorDescription ?: "未知错误"
            )
        }
        
        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            _voiceInputState.value = _voiceInputState.value.copy(
                isListening = false
            )
        }
        
        override fun onResult(results: RecognizerResult?, isLast: Boolean) {
            Log.d(TAG, "onResult: isLast=$isLast")
            results?.let {
                val text = parseResult(it.resultString)
                if (isLast) {
                    _voiceInputState.value = _voiceInputState.value.copy(
                        isProcessing = false,
                        transcribedText = _voiceInputState.value.partialTranscript + text,
                        partialTranscript = ""
                    )
                } else {
                    _voiceInputState.value = _voiceInputState.value.copy(
                        partialTranscript = _voiceInputState.value.partialTranscript + text
                    )
                }
            }
        }
        
        override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: Bundle?) {
            Log.d(TAG, "onEvent: eventType=$eventType")
        }
        
        override fun onVolumeChanged(volume: Int, data: ByteArray?) {
            _voiceInputState.value = _voiceInputState.value.copy(
                soundLevel = volume.toFloat()
            )
        }
    }
    
    private fun parseResult(json: String): String {
        return try {
            val jsonObject = org.json.JSONObject(json)
            val wordsArray = jsonObject.getJSONArray("ws")
            val stringBuilder = StringBuilder()
            
            for (i in 0 until wordsArray.length()) {
                val cwArray = wordsArray.getJSONObject(i).getJSONArray("cw")
                for (j in 0 until cwArray.length()) {
                    val word = cwArray.getJSONObject(j).getString("w")
                    stringBuilder.append(word)
                }
            }
            
            stringBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Parse result error", e)
            ""
        }
    }
}
*/