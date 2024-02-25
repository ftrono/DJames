package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.ftrono.DJames.application.uri_format
import com.ftrono.DJames.application.ext_format
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.prefs
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URLEncoder
import me.xdrop.fuzzywuzzy.FuzzySearch


class SpotifySearch() {
    private val TAG = SpotifySearch::class.java.simpleName
    private var query = SpotifyQuery()

    //GENERIC SPOTIFY SEARCH -> PLAY WITHIN ALBUM:
    fun genericSearch(searchData: JsonObject, getTwice: Boolean = false): JsonObject {
        //vars:
        var type = "track"   //searchData.get("play_type").asString
        var matchName = searchData.get("match_extracted").asString
        var artistName = searchData.get("artist_confirmed").asString

        //BUILD GET REQUEST:
        var baseURL = "https://api.spotify.com/v1/search"
        val encodedMatchName: String = URLEncoder.encode(matchName, "UTF-8")

        //Check if LIVE to be preferred or not:
        var live = false
        for (tok in matchName.split(" ")) {
            if (tok.lowercase() == "live") {
                live = true
                break
            }
        }

        var returnJSON = JsonObject()
        var url = baseURL
        var items = JsonArray()
        var items2 = JsonArray()

        //Extract query params:
        var qParams = ArrayList<String>()
        if (artistName != "") {
            qParams.add("${type}:${matchName}")
            qParams.add("artist:${artistName}")
        }

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //FIRST REQUEST:
        //Compose query:
        var url1 = url
        if (qParams.isNotEmpty()) {
            var queryParams = qParams.joinToString("&", prefix = "&")
            val encodedParams: String =
                URLEncoder.encode(queryParams, "UTF-8").replace("%26", "%20").replace("%3A", ":")
            url1 += "?q=${encodedMatchName}${encodedParams}&type=${type}&limit=10&market=IT"
        } else {
            url1 += "?q=${encodedMatchName}&type=${type}&limit=10&market=IT"
        }
        Log.d(TAG, url1)

        //First query (uses Params):
        var respJSON = query.querySpotify(type = "get", url = url1, jsonHeads = jsonHeads)
        Log.d(TAG, respJSON.toString())
        var n_items = 0
        var keySet = respJSON.keySet()
        if (keySet.size == 0) {
            Log.d(TAG, "ERROR: Spotify Search results not received!!")
            //Log.d(TAG, "returnJSON: ${returnJSON}")
        } else {
            //Analyse response & get index of best result:
            var tracks = respJSON.getAsJsonObject("tracks")
            items = tracks.getAsJsonArray("items")
            n_items = items.size()
        }
        //Log:
        var logQueries = JsonArray()
        var logJSON = JsonObject()
        logJSON.addProperty("type", "genericSearch")
        logJSON.addProperty("url", url1)
        logJSON.addProperty("n_items", n_items)
        logQueries.add(logJSON)

        //SECOND REQUEST:
        var url2 = url
        if (getTwice || items.isEmpty()) {
            //Compose query:
            if (artistName != "") {
                val encodedArtistName: String = URLEncoder.encode(artistName, "UTF-8")
                url2 += "?q=${encodedMatchName}+by+${encodedArtistName}&type=${type}&limit=10&market=IT"
            } else {
                url2 += "?q=${encodedMatchName}&type=${type}&limit=10&market=IT"
            }

            //Second query:
            if (url2 != url1) {
                Log.d(TAG, url2)
                respJSON = query.querySpotify(type = "get", url = url2, jsonHeads = jsonHeads)
                n_items = 0
                keySet = respJSON.keySet()
                if (keySet.size == 0) {
                    Log.d(TAG, "ERROR: Spotify Search results not received!!")
                    //Log.d(TAG, "returnJSON: ${returnJSON}")
                } else {
                    //Analyse response & get index of best result:
                    var tracks = respJSON.getAsJsonObject("tracks")
                    items2 = tracks.getAsJsonArray("items")
                    n_items = items2.size()
                }
                //Log:
                var logJSON = JsonObject()
                logJSON.addProperty("type", "genericSearch")
                logJSON.addProperty("url", url2)
                logJSON.addProperty("n_items", n_items)
                logQueries.add(logJSON)

                //MERGE RESULTS:
                for (item in items2) {
                    if (!items.contains(item)) {
                        items.add(item)
                    }
                }
            }
        }
        last_log!!.add("spotify_queries", logQueries)
        if (items.size() == 0) {
            Log.d(TAG, "ERROR: Spotify Search results not received!!")
            //Log.d(TAG, "returnJSON: ${returnJSON}")
            return returnJSON
        } else {
            //GET BEST RESULT:
            var bestResult = getBestResult(items, matchName, artistName, live)

            //ID & uri:
            var id = bestResult.get("id").asString
            returnJSON.addProperty("id", id)
            returnJSON.addProperty("uri", "$uri_format$id")
            returnJSON.addProperty("spotify_URL", "$ext_format$id")

            //Track name:
            returnJSON.add("song_name", bestResult.get("name"))

            //Artist name:
            var artists = bestResult.getAsJsonArray("artists")
            var firstArtist = artists.get(0).asJsonObject
            returnJSON.add("artist_name", firstArtist.get("name"))

            //CONTEXT:
            //Album name:
            var album = bestResult.get("album").asJsonObject
            returnJSON.addProperty("album_name", album.get("name").asString)
            returnJSON.addProperty("album_uri", album.get("uri").asString)

            //(TEMP) Context -> album:
            returnJSON.addProperty("context_type", "Album")
            returnJSON.addProperty("context_uri", album.get("uri").asString)
            returnJSON.addProperty("context_name", album.get("name").asString)

            //Artwork:
            if (album.has("images")) {
                try {
                    var images = album.getAsJsonArray("images")
                    var firstImage = images.get(0).asJsonObject
                    returnJSON.add("artwork", firstImage.get("url"))
                } catch (e: Exception) {
                    Log.d(TAG, "Unable to retrieve album artwork: ", e)
                }
            }

            Log.d(TAG, "Spotify Search results successfully processed!")
            Log.d(TAG, "returnJSON: ${returnJSON}")
            return returnJSON
        }
    }


