package com.wang.phoneaiassistant.data.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.wang.phoneaiassistant.whispertflite.asr.Whisper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperVoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IVoiceInputManager {

    private val TAG = "WhisperVoiceInputManager"

    private val _voiceInputState = MutableStateFlow(VoiceInputState())
    override val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState

    // asset names in app/src/main/assets/
    private val modelAssetName = "whisper-tiny.tflite"
    private val vocabAssetName = "filters_vocab_multilingual.bin"

    private val mWhisper: Whisper = Whisper(context)

    // recording fields
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // temporary files
    private var rawPcmFile: File? = null
    private var wavFile: File? = null

    override fun initializeSpeechRecognizer(context: Context) {
        try {
            val modelPath = copyAssetToFile(modelAssetName)
            val vocabPath = copyAssetToFile(vocabAssetName)

            Log.i(TAG, "Loading model from: $modelPath")
            mWhisper.loadModel(modelPath, vocabPath, true)

            mWhisper.setListener(object : Whisper.WhisperListener {
                override fun onUpdateReceived(message: String) {
                    Log.d(TAG, "Whisper update: $message")
                    // use partialTranscript for incremental updates
                    _voiceInputState.value = _voiceInputState.value.copy(partialTranscript = message)
                }

                override fun onResultReceived(result: String) {
                    Log.d(TAG, "Whisper result: $result")
                    _voiceInputState.value = _voiceInputState.value.copy(
                        transcribedText = result,
                        partialTranscript = "",
                        isProcessing = false
                    )
                }
            })

            _voiceInputState.value = _voiceInputState.value.copy(isProcessing = false)
        } catch (e: Exception) {
            Log.e(TAG, "initializeSpeechRecognizer failed", e)
            _voiceInputState.value = _voiceInputState.value.copy(error = e.message ?: "init_error")
        }
    }

    override fun startListening(context: Context, language: String) {
        // check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val err = "RECORD_AUDIO permission not granted"
            Log.w(TAG, err)
            _voiceInputState.value = _voiceInputState.value.copy(error = err)
            return
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize
        )

        rawPcmFile = File(context.cacheDir, "temp_recording.pcm")
        wavFile = File(context.cacheDir, "temp_recording.wav")

        // ensure old files removed
        rawPcmFile?.delete()
        wavFile?.delete()

        isRecording = true

        audioRecord?.startRecording()
        _voiceInputState.value = _voiceInputState.value.copy(isListening = true, isProcessing = false)

        recordingJob = scope.launch {
            try {
                FileOutputStream(rawPcmFile!!).use { fos ->
                    val buffer = ByteArray(minBufferSize)
                    while (isActive && isRecording) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            fos.write(buffer, 0, read)

                            // very basic rough sound level approximation (RMS)
                            var sum = 0L
                            var i = 0
                            while (i < read) {
                                val low = buffer[i].toInt() and 0xff
                                val high = if (i + 1 < read) buffer[i + 1].toInt() shl 8 else 0
                                val sample = high or low
                                sum += (sample * sample).toLong()
                                i += 2
                            }
                            val rms = if (read > 0) kotlin.math.sqrt(sum.toDouble() / (read / 2)).toFloat() else 0f
                            _voiceInputState.value = _voiceInputState.value.copy(soundLevel = rms)
                        }
                    }
                    fos.flush()
                }

                // after recording stopped, convert pcm->wav
                _voiceInputState.value = _voiceInputState.value.copy(isProcessing = true, isListening = false)
                pcmToWav(rawPcmFile!!, wavFile!!, sampleRate, 1, 16)

                // hand wav to Whisper
                mWhisper.setFilePath(wavFile!!.absolutePath)
                mWhisper.setAction(Whisper.ACTION_TRANSCRIBE)
                mWhisper.start()

                _voiceInputState.value = _voiceInputState.value.copy(isProcessing = true)
            } catch (e: CancellationException) {
                Log.i(TAG, "recording cancelled")
            } catch (t: Throwable) {
                Log.e(TAG, "recording error", t)
                _voiceInputState.value = _voiceInputState.value.copy(error = t.message ?: "record_error", isProcessing = false, isListening = false)
            }
        }
    }

    override fun stopListening() {
        try {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            // recordingJob will finish copying and start whisper transcription
            _voiceInputState.value = _voiceInputState.value.copy(isListening = false)
        } catch (t: Throwable) {
            Log.e(TAG, "stopListening error", t)
            _voiceInputState.value = _voiceInputState.value.copy(error = t.message ?: "stop_error")
        }
    }

    override fun clearTranscription() {
        _voiceInputState.value = VoiceInputState()
    }

    override fun destroy() {
        try {
            isRecording = false
            recordingJob?.cancel()
            scope.cancel()
            try {
                mWhisper.stop()
            } catch (_: Throwable) {
                // ignore if method not present
            }
        } catch (t: Throwable) {
            Log.e(TAG, "destroy error", t)
        }
    }

    // ---------------- helpers -----------------

    private fun copyAssetToFile(assetName: String): String {
        val outDir = File(context.filesDir, "models")
        if (!outDir.exists()) outDir.mkdirs()
        val outFile = File(outDir, assetName)
        if (outFile.exists() && outFile.length() > 0) return outFile.absolutePath

        context.assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        return outFile.absolutePath
    }

    private fun pcmToWav(pcmFile: File, wavFile: File, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalAudioLen = pcmFile.length()
        val totalDataLen = totalAudioLen + 36

        FileInputStream(pcmFile).use { fis ->
            FileOutputStream(wavFile).use { fos ->
                writeWavHeader(fos, totalAudioLen, totalDataLen, sampleRate.toLong(), channels, byteRate.toLong(), bitsPerSample)
                val buffer = ByteArray(4096)
                var read: Int
                while (fis.read(buffer).also { read = it } > 0) {
                    fos.write(buffer, 0, read)
                }
                fos.flush()
            }
        }
    }

    private fun writeWavHeader(out: OutputStream, totalAudioLen: Long, totalDataLen: Long,
                               longSampleRate: Long, channels: Int, byteRate: Long, bitsPerSample: Int) {
        val header = ByteArray(44)

        // RIFF/WAVE header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        // totalDataLen + 8
        writeInt(header, 4, (totalDataLen + 8).toInt())
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // 'fmt ' chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeInt(header, 16, 16) // Sub-chunk size
        writeShort(header, 20, 1.toShort()) // AudioFormat = 1 (PCM)
        writeShort(header, 22, channels.toShort())
        writeInt(header, 24, longSampleRate.toInt())
        writeInt(header, 28, byteRate.toInt())
        writeShort(header, 32, (channels * bitsPerSample / 8).toShort())
        writeShort(header, 34, bitsPerSample.toShort())

        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeInt(header, 40, totalAudioLen.toInt())

        out.write(header, 0, 44)
    }

    private fun writeInt(header: ByteArray, offset: Int, value: Int) {
        header[offset] = (value and 0xff).toByte()
        header[offset + 1] = ((value shr 8) and 0xff).toByte()
        header[offset + 2] = ((value shr 16) and 0xff).toByte()
        header[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun writeShort(header: ByteArray, offset: Int, value: Short) {
        header[offset] = (value.toInt() and 0xff).toByte()
        header[offset + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }
}

// ---------------- VoiceInputState data class ----------------

data class VoiceInputState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val transcribedText: String = "",
    val partialTranscript: String = "",
    val soundLevel: Float = 0f,
    val error: String? = null
)
