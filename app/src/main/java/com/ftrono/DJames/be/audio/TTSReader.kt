package com.ftrono.DJames.be.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.ftrono.DJames.application.utils
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlin.coroutines.resume

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.ftrono.DJames.application.jsonNoPrint
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.ttsModelId
import com.ftrono.DJames.application.ttsOutputFormat
import com.ftrono.DJames.application.ttsSampleRate
import com.ftrono.DJames.application.ttsSimilarityBoost
import com.ftrono.DJames.application.ttsSpeed
import com.ftrono.DJames.application.ttsStability
import com.ftrono.DJames.application.ttsTimeoutMs
import com.ftrono.DJames.application.ttsVoiceIdMap
import com.ftrono.DJames.be.utils.HttpClient
import com.ftrono.DJames.be.models.TTSRequest
import com.ftrono.DJames.be.models.TTSVoiceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody


class TTSReader(
    private val context: Context,
    private val apiKey: String,
) {
    private val TAG = this::class.java.simpleName

    @Volatile
    private var currentAudioTrack: AudioTrack? = null

    fun speak(
        message: String,
        isIntro: Boolean = false,
    ) {
        val text = utils.cleanString(message, emojiOnly = true)
        runBlocking {
            try {
                ttsReadApi(message, isIntro)
            } catch (e: Exception) {
                ttsReadNative(
                    context = context,
                    message = text,
                    langCode = prefs.queryLanguage,
                )
            }
        }
    }

    // HELPERS: TTS directly read:
    private suspend fun ttsReadApi(message: String, isIntro: Boolean = false) {
        try {
            val audioBytes = withContext(Dispatchers.IO) {
                ttsRequestApi(
                    text = message,
                    isIntro = isIntro,
                )
            }

            if (audioBytes == null || audioBytes.isEmpty()) {
                Log.w(TAG, "ttsRead(): empty TTS audio response.")
                return ttsReadNative(
                    context = context,
                    message = message,
                    langCode = prefs.queryLanguage,
                )
            }

            playVoice(audioBytes = audioBytes)
            release()
        } catch (t: Throwable) {
            Log.e(TAG, "ttsRead(): failed.", t)
            ttsReadNative(
                context = context,
                message = message,
                langCode = prefs.queryLanguage,
            )
        }
    }

    // TODO: Play voice file:
    private suspend fun ttsReadFile(message: String) {
        try {
            val rawResId: Int = 0   // R.raw.voice_prompt
            playVoice(rawResId = rawResId)
            release()
        } catch (t: Throwable) {
            Log.e(TAG, "ttsRead(): failed.", t)
        }
    }

    // Android native:
    private suspend fun ttsReadNative(
        context: Context,
        message: String,
        langCode: String
    ): Unit = suspendCancellableCoroutine { continuation ->

        //Set Locale:
        var locale = Locale.UK
        if (langCode == "it") {
            locale = Locale.ITALY
        }
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->

            if (status == TextToSpeech.SUCCESS) {
                val result = tts!!.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e(TAG, "Language ${langCode} not supported!")

                } else {
                    tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) {
                        }

                        override fun onDone(utteranceId: String) {
                            continuation.resume(Unit)
                        }

                        override fun onError(utteranceId: String) {
                            Log.e(TAG, "Error on $utteranceId")
                            //continuation.resume(Unit)
                        }
                    })
                    tts!!.speak(
                        message,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        utils.generateRandomString(8)
                    )
                }
            } else {
                Log.e(TAG, "Initialization Failed!")
                continuation.resume(Unit)
            }
        }
    }


    private fun TTSClassToRequestBody(ttsRequest: TTSRequest): RequestBody {
        val jsonBody = jsonNoPrint.encodeToString(ttsRequest)
        return jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
    }

    // API request:
    private fun ttsRequestApi(text: String, isIntro: Boolean = false): ByteArray? {
        val requestBody = TTSClassToRequestBody(
            TTSRequest(
                text = text,
                model_id = ttsModelId,
                voice_settings = TTSVoiceSettings(
                    speed = if (isIntro) ttsSpeed - 0.1 else ttsSpeed,
                    stability = ttsStability,
                    similarity_boost = ttsSimilarityBoost,
                )
            )
        )

        val httpClient = HttpClient()
        val client = httpClient.getClient(ttsTimeoutMs)
        val ttsVoiceId = ttsVoiceIdMap[prefs.voiceAccent]

        val url = "https://api.elevenlabs.io/v1/text-to-speech/$ttsVoiceId?output_format=$ttsOutputFormat"

        val request = Request.Builder()
            .url(url)
            .header("xi-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        var audioBytes: ByteArray? = null

        runBlocking {
            val response = client.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    val errorBody = res.body?.string().orEmpty()
                    Log.w(
                        TAG,
                        "ttsRequestApi(): failed. code=${res.code}, body=$errorBody"
                    )
                    return@runBlocking
                }

                // Reading raw bytes, because PCM audio is binary:
                audioBytes = res.body?.bytes()
                Log.d(
                    TAG,
                    "ttsRequestApi(): success. bytes=${audioBytes?.size ?: 0}"
                )
            }
        }

        return audioBytes
    }

    // Play raw PCM 16-bit mono audio (custom sample rate) and waits until playback completes:
    // (NOTE: Using AudioTrack instead of MediaPlayer not to hook into transport controls)
    private suspend fun playVoice(
        audioBytes: ByteArray? = null,
        rawResId: Int? = null,
    ): Unit =
        suspendCancellableCoroutine { continuation ->
            try {
                // Stop any previous short TTS still playing.
                currentAudioTrack?.runCatching {
                    pause()
                    flush()
                    stop()
                    release()
                }
                currentAudioTrack = null

                // Manage API vs saved file:
                val pcmBytes = when {
                    rawResId != null -> {
                        context.resources.openRawResource(rawResId).use { it.readBytes() }
                    }
                    audioBytes != null -> {
                        audioBytes
                    }
                    else -> {
                        continuation.resume(Unit)
                        return@suspendCancellableCoroutine
                    }
                }

                // Settings:
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioEncoding = AudioFormat.ENCODING_PCM_16BIT

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(ttsSampleRate)
                    .setEncoding(audioEncoding)
                    .setChannelMask(channelConfig)
                    .build()

                val minBuffer = AudioTrack.getMinBufferSize(
                    ttsSampleRate,
                    channelConfig,
                    audioEncoding
                )

                val bufferSize = maxOf(minBuffer, pcmBytes.size)

                val track = AudioTrack(
                    audioAttributes,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )

                currentAudioTrack = track

                // Play AudioTrack:
                continuation.invokeOnCancellation {
                    currentAudioTrack?.runCatching {
                        pause()
                        flush()
                        stop()
                        release()
                    }
                    currentAudioTrack = null
                }

                val written = track.write(pcmBytes, 0, pcmBytes.size)
                if (written <= 0) {
                    Log.e(TAG, "playVoice(): AudioTrack.write failed: $written")
                    track.release()
                    currentAudioTrack = null
                    continuation.resume(Unit)
                    return@suspendCancellableCoroutine
                }

                val bytesPerFrame = 2   // mono 16-bit PCM = 2 bytes/frame
                val totalFrames = pcmBytes.size / bytesPerFrame

                track.notificationMarkerPosition = totalFrames
                track.setPlaybackPositionUpdateListener(
                    object : AudioTrack.OnPlaybackPositionUpdateListener {
                        override fun onMarkerReached(audioTrack: AudioTrack) {
                            audioTrack.runCatching {
                                stop()
                                flush()
                                release()
                            }
                            if (currentAudioTrack === audioTrack) {
                                currentAudioTrack = null
                            }
                            continuation.resume(Unit)
                        }

                        override fun onPeriodicNotification(audioTrack: AudioTrack) {
                            // no-op
                        }
                    }
                )

                track.play()

            } catch (t: Throwable) {
                Log.e(TAG, "playVoice(): failed.", t)
                currentAudioTrack?.runCatching { release() }
                currentAudioTrack = null
                continuation.resume(Unit)
            }
        }

    fun release() {
        currentAudioTrack?.runCatching {
            pause()
            flush()
            stop()
            release()
        }
        currentAudioTrack = null
    }
}