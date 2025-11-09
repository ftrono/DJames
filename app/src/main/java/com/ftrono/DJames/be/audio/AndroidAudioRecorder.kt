package com.ftrono.DJames.be.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresPermission
import com.arthenica.ffmpegkit.FFmpegKit
import com.ftrono.DJames.application.*
import com.ftrono.DJames.be.models.RecDetails
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize as RtcFrameSize
import com.konovalov.vad.webrtc.config.Mode as RtcMode
import com.konovalov.vad.webrtc.config.SampleRate as RtcSampleRate
import java.io.File
import java.io.FileOutputStream


class AndroidAudioRecorder(private val context: Context) {
    private val TAG = this::class.java.simpleName
    private val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private lateinit var audioRecorder: AudioRecord
    private var recFilePcm: File? = null
    private var recFileFlac: File? = null
    private var speechPct: Int = 0


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(
        messageMode: Boolean = false,
        messageType: String = "",
    ) {
        try {
            //Create dir:
            var flacDir = recDir
            if (prefs.recToDownloads) {
                flacDir = File(downloadsDir, "djames_rec")
                flacDir.mkdirs()
            }

            //Create files:
            val timestamp = utils.getCurrentTimestamp()
            lastRecordingName = "$timestamp.flac"
            recFilePcm = File(recDir, "$timestamp.pcm")
            recFileFlac = File(flacDir, lastRecordingName)

            // Params:
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(recSamplingRate, channelConfig, audioFormat)
            val silenceThreshold = if (messageMode) {
                    silencePatienceMess * 1000   // ms
                } else {
                    silencePatienceQueries * 1000   // ms
                }

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
                MediaRecorder.AudioSource.DEFAULT,   // ALWAYS USE DEFAULT HERE!
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
            val rtcVad = VadWebRTC(
                sampleRate = RtcSampleRate.SAMPLE_RATE_48K,
                frameSize = RtcFrameSize.FRAME_SIZE_480,   // 10ms @ 48kHz
                mode = RtcMode.VERY_AGGRESSIVE,
                silenceDurationMs = 0,   // handle silence detection manually
                speechDurationMs = 0
            )

            // FFT Filters:
            val bpFilter1 = NoiseBPFilter()
            val bpFilter2 = NoiseBPFilter()

            // Rec buffers -> For VAD: 16-bit PCM → 2 bytes per sample:
            val chunkSize = rtcVad.frameSize.value * 2
            val buffer = ByteArray(bufferSize * 2)  // For Recorder: raw PCM storage
            val cleanedFrame = ByteArray(chunkSize)
            val cleanedFrame2 = ByteArray(chunkSize)

            // Monitoring:
            var silenceMs = 0L
            val frameDurationMs = 10L   // each FRAME_SIZE_480 = 10 ms
            var isSpeech = false
            var stopMax = false
            var numFrames = 0
            var numSpeech = 0

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
                        numFrames ++

                        //Mute frequencies & run VAD on each cleaned frame:
                        if (prefs.enableNoiseSuppression) {
                            // 1st pass:
                            bpFilter1.processInto(
                                inBytes = frame,
                                inOffset = 0,
                                outBytes = cleanedFrame,
                                minFreqHz = prefs.recMinFreq,
                                maxFreqHz = prefs.recMaxFreq,
                            )
                            if (prefs.enableSecondNoiseSuppression) {
                                // 2nd pass:
                                bpFilter2.processInto(
                                    inBytes = cleanedFrame,
                                    inOffset = 0,
                                    outBytes = cleanedFrame2,
                                    minFreqHz = prefs.recMinFreq + prefs.secondNoiseDelta,
                                    maxFreqHz = prefs.recMaxFreq - prefs.secondNoiseDelta,
                                )
                                // VAD:
                                isSpeech = rtcVad.isSpeech(cleanedFrame2)
                                output.write(if (prefs.recToDownloads) cleanedFrame2 else cleanedFrame)   //Write rec file
                            } else {
                                // VAD:
                                isSpeech = rtcVad.isSpeech(cleanedFrame)
                                output.write(cleanedFrame)   //Write rec file
                            }
                        } else {
                            isSpeech = rtcVad.isSpeech(frame)
                            output.write(frame)   //Write rec file
                        }

                        //Write:
                        recordingTime += frameDurationMs
                        stopMax = recordingTime >= maxTime

                        if (silenceEnabled && !isSpeech) {
                            // NO SPEECH -> Increase patience countdown:
                            silenceMs += frameDurationMs
                            if (silenceMs >= silenceThreshold) {
                                // STOP:
                                Log.d(TAG, "RECORDER: silence detected for more that ${silenceThreshold / 1000} seconds! -> STOPPING!")
                                isRecording = false
                                break
                            }
                        } else {
                            // IS SPEECH or NO SILENCE DETECTION -> Reset patience countdown:
                            silenceMs = 0L
                            numSpeech ++
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

            // Calculate speechPct:
            speechPct = if (numFrames == 0) 0 else {
                ((numSpeech.toFloat() / numFrames.toFloat())* 100).toInt()
            }
            Log.d(TAG, "SpeechPct: $speechPct -> $numSpeech / $numFrames")

            // Close VAD:
            rtcVad.close()

        } catch (e: Exception) {
            recordingFail = true
            speechPct = 0
            Log.w(TAG, "ERROR: Recorder start FAIL.", e)
        }
    }

    fun stop(): RecDetails {
        try {
            audioRecorder.stop()
            audioRecorder.release()
            recordingMode = false
            //Convert:
            convertAudioFile(source = recFilePcm!!, target = recFileFlac!!)
            return RecDetails(
                recPath = recFileFlac!!.absolutePath,
                speechPct = speechPct,
            )
        } catch (e: Exception) {
            recordingFail = true
            speechPct = 0
            Log.w(TAG, "ERROR: Recorder stop FAIL.")
            return RecDetails()
        }
    }

    fun convertAudioFile(source: File, target: File) {
        try {
            val cmd = "-f s16le -ar ${recSamplingRate} -ac 1 -i ${source.absolutePath} -c:a flac ${target.absolutePath} -y -loglevel quiet"
            val session = FFmpegKit.execute(cmd)
            Log.d(TAG, "Conversion return: ${session.returnCode}")
            //Log.d(TAG, "Conversion output: ${session.output}")
            Log.d(TAG, "Conversion fail track (if any): ${session.failStackTrace}")
        } catch (e: Exception) {
            recordingFail = true
            speechPct = 0
            Log.w(TAG, "Audio file conversion error: ", e)
        }
    }

}