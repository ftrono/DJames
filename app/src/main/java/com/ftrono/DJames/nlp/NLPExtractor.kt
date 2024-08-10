package com.ftrono.DJames.nlp

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
import kotlin.math.roundToInt


class NLPExtractor (private val context: Context) {
    private val TAG = NLPExtractor::class.java.simpleName
    private val utils = Utilities()

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

            //Check each evaluated item:
            for (eval in listEvalued) {
                for (current in vocArray) {
                    if (filter == "playlist") {
                        var namePartial = FuzzySearch.partialRatio(current, eval)
                        var nameFull = FuzzySearch.ratio(current, eval)
                        score = listOf<Int>(namePartial, nameFull).average().roundToInt()
                    } else {
                        score = FuzzySearch.ratio(current, eval)
                    }
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
                    if (itemDetails.has("contact_language")) {
                        var language = itemDetails.get("contact_language").asString
                        matchConfirmed.addProperty("contact_language", "$language")
                    }
                }
                Log.d(TAG, "VOCABULARY MATCH: ${matchConfirmed.get("text_confirmed")}")
            }
        }

        return matchConfirmed
    }


    //Match contact from user query against user vocabulary:
    fun extractContact(queryText: String, fullLanguage: String = ""): JsonObject {
        var phone = ""
        var queryClean = queryText
        //Init log:
        var contactExtractor = JsonObject()
        contactExtractor.addProperty("contact_extracted", "")
        contactExtractor.addProperty("contact_confirmed", "")
        contactExtractor.addProperty("contact_phone", "")
        contactExtractor.addProperty("contact_language", "")

        //Get user vocabulary:
        var vocContacts = utils.getVocabulary(filter="contact")
        if (vocContacts.isEmpty()) {
            return contactExtractor
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
                    if (vocMatch.has("contact_language")) {
                        contactExtractor.addProperty("contact_language", vocMatch.get("contact_language").asString)
                    }
                }
                Log.d(TAG, "CONTACT CONFIRMED: $contactExtractor")
            }
        }
        last_log!!.add("contact_extractor", contactExtractor)
        return contactExtractor
    }

}