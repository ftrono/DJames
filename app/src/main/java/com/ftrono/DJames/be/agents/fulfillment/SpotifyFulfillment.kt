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
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.kaigraph.data.StateInfo
import com.ftrono.DJames.be.database.ActionType
import com.ftrono.DJames.be.spotify.SpotifySearch


class SpotifyFulfillment (private var context: Context) {
    private val TAG = this::class.java.simpleName

    //Play a song (with custom context), an album, an artist or a playlist: PART 1:
    fun playItem1(prevState: StateInfo): StateInfo {
        // Manage state:
        var updState = prevState
        var intentName = updState.intentName
        var queryText = updState.messages.last().content

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
        val ttsToRead = defaultReplies.replyPlayRequest(intentName, updState.reqLangName, contextType)
        val aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = ttsToRead
            )
        )

        //updState:
        updState.interrupt = true
        updState.aiReplies = aiReplies
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
        updState.attachments.spotifyPlay = playable
        Log.d(TAG, updState.toString())

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
            val libMatches = libUtils.matchLibrary("artist", artistConfirmed)
            if (libMatches.isNotEmpty()) {
                artistConfirmed = libMatches[0].matchName
            }
            extractorInfo.artistConfirmed = artistConfirmed
        }

        //GET SPOTIFY PLAY INFO:
        //Search tracks + album:
        var search = SpotifySearch(context)
        var spotQuery = search.searchPlayable(
            playType = playType,
            matchName = extractorInfo.matchExtracted.lowercase(),
            detailName = extractorInfo.artistConfirmed.lowercase(),
        )   //TODO: Tracks & albums don't pass through Library yet!

        //A) EMPTY QUERY RESULT:
        if (spotQuery.spotifyMatches.isEmpty()) {
            return fulfillmentUtils.fallback(updState, notUnderstood=true)

        } else {
            //B) SPOTIFY RESULT RECEIVED!
            spotQuery.spotifyMatches = search.rescoreResults(playType, spotQuery.spotifyMatches)
            val playable = spotQuery.spotifyMatches[0]
            updState.attachments.spotifyQueries = mutableListOf(spotQuery)
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
                    val libMatches = libUtils.matchLibrary("playlist", contextExtracted)

                    if (libMatches.isNotEmpty()) {
                        Log.d(TAG, "Context -> Playlist in lib")
                        //Get playlist URL:
                        val itemInfo = libUtils.getLibItemById(libMatches[0].matchId)
                        playable.track!!.context = SpotifyContext(
                            type = "playlist",
                            name = itemInfo.name,
                            id = spotifyUtils.getSpotifyID(itemInfo.url)
                        )
                        extractorInfo.contextConfirmed = itemInfo.name
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
            extractorInfo.reqLanguage = reqLangCode
            updState.attachments.nlpExtractor = extractorInfo
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
            val libMatches = libUtils.matchLibrary("artist", artistExtracted)

            if (libMatches.isNotEmpty()) {
                Log.d(TAG, "PLAY -> Artist Top Tracks")
                //Build playable:
                val itemInfo = libUtils.getLibItemById(libMatches[0].matchId)
                playable.matchScore = libMatches[0].matchScore
                playable.id = spotifyUtils.getSpotifyID(itemInfo.url)
                playable.artist = SpotifyArtist(
                    id = playable.id,
                    name = itemInfo.name
                )
            }

        //B) PLAYLIST:
        } else if (playType == "playlist") {
            //Check playlist in library:
            val libMatches = libUtils.matchLibrary(playType, queryText)

            if (libMatches.isNotEmpty()) {
                //Build playable:
                Log.d(TAG, "PLAY -> Playlist in lib")
                val itemInfo = libUtils.getLibItemById(libMatches[0].matchId)
                playable.matchScore = libMatches[0].matchScore
                playable.id = spotifyUtils.getSpotifyID(itemInfo.url)
                playable.playlist = SpotifyPlaylist(
                    id = playable.id,
                    name = itemInfo.name
                )
            }
        }

        //Search in Spotify:
        if (playable.id == "") {
            val search = SpotifySearch(context)
            var spotQuery = search.searchPlayable(
                playType = playType,
                matchName = extractorInfo.matchExtracted.lowercase(),
                detailName = extractorInfo.artistConfirmed.lowercase(),
            )   //TODO: Tracks & albums don't pass through Library yet!

            //A) EMPTY QUERY RESULT:
            if (spotQuery.spotifyMatches.isEmpty()) {
                return fulfillmentUtils.fallback(updState, notUnderstood=true)

            } else {
                //B) SPOTIFY RESULT RECEIVED!
                spotQuery.spotifyMatches = search.rescoreResults(playType, spotQuery.spotifyMatches)
                playable = spotQuery.spotifyMatches[0]
                updState.attachments.spotifyQueries = mutableListOf(spotQuery)
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
            extractorInfo.reqLanguage = reqLangCode
            updState.attachments.nlpExtractor = extractorInfo
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
        val libMatches = libUtils.matchLibrary(playType, queryText)
        if (libMatches.isNotEmpty()) {
            Log.d(TAG, "PLAY -> Podcast in lib")
            //1) Context -> Podcast:
            val itemInfo = libUtils.getLibItemById(libMatches[0].matchId)
            playable.matchScore = libMatches[0].matchScore

            //2) Item -> GET latest podcast episode:
            //TODO: latest episode only!
            try {
                val podcastId = spotifyUtils.getSpotifyID(itemInfo.url)
                playable.episode = spotifyUtils.getPodcastEpisodes(context, podcastId, itemInfo.name, latestOnly = true)[0]
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
            extractorInfo.reqLanguage = reqLangCode
            updState.attachments.nlpExtractor = extractorInfo
            updState.attachments.spotifyPlay = playable
        }

        //Build return
        Log.d(TAG, updState.toString())
        return updState
    }

}