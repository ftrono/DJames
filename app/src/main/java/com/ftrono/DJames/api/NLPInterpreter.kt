package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.R
import com.google.gson.JsonObject
import android.content.Context
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.maxThreshold
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.io.BufferedReader
import java.io.InputStreamReader


class NLPInterpreter (private val context: Context) {
    private val TAG = NLPInterpreter::class.java.simpleName
    private val utils = Utilities()

    //NOTE: To be called only if Intent HAS a song name inside!
    fun extractMatches(queryText: String): JsonObject {
        //INIT:
        var retExtracted = JsonObject()
        var playType = "track"
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
                    if (!artistsTemp.contains(artist) && score >= maxThreshold) {
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
        var vocArtists = utils.getVocabularyArray(filter="artist")
        var vocMatch = matchVocabulary(filter="artist", text=artistConfirmed, vocArray=vocArtists)
        if (!vocMatch.isEmpty) {
            //Replace:
            artistConfirmed = vocMatch.get("text_confirmed").asString
        }
        Log.d(TAG, "ARTIST CONFIRMED: $artistConfirmed")
        return  artistConfirmed
    }


    fun matchVocabulary(filter: String, text: String, vocArray: JsonArray): JsonObject {
        var matchConfirmed = JsonObject()
        if (text != "" && !vocArray.isEmpty()) {
            //Init:
            var score = 0
            var current = ""
            var curName = ""
            var listEvalued = text.split(", ")
            var listConfirmed = ArrayList<String>()
            var scoresMap = mutableMapOf<String, Int>()

            //Check each evaluated artist:
            for (eval in listEvalued) {
                for (vocArtJs in vocArray) {
                    current = vocArtJs.asString
                    //Split if needed:
                    if (filter == "playlist" || filter == "contact") {
                        var temp = current.split(" %%% ")
                        curName = temp[0]
                    } else {
                        curName = current
                    }
                    score = FuzzySearch.ratio(curName, eval)
                    Log.d(TAG, "VOC CONFIRMATION: COMPARING $curName WITH $eval, MATCH: $score")
                    //Add only best matches:
                    if (!scoresMap.keys.contains(current) && score >= midThreshold) {
                        scoresMap[current] = score
                    }
                }
                if (scoresMap.isNotEmpty()) {
                    //Sort and get highest match:
                    val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap()
                    Log.d(TAG, "SORTED MAP FOR $eval: $sortedScores")
                    listConfirmed.add(sortedScores.keys.toList()[0])
                    last_log!!.addProperty("voc_score", sortedScores.values.toList()[0])
                } else {
                    if (filter == "artist") {
                        //Keep original eval:
                        listConfirmed.add(eval)
                    }
                }
            }

            if (listConfirmed.isNotEmpty()) {
                //Final:
                if (filter == "playlist" || filter == "contact") {
                    var temp = listConfirmed[0].split(" %%% ")
                    matchConfirmed.addProperty("text_confirmed", temp[0].strip())
                    matchConfirmed.addProperty("detail_confirmed", temp[1].strip())
                } else {
                    matchConfirmed.addProperty("text_confirmed", listConfirmed.joinToString(", ", "", ""))
                }

            }
        }
        Log.d(TAG, "VOCABULARY MATCH: ${matchConfirmed.get("text_confirmed")}")
        return matchConfirmed
    }


    //Hand contact from user query against user vocabulary:
    fun extractToCall(queryText: String): String {
        var toCall = ""
        //Init log:
        var contactExtractor = JsonObject()
        contactExtractor.addProperty("contact_extracted", "")
        contactExtractor.addProperty("contact_confirmed", "")
        contactExtractor.addProperty("contact_phone", "")

        //Get user vocabulary:
        var vocContacts = utils.getVocabularyArray(filter="contact")
        if (vocContacts.isEmpty()) {
            return toCall
        } else {
            //Search items:
            val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents)))
            val sourceSents = JsonParser.parseReader(reader).asJsonObject
            val callSents = sourceSents.get("call_sents").asJsonArray   //"call" (full phrasing)

            //"call":
            var callInd = -1
            var callStr = ""
            for (sentEl in callSents) {
                val sent = sentEl.asString
                callInd = queryText.indexOf(sent, ignoreCase = true)
                //"call" (full phrasing) found:
                if (callInd > -1) {
                    callStr = sent
                    break
                }
            }

            //Match contact name:
            if (callInd > -1) {
                var toCallExtracted = queryText.slice((callInd + callStr.length)..queryText.lastIndex).strip()
                contactExtractor.addProperty("contact_extracted", toCallExtracted)
                Log.d(TAG, "CONTACT EXTRACTED: $toCallExtracted")
                //2) Match extracted contact name with user vocabulary:
                var vocMatch = matchVocabulary(filter="contact", text=toCallExtracted, vocArray=vocContacts)
                if (!vocMatch.isEmpty) {
                    //Replace:
                    var phone = vocMatch.get("detail_confirmed").asString
                    contactExtractor.addProperty("contact_confirmed", vocMatch.get("text_confirmed").asString)
                    contactExtractor.addProperty("contact_phone", phone)
                    toCall = "tel:${phone}"
                }
                Log.d(TAG, "CONTACT CONFIRMED: $toCall")
            }
        }
        last_log!!.add("contact_extractor", contactExtractor)
        return toCall
    }


}