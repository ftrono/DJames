package com.ftrono.DJames.be.spotify

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastLog
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.likedSongsUri
import com.ftrono.DJames.application.logUtils
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.reqPlayLinkName
import com.ftrono.DJames.application.spotIntroUri
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.NlpQueryModel
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.nlp.NLPExtractor
import com.ftrono.DJames.be.nlp.NLPMatcher


class SpotifyFulfillment (private var context: Context) {
    private val TAG = SpotifyFulfillment::class.java.simpleName


    //Play a song (with custom context), an album, an artist or a playlist: PART 1:
    fun playItem1(resultsNLP: NlpQueryModel): DispatcherInfo {
        var dispatcherInfo = DispatcherInfo()
        logUtils.openLog()
        lastLog.nlpQueries.add(resultsNLP)

        //Detect & process requested languages:
        var intentName = resultsNLP.intentName
        var detLanguage = resultsNLP.reqLanguage
        var reqLangCode = utils.getLanguageCode(detLanguage, prefs.queryLanguage)
        var reqLangName = utils.getLanguageName(reqLangCode)

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

        if (intentName == "PlayPodcast") {
            //PLAYLIST:
            playType = "podcast"
            contextType = "podcast"
            ttsToRead = "Tell me the name of the podcast in ${reqLangName}."
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)

        } else if (intentName == "PlayPlaylist") {
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

        //dispatcherInfo:
        dispatcherInfo.intentName = resultsNLP.intentName
        dispatcherInfo.followUp = true
        dispatcherInfo.reqLanguage = reqLangCode
        dispatcherInfo.playType = playType
        dispatcherInfo.contextType = contextType
        Log.d(TAG, dispatcherInfo.toString())

        return dispatcherInfo
    }


    //Play Liked Songs collection:
    fun playCollection(resultsNLP: NlpQueryModel): DispatcherInfo {
        var dispatcherInfo = DispatcherInfo()
        var playable = SpotifyPlayable(
            type = "collection",
            name = "Liked Songs"
        )
        logUtils.openLog()

        //PLAY -> Liked Songs:
        Log.d(TAG, "PLAY -> Liked Songs")

        //Read & dim:
        fulfillmentUtils.releaseAudioFocus()

        //Log:
        lastLog.nlpQueries.add(resultsNLP)

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


        //PLAY:
        val spotifyPlayer = SpotifyPlayer(context)
        spotifyPlayer.spotifyPlay(playable)

        //dispatcherInfo:
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())

