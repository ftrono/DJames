package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.R
import com.google.gson.JsonObject
import android.content.Context
import com.ftrono.DJames.application.matchThreshold
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import me.xdrop.fuzzywuzzy.FuzzySearch
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
        var contextLiked = false
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
                //Check "liked songs":
                else if (sent.contains("liked songs") || sent.contains("saved songs") || sent.contains("my songs")) {
                    contextLiked = true
                }
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
                        if (!currentContext && !contextLiked) contextExtracted = queryText.slice((contextInd + contextStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..queryText.lastIndex).strip()

                    } else {
                        //"play" before "context":
                        if (!currentContext && !contextLiked) contextExtracted = queryText.slice((contextInd + contextStr.length)..queryText.lastIndex) .strip()
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
                        if (!currentContext && !contextLiked) contextExtracted = queryText.slice((contextInd + contextStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..queryText.lastIndex).strip()

                    } else if (byInd in (contextInd + 1)..<playInd) {
                        //order: "context", "by", "play":
                        if (!currentContext && !contextLiked) contextExtracted = queryText.slice((contextInd + contextStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..queryText.lastIndex).strip()

                    } else if (byInd in (playInd + 1)..<contextInd) {
                        //order: "play", "by", "context":
                        matchExtracted = queryText.slice((playInd + playStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< contextInd).strip()
                        if (!currentContext && !contextLiked) contextExtracted = queryText.slice((contextInd + contextStr.length)..queryText.lastIndex).strip()

                    } else if (contextInd in (playInd + 1)..<byInd) {
                        //order: "play", "context", "by":
                        matchExtracted = queryText.slice((playInd + playStr.length)..< contextInd).strip()
                        if (!currentContext && !contextLiked) contextExtracted = queryText.slice((contextInd + contextStr.length)..< byInd).strip()
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..queryText.lastIndex).strip()

                    } else if (playInd in (byInd + 1)..<contextInd) {
                        //order: "by", "play", "context":
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< playInd).strip()
                        matchExtracted = queryText.slice((playInd + playStr.length)..< contextInd).strip()
                        if (!currentContext && !contextLiked) contextExtracted = queryText.slice((contextInd + contextStr.length)..queryText.lastIndex).strip()

                    } else {
                        //order: "by", "context", "play":
                        if (!currentArtist) artistExtracted = queryText.slice((byInd + byStr.length)..< contextInd).strip()
                        if (!currentContext && !contextLiked) contextExtracted = queryText.slice((contextInd + contextStr.length)..< playInd).strip()
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
        retExtracted.addProperty("context_liked", contextLiked)
        retExtracted.addProperty("context_extracted", contextExtracted)
        //Log.d(TAG, "NLP EXTRACTOR RESULTS: $retExtracted")
        return retExtracted
    }


    //Double check artists between DF & NLP Extractor:
    fun checkArtists(artistsNlp: JsonArray, artistExtracted: String): String {
        var artistConfirmed = ""

        //1) CHECK NLP ENTITIES VS ORIGINAL TEXT EXTRACTION:
        if (artistsNlp.isEmpty) {
            //Confirm artists extracted by Extractor:
            artistConfirmed = artistExtracted
        } else if (artistExtracted != "") {
            var score = 0
            var artist = ""
            var artistsTemp = ArrayList<String>()
            //Split artists extracted:
            var listExtracted = artistExtracted.split(" and ")
            //Match one by one the artists extracted by DF with those extracted by Extractor:
            for (extr in listExtracted) {
                for (artJs in artistsNlp) {
                    artist = artJs.asString
                    score = FuzzySearch.ratio(artist.lowercase(), extr)
                    Log.d(TAG, "EVALUATION: COMPARING $artist WITH $extr, MATCH: $score")
                    if (!artistsTemp.contains(artist) && score >= matchThreshold) {
                        artistsTemp.add(artist)
                    }
                }
            }

            Log.d(TAG, "Evalued Artists List: $artistsTemp")

            //Priority to DF if matches found:
            if (listExtracted.size > artistsTemp.size) {
                artistConfirmed = artistExtracted
            } else {
                artistConfirmed = artistsTemp.joinToString(", ", "", "")
            }
        }

        //2) Hand check artist evalued against user vocabulary:
        val utils = Utilities()
        var vocArtists = utils.getVocabularyArray(filter="artist")
        if (artistConfirmed != "" && !vocArtists.isEmpty()) {
            var score = 0
            var artist = ""
            var listEvalued = artistConfirmed.split(", ")
            var listConfirmed = ArrayList<String>()
            var scoresMap = mutableMapOf<String, Int>()
            //Check each evaluated artist:
            for (eval in listEvalued) {
                for (vocArtJs in vocArtists) {
                    artist = vocArtJs.asString
                    score = FuzzySearch.ratio(artist.lowercase(), eval)
                    Log.d(TAG, "VOC CONFIRMATION: COMPARING $artist WITH $eval, MATCH: $score")
                    //Add only best matches:
                    if (!scoresMap.keys.contains(artist) && score >= matchThreshold) {
                        scoresMap[artist] = score
                    }
                }
                if (scoresMap.isNotEmpty()) {
                    //Sort and get highest match:
                    val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap()
                    Log.d(TAG, "SORTED MAP FOR $eval: $sortedScores")
                    listConfirmed.add(sortedScores.keys.toList()[0])
                } else {
                    //Keep original eval:
                    listConfirmed.add(eval)
                }
            }
            if (listConfirmed.isNotEmpty()) {
                //Replace:
                artistConfirmed = listConfirmed.joinToString(", ", "", "")
            }
        }
        Log.d(TAG, "ARTISTS CONFIRMED: $artistConfirmed")
        return  artistConfirmed
    }


}