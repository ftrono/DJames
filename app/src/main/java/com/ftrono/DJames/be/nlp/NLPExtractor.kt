package com.ftrono.DJames.be.nlp

import android.util.Log
import com.ftrono.DJames.R
import com.google.gson.JsonObject
import android.content.Context
import com.ftrono.DJames.application.gMapsLinkFormat
import com.ftrono.DJames.application.lastLog
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.maxThreshold
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.ItemInfoUse
import com.google.gson.JsonParser
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.io.BufferedReader
import java.io.InputStreamReader


class NLPExtractor (private val context: Context) {
    private val TAG = NLPExtractor::class.java.simpleName

    //SONGS + ALBUM: Extract play info:
    fun extractPlayInfo(queryText: String, reqLanguage: String, playType: String, contextType: String): ExtractorInfo {
        //INIT:
        var retExtracted = ExtractorInfo()
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
            var contextSents = mutableListOf(
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
                matchExtracted = queryText.slice(0..< contextInd).trim()
                contextExtracted = queryText.slice((contextInd + contextStr.length)..< queryText.length).trim()
            }
            else {
                contextTypeConfirmed = "album"
            }
        }

        //2) Extract artist info:
        var byStr = ""
        var byFullSents = mutableListOf(
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
            artistExtracted = matchExtracted.slice((byInd + byStr.length)..< matchExtracted.length).trim()
            matchExtracted = matchExtracted.slice(0..< byInd).trim()

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
                artistExtracted = matchExtracted.slice((byInd + byStr.length)..< matchExtracted.length).trim()
                matchExtracted = matchExtracted.slice(0..< byInd).trim()

            }
        }

        //FILL RET JSON:
        retExtracted.playType = playType
        retExtracted.matchExtracted = matchExtracted
        retExtracted.artistExtracted = artistExtracted
        retExtracted.contextType = contextTypeConfirmed
        retExtracted.contextExtracted = contextExtracted
        Log.d(TAG, "NLP EXTRACTOR RESULTS: $retExtracted")

        return retExtracted
    }


    //Double check artists between DF & NLP Extractor:
    fun checkArtists(artistsNlp: MutableList<String>, artistExtracted: String, reqLanguage: String): String {
        var artistConfirmed = ""

        //1) CHECK NLP ENTITIES VS ORIGINAL TEXT EXTRACTION:
        if (artistsNlp.isEmpty()) {
            //Confirm artists extracted by Extractor:
            artistConfirmed = artistExtracted

        } else if (artistExtracted != "") {
            var score = 0
            var artist = ""
            var artistsTemp = mutableListOf<String>()
            //Split artists extracted:
            var listExtracted: List<String> = listOf<String>()
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
            for (extr in listExtracted) {
                for (artist in artistsNlp) {
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
        Log.d(TAG, "ARTIST CONFIRMED: $artistConfirmed")
        return artistConfirmed
    }


    //Match contact from user query against user library:
    fun extractContact(queryText: String, fullLanguage: String = ""): String {
        val filter = "contact"
        var queryClean = queryText
        var contactExtracted = ""

        if (libUtils.getCollectionSize(filter) > 0) {
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
                queryClean = queryClean.trim()
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
                contactExtracted = queryClean.slice((toInd + toStr.length)..queryClean.lastIndex).trim()
                Log.d(TAG, "CONTACT EXTRACTED: $contactExtracted")
            }
        }
        return contactExtracted
    }


    //SendMessage: Extract type of message to send:
    fun extractMessageType(queryText: String): String {
        //TODO: Provisional:
        var messageType = ""
        if (queryText.contains("voice") || queryText.contains("vocal") || queryText.contains("audio")) {
            messageType = "voice"
        } else if (queryText.contains("whatsapp")) {
            messageType = "whatsapp"
        }
        return messageType
    }


    //PlayArtist: Extract name of playLink to play:
    fun extractPlayLink(queryText: String): String {
        //TODO: Provisional:
        var playLinkName = ""
        if (queryText.contains("radio")) {
            playLinkName = "spotify_radio"
        } else if (queryText.contains("mix")) {
            playLinkName = "spotify_mix"
        }
        return playLinkName
    }


    //Route: extract Route Info from Message text:
    fun extractRoute(text: String, language: String): ItemInfoUse {
        var routeInfo = ItemInfoUse(
            type = "route"
        )
        routeInfo.language = language
        //TODO TEMP:
        var routeComps = text.split(" tramite ")
        // Comps[0] -> name, Comps[1] -> detail:
        routeInfo.name = utils.capitalizeWords(routeComps[0])
        var viaText = ""
        if (routeComps.size > 1) {
            if (routeComps[1] != "") {
                viaText = routeComps[1].trim()
            }
        }
        routeInfo.detail = utils.capitalizeWords(viaText)
        return routeInfo
    }

}