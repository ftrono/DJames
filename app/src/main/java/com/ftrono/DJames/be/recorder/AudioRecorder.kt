package com.ftrono.DJames.be.recorder

import java.io.File

interface AudioRecorder {
    fun start(directory: File)
    fun getMaxAmplitude(): Int
    fun stop(convert: Boolean = false): File
}