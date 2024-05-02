package com.ftrono.DJames.api

import com.google.gson.JsonObject
import android.util.Log
import android.content.Context
import com.ftrono.DJames.application.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class SpotifyInterpreter (private val context: Context) {
    private val TAG = SpotifyInterpreter::class.java.simpleName
    private val nlpInterpreter = NLPInterpreter(context)


    fun dispatchCall(resultsNLP: JsonObject, reqLanguage: String): JsonObject {
        //Init:
        var returnJSON = JsonObject()
        var artistConfirmed = ""
        //val intentName = resultsNLP.get("intent").asString

        //1) Call NLP Extractor:
        var matchExtracted = nlpInterpreter.extractMatches(queryText=resultsNLP.get("query_text").asString.lowercase(), reqLanguage=reqLanguage)
        var artistExtracted = ""
        try {
            artistExtracted = matchExtracted.get("artist_extracted").asString
        } catch (e: Exception) {
            Log.d(TAG, "No artist_extracted in nlpInterpreter.extractMatches()")
        }

        //2) Double check DF artists with NLP Extractor:
        if (!matchExtracted.isEmpty) {
            val artistsNlp = resultsNLP.get("artists").asJsonArray
            artistConfirmed = nlpInterpreter.checkArtists(artistsNlp, artistExtracted, reqLanguage=reqLanguage)
            matchExtracted.addProperty("artist_confirmed", artistConfirmed)
            //Add to log:
            last_log!!.add("nlp_extractor", matchExtracted)
            Log.d(TAG, "NLP EXTRACTOR RESULTS: $matchExtracted")

            //3) DISPATCH SPOTIFY CALLS ACCORDING TO NLP MATCHES EXTRACTED:
            var search = SpotifySearch()
            returnJSON = search.genericSearch(searchData=matchExtracted, reqLanguage=reqLanguage)

            //4) CONTEXT:
            //context vars:
            var contextType = matchExtracted.get("context_type").asString
            var contextLiked = matchExtracted.get("context_liked").asBoolean
            var contextName = matchExtracted.get("context_extracted").asString

            //Match playlist name with voc:
            if (contextType != "album") {
                if (contextLiked) {
                    //Context -> Liked Songs:
                    Log.d(TAG, "Context -> Liked Songs")
                    returnJSON.addProperty("context_type", "playlist")
                    returnJSON.addProperty("context_uri", likedSongsUri.replace("replaceUserId", prefs.spotUserId))
                    returnJSON.addProperty("context_name", "Liked Songs")
                } else {
                    //Check Playlists in vocabulary:
                    var playlistMatch = nlpInterpreter.matchVocabulary("playlist", contextName, utils.getVocabulary("playlist"))
                    if (playlistMatch.has("text_confirmed")) {
                        Log.d(TAG, "Context -> Playlist in voc")
                        var playlistUrl = playlistMatch.get("detail_confirmed").asString
                        var playlistId = playlistUrl.split("/").last()
                        //Context -> Playlist:
                        returnJSON.addProperty("context_type", "playlist")
                        returnJSON.addProperty("context_uri", "spotify:playlist:${playlistId}")
                        returnJSON.addProperty("context_name", playlistMatch.get("text_confirmed").asString)
                    } else {
                        //Playlist not found -> Context -> album:
                        Log.d(TAG, "Context -> album")
                        returnJSON.addProperty("context_type", "album")
                        returnJSON.addProperty("context_uri", returnJSON.get("album_uri").asString)
                        returnJSON.addProperty("context_name", returnJSON.get("album_name").asString)
                    }
                }
            } else {
                //Context -> album:
                Log.d(TAG, "Context -> album")
                returnJSON.addProperty("context_type", "album")
                returnJSON.addProperty("context_uri", returnJSON.get("album_uri").asString)
                returnJSON.addProperty("context_name", returnJSON.get("album_name").asString)
            }

        }
        return returnJSON
    }


    fun playInternally(resultJSON: JsonObject, useAlbum: Boolean = false): Int {
        var ret = -1
        //BUILD PUT REQUEST:
        var url = "https://api.spotify.com/v1/me/player/play"
        var offset = JsonObject()  //Context
        offset.addProperty("uri", resultJSON.get("uri").asString)

        var jsonBody = JsonObject()
        if (!useAlbum) {
            //Use requested context:
            jsonBody.addProperty("context_uri", resultJSON.get("context_uri").asString)
        } else {
            //use album context:
            jsonBody.addProperty("context_uri", resultJSON.get("album_uri").asString)
        }
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
                    Log.d(TAG, "PLAY INTERNALLY: request sent.")
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

    //Get Playback state:
    fun getPlaybackState(): Int {
        var ret = -1
        //BUILD GET REQUEST:
        var url = "https://api.spotify.com/v1/me/player"

        var headers = Headers.Builder()
            .add("Authorization", "Bearer ${prefs.spotifyToken}")
            .build()

        var request = Request.Builder()
            .url(url)
            .headers(headers)
            .build()

        try {
            runBlocking {
                var response = utils.makeRequest(client, request)
                Log.d(TAG, response.toString())
                if (response == "") {
                    //204: WRONG CONTEXT:
                    Log.d(TAG, "PLAYBACK STATE: 204")
                    ret = 204
                } else {
                    try {
                        var respJSON = JsonParser.parseString(response).asJsonObject
                        if (respJSON.has("item")) {
                            if (respJSON.get("item").toString() != "null") {
                                //200: CONTEXT OK:
                                Log.d(TAG, "PLAYBACK STATE: 200")
                                ret = 200
                            } else {
                                Log.d(TAG, "PLAYBACK STATE: Error response")
                            }
                        } else {
                            Log.d(TAG, "PLAYBACK STATE: Error response")
                        }
                    } catch (e:Exception) {
                        Log.d(TAG, "PLAYBACK STATE: Could not parse response. Error: ", e)
                    }
                }
            }
            return ret
        } catch (e: Exception) {
            Log.d(TAG, "PLAYBACK STATE ERROR: ", e)
            return -1
        }
    }

}