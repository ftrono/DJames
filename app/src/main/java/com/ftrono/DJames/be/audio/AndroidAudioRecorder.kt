package com.ftrono.DJames.be.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.arthenica.ffmpegkit.FFmpegKit
import com.ftrono.DJames.application.*
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize as RtcFrameSize
import com.konovalov.vad.webrtc.config.Mode as RtcMode
import com.konovalov.vad.webrtc.config.SampleRate as RtcSampleRate
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize as SileroFrameSize
import com.konovalov.vad.silero.config.Mode as SileroMode
import com.konovalov.vad.silero.config.SampleRate as SileroSampleRate
import java.io.File
import java.io.FileOutputStream


class AndroidAudioRecorder(private val context: Context) {
    private val TAG = AndroidAudioRecorder::class.java.simpleName
    // private val saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private lateinit var audioRecorder: AudioRecord
    private var recFilePcm: File? = null
    private var recFileFlac: File? = null


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(
        messageMode: Boolean = false,
        messageType: String = "",
    ) {
        try {
            //Create files:
            val timestamp = utils.getCurrentTimestamp()
            lastRecordingName = "$timestamp.flac"
            recFilePcm = File(recDir, "$timestamp.pcm")
            recFileFlac = File(recDir, lastRecordingName)

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

            //Create recorder:
            audioRecorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                recSamplingRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            // START:
            recordingMode = true
            var isRecording = true
            audioRecorder.startRecording()

            // Monitor:
            lateinit var rtcVad: VadWebRTC
            lateinit var sileroVad: VadSilero

            if (prefs.silenceDetector == "Silero") {
                sileroVad = VadSilero(
                    context = context,
                    sampleRate = SileroSampleRate.SAMPLE_RATE_16K,
                    frameSize = SileroFrameSize.FRAME_SIZE_512,
                    mode = SileroMode.AGGRESSIVE,
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
            val chunkSize = if (prefs.silenceDetector == "Silero") {
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
                        output.write(frame)
                        recordingTime += frameDurationMs
                        stopMax = recordingTime >= maxTime

                        // Run VAD on each frame:
                        isSpeech = if (prefs.silenceDetector == "Silero") {
                            sileroVad.isSpeech(frame)
                        } else {
                            rtcVad.isSpeech(frame)
                        }

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
            if (prefs.silenceDetector == "Silero") {
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
}