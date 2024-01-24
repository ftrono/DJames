package com.ftrono.DJames.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import android.util.Log
import com.ftrono.DJames.application.*

//import android.media.AudioFormat
//import android.provider.MediaStore.Audio.Media

class AndroidAudioRecorder(private val context: Context): AudioRecorder {
    private val TAG = AndroidAudioRecorder::class.java.simpleName
    private var recorder: MediaRecorder? = null

    private fun createRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    override fun start(outputFile: File) {
        try {
            createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.DEFAULT)   //.MIC
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(FileOutputStream(outputFile).fd)

                prepare()
                start()

                recorder = this
            }
        } catch (e: Exception) {
            Log.d(TAG, "ERROR: Recorder not started.", e)
            //Toast.makeText(context, "ERROR: Recorder not started.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun stop() {
        try {
            recorder!!.stop()
            recorder!!.reset()
            recorder!!.release()
            recorder = null
        } catch (e: Exception) {
            Log.d(TAG, "ERROR: Recorder not stopped.", e)
            //Toast.makeText(context, "ERROR: Recorder not stopped.", Toast.LENGTH_SHORT).show()
        }
    }
}