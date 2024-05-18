package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.R
import com.google.gson.JsonObject
import android.content.Context
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.maxThreshold
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.supportedLanguageCodes
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
    fun extractMatches(queryText: String, reqLanguage: String): JsonObject {
        //INIT:
        var retExtracted = JsonObject()
        var playType = "track"
        var matchExtracted = ""
        var currentArtist = false
        var artistExtracted = ""
        var contextType = "album"
        var currentContext = false
        var contextLiked = false
        var contextExtracted = ""
        //Tools:
        var byNumber = 0
        var byFull = false
        //Query language:
        var queryLanguage = supportedLanguageCodes[prefs.queryLanguage.toInt()]
        if (reqLanguage != "") {
            queryLanguage = reqLanguage
        }

        //Search items:
        var reader: BufferedReader? = null
        if (queryLanguage == "it") {
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_ita)))   //"ita"
        } else {
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_eng)))   //"eng"
        }
        //Load:
        val sourceSents = JsonParser.parseReader(reader).asJsonObject
        val thisWords = sourceSents.get("this_words").asJsonArray   //"this"
        val contextSents = sourceSents.get("context_sents").asJsonArray   //"context"
        val contextLikedSents = sourceSents.get("context_liked_sents").asJsonArray   //"liked songs"
        val playSents = sourceSents.get("play_sents").asJsonArray   //"play"
        val bySents = sourceSents.get("by_sents").asJsonArray   //"by" (full phrasing)
        val byWordsJson = sourceSents.get("by_words").asJsonArray   //"by" (single word)
        var byWords = mutableListOf<String>()
        for (wordEl in byWordsJson) {
            val word = wordEl.asString
            byWords.add(word)
        }
        Log.d(TAG, "BY_WORDS: $byWords")

        //1) COUNTERS:
        // Count number of occurrences of the world "by":
        for (tok in queryText.split(" ")) {
            if (byWords.contains(tok)) byNumber++
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
                for (wordEl in thisWords) {
                    val word = wordEl.asString
                    if (sent.contains(word)) {
                        currentArtist = true
                        break
                    }
                }
                break
            }
        }
        if (byInd == -1) {
            //If not full phrasing for "by" -> look for just "by":
            for (word in byWords) {
                byInd = queryText.indexOf(word, ignoreCase = true)
                if (byInd > -1) {
                    byStr = word
                    break
                }
            }
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
                if (sent.contains("playlist")) {
                    contextType = "playlist"
                }
                //check "current":
                for (wordEl in thisWords) {
                    val word = wordEl.asString
                    if (sent.contains(word)) {
                        currentContext = true
                        break
                    }
                }
                //Check "liked songs":
                if (!currentContext) {
                    //check "current":
                    for (likedEl in contextLikedSents) {
                        val likedSent = likedEl.asString
                        if (sent.contains(likedSent)) {
                            contextLiked = true
                            contextType = "playlist"
                            break
                        }
                    }
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
        retExtracted.addProperty("artist_confirmed", "")
        retExtracted.addProperty("context_type", contextType)
        retExtracted.addProperty("context_current", currentContext)
        retExtracted.addProperty("context_liked", contextLiked)
        retExtracted.addProperty("context_extracted", contextExtracted)
        retExtracted.addProperty("context_confirmed", "")
        //Log.d(TAG, "NLP EXTRACTOR RESULTS: $retExtracted")
        return retExtracted
    }


    //Double check artists between DF & NLP Extractor:
    fun checkArtists(artistsNlp: JsonArray, artistExtracted: String, reqLanguage: String): String {
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
            var listExtracted: List<String>? = null
            //language:
            var queryLanguage = supportedLanguageCodes[prefs.queryLanguage.toInt()]
            if (reqLanguage != "") {
                queryLanguage = reqLanguage
            }
            if (queryLanguage == "it") {
                listExtracted = artistExtracted.split(" e ")
            } else {
                listExtracted = artistExtracted.split(" and ")
            }
            //Match one by one the artists extracted by DF with those extracted by Extractor:
            for (extr in listExtracted!!) {
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
        var vocArtists = utils.getVocabulary(filter="artist")
        var vocMatch = matchVocabulary(filter="artist", text=artistConfirmed, vocJson=vocArtists)
        if (!vocMatch.isEmpty) {
            //Replace:
            artistConfirmed = vocMatch.get("text_confirmed").asString
        }
        Log.d(TAG, "ARTIST CONFIRMED: $artistConfirmed")
        return  artistConfirmed
    }


    //Match item from user query against user vocabulary:
    fun matchVocabulary(filter: String, text: String, vocJson: JsonObject): JsonObject {
        var matchConfirmed = JsonObject()
        val vocArray = vocJson.keySet().toList()
        if (text != "" && vocArray.isNotEmpty()) {
            //Init:
            var score = 0
            var listEvalued = text.split(", ")
            var listConfirmed = ArrayList<String>()
            var scoresMap = mutableMapOf<String, Int>()

            //Check each evaluated artist:
            for (eval in listEvalued) {
                for (current in vocArray) {
                    score = FuzzySearch.ratio(current, eval)
                    Log.d(TAG, "VOC CONFIRMATION: COMPARING $current WITH $eval, MATCH: $score")
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
                Log.d(TAG, "listConfirmed: $listConfirmed")
                var matchName = listConfirmed[0]
                var itemDetails = JsonObject()
                matchConfirmed.addProperty("text_confirmed", matchName)
                if (filter == "playlist") {
                    itemDetails = vocJson.get(matchName).asJsonObject
                    matchConfirmed.addProperty("detail_confirmed", itemDetails.get("playlist_URL").asString)
                } else if(filter == "contact") {
                    itemDetails = vocJson.get(matchName).asJsonObject
                    var prefix = itemDetails.get("prefix").asString
                    var phone = itemDetails.get("phone").asString
                    matchConfirmed.addProperty("detail_confirmed", "${prefix}${phone}")
                }
                Log.d(TAG, "VOCABULARY MATCH: ${matchConfirmed.get("text_confirmed")}")
            }
        }

        return matchConfirmed
    }


    //Match contact from user query against user vocabulary:
    fun extractContact(queryText: String, fullLanguage: String = ""): String {
        var phone = ""
        var queryClean = queryText
        //Init log:
        var contactExtractor = JsonObject()
        contactExtractor.addProperty("contact_extracted", "")
        contactExtractor.addProperty("contact_confirmed", "")
        contactExtractor.addProperty("contact_phone", "")

        //Get user vocabulary:
        var vocContacts = utils.getVocabulary(filter="contact")
        if (vocContacts.isEmpty()) {
            return phone
        } else {
            //Search items:
            var reader: BufferedReader? = null
            //Calls / message requests -> only use default query language:
            if (prefs.queryLanguage.toInt() == 1) {
                reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_ita)))   //"ita"
            } else {
                reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_eng)))   //"eng"
            }
            //Load:
            val sourceSents = JsonParser.parseReader(reader).asJsonObject
            val phoneSents = sourceSents.get("phone_sents").asJsonArray   //"call / message" (full phrasing)
            val introLang = sourceSents.get("intro_lang").asJsonArray   //" in "

            //clean sent from language:
            if (fullLanguage != "") {
                //remove requested language name:
                queryClean = queryClean.replace(fullLanguage, "")
                for (intro in introLang) {
                    var introStr = intro.asString
                    queryClean = queryClean.replace(introStr, "")
                }
                queryClean = queryClean.strip()
            }

            //"to / call / message":
            var toInd = -1
            var toStr = ""
            for (sentEl in phoneSents) {
                val sent = sentEl.asString
                toInd = queryClean.indexOf(sent, ignoreCase = true)
                //found:
                if (toInd > -1) {
                    toStr = sent
                    break
                }
            }

            //Match contact name:
            if (toInd > -1) {
                //slice:
                Log.d(TAG, "TO_STR: $toStr")
                var contactExtracted = queryClean.slice((toInd + toStr.length)..queryClean.lastIndex).strip()
                contactExtractor.addProperty("contact_extracted", contactExtracted)
                Log.d(TAG, "CONTACT EXTRACTED: $contactExtracted")
                //2) Match extracted contact name with user vocabulary:
                var vocMatch = matchVocabulary(filter="contact", text=contactExtracted, vocJson=vocContacts)
                if (!vocMatch.isEmpty) {
                    //Replace:
                    phone = vocMatch.get("detail_confirmed").asString
                    contactExtractor.addProperty("contact_confirmed", vocMatch.get("text_confirmed").asString)
                    contactExtractor.addProperty("contact_phone", phone)
                }
                Log.d(TAG, "PHONE CONFIRMED: $phone")
            }
        }
        last_log!!.add("contact_extractor", contactExtractor)
        return phone
    }

}