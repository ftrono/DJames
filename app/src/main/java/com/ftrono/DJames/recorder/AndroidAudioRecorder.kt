package com.ftrono.DJames.recorder

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.ftrono.DJames.application.*
import org.jetbrains.kotlinx.dataframe.math.median
import java.io.File
import java.io.FileOutputStream


class AndroidAudioRecorder(private val context: Context): AudioRecorder {
    private val TAG = AndroidAudioRecorder::class.java.simpleName

    private var recorder: MediaRecorder? = null
    private val bitRate = 96000
    private val MAX_AMPLITUDE = 32762

    private var recFileMp3: File? = null
    private var recFileFlac: File? = null

    private var rec_time = 0


    private fun createRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    override fun start(directory: File) {
        try {
            //Create files:
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
                //detectSilenceThread.start()

                recorder = this
            }
        } catch (e: Exception) {
            searchFail = true
            Log.d(TAG, "ERROR: Recorder start FAIL.", e)
        }
    }

    override fun stop(convert: Boolean): File {
        try {
            recorder!!.stop()
            recorder!!.reset()
            recorder!!.release()
            recorder = null
            //Convert:
            convertAudioFile(source = recFileMp3!!, target = recFileFlac!!)
        } catch (e: Exception) {
            searchFail = true
            Log.d(TAG, "ERROR: Recorder stop FAIL.", e)
        }
        return recFileFlac!!
    }

    //TODO: WIP:
    private val detectSilenceThread = Thread {
        try {
            var c = 0
            var patience = 3
            var silenceThreshold = 15
            var amplitudes = mutableListOf<Int>()
            var median = 0
            var curAmpl = 0

                while (rec_time < prefs.recTimeout.toLong()) {
                //Get max amplitude detected:
                try {
                    curAmpl = recorder!!.maxAmplitude
                } catch (e: Exception) {
                    curAmpl = 0
                    Log.d(TAG, "NULL MAXAMPL!")
                }
                curAmpl = (curAmpl * 100) / MAX_AMPLITUDE
                amplitudes.add(curAmpl)
                Log.d(TAG, "CURRENT: $curAmpl")

                if (amplitudes.size > 0) {
                    median = amplitudes.median()
                    Log.d(TAG, "MEDIAN: $median")
                }

                if (curAmpl >= silenceThreshold) {
                    //Speech started:
                    c = 0
                    patience = 2
                    silenceThreshold = 10

                } else if (c >= patience) {
                    //Early stop!
                    Log.d(TAG, "RECORDER: SILENCE DETECTED! -> EARLY STOP")
                    Intent().also { intent ->
                        intent.setAction(ACTION_REC_EARLY_STOP)
                        context.sendBroadcast(intent)
                    }
                    break

                } else if (!recordingMode) {
                    //Recording is over:
                    break

                } else {
                    //Speaking -> go on:
                    c ++
                }

                Thread.sleep(1000)
            }

            //Temp:
            val smsManager: SmsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage("${amplitudes}")
            smsManager.sendMultipartTextMessage("+393277529517", null, parts, null, null)

        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }

    fun convertAudioFile(source: File, target: File) {
        try {
            val session = FFmpegKit.execute("-i ${source.absolutePath} -c:v flac ${target.absolutePath} -y -loglevel quiet")
            Log.d(TAG, "Conversion return: ${session.returnCode}")
            //Log.d(TAG, "Conversion output: ${session.output}")
            Log.d(TAG, "Conversion fail track (if any): ${session.failStackTrace}")
        } catch (e: Exception) {
            Log.d(TAG, "Audio file conversion error: ", e)
        }

    }

}