package com.ftrono.DJames.recorder

import java.io.File

interface AudioRecorder {
    fun start(directory: File)
    fun stop(convert: Boolean = false): File
}