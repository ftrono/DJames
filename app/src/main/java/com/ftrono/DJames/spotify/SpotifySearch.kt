package com.ftrono.DJames.spotify

import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.ftrono.DJames.application.ext_format
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.playThreshold
import com.ftrono.DJames.application.prefs
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URLEncoder
import me.xdrop.fuzzywuzzy.FuzzySearch
import kotlin.math.roundToInt


class SpotifySearch() {
    private val TAG = SpotifySearch::class.java.simpleName
    private var query = SpotifyQuery()

    //SEARCH TRACKS OR ALBUMS:
    fun searchTrackOrAlbum(searchData: JsonObject): JsonObject {
        //vars:
        var type = searchData.get("play_type").asString
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

        //Tools:
        var returnJSON = JsonObject()
        var url = baseURL
        var items = JsonArray()
        var items2 = JsonArray()
        var bestMatches = JsonArray()
        var bestInd = 0
        var bestScore = 0
        var bestResult = JsonObject()

        //Log:
        var logQueries = JsonArray()
        var logJSON = JsonObject()

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
            url1 += "?q=${encodedMatchName}${encodedParams}&type=${type}&limit=10&market=${prefs.spotCountry}"
        } else {
            url1 += "?q=${encodedMatchName}&type=${type}&limit=10&market=${prefs.spotCountry}"
        }
        Log.d(TAG, url1)

        //First query (uses Params):
        var respJSON = query.querySpotify(type = "get", url = url1, jsonHeads = jsonHeads)
        var n_items = 0
        //Log:
        logJSON.addProperty("type", "searchTrackOrAlbum")
        logJSON.addProperty("url", url1)
        //Check content:
        var keySet = respJSON.keySet()
        if (keySet.size == 0) {
            //Empty:
            logJSON.addProperty("n_items", n_items)
            Log.d(TAG, "ERROR: Spotify Search results not received!!")
        } else {
            //Analyse response & get index of best result:
            items = respJSON.getAsJsonObject("${type}s").getAsJsonArray("items")
            n_items = items.size()
            logJSON.addProperty("n_items", n_items)
            //Get best score:
            if (n_items > 0) {
                bestMatches = getBestMatches(items, type, matchName, artistName, live)
                logJSON.add("spotify_matches", bestMatches)
                //Best:
                bestInd = bestMatches[0].asJsonObject.get("pos").asInt
                bestScore = bestMatches[0].asJsonObject.get("score").asInt
                bestResult = items.get(bestInd).asJsonObject
            }
        }
        //Log:
        logQueries.add(logJSON)

        //SECOND REQUEST:
        var url2 = url
        if (bestScore <= playThreshold || items.isEmpty()) {
            //Compose query:
            if (artistName != "") {
                val encodedArtistName: String = URLEncoder.encode(artistName, "UTF-8")
                url2 += "?q=${encodedMatchName}+by+${encodedArtistName}&type=${type}&limit=10&market=${prefs.spotCountry}"
            } else {
                url2 += "?q=${encodedMatchName}&type=${type}&limit=10&market=${prefs.spotCountry}"
            }

            //Second query:
            if (url2 != url1) {
                Log.d(TAG, url2)
                respJSON = query.querySpotify(type = "get", url = url2, jsonHeads = jsonHeads)
                n_items = 0
                //Log:
                logJSON = JsonObject()
                logJSON.addProperty("type", "searchTrackOrAlbum")
                logJSON.addProperty("url", url2)
                //Check content:
                keySet = respJSON.keySet()
                if (keySet.size == 0) {
                    //Empty:
                    logJSON.addProperty("n_items", n_items)
                    Log.d(TAG, "ERROR: Spotify Search results not received!!")
                } else {
                    //Analyse response & get index of best result:
                    items2 = respJSON.getAsJsonObject("${type}s").getAsJsonArray("items")
                    n_items = items2.size()
                    logJSON.addProperty("n_items", n_items)
                    //Get best score:
                    if (n_items > 0) {
                        bestMatches = getBestMatches(items2, type, matchName, artistName, live)
                        logJSON.add("spotify_matches", bestMatches)
                        //Best:
                        var bestScore2 = bestMatches[0].asJsonObject.get("score").asInt
                        if (bestScore2 > bestScore) {
                            //Overwrite global best:
                            bestInd = bestMatches[0].asJsonObject.get("pos").asInt
                            bestScore = bestScore2
                            bestResult = items2.get(bestInd).asJsonObject
                        }
                    }
                }
                //Log:
                logQueries.add(logJSON)
            }
        }
        last_log!!.add("spotify_queries", logQueries)
        last_log!!.addProperty("best_score", bestScore)

