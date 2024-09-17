package com.ftrono.DJames.spotify

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.ext_format
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.likedSongsUri
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.supportedLanguageCodes
import com.ftrono.DJames.nlp.NLPExtractor
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject


class SpotifyFulfillment (private var context: Context) {
    private val TAG = SpotifyFulfillment::class.java.simpleName
    private val utils = Utilities()


    //Play a song (with custom context), an album, an artist or a playlist: PART 1:
    fun playItem1(resultsNLP: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        utils.openLog()

        //Detect & process requested languages:
        var intentName = resultsNLP.get("intent_name").asString
        var detLanguage = resultsNLP.get("reqLanguage").asString
        var defaultLangCode = supportedLanguageCodes[prefs.queryLanguage.toInt()]
        var reqLangCode = utils.getLanguageCode(context, detLanguage, defaultLangCode)
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
            utils.ttsRead(context, defaultLangCode, ttsToRead, dimAudio=false)

        } else if (intentName == "PlayArtist") {
            //ARTIST:
            playType = "artist"
            contextType = "playlist"
            ttsToRead = "Tell me the name of the artist in ${reqLangName}."
            utils.ttsRead(context, defaultLangCode, ttsToRead, dimAudio=false)

        } else if (intentName == "PlayAlbum") {
            //ALBUM:
            playType = "album"
            contextType = "album"
            if (reqLangCode == "en") {
                ttsToRead = "Tell me in ${reqLangName}: name of the album, by, name of the artist."
            } else {
                ttsToRead = "Tell me in ${reqLangName} the name of the album and the name of the artist."
            }
            utils.ttsRead(context, defaultLangCode, ttsToRead, dimAudio=false)

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

                //TODO: eng only!
                if (reqLangCode == "en") {
                    ttsToRead = "Tell me in ${reqLangName}: name of the song, by, name of the artist."
                } else {
                    ttsToRead = "Tell me in ${reqLangName} the name of the song and the name of the artist."
                }
            }
            utils.ttsRead(context, defaultLangCode, ttsToRead, dimAudio=false)
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
        utils.releaseAudioFocus()

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

        //TODO: eng only!
        var ttsToRead = "Playing your Liked Songs collection!"
        var defaultLangCode = supportedLanguageCodes[prefs.queryLanguage.toInt()]
        utils.ttsRead(context, defaultLangCode, ttsToRead, dimAudio=true)

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
        var contextExtracted = extractorInfo.get("context_extracted").asString
        var playlistMatch = JsonObject()

        if (artistExtracted != "") {
            //Confirm artists with vocabulary check:
            val artistsNlp = resultsNLP.get("artists").asJsonArray
            artistConfirmed = nlpExtractor.checkArtists(artistsNlp, artistExtracted, reqLanguage=reqLangCode)
            extractorInfo.addProperty("artist_confirmed", artistConfirmed)
            last_log!!.add("nlp_extractor", extractorInfo)
        }

        //GET SPOTIFY PLAY INFO:
        //Search tracks + album:
        var search = SpotifySearch()
        var playInfo = search.searchTrackOrAlbum(searchData=extractorInfo)

