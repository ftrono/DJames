package com.ftrono.DJames.be.spotify

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.ftrono.DJames.application.lastAiMessage
import com.ftrono.DJames.application.playThreshold
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotifyQueryLimit
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.SpotifyMatchModel
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.database.SpotifyQueryModel
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import me.xdrop.fuzzywuzzy.FuzzySearch
import kotlin.math.roundToInt


class SpotifySearch(private val context: Context) {
    private val TAG = SpotifySearch::class.java.simpleName
    private var query = SpotifyQuery(context)

    //SEARCH PLAYABLE:
    fun searchPlayable(searchData: ExtractorInfo): SpotifyPlayable {
        //Get context:
        var playType = searchData.playType
        var matchName = searchData.matchExtracted.lowercase()
        var artistName = searchData.artistConfirmed.lowercase()

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
        var url = baseURL
        var items = JsonArray()
        var items2 = JsonArray()
        var spotifyQueries = mutableListOf<SpotifyQueryModel>()
        var bestMatches = mutableListOf<SpotifyMatchModel>()
        var bestScore = 0
        var bestResult = SpotifyPlayable()

        //Extract query params:
        var qParams = mutableListOf<String>()
        if (artistName != "") {
            qParams.add("${playType}:${matchName}")
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
            url1 += "?q=${encodedMatchName}${encodedParams}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"
        } else {
            url1 += "?q=${encodedMatchName}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"
        }
        Log.d(TAG, url1)

        //First query (uses Params):
        var response = query.querySpotify(type = "get", url = url1, jsonHeads = jsonHeads)
        if (response.body != "") {
            var respJSON = JsonParser.parseString(response.body).asJsonObject
            var n_items = 0
            //Log:
            var curQuery = SpotifyQueryModel()
            curQuery.type = playType
            curQuery.url = url1
            //Check content:
            var keySet = respJSON.keySet()
            if (keySet.size == 0) {
                //Empty:
                curQuery.numItems = n_items
                Log.d(TAG, "ERROR: Spotify Search results not received!!")
            } else {
                //Analyse response & get index of best result:
                items = respJSON.getAsJsonObject("${playType}s").getAsJsonArray("items")
                n_items = items.size()
                curQuery.numItems = n_items
                //Get best score:
                if (n_items > 0) {
                    bestMatches = getBestMatches(items, playType, matchName, artistName, live)
                    curQuery.spotifyMatches = bestMatches
                    //Best:
                    bestScore = bestMatches[0].score
                    bestResult = bestMatches[0].playable
                }
            }
            //Log:
            spotifyQueries.add(curQuery)
        }

        //SECOND REQUEST:
        var url2 = url
        if (bestScore <= playThreshold || items.isEmpty()) {
            //Compose query:
            if (artistName != "") {
                val encodedArtistName: String = URLEncoder.encode(artistName, "UTF-8")
                url2 += "?q=${encodedMatchName}+by+${encodedArtistName}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"
            } else {
                url2 += "?q=${encodedMatchName}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"
            }

            //Second query:
            if (url2 != url1) {
                Log.d(TAG, url2)
                response = query.querySpotify(type = "get", url = url2, jsonHeads = jsonHeads)
                if (response.body != "") {
                    var respJSON = JsonParser.parseString(response.body).asJsonObject
                    var n_items = 0
                    //Log:
                    var curQuery = SpotifyQueryModel()
                    curQuery.type = "searchTrackOrAlbum"
                    curQuery.url = url2
                    //Check content:
                    var keySet = respJSON.keySet()
                    if (keySet.size == 0) {
                        //Empty:
                        curQuery.numItems = n_items
                        Log.d(TAG, "ERROR: Spotify Search results not received!!")
                    } else {
                        //Analyse response & get index of best result:
                        items2 = respJSON.getAsJsonObject("${playType}s").getAsJsonArray("items")
                        n_items = items2.size()
                        curQuery.numItems = n_items
                        //Get best score:
                        if (n_items > 0) {
                            bestMatches = getBestMatches(items2, playType, matchName, artistName, live)
                            curQuery.spotifyMatches = bestMatches
                            //Best:
                            var bestScore2 = bestMatches[0].score
                            if (bestScore2 > bestScore) {
                                //Overwrite global best:
                                bestScore = bestScore2
                                bestResult = bestMatches[0].playable
                            }
                        }
                    }
                    //Log:
                    spotifyQueries.add(curQuery)
                }
            }
        }
        lastAiMessage.attachments.spotifyQueries = spotifyQueries
        lastAiMessage.attachments.matchScore = bestScore
        Log.d(TAG, "BEST RESULT: $bestResult")
        return bestResult
    }


