package com.ftrono.DJames.be.audio

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.ftrono.DJames.application.*
import java.io.File
import java.io.FileOutputStream


class AndroidAudioRecorder(private val context: Context) {
    private val TAG = AndroidAudioRecorder::class.java.simpleName

    private var mediaRecorder: MediaRecorder? = null
    private val bitRate = 96000
    private val MAX_AMPLITUDE = 32762

    private var recFileMp3: File? = null
    private var recFileFlac: File? = null


    private fun createRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    fun start(
        messageMode: Boolean = false,
        messageType: String = "",
    ) {
        try {
            //Create files:
            val directory = context.cacheDir
            recFileMp3 = File(directory, "$recFileName.mp3")
            recFileFlac = File(directory, "$recFileName.flac")
            //Init & start Recorder:
            createRecorder().apply {
                // setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(bitRate)
                setAudioSamplingRate(recSamplingRate)
                setAudioChannels(1)   //mono
                setOutputFile(FileOutputStream(recFileMp3).fd)

                prepare()
                start()

                mediaRecorder = this
            }

            // Monitor audio:
            whileRecording(
                messageMode = messageMode,
                messageType = messageType,
            )

        } catch (e: Exception) {
            recordingFail = true
            Log.w(TAG, "ERROR: Recorder start FAIL.", e)
        }
    }

    fun getMaxAmplitude(): Int {
        //Get max amplitude detected (in %):
        try {
            //Get and convert scale:
            var curAmpl = mediaRecorder!!.maxAmplitude
            curAmpl = (curAmpl * 100) / MAX_AMPLITUDE
            return curAmpl
        } catch (e: Exception) {
            return 0
        }
    }

    fun stop(): File {
        try {
            mediaRecorder!!.stop()
            mediaRecorder!!.reset()
            mediaRecorder!!.release()
            mediaRecorder = null
            //Convert:
            convertAudioFile(source = recFileMp3!!, target = recFileFlac!!)
        } catch (e: Exception) {
            recordingFail = true
            Log.w(TAG, "ERROR: Recorder stop FAIL.", e)
        }
        return recFileFlac!!
    }

    fun convertAudioFile(source: File, target: File) {
        try {
            val session = FFmpegKit.execute("-i ${source.absolutePath} -c:v flac ${target.absolutePath} -y -loglevel quiet")
            Log.d(TAG, "Conversion return: ${session.returnCode}")
            //Log.d(TAG, "Conversion output: ${session.output}")
            Log.d(TAG, "Conversion fail track (if any): ${session.failStackTrace}")
        } catch (e: Exception) {
            Log.w(TAG, "Audio file conversion error: ", e)
        }

    }


    //While recording:
    fun whileRecording(
        messageMode: Boolean = false,
        messageType: String = "",
    ) {
        try {
            var c = 0
            var deltaThreshold = 5
            var amplitudes = mutableListOf<Int>()
            var min = 0
            var max = 0
            var std = 0
            var curAmpl = 0
            var maxTime = prefs.recTimeout.toLong()
            if (messageMode && messageType == "voice") {
                maxTime = maxAudioRecTimeout
            } else if (messageMode) {
                maxTime = prefs.messageTimeout.toLong()
            }

            //ONCE EVERY SECOND:
            while (recordingTime < maxTime) {

                if (!recordingMode && recordingTime > 0) {
                    //Recording is over:
                    break

                } else if ((messageMode && prefs.silenceEnabledMess) || (!messageMode && prefs.silenceEnabledQueries)) {
                    //Get max amplitude detected (in %):
                    if (recordingTime > 1) {
                        curAmpl = getMaxAmplitude()
                        amplitudes.add(curAmpl)
                        Log.d(TAG, "CURRENT: $curAmpl")
                    }

                    try {
                        min = amplitudes.filter { it > 0 }.min()
                        max = amplitudes.max()
                        std = utils.getStDev(amplitudes.filter { it > 0 })
                    } catch (e: Exception) {
                        Log.w(TAG, "No min/max.")
                    }

                    //Tolerance period ended:
                    if (recordingTime == silenceInitPatience) {

                        if ((max - min) <= deltaThreshold) {
                            //Early stop!
                            Log.d(TAG, "RECORDER: SILENCE DETECTED! -> EARLY STOP")
                            Intent().also { intent ->
                                intent.setAction(ACTION_REC_STOP)
                                context.sendBroadcast(intent)
                            }
                            break

                        } else {
                            c = 0
                        }

                    } else if (recordingTime > silencePatience) {

                        if ((max - curAmpl) <= std) {
                            //Go on:
                            c = 0

                        } else if (c >= silencePatience) {
                            //Early stop!
                            Log.d(TAG, "RECORDER: SILENCE DETECTED! -> EARLY STOP")
                            Intent().also { intent ->
                                intent.setAction(ACTION_REC_STOP)
                                context.sendBroadcast(intent)
                            }
                            break

                        } else {
                            //Speaking -> go on:
                            c++
                        }
                    }
                }
                Thread.sleep(500)
                recordingTime ++
            }

            Log.d(TAG, "AMPLITUDES: $amplitudes")
            Log.d(TAG, "MIN: $min")
            Log.d(TAG, "MAX: $max")
            Log.d(TAG, "STD: $std")

            if (recordingMode) {
                //STOP recording:
                Intent().also { intent ->
                    intent.setAction(ACTION_REC_STOP)
                    context.sendBroadcast(intent)
                }
            }

        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted: exception.", e)
        }
    }

}