        //A) EMPTY QUERY RESULT:
        if (!playInfo.has("uri")) {
            //Close log:
            utils.closeLog(context)
            return utils.fallback()

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            utils.releaseAudioFocus()

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
                playlistMatch = nlpExtractor.matchVocabulary("playlist", contextExtracted, utils.getVocabulary("playlist"))

                if (playlistMatch.has("text_confirmed")) {
                    Log.d(TAG, "Context -> Playlist in voc")
                    var contextConfirmed = playlistMatch.get("text_confirmed").asString
                    var playlistUrl = playlistMatch.get("detail_confirmed").asString
                    var playlistId = playlistUrl.split("/").last()
                    extractorInfo.addProperty("context_confirmed", contextConfirmed)

                    //Context -> Playlist:
                    var uri = "spotify:playlist:${playlistId}"
                    playInfo.addProperty("context_type", "playlist")
                    playInfo.addProperty("context_uri", uri)
                    playInfo.addProperty("context_name", contextConfirmed)
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
            //TODO: eng only!
            var ttsToRead = "Playing the $playType $itemName, by $artist."
            var defaultLangCode = supportedLanguageCodes[prefs.queryLanguage.toInt()]
            utils.ttsRead(context, defaultLangCode, ttsToRead, dimAudio=true)

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
        var contextConfirmed = ""
        var bySpotify = false
        var playlistMatch = JsonObject()
        var playInfo = JsonObject()

        var extractorInfo = JsonObject()
        extractorInfo.addProperty("play_type", playType)
        extractorInfo.addProperty("match_extracted", nlp_queryText)
        extractorInfo.addProperty("context_type", "playlist")

        var nlpExtractor = NLPExtractor(context)
        if (playType == "artist") {
            bySpotify = true
            //Confirm artists with vocabulary check:
            val artistsNlp = resultsNLP.get("artists").asJsonArray
            artistConfirmed = nlpExtractor.checkArtists(artistsNlp, matchName, reqLanguage=reqLangCode)
            extractorInfo.addProperty("artist_confirmed", artistConfirmed)

        } else if (playType == "playlist") {
            //Check playlist in vocabulary:
            playlistMatch = nlpExtractor.matchVocabulary("playlist", matchName, utils.getVocabulary("playlist"))

            if (playlistMatch.has("text_confirmed")) {
                Log.d(TAG, "PLAY -> Playlist in voc")
                contextConfirmed = playlistMatch.get("text_confirmed").asString
                extractorInfo.addProperty("context_confirmed", contextConfirmed)
                var playlistUrl = playlistMatch.get("detail_confirmed").asString
                var playlistId = playlistUrl.split("/").last()

                //PLAY -> Playlist:
                playInfo.addProperty("play_type", "playlist")
                playInfo.addProperty("context_type", "playlist")
                playInfo.addProperty("match_name", contextConfirmed)
                var uri = "spotify:playlist:${playlistId}"
                playInfo.addProperty("uri", uri)
                playInfo.addProperty("context_uri", uri)
                playInfo.addProperty("context_name", contextConfirmed)
                playInfo.addProperty("spotify_URL", "${ext_format}$playType/$playlistId")

            } else {
                //Check if playlist by spotify:
                if (matchName.contains("spotify")) {
                    bySpotify = true
                }
            }
        }
        extractorInfo.addProperty("by_spotify", bySpotify)

        if (contextConfirmed == "") {
            //Search in Spotify:
            //PLAY -> Playlist not found: search in Spotify!
            //SEARCH PLAYLIST:
            var search = SpotifySearchPlaylists()
            playInfo = search.searchPlaylists(searchData = extractorInfo, bySpotify = bySpotify)

            if (!playInfo.isEmpty) {
                Log.d(TAG, "PLAY -> Playlist found")
                //PLAY -> Playlist:
                playInfo.addProperty("context_type", "playlist")
                var id = playInfo.get("id").asString
                var uri = "spotify:playlist:${id}"
                playInfo.addProperty("uri", uri)
                playInfo.addProperty("context_uri", uri)
                var playlistName = playInfo.get("match_name").asString
                playInfo.addProperty("context_name", playlistName)
                playInfo.addProperty("spotify_URL", "${ext_format}playlist/$id")

            } else {
                //PLAY -> Playlist not found:
                Log.d(TAG, "PLAY -> Playlist not found!")
            }
        }

        //PLAY:
        //A) EMPTY QUERY RESULT:
        if (!playInfo.has("uri")) {
            //Close log:
            utils.closeLog(context)
            return utils.fallback()

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            utils.releaseAudioFocus()

            //Wait 1 sec:
            Thread.sleep(1000)

            //Read TTS:
            //TODO: eng only!
            var ttsToRead = ""
            if (contextConfirmed != "") {
                ttsToRead = "Playing your playlist ${contextConfirmed}!"
            } else {
                var playlistName = playInfo.get("match_name").asString.lowercase()
                var owner = playInfo.get("owner").asString.lowercase()
                ttsToRead = "Playing the playlist ${playlistName}, by ${owner}!"
            }
            var defaultLangCode = supportedLanguageCodes[prefs.queryLanguage.toInt()]
            utils.ttsRead(context, defaultLangCode, ttsToRead, dimAudio=true)

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


}