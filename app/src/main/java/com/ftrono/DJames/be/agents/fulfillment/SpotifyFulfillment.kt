package com.ftrono.DJames.be.agents.fulfillment

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotCollectionName
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.SpotifyArtist
import com.ftrono.DJames.be.database.SpotifyContext
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.database.SpotifyPlaylist
import com.ftrono.DJames.be.agents.data.ActionType
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.spotify.SpotifySearch


class SpotifyFulfillment (private var context: Context) {
    private val TAG = this::class.java.simpleName

    //Play a song (with custom context), an album, an artist or a playlist: PART 1:
    fun playItem1(prevState: StateInfo): StateInfo {
        // Manage state:
        var updState = prevState
        var intentName = updState.intentName
        var queryText = updState.messages.last().content

        //Detect & process requested languages:
        var reqLangCode = updState.reqLangCode
        var reqLangName = updState.reqLangName

        //Distinguish by intent & build voice response:
        var playType = ""
        var contextType = ""

        if (intentName == "PlayPodcast") {
            //PODCAST:
            playType = "podcast"
            contextType = "podcast"

        } else if (intentName == "PlayPlaylist") {
            //PLAYLIST:
            playType = "playlist"
            contextType = "playlist"

        } else if (intentName == "PlayArtist") {
            //ARTIST:
            playType = "artist"
            contextType = "playlist"

        } else if (intentName == "PlayAlbum") {
            //ALBUM:
            playType = "album"
            contextType = "album"

        } else {
            //TRACK:
            playType = "track"
            // Select context:   TODO: eng only!
            if (queryText.contains("playlist")) {
                contextType = "playlist"
            } else {
                if (queryText.contains("collection") || queryText.contains("liked") || queryText.contains("saved")) {
                    contextType = "collection"
                } else {
                    contextType = "album"
                }
            }
        }

        //Reply:
        val ttsToRead = defaultReplies.replyPlayRequest(intentName, reqLangName, contextType)
        val aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = ttsToRead
            )
        )

        //updState:
        updState.interrupt = true
        updState.aiReplies = aiReplies
        updState.reqLangCode = reqLangCode
        updState.playType = playType
        updState.contextType = contextType
        Log.d(TAG, updState.toString())

        return updState
    }


    //Play Liked Songs collection:
    fun playCollection(prevState: StateInfo): StateInfo {
        // Manage state:
        var updState = prevState
        var playable = SpotifyPlayable(
            id = "collection",   // Important!
            type = "playlist",
            playlist = SpotifyPlaylist(
                id = "collection",   // Important!
                name = spotCollectionName,
                owner = "Spotify",
            ),
        )

        //PLAY -> Liked Songs:
        Log.d(TAG, "PLAY -> Liked Songs")

        //Reply:
        val ttsToRead = defaultReplies.replyPlayIntro(playable)
        val aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = ttsToRead
            )
        )

        //updState:
        updState.aiReplies = aiReplies
        updState.actionType = ActionType.PLAY
        Log.d(TAG, updState.toString())

        //Update message:
        updState.actionType = updState.actionType
        updState.attachments.spotifyPlay = playable
        updState.attachments.spotifyPlay = playable
        return updState
    }


    //Play a song or an album: PART 2:
    fun playSongAlbum2(prevState: StateInfo): StateInfo {
        // Manage state:
        var updState = prevState
        var queryText = updState.messages.last().content
        var playType = updState.playType
        var reqLangCode = updState.reqLangCode
        var contextType = updState.contextType

        //Extract play info:
        var nlpExtractor = NLPExtractor(context)
        var extractorInfo = nlpExtractor.extractPlayInfo(queryText, reqLangCode, playType, contextType)

        contextType = extractorInfo.contextType
        var artistExtracted = extractorInfo.artistExtracted
        var artistConfirmed = ""
        var contextExtracted = extractorInfo.contextExtracted

        if (artistExtracted != "") {
            //Confirm artists with library check:
            artistConfirmed = nlpExtractor.checkArtists(updState.attachments.entityArtists, artistExtracted, reqLangCode)
            val libMatch = libUtils.matchLibrary("artist", artistConfirmed)
            if (libMatch.matchId > -1) {
                artistConfirmed = libUtils.getLibItemName(libMatch.matchId)
            }
            extractorInfo.artistConfirmed = artistConfirmed
            updState.attachments.matchScore = libMatch.matchScore
        }

        //GET SPOTIFY PLAY INFO:
        //Search tracks + album:
        var search = SpotifySearch(context)
        var spotResults = search.searchPlayable(searchData=extractorInfo)   //TODO: Tracks & albums don't pass through Library yet!
        var playable = spotResults.bestResult

        //A) EMPTY QUERY RESULT:
        if (playable == null || playable.id == "") {
            return fulfillmentUtils.fallback(updState, notUnderstood=true)

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            //Wait 1 sec:
            Thread.sleep(1000)

            //CONTEXT:
            if (playType == "track") {
                if (contextType == "collection") {
                    //Context -> Liked Songs:
                    Log.d(TAG, "Context -> Liked Songs")
                    playable.track!!.context = SpotifyContext(
                        id = "collection",
                        type = "playlist",
                        name = spotCollectionName
                    )

                } else if (contextExtracted != "") {
                    //Confirm context playlist with library check:
                    val libMatch = libUtils.matchLibrary("playlist", contextExtracted)

                    if (libMatch.matchId > -1) {
                        Log.d(TAG, "Context -> Playlist in lib")
                        //Get playlist URL:
                        val itemInfo = libUtils.getLibItemById(libMatch.matchId)
                        playable.track!!.context = SpotifyContext(
                            type = "playlist",
                            name = itemInfo.name,
                            id = spotifyUtils.getSpotifyID(itemInfo.url)
                        )
                        extractorInfo.contextConfirmed = itemInfo.name
                        updState.attachments.matchScore = libMatch.matchScore
                    }

                    // TODO: allow use other specific albums as context!
                }
            }

            //Reply:
            val introText = defaultReplies.replyPlayIntro(playable)
            val detailText = defaultReplies.replyPlayDetail(playable)
            val aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = introText
                ),
                AiReply(
                    langCode = reqLangCode,
                    text = detailText
                )
            )

            //updState:
            updState.aiReplies = aiReplies
            updState.actionType = ActionType.PLAY

            //Update message:
            extractorInfo.reqLanguage = reqLangCode
            updState.actionType = updState.actionType
            updState.attachments.nlpExtractor = extractorInfo
            updState.attachments.spotifyPlay = playable
            updState.attachments.matchScore = spotResults.matchScore
            updState.attachments.spotifyQueries = spotResults.spotifyQueries
            updState.attachments.spotifyPlay = playable
        }

        Log.d(TAG, updState.toString())
        return updState
    }


    //Play an artist or a playlist: PART 2:
    fun playArtistPlaylist2(prevState: StateInfo): StateInfo {
        // Manage state:
        var updState = prevState
        var queryText = updState.messages.last().content
        var playType = updState.playType
        var reqLangCode = updState.reqLangCode

        val extractorInfo = ExtractorInfo()
        var playable = SpotifyPlayable(
            type = playType
        )

        //Extractor:
        var nlpExtractor = NLPExtractor(context)
        extractorInfo.matchExtracted = queryText
        extractorInfo.playType = playType
        extractorInfo.contextType = playType

        // A) ARTIST:
        if (playType == "artist") {
            //Confirm artists with library check:
            val artistExtracted = nlpExtractor.checkArtists(updState.attachments.entityArtists, queryText, reqLangCode)
            val libMatch = libUtils.matchLibrary("artist", artistExtracted)

            if (libMatch.matchId > -1) {
                Log.d(TAG, "PLAY -> Artist Top Tracks")
                //Build playable:
                val itemInfo = libUtils.getLibItemById(libMatch.matchId)
                playable.id = spotifyUtils.getSpotifyID(itemInfo.url)
                playable.artist = SpotifyArtist(
                    id = playable.id,
                    name = itemInfo.name
                )
                updState.attachments.matchScore = libMatch.matchScore
            }

        //B) PLAYLIST:
        } else if (playType == "playlist") {
            //Check playlist in library:
            val libMatch = libUtils.matchLibrary(playType, queryText)

            if (libMatch.matchId > -1) {
                //Build playable:
                Log.d(TAG, "PLAY -> Playlist in lib")
                val itemInfo = libUtils.getLibItemById(libMatch.matchId)
                playable.id = spotifyUtils.getSpotifyID(itemInfo.url)
                playable.playlist = SpotifyPlaylist(
                    id = playable.id,
                    name = itemInfo.name
                )
                updState.attachments.matchScore = libMatch.matchScore

            }
        }

        //Search in Spotify:
        if (playable.id == "") {
            val search = SpotifySearch(context)
            val spotResults = search.searchPlayable(extractorInfo)
            if (spotResults.bestResult != null) {
                playable = spotResults.bestResult!!
                updState.attachments.matchScore = spotResults.matchScore
                updState.attachments.spotifyQueries = spotResults.spotifyQueries

            }
        }

        //PLAY:
        //A) EMPTY QUERY RESULT:
        if (playable.id == "") {
            Log.d(TAG, "PLAY -> Artist / Playlist not found!")
            return fulfillmentUtils.fallback(updState, notUnderstood=true)

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            Log.d(TAG, "PLAY -> Artist / Playlist found!")
            //Wait 1 sec:
            Thread.sleep(1000)

            //Reply:
            var introText = defaultReplies.replyPlayIntro(playable)
            var detailText = defaultReplies.replyPlayDetail(playable)
            val aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = introText
                ),
                AiReply(
                    langCode = reqLangCode,
                    text = detailText
                )
            )

            //updState:
            updState.aiReplies = aiReplies
            updState.actionType = ActionType.PLAY

            //Update message:
            extractorInfo.reqLanguage = reqLangCode
            updState.actionType = updState.actionType
            updState.attachments.nlpExtractor = extractorInfo
            updState.attachments.spotifyPlay = playable
            updState.attachments.spotifyPlay = playable
        }

        Log.d(TAG, updState.toString())
        return updState
    }


    //Play a podcast: PART 2:
    fun playPodcast2(prevState: StateInfo): StateInfo {
        // Manage state:
        var updState = prevState
        var queryText = updState.messages.last().content
        var playType = updState.playType
        var reqLangCode = updState.reqLangCode

        val extractorInfo = ExtractorInfo()
        var playable = SpotifyPlayable(
            type = "episode",   //IMPORTANT!
        )

        //Extractor:
        extractorInfo.matchExtracted = queryText
        extractorInfo.playType = playType
        extractorInfo.contextType = playType


        //PODCAST:
        val libMatch = libUtils.matchLibrary(playType, queryText)
        if (libMatch.matchId > -1) {
            Log.d(TAG, "PLAY -> Podcast in lib")
            //1) Context -> Podcast:
            val itemInfo = libUtils.getLibItemById(libMatch.matchId)
            updState.attachments.matchScore = libMatch.matchScore

            //2) Item -> GET latest podcast episode:
            //TODO: latest episode only!
            try {
                playable.episode = spotifyUtils.getPodcastEpisodes(context, itemInfo, latestOnly = true)[0]
                playable.id = playable.episode!!.id

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
        if (playable.episode == null || playable.id == "") {
            Log.d(TAG, "PLAY -> Podcast not found!")
            return fulfillmentUtils.fallback(updState, notUnderstood=true)

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            //Wait 1 sec:
            Thread.sleep(1000)

            //Reply:
            var introText = defaultReplies.replyPlayIntro(playable)
            var detailText = utils.cleanString(defaultReplies.replyPlayDetail(playable), emojiOnly = true)
            val aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = introText
                ),
                AiReply(
                    langCode = reqLangCode,
                    text = detailText
                )
            )

            //updState:
            updState.aiReplies = aiReplies
            updState.actionType = ActionType.PLAY

            //Update message:
            extractorInfo.reqLanguage = reqLangCode
            updState.actionType = updState.actionType
            updState.attachments.nlpExtractor = extractorInfo
            updState.attachments.spotifyPlay = playable
            updState.attachments.spotifyPlay = playable
        }

        //Build return
        Log.d(TAG, updState.toString())
        return updState
    }

}