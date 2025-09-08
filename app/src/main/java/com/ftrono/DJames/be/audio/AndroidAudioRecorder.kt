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
import java.io.File
import java.io.FileOutputStream
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.SampleRate
import com.konovalov.vad.webrtc.config.Mode


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
            val directory = recDir
            val timestamp = utils.getCurrentTimestamp()
            lastRecordingName = "$timestamp.flac"
            recFilePcm = File(directory, "$timestamp.pcm")
            recFileFlac = File(directory, lastRecordingName)

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
                }*1000

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
            VadWebRTC(
                sampleRate = SampleRate.SAMPLE_RATE_48K,
                frameSize = FrameSize.FRAME_SIZE_480,   // 10ms @ 48kHz
                mode = Mode.VERY_AGGRESSIVE,
                silenceDurationMs = 0,   // handle silence detection manually
                speechDurationMs = 0
            ).use { vad ->

                // Rec buffers:
                val chunkSize = vad.frameSize.value * 2 // For VAD: 16-bit PCM → 2 bytes per sample
                val buffer = ByteArray(bufferSize * 2)  // For Recorder: raw PCM storage

                // Monitoring:
                var silenceMs = 0L
                val frameDurationMs = 10L   // each FRAME_SIZE_480 = 10 ms

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

                            // Run VAD on each frame:
                            if (silenceEnabled && !vad.isSpeech(frame)) {
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