        //Close log:
        lastLog.spotifyPlay = playable
        logUtils.saveLog(context)
        return dispatcherInfo
    }


    //Play a song or an album: PART 2:
    fun playSongAlbum2(resultsNLP: NlpQueryModel, prevStatus: DispatcherInfo): DispatcherInfo {
        // Context:
        var playType = prevStatus.playType
        var reqLangCode = prevStatus.reqLanguage
        var contextType = prevStatus.contextType
        // Returns:
        val dispatcherInfo = DispatcherInfo()

        //Log:
        lastLog.nlpQueries.add(resultsNLP)

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }

        //Extract play info:
        val nlpMatcher = NLPMatcher(context)
        var nlpExtractor = NLPExtractor(context)
        var extractorInfo = nlpExtractor.extractPlayInfo(nlp_queryText, reqLangCode, playType, contextType)

        contextType = extractorInfo.contextType
        var artistExtracted = extractorInfo.artistExtracted
        var artistConfirmed = ""
        var contextExtracted = extractorInfo.contextExtracted

        if (artistExtracted != "") {
            //Confirm artists with vocabulary check:
            artistConfirmed = nlpExtractor.checkArtists(resultsNLP.artists, artistExtracted, reqLangCode)
            val vocMatchId = nlpMatcher.matchVocabulary("artist", artistConfirmed)
            if (vocMatchId != "") {
                artistConfirmed = libUtils.getItemName("artist", vocMatchId)
            }
            extractorInfo.artistConfirmed = artistConfirmed
        }

        //GET SPOTIFY PLAY INFO:
        //Search tracks + album:
        var search = SpotifySearch(context)
        var playable = search.searchPlayable(searchData=extractorInfo)

        //A) EMPTY QUERY RESULT:
        if (playable.id == "") {
            //Close log:
            logUtils.saveLog(context)
            return fulfillmentUtils.fallback()

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            fulfillmentUtils.releaseAudioFocus()

            //Wait 1 sec:
            Thread.sleep(1000)

            //CONTEXT:
            if (contextType == "collection") {
                //Context -> Liked Songs:
                Log.d(TAG, "Context -> Liked Songs")
                playable.contextType = "collection"
                playable.contextName = "Liked Songs"
                playable.contextUri = likedSongsUri.replace("replaceUserId", prefs.spotUserId)

            } else if (playType == "track" && contextExtracted != "") {
                //Confirm context playlist with vocabulary check:
                val nlpMatcher = NLPMatcher(context)
                val vocMatchId = nlpMatcher.matchVocabulary("playlist", contextExtracted)

                if (vocMatchId != "") {
                    Log.d(TAG, "Context -> Playlist in voc")
                    //Get playlist URL:
                    val itemInfo = libUtils.getItemInfoUse("playlist", vocMatchId)
                    playable.contextType = "playlist"
                    playable.contextName = itemInfo.name
                    extractorInfo.contextConfirmed = itemInfo.name
                    playable.owner = itemInfo.detail
                    playable.contextUri = "$spotIntroUri:playlist:${spotifyUtils.getSpotifyID(itemInfo.url)}"
                }
            }

            //Read TTS:
            var itemName = playable.name
            var artist = playable.artistsNames.joinToString(" feat. ")
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
            lastLog.nlpExtractor = extractorInfo
            lastLog.spotifyPlay = playable

            //PLAY:
            val spotifyPlayer = SpotifyPlayer(context)
            spotifyPlayer.spotifyPlay(playable)
        }

        //Build return
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())

        //Close log:
        logUtils.saveLog(context)
        return dispatcherInfo
    }


    //Play an artist or a playlist: PART 2:
    fun playArtistPlaylist2(resultsNLP: NlpQueryModel, prevStatus: DispatcherInfo): DispatcherInfo {
        // Context:
        var matchName = nlp_queryText
        var playType = prevStatus.playType
        var reqLangCode = prevStatus.reqLanguage
        // Returns:
        val dispatcherInfo = DispatcherInfo()
        val extractorInfo = ExtractorInfo()
        var playable = SpotifyPlayable(type=playType)

        //Log:
        lastLog.nlpQueries.add(resultsNLP)

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }

        //Extractor:
        var nlpExtractor = NLPExtractor(context)
        val nlpMatcher = NLPMatcher(context)
        extractorInfo.matchExtracted = nlp_queryText
        extractorInfo.playType = playType
        extractorInfo.contextType = playType

        // A) ARTIST:
        if (playType == "artist") {
            //Confirm artists with vocabulary check:
            val artistExtracted = nlpExtractor.checkArtists(resultsNLP.artists, matchName, reqLangCode)
            extractorInfo.artistConfirmed = artistExtracted
            val vocMatchId = nlpMatcher.matchVocabulary("artist", artistExtracted)

            if (vocMatchId != "") {
                //Build playable:
                val itemInfo = libUtils.getItemInfoUse("artist", vocMatchId)
                extractorInfo.artistConfirmed = itemInfo.name
                //Match playLinkName:
                val playLinks = itemInfo.playLinks
                val matchedPlayName = if (playLinks.containsKey(reqPlayLinkName)) reqPlayLinkName else itemInfo.defaultKey
                Log.d(TAG, "Matched PlayName: $matchedPlayName")
                if (matchedPlayName == "artist") {
                    Log.d(TAG, "PLAY -> Artist Top Tracks")
                    playable.id = spotifyUtils.getSpotifyID(itemInfo.url)
                    playable.name = itemInfo.name
                } else {
                    Log.d(TAG, "PLAY -> Artist playlist in voc")
                    val playLink = itemInfo.playLinks[matchedPlayName]!!
                    playType = "playlist"
                    playable.type = playType
                    playable.name = playLink.name
                    playable.owner = playLink.owner
                    playable.id = spotifyUtils.getSpotifyID(playLink.spotifyUrl)
                }
            }

        //B) PLAYLIST:
        } else if (playType == "playlist") {
            //Check playlist in vocabulary:
            val nlpMatcher = NLPMatcher(context)
            val vocMatchId = nlpMatcher.matchVocabulary(playType, matchName)

            if (vocMatchId != "") {
                //Build playable:
                Log.d(TAG, "PLAY -> Playlist in voc")
                val itemInfo = libUtils.getItemInfoUse(playType, vocMatchId)
                playable.name = itemInfo.name
                playable.owner = itemInfo.detail
                playable.id = spotifyUtils.getSpotifyID(itemInfo.url)
            }
        }

        //Search in Spotify:
        if (playable.id == "") {
            val search = SpotifySearch(context)
            playable = search.searchPlayable(extractorInfo)
        }

        //PLAY:
        //A) EMPTY QUERY RESULT:
        if (playable.id == "") {
            Log.d(TAG, "PLAY -> Artist / Playlist not found!")
            //Close log:
            //logUtils.saveLog(context)
            return fulfillmentUtils.fallback()

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            Log.d(TAG, "PLAY -> Artist / Playlist found!")
            fulfillmentUtils.releaseAudioFocus()

            //Wait 1 sec:
            Thread.sleep(1000)

            //TTS:
            var introText = ""
            var detailText = ""
            val ownerString = if (playable.owner == "") "" else ", by ${playable.owner}"
            //Read:
            if (playable.owner == prefs.spotUserName) {
                introText = "Playing your playlist: "
                detailText = playable.name
            } else if (playType == "artist") {
                introText = "Playing top tracks for the artist: "
                detailText = playable.name
            } else {
                introText = "Playing the $playType: "
                detailText = "${playable.name}${ownerString}."
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
            lastLog.nlpExtractor = extractorInfo
            lastLog.spotifyPlay = playable

            //PLAY:
            val spotifyPlayer = SpotifyPlayer(context)
            spotifyPlayer.spotifyPlay(playable)
        }

        //Build return
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())

        //Close log:
        logUtils.saveLog(context)
        return dispatcherInfo
    }


    //Play a podcast: PART 2:
    fun playPodcast2(resultsNLP: NlpQueryModel, prevStatus: DispatcherInfo): DispatcherInfo {
        // Context:
        var matchName = nlp_queryText
        var playType = prevStatus.playType
        var reqLangCode = prevStatus.reqLanguage
        // Returns:
        val dispatcherInfo = DispatcherInfo()
        val extractorInfo = ExtractorInfo()
        var playable = SpotifyPlayable(type="episode")   //IMPORTANT!

        //Log:
        lastLog.nlpQueries.add(resultsNLP)

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }

        //Extractor:
        val nlpMatcher = NLPMatcher(context)
        extractorInfo.matchExtracted = nlp_queryText
        extractorInfo.playType = playType
        extractorInfo.contextType = playType


        //PODCAST:
        val vocMatchId = nlpMatcher.matchVocabulary(playType, matchName)
        if (vocMatchId != "") {
            Log.d(TAG, "PLAY -> Podcast in voc")
            //1) Context -> Podcast:
            val itemInfo = libUtils.getItemInfoUse(playType, vocMatchId)
            val podcastId = spotifyUtils.getSpotifyID(itemInfo.url)
            playable.contextType = "podcast"
            playable.contextUri = "$spotIntroUri:$podcastId:$podcastId"
            playable.contextName = itemInfo.name
            playable.publisher = itemInfo.detail
            playable.languages = mutableListOf(itemInfo.language)   //TODO

            //2) Item -> GET latest podcast episode:
            //TODO: latest episode only!
            try {
                val episodeInfo = spotifyUtils.getPodcastEpisodes(context, podcastId, latestOnly = true)[0]
                playable.id = episodeInfo.id
                playable.name = episodeInfo.name
                playable.releaseDate = episodeInfo.releaseDate
                playable.fullyPlayed = episodeInfo.fullyPlayed
                playable.resumePositionMs = episodeInfo.resumePositionMs

            } catch (e: Exception) {
                //PLAY -> Episodes not found:
                Log.d(TAG, "PLAY -> Episodes not found! ", e)
            }

        } else {
            //PLAY -> Podcast not found:
            Log.d(TAG, "PLAY -> Podcast not found!")
        }

        //PLAY:
        //A) EMPTY QUERY RESULT:
        if (playable.id == "") {
            Log.d(TAG, "PLAY -> Podcast not found!")
            //Close log:
            //logUtils.saveLog(context)
            return fulfillmentUtils.fallback()

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            fulfillmentUtils.releaseAudioFocus()

            //Wait 1 sec:
            Thread.sleep(1000)

            //Read TTS:
            var introText = ""
            var detailText = ""
            if (playable.releaseDate != "") {
                introText = "Playing the latest episode dated ${playable.releaseDate}: "
                detailText = playable.name
            } else {
                introText = "Playing the latest episode: "
                detailText = playable.name
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
            lastLog.nlpExtractor = extractorInfo
            lastLog.spotifyPlay = playable

            //PLAY:
            val spotifyPlayer = SpotifyPlayer(context)
            spotifyPlayer.spotifyPlay(playable)
        }

        //Build return
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())

        //Close log:
        logUtils.saveLog(context)
        return dispatcherInfo
    }

}