        //EXTRACT INFO:
        if (!bestResult.isEmpty) {
            Log.d(TAG, "BEST RESULT: INDEX $bestInd, ITEM: $bestResult")
            returnJSON.addProperty("play_type", type)

            //ID & uri:
            var id = bestResult.get("id").asString
            returnJSON.addProperty("id", id)
            returnJSON.addProperty("uri", "spotify:$type:$id")
            returnJSON.addProperty("spotify_URL", "${ext_format}$type/$id")

            //Item name:
            returnJSON.add("match_name", bestResult.get("name"))

            //Artist name:
            var artists = bestResult.getAsJsonArray("artists")
            var firstArtist = artists.get(0).asJsonObject
            returnJSON.add("artist_name", firstArtist.get("name"))

            //Album name:
            if (type == "album") {
                returnJSON.addProperty("album_type", bestResult.get("album_type").asString)
                returnJSON.addProperty("album_name", bestResult.get("name").asString)
                returnJSON.addProperty("album_uri", bestResult.get("uri").asString)
            } else {
                var album = bestResult.get("album").asJsonObject
                returnJSON.addProperty("album_type", album.get("album_type").asString)
                returnJSON.addProperty("album_name", album.get("name").asString)
                returnJSON.addProperty("album_uri", album.get("uri").asString)
            }

            Log.d(TAG, "Spotify Item Search results successfully processed!")
            Log.d(TAG, "returnJSON: ${returnJSON}")
        }
        return returnJSON
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
    fun getBestMatches(items: JsonArray, type: String, matchName: String, artistName: String, live: Boolean): JsonArray {
        //Analyse Spotify query result:
        //GET BEST RESULT:
        var c = 0
        var matchesArray = ArrayList<JsonObject>()
        var ids = ArrayList<String>()
        var scoresMap = mutableMapOf<Int, Int>()
        for (item in items) {
            var scoreJson = JsonObject()
            scoreJson.addProperty("pos", c)
            var currItem = item.asJsonObject
            //Key info:
            val re = Regex("[^A-Za-z0-9 ]")
            var name = re.replace(currItem.get("name").asString, "")
            scoreJson.addProperty("name", name)
            if (type == "album") {
                scoreJson.addProperty("albumType", currItem.get("album_type").asString)
            } else {
                scoreJson.addProperty("albumType", currItem.get("album").asJsonObject.get("album_type").asString)
                scoreJson.addProperty("albumName", currItem.get("album").asJsonObject.get("name").asString)
            }
            ids.add(currItem.get("id").asString)
            //Artists name:
            var foundArtists = ArrayList<String>()
            var artists = currItem.getAsJsonArray("artists")
            for (artist in artists) {
                foundArtists.add(artist.asJsonObject.get("name").asString)
            }
            scoreJson.addProperty("artists", foundArtists.joinToString(", ", "", ""))

            //calculate similarity:
            //var intersection = 0
            var nameSet = 0
            var namePartial = 0
            var nameFull = 0
            var artistSet = 0
            var artistPartial = 0
            var sumScore = 0
            var score = 0

            //TODO: always update n_metrics below!
            if (artistName == "") {
                var curToMatch = scoreJson.get("name").asString + " " + scoreJson.get("artists").asString
                nameSet = FuzzySearch.tokenSetRatio(curToMatch, matchName)
                namePartial = FuzzySearch.partialRatio(curToMatch, matchName)
                nameFull = FuzzySearch.ratio(curToMatch, matchName)
                //intersection = utils.countIntersection(toMatch = curToMatch, target = matchName)
                sumScore = listOf<Int>(nameSet, namePartial, nameFull).sum()
                score = listOf<Int>(nameSet, namePartial, nameFull).average().roundToInt()
            } else {
                nameSet = FuzzySearch.tokenSetRatio(scoreJson.get("name").asString, matchName)
                namePartial = FuzzySearch.partialRatio(scoreJson.get("name").asString, matchName)
                nameFull = FuzzySearch.ratio(scoreJson.get("name").asString, matchName)
                artistSet = FuzzySearch.tokenSetRatio(scoreJson.get("artists").asString, artistName)
                artistPartial = FuzzySearch.partialRatio(scoreJson.get("artists").asString, artistName)
                // artistFull = FuzzySearch.ratio(scoreJson.get("artists").asString, artistName)
                sumScore = listOf<Int>(nameSet, namePartial, nameFull, artistSet, artistPartial).sum()
                score = listOf<Int>(nameSet, namePartial, nameFull, artistSet, artistPartial).average().roundToInt()
            }

            //scoreJson.addProperty("intersection", intersection)
            scoreJson.addProperty("nameSetSimilarity", nameSet)
            scoreJson.addProperty("namePartialSimilarity", namePartial)
            scoreJson.addProperty("nameFullSimilarity", nameFull)
            scoreJson.addProperty("artistSetSimilarity", artistSet)
            scoreJson.addProperty("artistPartialSimilarity", artistPartial)
//            scoreJson.addProperty("artistFullSimilarity", artistFull)

            //Check if live:
            for (tok in name.split(" ")) {
                if (!live && tok.lowercase() == "live") {
                    score -= deltaSimilarity
                    sumScore -= deltaSimilarity
                    break
                }
            }
            scoreJson.addProperty("score", score)
            scoreJson.addProperty("sum_score", sumScore)
            scoresMap[c] = sumScore
            matchesArray.add(scoreJson)
            Log.d(TAG, scoreJson.toString())
            c += 1
        }

        //Check saved:
        if (type == "track") {
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
        } else {
            //set all items to Not Saved:
            for (el in matchesArray) {
                el.addProperty("saved", false)
            }
        }

        //TODO: n_metrics to update always:
        var n_metrics = 5
        if (artistName == "") {
            n_metrics = 3
        }

        //Sort map:
        Log.d(TAG, "MAP: $scoresMap")
        val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap().mapValues {  it.value / n_metrics }
        Log.d(TAG, "SORTED MAP: $sortedScores")
        //Exclude lower items:
        var scoreThreshold = sortedScores.values.elementAt(0) - deltaSimilarity
        if (scoreThreshold < 0) {
            scoreThreshold = 0
        }
        var bestScores = sortedScores.filter { (key, value) -> value >= scoreThreshold}
        Log.d(TAG, "FILTERED MAP: $bestScores")
        //Default best Ind is the first (max score):
        var bestInd = sortedScores.keys.elementAt(0)

        //Get saved & album (if present):
        var bestType = ""
        var albumFound = false
        for (k in bestScores.keys) {
            var result = matchesArray[k]
            //0) STORE THE FACT THAT YOU FOUND AN ALBUM:
            if (result.get("albumType").asString == "album") {
                albumFound = true
            }
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
            } else if (!albumFound && bestType != "saved" && result.get("albumType").asString == "album") {
                bestInd = k
                bestType = "album"
            }
        }

        //RETURN PREPARATION:
        var bestMatches = JsonArray()
        //position 0 -> BEST:
        bestMatches.add(matchesArray[bestInd])
        //the other results:
        for (k in bestScores.keys) {
            if (k != bestInd) {
                bestMatches.add(matchesArray[k])
            }
        }
        return bestMatches
    }

}
