package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.ftrono.DJames.application.prefs
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URLEncoder
import com.google.gson.JsonPrimitive
import me.xdrop.fuzzywuzzy.FuzzySearch


class SpotifySearch() {
    private val TAG = SpotifySearch::class.java.simpleName
    private var query = SpotifyQuery()

    //GENERIC SPOTIFY SEARCH -> PLAY WITHIN ALBUM:
    fun genericSearch(type: String, matchName: String, artistName: String, getTwice: Boolean = false): JsonObject {
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
        qParams.add("${type}:${matchName}")
        if (artistName != "") {
            qParams.add("artist:${artistName}")
        }

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //FIRST REQUEST:
        //Query params:
        var url1 = url
        if (qParams.isNotEmpty()) {
            var queryParams = qParams.joinToString("&", prefix = "&")
            val encodedParams: String = URLEncoder.encode(queryParams, "UTF-8").replace("%26", "%20").replace("%3A", ":")
            url1 += "?q=${encodedMatchName}${encodedParams}&type=${type}&limit=10&market=IT"
        } else {
            url1 += "?q=${encodedMatchName}&type=${type}&limit=10&market=IT"
        }
        Log.d(TAG, url1)

        var respJSON = query.querySpotify(type="get", url=url1, jsonHeads=jsonHeads)
        var keySet = respJSON.keySet()
        if (keySet.size == 0) {
            Log.d(TAG, "ERROR: Spotify Search results not received!!")
            //Log.d(TAG, "returnJSON: ${returnJSON}")
        } else {
            //Analyse response & get index of best result:
            var tracks = respJSON.getAsJsonObject("tracks")
            items = tracks.getAsJsonArray("items")
        }

        //SECOND REQUEST:
        //Query params:
        var url2 = url
        if (artistName != "") {
            val encodedArtistName: String = URLEncoder.encode(artistName, "UTF-8")
            url2 += "?q=${encodedMatchName}+by+${encodedArtistName}&type=${type}&limit=10&market=IT"
        } else {
            url2 += "?q=${encodedMatchName}&type=${type}&limit=10&market=IT"
        }
        Log.d(TAG, url2)

        if (getTwice && url2 != url1) {
            respJSON = query.querySpotify(type = "get", url = url2, jsonHeads = jsonHeads)
            keySet = respJSON.keySet()
            if (keySet.size == 0) {
                Log.d(TAG, "ERROR: Spotify Search results not received!!")
                //Log.d(TAG, "returnJSON: ${returnJSON}")
            } else {
                //Analyse response & get index of best result:
                var tracks = respJSON.getAsJsonObject("tracks")
                items2 = tracks.getAsJsonArray("items")
            }

            //MERGE RESULTS:
            for (item in items2) {
                if (!items.contains(item)) {
                    items.add(item)
                }
            }
        }
        if (items.size() == 0) {
            Log.d(TAG, "ERROR: Spotify Search results not received!!")
            //Log.d(TAG, "returnJSON: ${returnJSON}")
            return returnJSON
        } else {
            //GET BEST RESULT:
            var bestResult = getBestResult(items, matchName, artistName, live)

            //Uri:
            var uri = bestResult.get("uri").asString
            returnJSON.add("uri", JsonPrimitive(uri))
            Log.d(TAG, "uri: ${uri}")

            //Spotify URL:
            var extUrls = bestResult.getAsJsonObject("external_urls")
            var spotifyURL = extUrls.get("spotify").asString
            returnJSON.add("spotify_URL", JsonPrimitive(spotifyURL))
            Log.d(TAG, "spotify_URL: ${spotifyURL}")

            //Track name:
            returnJSON.add("song_name", bestResult.get("name"))

            //Artist name:
            var artists = bestResult.getAsJsonArray("artists")
            var firstArtist = artists.get(0).asJsonObject
            returnJSON.add("artist_name", firstArtist.get("name"))

            //CONTEXT:
            //Album name:
            var album = bestResult.get("album").asJsonObject
            returnJSON.add("album_name", album.get("name"))
            returnJSON.add("album_uri", album.get("uri"))

            //(TEMP) Context -> album:
            returnJSON.add("context_type", JsonPrimitive("Album"))
            returnJSON.add("context_uri", album.get("uri"))
            returnJSON.add("context_name", album.get("name"))

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
        //QUERYSPOTIFY() MUST RETURN ORIGINAL PAYLOAD, NOT AS JSONOBJECT!!!!!!!!!!!!!
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
        var bestInd = sortedScores.keys.elementAt(0)
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
        //GET FULL BEST JSON:
        bestResult = items.get(bestInd).asJsonObject
        Log.d(TAG, "BEST RESULT: INDEX $bestInd, ITEM: $bestResult")
        return bestResult
    }

}
