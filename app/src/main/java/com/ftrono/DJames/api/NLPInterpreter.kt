package com.ftrono.DJames.api

import java.io.File


class NLPInterpreter {
    private val TAG = NLPInterpreter::class.java.simpleName

    fun queryNLP(recFile: File): Array<String> {
        //TEMP:
        var q = "Clarity"
        var qTrack = "track=Clarity"
        var qArtist = "artist=John Mayer"
        //var qType = "type=track"
        var results = arrayOf(q, qTrack, qArtist)
        return results
    }


}