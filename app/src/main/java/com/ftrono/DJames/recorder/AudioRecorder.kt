package com.ftrono.DJames.recorder

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)

    fun stop(outputFile: File, convFile: File)
}