    //GENERIC SPOTIFY SEARCH -> PLAY WITHIN ALBUM:
    fun checkSaved(ids: MutableList<String>): JsonArray {
        var returnArray = JsonArray()
        //BUILD GET REQUEST:
        var url = "https://api.spotify.com/v1/me/tracks/contains?ids="

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //Query params:
        url += ids.joinToString(",", "", "")
        Log.d(TAG, url)

        var response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        if (response.body != "") {
            returnArray = JsonParser.parseString(response.body).asJsonArray
        }
        return returnArray
    }

    //Spotify: get Best Result:
    fun getBestMatches(items: JsonArray, playType: String, matchName: String, artistName: String, live: Boolean): MutableList<SpotifyMatchModel> {
        val re = Regex("[^A-Za-z0-9 ]")
        var c = 0
        var allMatches = mutableListOf<SpotifyMatchModel>()
        var ids = mutableListOf<String>()
        var scoresMap = mutableMapOf<Int, Int>()
        for (item in items) {
            try {
                var curMatch = SpotifyMatchModel()
                curMatch.pos = c
                var curJson = item.asJsonObject

                // Extract key info:
                curMatch.playable.type = playType
                curMatch.playable.name = curJson.get("name").asString
                ids.add(curJson.get("id").asString)
                curMatch.playable.id = curJson.get("id").asString

                if (playType == "track" || playType == "album") {
                    // Extract artists:
                    var artists = curJson.getAsJsonArray("artists")
                    for (artist in artists) {
                        curMatch.playable.artistsIds.add(artist.asJsonObject.get("id").asString)
                        curMatch.playable.artistsNames.add(artist.asJsonObject.get("name").asString)
                    }
                    // Extract album:
                    if (playType == "album") {
                        curMatch.playable.albumType = curJson.get("album_type").asString
                    } else {
                        curMatch.playable.albumId =
                            curJson.get("album").asJsonObject.get("id").asString
                        curMatch.playable.albumType =
                            curJson.get("album").asJsonObject.get("album_type").asString
                        curMatch.playable.albumName =
                            curJson.get("album").asJsonObject.get("name").asString
                    }

                } else if (playType == "playlist") {
                    curMatch.playable.owner = curJson.get("owner").asJsonObject.get("display_name").asString
                }

                // Calculate similarity:
                var score = 0
                if (artistName == "") {
                    // Match name only:
                    var stringToMatch = re.replace(
                        curMatch.playable.name,
                        ""
                    ) + " " + re.replace(
                        curMatch.playable.artistsNames.joinToString(", ", "", ""),
                        ""
                    )
                    stringToMatch = stringToMatch.lowercase()
                    curMatch.nameSetSimilarity = FuzzySearch.tokenSetRatio(stringToMatch, matchName)
                    curMatch.namePartialSimilarity =
                        FuzzySearch.partialRatio(stringToMatch, matchName)
                    curMatch.nameFullSimilarity = FuzzySearch.ratio(stringToMatch, matchName)
                    score = listOf<Int>(
                        curMatch.nameSetSimilarity,
                        curMatch.namePartialSimilarity,
                        curMatch.nameFullSimilarity
                    ).average().roundToInt()

                } else {
                    // Match name and artist:
                    var tempName = re.replace(curMatch.playable.name.lowercase(), "")
                    var tempArtists = re.replace(
                        curMatch.playable.artistsNames.joinToString(", ", "", "").lowercase(), ""
                    )
                    curMatch.nameSetSimilarity = FuzzySearch.tokenSetRatio(tempName, matchName)
                    curMatch.namePartialSimilarity = FuzzySearch.partialRatio(tempName, matchName)
                    curMatch.nameFullSimilarity = FuzzySearch.ratio(tempName, matchName)
                    curMatch.artistSetSimilarity =
                        FuzzySearch.tokenSetRatio(tempArtists, artistName)
                    curMatch.artistPartialSimilarity =
                        FuzzySearch.partialRatio(tempArtists, artistName)
                    score = listOf<Int>(
                        curMatch.nameSetSimilarity,
                        curMatch.namePartialSimilarity,
                        curMatch.nameFullSimilarity,
                        curMatch.artistSetSimilarity,
                        curMatch.artistPartialSimilarity
                    ).average().roundToInt()
                }

                //Check if live:
                if (playType == "track" || playType == "artist") {
                    for (tok in curMatch.playable.name.lowercase().split(" ")) {
                        if (!live && tok.lowercase() == "live") {
                            score -= deltaSimilarity
                            break
                        }
                    }
                }

                // Store score:
                curMatch.score = score
                scoresMap[c] = score
                allMatches.add(curMatch)
                Log.d(TAG, curMatch.toString())

            } catch (e: Exception) {
                Log.w(TAG, "Error: Spotify match item skipped. ", e)
            }
            c += 1
        }

        //Check saved:
        if (playType == "track") {
            var savedIds = checkSaved(ids)
            Log.d(TAG, "Check saved: $savedIds")
            if (savedIds.size() > 0) {
                //add Saved info:
                var i = 0
                for (cur in allMatches) {
                    cur.playable.saved = savedIds[i].asBoolean
                    i ++
                }
            }
        }

        //Sort map:
        Log.d(TAG, "MAP: $scoresMap")
        val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap()
        Log.d(TAG, "SORTED MAP: $sortedScores")
        //Exclude lower score items:
        var scoreThreshold = sortedScores.values.elementAt(0) - deltaSimilarity
        if (scoreThreshold < 0) {
            scoreThreshold = 0
        }
        var bestScores = sortedScores.filter { (key, value) -> value >= scoreThreshold}
        Log.d(TAG, "FILTERED MAP: $bestScores")
        //Default best Ind is the first (max score):
        var bestInd = sortedScores.keys.elementAt(0)

        // Find a track saved & from album (if present):
        if (playType == "track") {
            var bestType = ""
            var albumFound = false
            for (k in bestScores.keys) {
                var curMatch = allMatches[k]
                //0) STORE THE FACT THAT YOU FOUND AN ALBUM:
                if (curMatch.playable.albumType == "album") {
                    albumFound = true
                }
                //A) If SAVED:
                if (curMatch.playable.saved) {
                    //PRIORITY 1) If album -> BEST FOUND -> STOP!
                    if (curMatch.playable.albumType == "album") {
                        bestInd = k
                        break
                        //PRIORITY 2) If just saved & not a best saved track found before -> update best:
                    } else if (bestInd == 0) {
                        bestInd = k
                        bestType = "saved"
                    }
                    //PRIORITY 3) If just album & not a saved track found before -> update best:
                } else if (!albumFound && bestType != "saved" && curMatch.playable.albumType == "album") {
                    bestInd = k
                    bestType = "album"
                }
            }
        }

        var bestMatches = mutableListOf<SpotifyMatchModel>()
        // Place best item first:
        bestMatches.add(allMatches[bestInd])
        // Next, add the other results:
        for (k in bestScores.keys) {
            if (k != bestInd) {
                bestMatches.add(allMatches[k])
            }
        }
        return bestMatches
    }

}
