package com.ftrono.DJames.be.spotify

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.ext_format
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.likedSongsUri
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.reqPlayLinkName
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.nlp.NLPExtractor
import com.google.gson.JsonObject


class SpotifyFulfillment (private var context: Context) {
    private val TAG = SpotifyFulfillment::class.java.simpleName


    //Play a song (with custom context), an album, an artist or a playlist: PART 1:
    fun playItem1(resultsNLP: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        utils.openLog()

        //Detect & process requested languages:
        var intentName = resultsNLP.get("intent_name").asString
        var detLanguage = resultsNLP.get("reqLanguage").asString
        var reqLangCode = utils.getLanguageCode(context, detLanguage, prefs.queryLanguage)
        var reqLangName = utils.getLanguageName(context, reqLangCode)

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }

        //Distinguish by intent & build voice response:
        var ttsToRead = ""
        var playType = ""
        var contextType = ""

        if (intentName == "PlayPlaylist") {
            //PLAYLIST:
            playType = "playlist"
            contextType = "playlist"
            ttsToRead = "Tell me the name of the playlist in ${reqLangName}."
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)

        } else if (intentName == "PlayArtist") {
            //ARTIST:
            val nlpExtractor = NLPExtractor(context)
            playType = "artist"
            contextType = "playlist"
            reqPlayLinkName = nlpExtractor.extractPlayLink(nlp_queryText)
            ttsToRead = "Tell me the name of the artist in ${reqLangName}."
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)

        } else if (intentName == "PlayAlbum") {
            //ALBUM:
            playType = "album"
            contextType = "album"
            if (reqLangCode == "en") {
                ttsToRead = "Tell me in ${reqLangName}: name of the album, by, name of the artist."
            } else {
                ttsToRead = "Tell me in ${reqLangName} the name of the album and the name of the artist."
            }
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)

        } else {
            //TRACK:
            playType = "track"
            if (nlp_queryText.contains("playlist")) {
                //Context -> playlist:
                //TODO: eng only!
                contextType = "playlist"
                ttsToRead = "Tell me in ${reqLangName}: name of the song, by, name of the artist, from playlist, name of the playlist."

            } else {
                //TODO: eng only!
                if (nlp_queryText.contains("collection") || nlp_queryText.contains("liked") || nlp_queryText.contains("saved")) {
                    //Context -> collection:
                    contextType = "collection"
                } else {
                    //Context -> album:
                    contextType = "album"
                }

                //Read:
                if (reqLangCode == "en") {
                    ttsToRead = "Tell me in ${reqLangName}: name of the song, by, name of the artist."
                } else {
                    ttsToRead = "Tell me in ${reqLangName} the name of the song and the name of the artist."
                }
            }
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)
        }

        //processStatus:
        processStatus.addProperty("followUp", true)
        processStatus.addProperty("reqLanguage", reqLangCode)
        processStatus.addProperty("intent_name", intentName)
        processStatus.addProperty("play_type", playType)
        processStatus.addProperty("context_type", contextType)
        Log.d(TAG, processStatus.toString())

        //Log:
        last_log!!.addProperty("intent_name", intentName)
        return processStatus
    }


    //Play Liked Songs collection:
    fun playCollection(resultsNLP: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        utils.openLog()

        //Read & dim:
        fulfillmentUtils.releaseAudioFocus()

        //Log:
        last_log!!.addProperty("intent_name", resultsNLP.get("intent_name").asString)
        last_log!!.add("nlp", resultsNLP)

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }

        //Read:
        var ttsToRead = "Playing your Liked Songs collection!"
        val itemsToRead = listOf(
            mapOf(
                "language" to prefs.queryLanguage,
                "text" to ttsToRead
            )
        )
        fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=true)

        //PLAY -> Liked Songs:
        var playInfo = JsonObject()
        Log.d(TAG, "PLAY -> Liked Songs")
        playInfo.addProperty("play_type", "playlist")
        playInfo.addProperty("context_type", "playlist")
        var uri = likedSongsUri.replace("replaceUserId", prefs.spotUserId)
        playInfo.addProperty("uri", uri)
        playInfo.addProperty("context_uri", uri)
        playInfo.addProperty("context_name", "Liked Songs")
        playInfo.addProperty("spotify_URL", "${ext_format}collection/tracks")
        last_log!!.addProperty("voc_score", 100)
        last_log!!.add("spotify_play", playInfo)

        //PLAY:
        val spotifyPlayer = SpotifyPlayer(context)
        var ret = spotifyPlayer.spotifyPlay(playInfo)

        //processStatus:
        processStatus.addProperty("stopService", true)
        Log.d(TAG, processStatus.toString())

        //Close log:
        utils.closeLog(context)
        return processStatus
    }


    //Play a song or an album: PART 2:
    fun playSongAlbum2(resultsNLP: JsonObject, prevStatus: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        last_log!!.add("nlp", resultsNLP)

        //Detect & process requested languages:
        var playType = prevStatus.get("play_type").asString
        var reqLangCode = prevStatus.get("reqLanguage").asString
        var contextType = prevStatus.get("context_type").asString

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }

        //EXTRACT PLAY INFO:
        var nlpExtractor = NLPExtractor(context)
        var extractorInfo = nlpExtractor.extractPlayInfo(nlp_queryText, reqLangCode, playType, contextType)

        //PROCESS PLAY INFO:
        contextType = extractorInfo.get("context_type").asString
        var artistExtracted = extractorInfo.get("artist_extracted").asString
        var artistConfirmed = ""
        var artistJson = JsonObject()
        var contextExtracted = extractorInfo.get("context_extracted").asString
        var playlistMatchId = ""

        if (artistExtracted != "") {
            //Confirm artists with vocabulary check:
            val artistsNlp = resultsNLP.get("artists").asJsonArray
            artistJson = nlpExtractor.checkArtists(artistsNlp, artistExtracted, reqLangCode, getDetails=false)
            artistConfirmed = artistJson.get("text_confirmed").asString
            extractorInfo.addProperty("artist_confirmed", artistConfirmed)
            last_log!!.add("nlp_extractor", extractorInfo)
        }

        //GET SPOTIFY PLAY INFO:
        //Search tracks + album:
        var search = SpotifySearch(context)
        var playInfo = search.searchTrackOrAlbum(searchData=extractorInfo)

        //A) EMPTY QUERY RESULT:
        if (!playInfo.has("uri")) {
            //Close log:
            utils.closeLog(context)
            return utils.fallback()

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            fulfillmentUtils.releaseAudioFocus()

            //Wait 1 sec:
            Thread.sleep(1000)

            var contextAlbum = false

            if (contextType == "collection") {
                //Context -> Liked Songs:
                Log.d(TAG, "Context -> Liked Songs")
                var uri = likedSongsUri.replace("replaceUserId", prefs.spotUserId)
                playInfo.addProperty("context_type", "playlist")
                playInfo.addProperty("context_uri", uri)
                playInfo.addProperty("context_name", "Liked Songs")

            } else if (contextExtracted == "") {
                //Context -> Album:
                contextAlbum = true

            } else if (playType == "track") {
                //Confirm context playlist with vocabulary check:
                playlistMatchId = nlpExtractor.matchVocabulary("playlist", contextExtracted)

                if (playlistMatchId != "") {
                    Log.d(TAG, "Context -> Playlist in voc")
                    //Get playlist URL:
                    val itemInfo = libUtils.getItemInfoUse("playlist", playlistMatchId)
                    val playlistUrl = itemInfo.url
                    val playlistId = spotifyUtils.getSpotifyID(playlistUrl)
                    extractorInfo.addProperty("context_confirmed", playlistMatchId)

                    //Context -> Playlist:
                    val uri = "spotify:playlist:${playlistId}"
                    playInfo.addProperty("context_type", "playlist")
                    playInfo.addProperty("context_uri", uri)
                    playInfo.addProperty("context_name", itemInfo.name)
                    playInfo.addProperty("context_owner", itemInfo.detail)
                } else {
                    //Playlist not found:
                    contextAlbum = true
                }
            }

            if (contextAlbum) {
                //Context -> Album:
                Log.d(TAG, "Context -> album")
                playInfo.addProperty("context_type", "album")
                playInfo.addProperty("context_uri", playInfo.get("album_uri").asString)
                playInfo.addProperty("context_name", playInfo.get("album_name").asString)
            }

            //Read TTS:
            var itemName = playInfo.get("match_name").asString.lowercase()
            var artist = playInfo.get("artist_name").asString.lowercase()
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to "Playing the $playType: "
                ),
                mapOf(
                    "language" to reqLangCode,
                    "text" to "$itemName, by $artist."
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=true)

            //Player info:
            last_log!!.add("nlp_extractor", extractorInfo)
            last_log!!.add("spotify_play", playInfo)

            //PLAY:
            val spotifyPlayer = SpotifyPlayer(context)
            var ret = spotifyPlayer.spotifyPlay(playInfo)
        }

        //Build return
        processStatus.addProperty("stopService", true)
        Log.d(TAG, processStatus.toString())

        //Close log:
        utils.closeLog(context)
        return processStatus
    }


    //Play an artist or a playlist: PART 2:
    fun playArtistPlaylist2(resultsNLP: JsonObject, prevStatus: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        last_log!!.add("nlp", resultsNLP)

        //Detect & process requested languages:
        var playType = prevStatus.get("play_type").asString
        var reqLangCode = prevStatus.get("reqLanguage").asString

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }


        //PROCESS PLAY INFO:
        //item:
        var matchName = nlp_queryText
        var artistConfirmed = ""
        var artistJson = JsonObject()
        var contextConfirmed = ""
        var artistPlaylist = ""
        var ownerConfirmed = ""
        var playlistMatchId = ""
        var spotifyId = ""
        var uri = ""
        var playInfo = JsonObject()

        var extractorInfo = JsonObject()
        extractorInfo.addProperty("play_type", playType)
        extractorInfo.addProperty("match_extracted", nlp_queryText)
        extractorInfo.addProperty("text_confirmed", nlp_queryText)
        extractorInfo.addProperty("context_type", "playlist")

        var nlpExtractor = NLPExtractor(context)
        if (playType == "artist") {
            //Confirm artists with vocabulary check:
            //TODO: Extract name of artist playlist to play
            val artistsNlp = resultsNLP.get("artists").asJsonArray
            artistJson = nlpExtractor.checkArtists(artistsNlp, matchName, reqLangCode, playName=reqPlayLinkName, getDetails=true)
            artistConfirmed = artistJson.get("text_confirmed").asString
            extractorInfo.addProperty("text_confirmed", artistConfirmed)
            extractorInfo.addProperty("artist_confirmed", artistConfirmed)

            //Artist playlist:
            if (artistJson.has("play_URL")) {
                Log.d(TAG, "PLAY -> Artist playlist in voc")
                artistPlaylist = artistJson.get("play_URL").asString
                extractorInfo.addProperty("play_URL", artistPlaylist)

                if (artistJson.has("play_name")) {
                    // Artist playlist:
                    playType = "playlist"
                    contextConfirmed = artistJson.get("play_name").asString
                    ownerConfirmed = artistJson.get("play_owner").asString
                    extractorInfo.addProperty("context_confirmed", contextConfirmed)
                    extractorInfo.addProperty("owner_confirmed", ownerConfirmed)
                } else {
                    // Artist:
                    contextConfirmed = artistConfirmed
                    extractorInfo.addProperty("context_confirmed", contextConfirmed)
                }

                spotifyId = artistPlaylist.split("/").last()
                uri = "spotify:$playType:${spotifyId}"

                //PLAY -> Artist / artist playlist:
                playInfo.addProperty("play_type", playType)
                playInfo.addProperty("context_type", playType)
                playInfo.addProperty("match_name", contextConfirmed)
                playInfo.addProperty("uri", uri)
                playInfo.addProperty("context_uri", uri)
                playInfo.addProperty("context_name", contextConfirmed)
                playInfo.addProperty("context_owner", ownerConfirmed)
                playInfo.addProperty("spotify_URL", "${ext_format}$playType/$spotifyId")
            }

        } else if (playType == "playlist") {
            //Check playlist in vocabulary:
            playlistMatchId = nlpExtractor.matchVocabulary(playType, matchName)

            if (playlistMatchId != "") {
                Log.d(TAG, "PLAY -> Playlist in voc")
                //Get playlist URL:
                val itemInfo = libUtils.getItemInfoUse(playType, playlistMatchId)
                val playUrl = itemInfo.url
                contextConfirmed = itemInfo.name
                ownerConfirmed = itemInfo.detail
                extractorInfo.addProperty("text_confirmed", contextConfirmed)
                extractorInfo.addProperty("context_confirmed", contextConfirmed)
                extractorInfo.addProperty("owner_confirmed", ownerConfirmed)
                spotifyId = spotifyUtils.getSpotifyID(playUrl)

                //PLAY -> Playlist:
                playInfo.addProperty("play_type", playType)
                playInfo.addProperty("context_type", playType)
                playInfo.addProperty("match_name", contextConfirmed)
                uri = "spotify:$playType:${spotifyId}"
                playInfo.addProperty("uri", uri)
                playInfo.addProperty("context_uri", uri)
                playInfo.addProperty("context_name", contextConfirmed)
                playInfo.addProperty("context_owner", ownerConfirmed)
                playInfo.addProperty("spotify_URL", "${ext_format}$playType/$spotifyId")

            }
        }

        if (contextConfirmed == "") {
            //Search in Spotify:
            //PLAY -> Artist / Playlist not found: search in Spotify!
            val search = SpotifySearchArtistsPlaylists(context)
            //SEARCH ARTIST OR PLAYLIST:
            playInfo = search.searchArtistsPlaylists(extractorInfo, playType)

            if (!playInfo.isEmpty) {
                Log.d(TAG, "PLAY -> Artist / Playlist found")
                //PLAY -> Artist / Playlist:
                ownerConfirmed = playInfo.get("owner").asString
                playInfo.addProperty("context_type", playType)
                spotifyId = playInfo.get("id").asString
                uri = "spotify:$playType:${spotifyId}"
                playInfo.addProperty("uri", uri)
                playInfo.addProperty("context_uri", uri)
                val playlistName = playInfo.get("match_name").asString
                playInfo.addProperty("context_name", playlistName)
                playInfo.addProperty("spotify_URL", "${ext_format}$playType/$spotifyId")

            } else {
                //PLAY -> Artist / Playlist not found:
                Log.d(TAG, "PLAY -> Artist / Playlist not found!")
            }
        }

        //PLAY:
        //A) EMPTY QUERY RESULT:
        if (!playInfo.has("uri")) {
            //Close log:
            //utils.closeLog(context)
            return utils.fallback()

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            fulfillmentUtils.releaseAudioFocus()

            //Wait 1 sec:
            Thread.sleep(1000)

            //Read TTS:
            val playName = playInfo.get("match_name").asString.lowercase()
            val ownerString = if (ownerConfirmed == "") "" else ", by $ownerConfirmed"
            var introText = ""
            var detailText = ""
            if (ownerConfirmed == prefs.spotUserName) {
                introText = "Playing your playlist: "
                detailText = playName
            } else if (playType == "artist") {
                introText = "Playing top tracks for the artist: "
                detailText = playName
            } else {
                introText = "Playing the $playType: "
                detailText = "${playName}${ownerString}."
            }
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to introText
                ),
                mapOf(
                    "language" to reqLangCode,
                    "text" to detailText
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=true)

            //Player info:
            last_log!!.add("nlp_extractor", extractorInfo)
            last_log!!.add("spotify_play", playInfo)

            //PLAY:
            val spotifyPlayer = SpotifyPlayer(context)
            spotifyPlayer.spotifyPlay(playInfo)
        }

        //Build return
        processStatus.addProperty("stopService", true)
        Log.d(TAG, processStatus.toString())

        //Close log:
        utils.closeLog(context)
        return processStatus
    }

}