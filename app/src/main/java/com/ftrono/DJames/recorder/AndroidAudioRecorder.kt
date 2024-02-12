package com.ftrono.DJames.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recSamplingRate
import com.ftrono.DJames.application.searchFail
import java.io.File
import java.io.FileOutputStream


class AndroidAudioRecorder(private val context: Context): AudioRecorder {
    private val TAG = AndroidAudioRecorder::class.java.simpleName

    private var recorder: MediaRecorder? = null
    private val bitRate = 96000
    private val recFileName = "DJames_request"

    private var recFileMp3: File? = null
    private var recFileFlac: File? = null


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
                if (prefs.micType.toInt() == 0) {
                    //Current default mic:
                    setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                } else {
                    //Primary mic:
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(bitRate)
                setAudioSamplingRate(recSamplingRate)
                setAudioChannels(1)   //mono
                setOutputFile(FileOutputStream(recFileMp3).fd)

                prepare()
                start()

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