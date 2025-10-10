package com.ftrono.DJames.be.audio

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresPermission
import com.arthenica.ffmpegkit.FFmpegKit
import com.dl.rtnr.rtNoiseReducer
import com.ftrono.DJames.application.*
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize as RtcFrameSize
import com.konovalov.vad.webrtc.config.Mode as RtcMode
import com.konovalov.vad.webrtc.config.SampleRate as RtcSampleRate
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize as SileroFrameSize
import com.konovalov.vad.silero.config.Mode as SileroMode
import com.konovalov.vad.silero.config.SampleRate as SileroSampleRate
import com.konovalov.vad.yamnet.VadYamnet
import com.konovalov.vad.yamnet.config.FrameSize as YamnetFrameSize
import com.konovalov.vad.yamnet.config.Mode as YamnetMode
import com.konovalov.vad.yamnet.config.SampleRate as YamnetSampleRate
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


class AndroidAudioRecorder(private val context: Context) {
    private val TAG = AndroidAudioRecorder::class.java.simpleName
    private val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private lateinit var audioRecorder: AudioRecord
    private var recFilePcm: File? = null
    private var recFileFlac: File? = null

    // RTNR:
    private var rtNoiseReducer: rtNoiseReducer? = null

    fun initRTNR() {
        try {
            rtNoiseReducer = rtNoiseReducer(context as Activity)
            Log.d(TAG, "RTNR initialized")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to create noise reduction", e)
        }
    }


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(
        messageMode: Boolean = false,
        messageType: String = "",
    ) {
        try {
            //Create dir:
            val saveDir = File(downloadsDir, "djames_rec")
            saveDir.mkdirs()

            //Create files:
            val timestamp = utils.getCurrentTimestamp()
            lastRecordingName = "$timestamp.flac"
            recFilePcm = File(recDir, "$timestamp.pcm")
            recFileFlac = File(saveDir, lastRecordingName)

            // Params:
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(recSamplingRate, channelConfig, audioFormat)
            val silenceThreshold = silencePatienceSecs * 1000   // ms

            // Max time:
            var maxTime = if (messageMode && messageType == "voice") {
                    maxAudioRecTimeout
                } else if (messageMode) {
                    prefs.messageTimeout.toLong()
                } else {
                    prefs.recTimeout.toLong()
                }
            maxTime = maxTime*1000

            // Silence detection enabled:
            var silenceEnabled = if (messageMode) {
                prefs.silenceEnabledMess
            } else {
                prefs.silenceEnabledQueries
            }

            // Mic source:
            var micSource = if (prefs.useSourceMic) {
                MediaRecorder.AudioSource.DEFAULT
            } else {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            }

            //Create recorder:
            audioRecorder = AudioRecord(
                micSource,
                recSamplingRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            // Enable NS + AGC if present
            if (!prefs.useSourceMic) {
                if (NoiseSuppressor.isAvailable()) {
                    NoiseSuppressor.create(audioRecorder.audioSessionId)?.enabled = true
                }
                if (AutomaticGainControl.isAvailable()) {
                    AutomaticGainControl.create(audioRecorder.audioSessionId)?.enabled = true
                }
            }

            // START:
            recordingMode = true
            var isRecording = true
            audioRecorder.startRecording()

            // Monitor:
            lateinit var rtcVad: VadWebRTC
            lateinit var sileroVad: VadSilero
            lateinit var yamnetVad: VadYamnet

            if (prefs.silenceDetector == "Yamnet") {
                yamnetVad = VadYamnet(
                    context,
                    sampleRate = YamnetSampleRate.SAMPLE_RATE_16K,
                    frameSize = YamnetFrameSize.FRAME_SIZE_487,
                    mode = YamnetMode.NORMAL,
                    silenceDurationMs = 0,
                    speechDurationMs = 0
                )
            } else if (prefs.silenceDetector == "Silero") {
                sileroVad = VadSilero(
                    context = context,
                    sampleRate = SileroSampleRate.SAMPLE_RATE_16K,
                    frameSize = SileroFrameSize.FRAME_SIZE_512,
                    mode = SileroMode.NORMAL,
                    silenceDurationMs = 0,   // handle silence detection manually
                    speechDurationMs = 0
                )
            } else {
                rtcVad = VadWebRTC(
                    sampleRate = RtcSampleRate.SAMPLE_RATE_48K,
                    frameSize = RtcFrameSize.FRAME_SIZE_480,   // 10ms @ 48kHz
                    mode = RtcMode.VERY_AGGRESSIVE,
                    silenceDurationMs = 0,   // handle silence detection manually
                    speechDurationMs = 0
                )
            }

            // Rec buffers -> For VAD: 16-bit PCM → 2 bytes per sample:
            val chunkSize = if (prefs.silenceDetector == "Yamnet") {
                yamnetVad.frameSize.value * 2
            } else if (prefs.silenceDetector == "Silero") {
                sileroVad.frameSize.value * 2
            } else {
                rtcVad.frameSize.value * 2
            }
            val buffer = ByteArray(bufferSize * 2)  // For Recorder: raw PCM storage

            // Monitoring:
            var silenceMs = 0L
            val frameDurationMs = 10L   // each FRAME_SIZE_480 = 10 ms (keep also for Silero!)
            var isSpeech = false
            var stopMax = false

            FileOutputStream(recFilePcm).use { output ->
                while (isRecording && recordingMode && (recordingTime < maxTime)) {
                    // Reads audio samples from the microphone into raw buffer:
                    val read = audioRecorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue   // Skip if nothing read!

                    var i = 0
                    while (i + chunkSize <= read) {
                        // WebRTC VAD requires fixed-size frames (10/20/30 ms):
                        // Extract a frame: Process the buffer in steps of chunkSize (960 bytes)
                        val frame = buffer.copyOfRange(i, i + chunkSize)

                        // Apply RTNR noise reduction (256 samples expected):
                        if (prefs.enableNoiseSuppression) {
                            rtNoiseReducer?.let { reducer ->
                                val shortData = byteArrayToShortArray(frame)
                                if (shortData.size >= 256) {
                                    // Take first 256 samples (or handle in chunks if needed)
                                    val shortBlock = shortData.copyOfRange(0, 256)

                                    val doubleData = shortArrayToDoubleArray(shortBlock)
                                    val seOut = reducer.audioSE(doubleData)   // noise reduced output
                                    val processedShorts = doubleArrayToShortArray(seOut)
                                    val processedBytes = shortArrayToByteArray(processedShorts)

                                    // Replace first part of frame with processed bytes
                                    System.arraycopy(processedBytes, 0, frame, 0, processedBytes.size)
                                }
                            }
                        }

                        //Write:
                        output.write(frame)
                        recordingTime += frameDurationMs
                        stopMax = recordingTime >= maxTime

                        // Run VAD on each frame:
                        isSpeech = if (prefs.silenceDetector == "Yamnet") {
                            yamnetVad.classifyAudio("Speech", frame).label == "Speech"
                        } else if (prefs.silenceDetector == "Silero") {
                            sileroVad.isSpeech(frame)
                        } else {
                            rtcVad.isSpeech(frame)
                        }
                        // Log.d(TAG, "isSpeech: $isSpeech")

                        if (silenceEnabled && !isSpeech) {
                            // NO SPEECH -> Increase patience countdown:
                            silenceMs += frameDurationMs
                            if (silenceMs >= silenceThreshold) {
                                // STOP:
                                Log.d(TAG, "RECORDER: silence detected for more that ${silencePatienceSecs} seconds! -> STOPPING!")
                                isRecording = false
                                break
                            }
                        } else {
                            // IS SPEECH or NO SILENCE DETECTION -> Reset patience countdown:
                            silenceMs = 0L
                        }

                        i += chunkSize
                    }
                    if (stopMax) {
                        // STOP:
                        Log.d(TAG, "RECORDER: maxTime reached! -> STOPPING!")
                        isRecording = false
                        break
                    }
                }
                // STOP:
                if (recordingMode) {
                    //STOP recording:
                    Intent().also { intent ->
                        intent.setAction(ACTION_REC_STOP)
                        context.sendBroadcast(intent)
                    }
                }
            }

            // Close VAD:
            if (prefs.silenceDetector == "Yamnet") {
                yamnetVad.close()
            } else if (prefs.silenceDetector == "Silero") {
                sileroVad.close()
            } else {
                rtcVad.close()
            }


        } catch (e: Exception) {
            recordingFail = true
            Log.w(TAG, "ERROR: Recorder start FAIL.", e)
        }
    }

    fun stop(): File {
        try {
            audioRecorder.stop()
            audioRecorder.release()
            recordingMode = false
            //Convert:
            convertAudioFile(source = recFilePcm!!, target = recFileFlac!!)
        } catch (e: Exception) {
            recordingFail = true
            Log.w(TAG, "ERROR: Recorder stop FAIL.", e)
        }
        return recFileFlac!!
    }

    fun convertAudioFile(source: File, target: File) {
        try {
            val cmd = "-f s16le -ar ${recSamplingRate} -ac 1 -i ${source.absolutePath} -c:a flac ${target.absolutePath} -y -loglevel quiet"
            val session = FFmpegKit.execute(cmd)
            Log.d(TAG, "Conversion return: ${session.returnCode}")
            //Log.d(TAG, "Conversion output: ${session.output}")
            Log.d(TAG, "Conversion fail track (if any): ${session.failStackTrace}")
        } catch (e: Exception) {
            Log.w(TAG, "Audio file conversion error: ", e)
        }
    }


    // RTNR converters:
    private fun byteArrayToShortArray(bytes: ByteArray): ShortArray {
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        return shorts
    }

    private fun shortArrayToDoubleArray(shorts: ShortArray): DoubleArray {
        return DoubleArray(shorts.size) { i -> shorts[i].toDouble() }
    }

    private fun doubleArrayToShortArray(doubles: DoubleArray): ShortArray {
        return ShortArray(doubles.size) { i -> doubles[i].toInt().toShort() }
    }

    private fun shortArrayToByteArray(shorts: ShortArray): ByteArray {
        val buffer = ByteBuffer.allocate(shorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asShortBuffer().put(shorts)
        return buffer.array()
    }
}