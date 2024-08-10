package com.ftrono.DJames.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.ftrono.DJames.application.*
import java.io.File
import java.io.FileOutputStream


class AndroidAudioRecorder(private val context: Context): AudioRecorder {
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

                mediaRecorder = this
            }

        } catch (e: Exception) {
            searchFail = true
            Log.w(TAG, "ERROR: Recorder start FAIL.", e)
        }
    }

    override fun getMaxAmplitude(): Int {
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

    override fun stop(convert: Boolean): File {
        try {
            mediaRecorder!!.stop()
            mediaRecorder!!.reset()
            mediaRecorder!!.release()
            mediaRecorder = null
            //Convert:
            convertAudioFile(source = recFileMp3!!, target = recFileFlac!!)
        } catch (e: Exception) {
            searchFail = true
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

}