package com.ftrono.DJames.api

import com.google.gson.JsonObject
import android.util.Log
import android.content.Context
import com.ftrono.DJames.application.*
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class SpotifyInterpreter (private val context: Context) {
    private val TAG = SpotifyInterpreter::class.java.simpleName
    private var utils = Utilities()
    private val nlpInterpreter = NLPInterpreter(context)


    fun dispatchCall(resultsNLP: JsonObject, reqLanguage: String): JsonObject {
        //Init:
        var returnJSON = JsonObject()
        var artistConfirmed = ""
        var uri = ""
        //val intentName = resultsNLP.get("intent").asString

        //1) Call NLP Extractor:
        var matchExtracted = nlpInterpreter.extractMatches(queryText=resultsNLP.get("query_text").asString.lowercase(), reqLanguage=reqLanguage)
        //artist:
        var matchName = ""
        try {
            matchName = matchExtracted.get("match_extracted").asString
        } catch (e: Exception) {
            Log.d(TAG, "No match_extracted in nlpInterpreter.extractMatches()")
        }
        //artist:
        var artistExtracted = ""
        try {
            artistExtracted = matchExtracted.get("artist_extracted").asString
        } catch (e: Exception) {
            Log.d(TAG, "No artist_extracted in nlpInterpreter.extractMatches()")
        }
        //play type:
        var playType = "track"
        try {
            playType = matchExtracted.get("play_type").asString
            if (playType == "") {
                playType = "track"
            }
        } catch (e: Exception) {
            Log.d(TAG, "No play_type in nlpInterpreter.extractMatches()")
        }
        //liked songs:
        var contextLiked = false
        try {
            contextLiked = matchExtracted.get("context_liked").asBoolean
        } catch (e: Exception) {
            Log.d(TAG, "No context_liked in nlpInterpreter.extractMatches()")
        }

        //IF MATCHES:
        if (!matchExtracted.isEmpty) {
            //store play type:
            if (playType == "track") {
                returnJSON.addProperty("play_type", "track")
            } else {
                returnJSON.addProperty("play_type", playType)
            }

            //2) Double check DF artists with NLP Extractor:
            val artistsNlp = resultsNLP.get("artists").asJsonArray
            artistConfirmed = nlpInterpreter.checkArtists(artistsNlp, artistExtracted, reqLanguage=reqLanguage)
            matchExtracted.addProperty("artist_confirmed", artistConfirmed)

            //3) DISPATCH SPOTIFY CALLS ACCORDING TO NLP MATCHES EXTRACTED:
            if (playType == "playlist") {
                if (contextLiked) {
                    //PLAY -> Liked Songs:
                    Log.d(TAG, "PLAY -> Liked Songs")
                    returnJSON.addProperty("context_type", "playlist")
                    uri = likedSongsUri.replace("replaceUserId", prefs.spotUserId)
                    returnJSON.addProperty("uri", uri)
                    returnJSON.addProperty("context_uri", uri)
                    returnJSON.addProperty("context_name", "Liked Songs")
                    returnJSON.addProperty("spotify_URL", "${ext_format}collection/tracks")
                    last_log!!.addProperty("voc_score", 100)
                } else {
                    //PLAY -> Playlists in vocabulary:
                    var playlistMatch = nlpInterpreter.matchVocabulary("playlist", matchName, utils.getVocabulary("playlist"))
                    if (playlistMatch.has("text_confirmed")) {
                        Log.d(TAG, "PLAY -> Playlist in voc")
                        var contextConfirmed = playlistMatch.get("text_confirmed").asString
                        matchExtracted.addProperty("context_confirmed", contextConfirmed)
                        var playlistUrl = playlistMatch.get("detail_confirmed").asString
                        var playlistId = playlistUrl.split("/").last()
                        //PLAY -> Playlist:
                        returnJSON.addProperty("context_type", "playlist")
                        uri = "spotify:playlist:${playlistId}"
                        returnJSON.addProperty("uri", uri)
                        returnJSON.addProperty("context_uri", uri)
                        returnJSON.addProperty("context_name", contextConfirmed)
                        returnJSON.addProperty("spotify_URL", "${ext_format}$playType/$playlistId")
                    } else {
                        //PLAY -> Playlist not found: search in Spotify!
                        //SEARCH PLAYLIST:
                        var search = SpotifySearchPlaylists()
                        returnJSON = search.searchPlaylists(searchData = matchExtracted, bySpotify = false)

                        if (!returnJSON.isEmpty) {
                            Log.d(TAG, "PLAY -> Playlist found")
                            //PLAY -> Playlist:
                            returnJSON.addProperty("context_type", "playlist")
                            var id = returnJSON.get("id").asString
                            uri = "spotify:playlist:${id}"
                            returnJSON.addProperty("uri", uri)
                            returnJSON.addProperty("context_uri", uri)
                            returnJSON.addProperty("context_name", returnJSON.get("match_name").asString)
                            returnJSON.addProperty("spotify_URL", "${ext_format}playlist/$id")
                        } else {
                            //PLAY -> Playlist not found:
                            Log.d(TAG, "PLAY -> Playlist not found!")
                        }
                    }
                }
            } else if (playType == "artist") {
                //PLAY -> Artist: playlist "This is <artist name> by Spotify:
                //SEARCH PLAYLIST:
                var search = SpotifySearchPlaylists()
                returnJSON = search.searchPlaylists(searchData = matchExtracted, bySpotify = true)

                if (!returnJSON.isEmpty) {
                    Log.d(TAG, "PLAY -> Artist playlist found")
                    //PLAY -> Artist playlist:
                    returnJSON.addProperty("context_type", "playlist")
                    var id = returnJSON.get("id").asString
                    uri = "spotify:playlist:${id}"
                    returnJSON.addProperty("uri", uri)
                    returnJSON.addProperty("context_uri", uri)
                    returnJSON.addProperty("context_name", returnJSON.get("match_name").asString)
                    returnJSON.addProperty("spotify_URL", "${ext_format}playlist/$id")
                } else {
                    //PLAY -> Playlist not found:
                    Log.d(TAG, "PLAY -> Artist playlist not found!")
                }
            // TODO: ADD OTHER PLAY TYPE CASES HERE!
            } else {
                //SEARCH TRACKS + ALBUMS
                var search = SpotifySearch()
                returnJSON = search.searchTrackOrAlbum(searchData = matchExtracted, reqLanguage = reqLanguage)

                //4) CONTEXT:
                //context vars:
                var contextType = matchExtracted.get("context_type").asString
                var contextName = matchExtracted.get("context_extracted").asString

                //Match playlist name with voc:
                if (contextType != "album") {
                    if (contextLiked) {
                        //Context -> Liked Songs:
                        Log.d(TAG, "Context -> Liked Songs")
                        uri = likedSongsUri.replace("replaceUserId", prefs.spotUserId)
                        returnJSON.addProperty("context_type", "playlist")
                        returnJSON.addProperty("context_uri", uri)
                        returnJSON.addProperty("context_name", "Liked Songs")
                    } else {
                        //Check Playlists in vocabulary:
                        var playlistMatch = nlpInterpreter.matchVocabulary("playlist", contextName, utils.getVocabulary("playlist"))
                        if (playlistMatch.has("text_confirmed")) {
                            Log.d(TAG, "Context -> Playlist in voc")
                            var contextConfirmed = playlistMatch.get("text_confirmed").asString
                            matchExtracted.addProperty("context_confirmed", contextConfirmed)
                            var playlistUrl = playlistMatch.get("detail_confirmed").asString
                            var playlistId = playlistUrl.split("/").last()
                            //Context -> Playlist:
                            returnJSON.addProperty("context_type", "playlist")
                            uri = "spotify:playlist:${playlistId}"
                            returnJSON.addProperty("context_uri", uri)
                            returnJSON.addProperty("context_name", contextConfirmed)
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
        }
        //Add to log:
        last_log!!.add("nlp_extractor", matchExtracted)
        Log.d(TAG, "NLP EXTRACTOR RESULTS: $matchExtracted")
        return returnJSON
    }


    fun playInternally(resultJSON: JsonObject, useAlbum: Boolean = false): Int {
        var ret = -1
        //BUILD PUT REQUEST:
        var url = "https://api.spotify.com/v1/me/player/play"
        var playType = resultJSON.get("play_type").asString
        var offset = JsonObject()  //Context
        if (playType == "track") {
            //Start playing from the song:
            offset.addProperty("uri", resultJSON.get("uri").asString)
        } else {
            //Start playing from the beginning:
            offset.addProperty("position", 0)
        }

        var jsonBody = JsonObject()
        if (useAlbum && playType == "track") {
            //use album context:
            jsonBody.addProperty("context_uri", resultJSON.get("album_uri").asString)
        } else {
            //Use requested context:
            jsonBody.addProperty("context_uri", resultJSON.get("context_uri").asString)
        }
        jsonBody.add("offset", offset)
        jsonBody.addProperty("position_ms", 0)

        var body = jsonBody.toString().toRequestBody()

        //FIRST PUT REQUEST:
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
                //Log.d(TAG, response)
                try {
                    //Check if error 401:
                    var respJSON = JsonParser.parseString(response).asJsonObject
                    if (respJSON.has("error")) {
                        var errorJSON = respJSON.get("error").asJsonObject
                        var status = errorJSON.get("status").asString.toInt()
                        Log.d(TAG, "First Search answer: received error ${status}.")

                        //401 -> token expired!
                        if (status == 401) {
                            //Calling Refresh:
                            Log.d(TAG, "Refreshing token...")
                            var query = SpotifyQuery()
                            query.refreshAuth()

                            //SECOND PUT REQUEST:
                            headers = Headers.Builder()
                                .add("Authorization", "Bearer ${prefs.spotifyToken}")
                                .add("Content-Type", "application/json")
                                .build()

                            request = Request.Builder()
                                .url(url)
                                .put(body)
                                .headers(headers)
                                .build()

                            response = utils.makeRequest(client, request)
                            //Log.d(TAG, response)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "PlayInternally(): Response is not a JSON -> OK")
                }

                //CHECK RESPONSE:
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