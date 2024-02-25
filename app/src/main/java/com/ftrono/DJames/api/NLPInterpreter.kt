package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.R
import com.google.gson.JsonObject
import android.content.Context
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader

class NLPInterpreter (private val context: Context) {
    private val TAG = NLPInterpreter::class.java.simpleName

    //NOTE: To be called only if Intent HAS a song name inside!
    fun extractMatches(type: String, queryText: String): JsonObject {
        //INIT:
        var retExtracted = JsonObject()
        var playType = type
        var matchExtracted = ""
        var currentArtist = false
        var artistExtracted = ""
        var contextType = ""
        var currentContext = false
        var contextExtracted = ""
        //Tools:
        var byNumber = 0
        var byFull = false

        //Search items:
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents)))
        val sourceSents = JsonParser.parseReader(reader).asJsonObject
        val playSents = sourceSents.get("play_sents").asJsonArray   //"play"
        val bySents = sourceSents.get("by_sents").asJsonArray   //"by" (full phrasing)
        val contextSents = sourceSents.get("context_sents").asJsonArray   //"context"


        //1) COUNTERS:
        // Count number of occurrences of the world "by":
        for (tok in queryText.split(" ")) {
            if (tok == "by") byNumber++
        }


        //2) INDEXERS:
        //"play":
        var playInd = -1
        var playStr = ""
        for (sentEl in playSents) {
            val sent = sentEl.asString
            //Get index:
            playInd = queryText.indexOf(sent, ignoreCase = true)
            //"play" found:
            if (playInd > -1) {
                playStr = sent
                //Check what to play:
                if (sent.contains("album")) {
                    playType = "album"
                } else if (sent.contains("playlist")) {
                    playType = "playlist"
                }
                break
            }
        }

        //"by":
        var byInd = -1
        var byStr = ""
        for (sentEl in bySents) {
            val sent = sentEl.asString
            byInd = queryText.indexOf(sent, ignoreCase = true)
            //"by" (full phrasing) found:
            if (byInd > -1) {
                byFull = true
                byStr = sent
                //check "current":
                if (sent.contains("this ")) currentArtist = true
                break
            }
        }
        if (byInd == -1) {
            //If not full phrasing for "by" -> look for just "by":
            byInd = queryText.indexOf("by ", ignoreCase = true)
            if (byInd > -1) byStr = "by"
        }

        //"context":
        var contextInd = -1
        var contextStr = ""
        for (sentEl in contextSents) {
            val sent = sentEl.asString
            contextInd = queryText.indexOf(sent, ignoreCase = true)
            if (contextInd > -1) {
                contextStr = sent
                //Check context type:
                if (sent.contains("album")) {
                    contextType = "album"
                } else if (sent.contains("playlist")) {
                    contextType = "playlist"
                }
                //check "current":
                if (sent.contains("this")) currentContext = true
                break
            }
        }


        //3) SLICERS:
        if (playInd == -1) {
            //a) no "play" -> empty query!
            return retExtracted

        } else {
            //b) got "play":

            if (byInd == -1 || byNumber > 1 && !byFull) {
                //b.1) no "by" OR (multiple "by" & no "full by") -> just play the match name
                if (contextInd == -1) {
                    matchExtracted = queryText.slice((playInd + playStr.length)..queryText.lastIndex).strip()

                } else {
                    if (contextInd < playInd) {
                        //"context" before "play":
                        if (!currentContext) contextExtracted = queryText.slice((contextInd + contextStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..queryText.lastIndex).strip()

                    } else {
                        //"play" before "context":
                        if (!currentContext) contextExtracted = queryText.slice((contextInd + contextStr.length)..queryText.lastIndex) .strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..< contextInd).strip()
                    }
                }

            } else {
                //b.2) got one good "by" -> FULL EXTRACTION:

                //a) no context:
                if (contextInd == -1) {
                    if (playInd < byInd) {
                        //"play" comes before "by":
                        matchExtracted = queryText.slice((playInd + playStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..queryText.lastIndex).strip()

                    } else {
                        //"by" comes before "play":
                        matchExtracted = queryText.slice((playInd + playStr.length)..queryText.lastIndex).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< byInd).strip()
                    }

                } else {
                    //b) context:

                    if (playInd in (contextInd + 1)..<byInd) {
                        //order: "context", "play", "by":
                        if (!currentContext) contextExtracted = queryText.slice((contextInd + contextStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..queryText.lastIndex).strip()

                    } else if (byInd in (contextInd + 1)..<playInd) {
                        //order: "context", "by", "play":
                        if (!currentContext) contextExtracted = queryText.slice((contextInd + contextStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..queryText.lastIndex).strip()

                    } else if (byInd in (playInd + 1)..<contextInd) {
                        //order: "play", "by", "context":
                        matchExtracted = queryText.slice((playInd + playStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< contextInd).strip()
                        if (!currentContext) contextExtracted = queryText.slice((contextInd + contextStr.length)..queryText.lastIndex).strip()

                    } else if (contextInd in (playInd + 1)..<byInd) {
                        //order: "play", "context", "by":
                        matchExtracted = queryText.slice((playInd + playStr.length)..< contextInd).strip()
                        if (!currentContext) contextExtracted = queryText.slice((contextInd + contextStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..queryText.lastIndex).strip()

                    } else if (playInd in (byInd + 1)..<contextInd) {
                        //order: "by", "play", "context":
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..< contextInd).strip()
                        if (!currentContext) contextExtracted = queryText.slice((contextInd + contextStr.length)..queryText.lastIndex).strip()

                    } else {
                        //order: "by", "context", "play":
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< contextInd).strip()
                        if (!currentContext) contextExtracted = queryText.slice((contextInd + contextStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..queryText.lastIndex).strip()
                    }
                }
            }
        }

        //FILL RET JSON:
        retExtracted.addProperty("play_type", playType)
        retExtracted.addProperty("match_extracted", matchExtracted)
        retExtracted.addProperty("artist_current", currentArtist)
        retExtracted.addProperty("artist_extracted", artistExtracted)
        retExtracted.addProperty("context_type", contextType)
        retExtracted.addProperty("context_current", currentContext)
        retExtracted.addProperty("context_extracted", contextExtracted)
        Log.d(TAG, "NLP EXTRACTOR RESULTS: $retExtracted")

        //Add to log:
        last_log!!.add("nlp_extractor", retExtracted)

        return retExtracted
    }
}