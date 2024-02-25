package com.ftrono.DJames.api

import com.google.gson.JsonObject
import android.util.Log
import android.content.Context
import com.ftrono.DJames.application.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import me.xdrop.fuzzywuzzy.FuzzySearch
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class SpotifyInterpreter (private val context: Context) {
    private val TAG = SpotifyInterpreter::class.java.simpleName
    private val nlpInterpreter = NLPInterpreter(context)


    fun dispatchCall(resultsNLP: JsonObject): JsonObject {
        //Init:
        var returnJSON = JsonObject()
        var artistConfirmed = ""
        val playType = resultsNLP.get("type").asString

        //1) Call NLP Extractor:
        var matchExtracted = nlpInterpreter.extractMatches(type=playType, queryText=resultsNLP.get("query_text").asString.lowercase())
        val artistExtracted = matchExtracted.get("artist_extracted").asString

        //2) Double check DF artists with NLP Extractor:
        if (!matchExtracted.isEmpty) {
            val artistsNlp = resultsNLP.get("artists").asJsonArray
            if (artistsNlp.isEmpty) {
                //Confirm artists extracted by Extractor:
                artistConfirmed = artistExtracted
            } else {
                var artistsTemp = ArrayList<String>()
                //Match one by one the artists extracted by DF with those extracted by Extractor:
                var artist = ""
                for (artJs in artistsNlp) {
                    artist = artJs.asString
                    if (FuzzySearch.tokenSortRatio(artist, artistExtracted) >= matchThreshold) {
                        artistsTemp.add(artist)
                    }
                }
                //Priority to DF if matches confirmed:
                if (artistsTemp.isEmpty()) {
                    artistConfirmed = artistExtracted
                } else {
                    artistConfirmed = artistsTemp.joinToString(", ", "", "")
                }
            }
            matchExtracted.addProperty("artist_confirmed", artistConfirmed)
            //Add to log:
            last_log!!.add("nlp_extractor", matchExtracted)
            Log.d(TAG, "NLP EXTRACTOR RESULTS: $matchExtracted")

            //3) DISPATCH SPOTIFY CALLS ACCORDING TO NLP MATCHES EXTRACTED:
            var search = SpotifySearch()
            returnJSON = search.genericSearch(searchData=matchExtracted, getTwice=false)
        }
        return returnJSON
    }

    fun playInternally(resultJSON: JsonObject): Int {
        var ret = -1
        //BUILD PUT REQUEST:
        var url = "https://api.spotify.com/v1/me/player/play"
        var offset = JsonObject()  //Context
        offset.addProperty("uri", resultJSON.get("uri").asString)

        var jsonBody = JsonObject()
        jsonBody.addProperty("context_uri", resultJSON.get("context_uri").asString)
        jsonBody.add("offset", offset)
        jsonBody.addProperty("position_ms", 0)

        var body = jsonBody.toString().toRequestBody()

        var headers = Headers.Builder()
            .add("Authorization", "Bearer ${prefs.spotifyToken}")
            .add("Content-Type", "application/json")
            .build()

        var request = Request.Builder()
            .url(url)
            .put(body)
            .headers(headers)
            .build()

        try {
            runBlocking {
                var response = utils.makeRequest(client, request)
                if (response == "") {
                    Log.d(TAG, "PLAY INTERNALLY SUCCESS!")
                    ret = 0
                } else {
                    Log.d(TAG, "COULD NOT PLAY INTERNALLY.")
                    try {
                        var respJSON = JsonParser.parseString(response).asJsonObject
                        if (respJSON.has("error")) {
                            Log.d(TAG, "PLAY INTERNALLY: Error response: ${response}")
                        }
                    } catch (e:Exception) {
                        Log.d(TAG, "PLAY INTERNALLY: Could not parse response. Error: ", e)
                    }
                    ret = -1
                }
            }
            return ret
        } catch (e: Exception) {
            Log.d(TAG, "PLAY INTERNALLY ERROR: ", e)
            return -1
        }
    }

}