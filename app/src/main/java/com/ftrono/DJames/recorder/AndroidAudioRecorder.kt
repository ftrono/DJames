package com.ftrono.DJames.recorder

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.ftrono.DJames.application.recSamplingRate
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean


class AndroidAudioRecorder(context: Context): AudioRecorder {

    private val TAG = AndroidAudioRecorder::class.java.simpleName
    val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    //The bigger the factor is the less likely it is that samples will be dropped
    val BUFFER_SIZE_FACTOR = 2

    //Size of the buffer where the audio data is stored by Android
    val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        recSamplingRate,
        CHANNEL_CONFIG, AUDIO_FORMAT
    ) * BUFFER_SIZE_FACTOR

    val recordingInProgress = AtomicBoolean(false)
    var recorder: AudioRecord? = null
    var recordingThread: Thread? = null

    override fun start(outputFile: File) {
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.DEFAULT, recSamplingRate,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE
            )
            recorder!!.startRecording()
            recordingInProgress.set(true)
            recordingThread(outputFile)
            Log.d(TAG, "Recorder started.")
        } catch (e: SecurityException) {
            Log.d(TAG, "Recorder NOT started. Security Exception: ", e)
        }
    }

    override fun stop(outputFile: File, convFile: File) {
        try {
            if (null == recorder) {
                return
            }
            recordingInProgress.set(false)
            recorder!!.stop()
            recorder!!.release()
            recorder = null
            recordingThread = null
            Log.d(TAG, "Recorder stopped.")
            try {
                rawToWave(outputFile, convFile)
            } catch (e: Exception) {
                Log.d(TAG, "Recorder: conversion to WAV failed!")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Recorder NOT stopped: ", e)
        }
    }

    fun getBufferReadFailureReason(errorCode: Int): String {
        return when (errorCode) {
            AudioRecord.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
            AudioRecord.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
            AudioRecord.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
            AudioRecord.ERROR -> "ERROR"
            else -> "Unknown ($errorCode)"
        }
    }

    fun recordingThread(outputFile: File) {
        val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
        val recThread = Thread {
            try {
                synchronized(this) {
                    FileOutputStream(outputFile).use { outStream ->
                        while (recordingInProgress.get()) {
                            val result: Int = recorder!!.read(buffer, BUFFER_SIZE)
                            if (result < 0) {
                                throw RuntimeException(
                                    "Reading of audio buffer failed: " +
                                            getBufferReadFailureReason(result)
                                )
                            }
                            outStream.write(buffer.array(), 0, BUFFER_SIZE)
                            buffer.clear()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Writing of recorded audio failed: ", e)
            }
        }
        //start thread:
        recThread.start()
    }


    @Throws(IOException::class)
    private fun rawToWave(rawFile: File, waveFile: File) {
        val rawData = ByteArray(rawFile.length().toInt())
        var input: DataInputStream? = null
        try {
            input = DataInputStream(FileInputStream(rawFile))
            input.read(rawData)
        } finally {
            input?.close()
        }
        var output: DataOutputStream? = null
        try {
            output = DataOutputStream(FileOutputStream(waveFile))
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF") // chunk id
            writeInt(output, 36 + rawData.size) // chunk size
            writeString(output, "WAVE") // format
            writeString(output, "fmt ") // subchunk 1 id
            writeInt(output, 16) // subchunk 1 size
            writeShort(output, 1.toShort()) // audio format (1 = PCM)
            writeShort(output, 1.toShort()) // number of channels
            writeInt(output, 44100) // sample rate
            writeInt(output, recSamplingRate * 2) // byte rate
            writeShort(output, 2.toShort()) // block align
            writeShort(output, 16.toShort()) // bits per sample
            writeString(output, "data") // subchunk 2 id
            writeInt(output, rawData.size) // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            val shorts = ShortArray(rawData.size / 2)
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()[shorts]
            val bytes = ByteBuffer.allocate(shorts.size * 2)
            for (s in shorts) {
                bytes.putShort(s)
            }
            output.write(fullyReadFileToBytes(rawFile))
        } finally {
            output?.close()
        }
    }

    @Throws(IOException::class)
    fun fullyReadFileToBytes(f: File): ByteArray? {
        val size = f.length().toInt()
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        val fis = FileInputStream(f)
        try {
            var read = fis.read(bytes, 0, size)
            if (read < size) {
                var remain = size - read
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain)
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                    remain -= read
                }
            }
        } catch (e: IOException) {
            throw e
        } finally {
            fis.close()
        }
        return bytes
    }

    @Throws(IOException::class)
    private fun writeInt(output: DataOutputStream, value: Int) {
        output.write(value shr 0)
        output.write(value shr 8)
        output.write(value shr 16)
        output.write(value shr 24)
    }

    @Throws(IOException::class)
    private fun writeShort(output: DataOutputStream, value: Short) {
        output.write(value.toInt() shr 0)
        output.write(value.toInt() shr 8)
    }

    @Throws(IOException::class)
    private fun writeString(output: DataOutputStream, value: String) {
        for (i in 0 until value.length) {
            output.write(value[i].code)
        }
    }

}