    //GENERIC SPOTIFY SEARCH -> PLAY WITHIN ALBUM:
    fun checkSaved(ids: ArrayList<String>): JsonArray {
        //BUILD GET REQUEST:
        var url = "https://api.spotify.com/v1/me/tracks/contains?ids="

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //FIRST REQUEST:
        //Query params:
        url += ids.joinToString(",", "", "")
        Log.d(TAG, url)

        var saved = query.querySpotifyArray(type = "get", url = url, jsonHeads = jsonHeads)
        return saved
    }

    //Spotify: get Best Result:
    fun getBestResult(items: JsonArray, matchName: String, artistName: String, live: Boolean): JsonObject {
        var bestResult = JsonObject()
        //Analyse Spotify query result:
        //GET BEST RESULT:
        var c = 0
        var matchesArray = ArrayList<JsonObject>()
        var ids = ArrayList<String>()
        var scoresMap = mutableMapOf<Int, Int>()
        for (item in items) {
            var scoreJson = JsonObject()
            var currItem = item.asJsonObject
            //Key info:
            val re = Regex("[^A-Za-z0-9 ]")
            var name = re.replace(currItem.get("name").asString, "")
            scoreJson.addProperty("name", name)
            scoreJson.addProperty("albumType", currItem.get("album").asJsonObject.get("album_type").asString)
            ids.add(currItem.get("id").asString)
            //Artists name:
            var foundArtists = ArrayList<String>()
            var artists = currItem.getAsJsonArray("artists")
            for (artist in artists) {
                foundArtists.add(artist.asJsonObject.get("name").asString)
            }
            scoreJson.addProperty("artists", foundArtists.joinToString(", ", "", ""))
            //calculate similarity:
            scoreJson.addProperty("nameSimilarity", FuzzySearch.tokenSetRatio(scoreJson.get("name").asString, matchName))
            scoreJson.addProperty("artistSimilarity", FuzzySearch.tokenSetRatio(scoreJson.get("artists").asString, artistName))
            var score = scoreJson.get("nameSimilarity").asInt + scoreJson.get("artistSimilarity").asInt
            //Check if live:
            for (tok in name.split(" ")) {
                if (tok.lowercase() == "live") {
                    score -= deltaSimilarity
                    break
                }
            }
            scoreJson.addProperty("score", score)
            scoresMap[c] = score
            matchesArray.add(scoreJson)
            Log.d(TAG, scoreJson.toString())
            c += 1
        }

        //Check saved:
        var saved = checkSaved(ids)
        Log.d(TAG, "Check saved: $saved")
        if (saved.size() > 0) {
            //add Saved info:
            var i = 0
            for (el in matchesArray) {
                el.addProperty("saved", saved[i].asBoolean)
                i ++
            }
        } else {
            //set all tracks to Not Saved:
            for (el in matchesArray) {
                el.addProperty("saved", false)
            }
        }

        //Sort map:
        Log.d(TAG, "MAP: $scoresMap")
        val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap()
        Log.d(TAG, "SORTED MAP: $sortedScores")
        //Default best Ind is 0 (max score):
        var bestInd = 0
        //Exclude lower items:
        var scoreThreshold = sortedScores.values.elementAt(0) - deltaSimilarity
        if (scoreThreshold < 0) {
            scoreThreshold = 0
        }
        var bestScores = sortedScores.filter { (key, value) -> value >= scoreThreshold}
        Log.d(TAG, "FILTERED MAP: $bestScores")
        //Get album (if present):
        var bestType = ""
        for (k in bestScores.keys) {
            var result = matchesArray[k]
            //A) If SAVED:
            if (result.get("saved").asBoolean) {
                //PRIORITY 1) If album -> BEST FOUND -> STOP!
                if (result.get("albumType").asString == "album") {
                    bestInd = k
                    break
                //PRIORITY 2) If just saved & not a best saved track found before -> update best:
                } else if (bestInd == 0) {
                    bestInd = k
                    bestType = "saved"
                }
            //PRIORITY 3) If just album & not a saved track found before -> update best:
            } else if (bestInd == 0 && bestType != "saved" && result.get("albumType").asString == "album") {
                bestInd = k
                bestType = "album"
            }
        }
        //Log:
        var bestOnes = JsonArray()
        for (k in bestScores.keys) {
            bestOnes.add(matchesArray[k])
        }
        last_log!!.add("spotify_matches", bestOnes)
        last_log!!.addProperty("best_score", sortedScores[bestInd])
        //GET FULL BEST JSON:
        bestResult = items.get(bestInd).asJsonObject
        Log.d(TAG, "BEST RESULT: INDEX $bestInd, ITEM: $bestResult")
        return bestResult
    }

}
