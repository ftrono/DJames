package com.ftrono.DJames.nlp

import android.util.Log
import com.ftrono.DJames.R
import com.google.gson.JsonObject
import android.content.Context
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.maxThreshold
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt


class NLPExtractor (private val context: Context) {
    private val TAG = NLPExtractor::class.java.simpleName

    //SONGS + ALBUM: Extract play info:
    fun extractPlayInfo(queryText: String, reqLanguage: String, playType: String, contextType: String): JsonObject {
        //INIT:
        var retExtracted = JsonObject()
        var byNumber = 0
        var byInd = -1
        var contextInd = -1
        var matchExtracted = queryText
        var artistExtracted = ""
        var contextTypeConfirmed = contextType
        var contextExtracted = ""

        //1) Extract context info (if track only):
        if (playType == "track" && contextType == "playlist") {
            var contextStr = ""
            var contextSents = arrayListOf(
                "from the playlist",
                "from playlist",
                "playlist"
            )
            //Identify context string:
            for (sent in contextSents) {
                if (matchExtracted.contains(sent)) {
                    contextInd = matchExtracted.indexOf(sent, ignoreCase = true)
                    contextStr = sent
                    break
                }
            }

            //SLICE:
            if (contextInd > -1) {
                matchExtracted = queryText.slice(0..< contextInd).strip()
                contextExtracted = queryText.slice((contextInd + contextStr.length)..< queryText.length).strip()
            }
            else {
                contextTypeConfirmed = "album"
            }
        }

        //2) Extract artist info:
        var byStr = ""
        var byFullSents = arrayListOf(
            " by the artist ",
            " by artist ",
            " artist "
        )

        for (sent in byFullSents) {
            if (matchExtracted.contains(sent)) {
                byInd = matchExtracted.indexOf(sent, ignoreCase = true)
                byStr = sent
                break
            }
        }

        if (byInd > -1) {
            //SLICE:
            artistExtracted = matchExtracted.slice((byInd + byStr.length)..< matchExtracted.length).strip()
            matchExtracted = matchExtracted.slice(0..< byInd).strip()

        } else {
            //Count occurrences of the word "by":
            // Count number of occurrences of the world "by":
            byStr = "by"
            for (tok in matchExtracted.split(" ")) {
                if (byStr == tok) {
                    byNumber++
                }
            }

            if (byNumber == 1) {
                //SLICE:
                byInd = matchExtracted.indexOf(byStr, ignoreCase = true)
                artistExtracted = matchExtracted.slice((byInd + byStr.length)..< matchExtracted.length).strip()
                matchExtracted = matchExtracted.slice(0..< byInd).strip()

            }
        }

        //FILL RET JSON:
        retExtracted.addProperty("play_type", playType)
        retExtracted.addProperty("match_extracted", matchExtracted)
        retExtracted.addProperty("artist_extracted", artistExtracted)
        retExtracted.addProperty("artist_confirmed", "")
        retExtracted.addProperty("context_type", contextTypeConfirmed)
        retExtracted.addProperty("context_extracted", contextExtracted)
        retExtracted.addProperty("context_confirmed", "")
        Log.d(TAG, "NLP EXTRACTOR RESULTS: $retExtracted")

        return retExtracted
    }


    //Double check artists between DF & NLP Extractor:
    fun checkArtists(artistsNlp: JsonArray, artistExtracted: String, reqLanguage: String, playlistName: String = "default"): JsonObject {
        val filter = "artist"
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
            var queryLanguage = prefs.queryLanguage
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

        var artistJson = JsonObject()
        artistJson.addProperty("text_confirmed", artistConfirmed)

        //2) Hand check artist evalued against user vocabulary:
        val vocMatch = matchVocabulary(filter, text=artistConfirmed)
        if (vocMatch != "") {
            //Replace:
            artistConfirmed = vocMatch
            artistJson.addProperty("text_confirmed", artistConfirmed)
            artistJson.addProperty("detail_confirmed", utils.getPlayUrl(filter, artistConfirmed, playlistName))
        }
        Log.d(TAG, "ARTIST CONFIRMED: $artistJson")
        return  artistJson
    }


    //Match item from user query against user vocabulary:
    fun matchVocabulary(filter: String, text: String): String {
        var matchName = ""
        val vocArray = utils.getLibraryKeys(filter)
        if (text != "" && vocArray.isNotEmpty()) {
            //Init:
            var score = 0
            var listEvalued = text.split(", ")
            var listConfirmed = ArrayList<String>()
            var scoresMap = mutableMapOf<String, Int>()

            //Check each evaluated item:
            for (eval in listEvalued) {
                for (current in vocArray) {
                    if (filter == "playlist") {
                        var namePartial = FuzzySearch.partialRatio(current, eval.lowercase())
                        var nameFull = FuzzySearch.ratio(current, eval.lowercase())
                        score = listOf<Int>(namePartial, nameFull).average().roundToInt()
                    } else {
                        score = FuzzySearch.ratio(current, eval.lowercase())
                    }
                    Log.d(TAG, "VOC CONFIRMATION: COMPARING $current WITH ${eval.lowercase()}, MATCH: $score")
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
                }
            }
            //Final:
            if (listConfirmed.isNotEmpty()) {
                Log.d(TAG, "listConfirmed: $listConfirmed")
                matchName = listConfirmed[0]
                Log.d(TAG, "VOCABULARY MATCH: $matchName")
            }
        }
        return matchName
    }


    //Match contact from user query against user vocabulary:
    fun extractContact(queryText: String, fullLanguage: String = ""): JsonObject {
        val filter = "contact"
        var queryClean = queryText
        //Init log:
        var contactConfirmed = JsonObject()
        contactConfirmed.addProperty("contact_extracted", "")
        contactConfirmed.addProperty("contact_confirmed", "")
        contactConfirmed.addProperty("contact_phone", "")
        contactConfirmed.addProperty("contact_language", "")

        //Get user vocabulary:
        var vocContacts = utils.getLibraryKeys(filter)
        if (vocContacts.isNotEmpty()) {
            //Search items:
            var reader: BufferedReader? = null
            //Calls / message requests -> only use default query language:
            if (prefs.queryLanguage == "it") {
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
                contactConfirmed.addProperty("contact_extracted", contactExtracted)
                Log.d(TAG, "CONTACT EXTRACTED: $contactExtracted")
                //2) Match extracted contact name with user vocabulary:
                val vocMatch = matchVocabulary(filter, text=contactExtracted)
                if (vocMatch != "") {
                    val contactObj = utils.getLibraryItem(filter, vocMatch)
                    val mainObj = contactObj.get("main").asJsonObject
                    val prefix = mainObj.get("prefix").asString
                    val phone = mainObj.get("phone").asString
                    //Replace:
                    contactConfirmed.addProperty("contact_confirmed", vocMatch)
                    contactConfirmed.addProperty("contact_phone", "${prefix}${phone}")
                    contactConfirmed.addProperty("contact_language", contactObj.get("language").asString)
                }
                Log.d(TAG, "CONTACT CONFIRMED: $contactConfirmed")
            }
        }
        last_log!!.add("contact_extractor", contactConfirmed)
        return contactConfirmed
    }

}