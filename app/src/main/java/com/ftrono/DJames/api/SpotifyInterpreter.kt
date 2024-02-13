package com.ftrono.DJames.api

import com.google.gson.JsonObject
import android.util.Log
import com.ftrono.DJames.application.client
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class SpotifyInterpreter {
    private val TAG = SpotifyInterpreter::class.java.simpleName


    //NOTE: To be called only if Intent HAS a song name inside!
    fun extractMatchName(type: String, queryText: String, artistName: String, removeArtist: Boolean = false): ArrayList<String> {
        var matchName = ""
        var artistExtracted = ""
        var theType = false
        var findArtist = ArrayList<String>()
        findArtist.add("by the artist")
        findArtist.add("from the artist")

        //1) Slice string starting from "Play":
        var playStr = "play the $type "
        var playInd = queryText.indexOf(playStr, ignoreCase = true)
        if (playInd > -1) {
            theType = true
        } else {
            //Just "Play":
            playStr = "play "
            playInd = queryText.indexOf(playStr, ignoreCase = true)
        }
        if (playInd > -1) {
            Log.d(TAG, "PLAY INDEX: $playInd")
            playInd += playStr.length
            var textFromPlay = queryText.slice(playInd..queryText.lastIndex).strip()
            Log.d(TAG, "PLAY INDEX INCREASED: $playInd")
            Log.d(TAG, "TEXT FROM PLAY: $textFromPlay")

            matchName = textFromPlay
            if (removeArtist) {
                //2) Find artist prelude:
                var preludeInd = -1
                for (sent in findArtist) {
                    preludeInd = textFromPlay.indexOf(sent, ignoreCase = true)
                    if (preludeInd > -1) {
                        break
                    }
                }
                if (preludeInd > -1) {
                    //Cut:
                    matchName = textFromPlay.slice(0..preludeInd).strip()
                    Log.d(TAG, "PRELUDE INDEX: $preludeInd")
                    Log.d(TAG, "TEXT UP TO PRELUDE: $matchName")
                } else if (artistName != "") {
                    //3.A) Check artistName:
                    var artistInd = textFromPlay.indexOf(artistName, ignoreCase = true)
                    Log.d(TAG, "ARTIST INDEX: $artistInd")
                    if (artistInd > -1) {
                        //Check if previous word is "by":
                        var byInd = textFromPlay.indexOf("by", ignoreCase = true)
                        Log.d(TAG, "BY INDEX: $byInd")
                        if (byInd == (artistInd - 3)) {
                            artistInd -= 3
                            Log.d(TAG, "ARTIST INDEX CORRECTED: $artistInd")
                        }
                        matchName = textFromPlay.slice(0..(artistInd - 1)).strip()
                        Log.d(TAG, "TEXT UP TO ARTIST: $matchName")
                    }
                } else {
                    //3.B) Find word "by":
                    var byInd = textFromPlay.indexOf("by", ignoreCase = true)
                    Log.d(TAG, "BY INDEX: $byInd")
                    if (byInd > -1) {
                        var artistInd = byInd + 3
                        Log.d(TAG, "ARTIST INDEX FOUND: $artistInd")
                        artistExtracted = textFromPlay.slice(artistInd .. textFromPlay.lastIndex).strip()
                        Log.d(TAG, "ARTIST FOUND: $artistExtracted")
                    }
                    matchName = textFromPlay.slice(0..(byInd - 1)).strip()
                    Log.d(TAG, "TEXT UP TO ARTIST: $matchName")
                }
            }
        }
        try {
            var test = matchName.slice((matchName.lastIndex-6)..(matchName.lastIndex)).strip()
            Log.d(TAG, "TEST: $test")
            if (test.lowercase() == "by the") {
                var matchName2 = matchName.slice(0..(matchName.lastIndex-6)).strip()
                matchName = matchName2
            }
        } catch (e: Exception) {
            Log.d(TAG, "Shorter matchName.")
        }
        if (matchName == "" && theType) {
            matchName = "the $type"
        }
        Log.d(TAG, "CLEANED MATCH NAME: $matchName")
        var retArray = ArrayList<String>()
        retArray.add(matchName)
        retArray.add(artistExtracted)
        return retArray
    }

    fun dispatchCall(resultsNLP: JsonObject): JsonObject {
        //TEMP:
        var type = resultsNLP.get("type").asString
        var artistName = resultsNLP.get("artist").asString
        var matchArray = extractMatchName(type=type, queryText=resultsNLP.get("query_text").asString, artistName=artistName, removeArtist=true)
        var matchName = matchArray.get(0)
        if (artistName == "") {
            artistName = matchArray.get(1)
        }
        //DISPATCH SPOTIFY CALLS ACCORDING TO NLP RESULTS:
        var search = SpotifySearch()
        var returnJSON = search.genericSearch(type=type, matchName=matchName, artistName=artistName)
        return returnJSON
    }

    fun playInternally(resultJSON: JsonObject): Int {
        var ret = -1
        //BUILD PUT REQUEST:
        var url = "https://api.spotify.com/v1/me/player/play"
        var offset = JsonObject()  //Context
        offset.addProperty("uri", resultJSON.get("uri").asString)

        var jsonBody = JsonObject()
        jsonBody.add("context_uri", resultJSON.get("context_uri"))
        jsonBody.add("offset", offset)
        jsonBody.add("position_ms", JsonPrimitive(0))

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