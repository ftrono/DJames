package com.ftrono.DJames.be.spotify

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastAiMessage
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotCollectionName
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.NlpQueryModel
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.SpotifyArtist
import com.ftrono.DJames.be.database.SpotifyContext
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.database.SpotifyPlaylist
import com.ftrono.DJames.be.models.ActionType
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.nlp.NLPExtractor
import com.ftrono.DJames.be.nlp.NLPMatcher


class SpotifyFulfillment (private var context: Context) {
    private val TAG = SpotifyFulfillment::class.java.simpleName


    //Play a song (with custom context), an album, an artist or a playlist: PART 1:
    fun playItem1(resultsNLP: NlpQueryModel): DispatcherInfo {
        var dispatcherInfo = DispatcherInfo()

        //Detect & process requested languages:
        var intentName = resultsNLP.intentName
        var detLanguage = resultsNLP.reqLanguage
        var reqLangCode = utils.getLanguageCode(detLanguage, prefs.queryLanguage)
        var reqLangName = utils.getLanguageName(reqLangCode)

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
            if (nlp_queryText.contains("playlist")) {
                contextType = "playlist"
            } else {
                if (nlp_queryText.contains("collection") || nlp_queryText.contains("liked") || nlp_queryText.contains("saved")) {
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

        //dispatcherInfo:
        dispatcherInfo.aiReplies = aiReplies
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

        //dispatcherInfo:
        dispatcherInfo.aiReplies = aiReplies
        dispatcherInfo.actionType = ActionType.PLAY

        //dispatcherInfo:
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())

        //Update message:
        lastAiMessage.actionType = dispatcherInfo.actionType
        lastAiMessage.attachments.spotifyPlay = playable
        dispatcherInfo.playable = playable
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

        //Extract play info:
        val nlpMatcher = NLPMatcher(context)
        var nlpExtractor = NLPExtractor(context)
        var extractorInfo = nlpExtractor.extractPlayInfo(nlp_queryText, reqLangCode, playType, contextType)

        contextType = extractorInfo.contextType
        var artistExtracted = extractorInfo.artistExtracted
        var artistConfirmed = ""
        var contextExtracted = extractorInfo.contextExtracted

        if (artistExtracted != "") {
            //Confirm artists with library check:
            artistConfirmed = nlpExtractor.checkArtists(resultsNLP.artists, artistExtracted, reqLangCode)
            val libMatchId = nlpMatcher.matchLibrary("artist", artistConfirmed)
            if (libMatchId > -1) {
                artistConfirmed = libUtils.getLibItemName(libMatchId)
            }
            extractorInfo.artistConfirmed = artistConfirmed
        }

        //GET SPOTIFY PLAY INFO:
        //Search tracks + album:
        var search = SpotifySearch(context)
        var playable = search.searchPlayable(searchData=extractorInfo)   //TODO: Tracks & albums don't pass through Library yet!

        //A) EMPTY QUERY RESULT:
        if (playable.id == "") {
            return fulfillmentUtils.fallback(notUnderstood=true)

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
                    val nlpMatcher = NLPMatcher(context)
                    val libMatchId = nlpMatcher.matchLibrary("playlist", contextExtracted)

                    if (libMatchId > -1) {
                        Log.d(TAG, "Context -> Playlist in lib")
                        //Get playlist URL:
                        val itemInfo = libUtils.getLibItemById(libMatchId)
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

            //dispatcherInfo:
            dispatcherInfo.aiReplies = aiReplies
            dispatcherInfo.actionType = ActionType.PLAY

            //Update message:
            extractorInfo.reqLanguage = reqLangCode
            lastAiMessage.actionType = dispatcherInfo.actionType
            lastAiMessage.attachments.nlpExtractor = extractorInfo
            lastAiMessage.attachments.spotifyPlay = playable
            dispatcherInfo.playable = playable
        }

        //Build return
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())
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
        var playable = SpotifyPlayable(
            type=playType
        )

        //Extractor:
        var nlpExtractor = NLPExtractor(context)
        val nlpMatcher = NLPMatcher(context)
        extractorInfo.matchExtracted = nlp_queryText
        extractorInfo.playType = playType
        extractorInfo.contextType = playType

        // A) ARTIST:
        if (playType == "artist") {
            //Confirm artists with library check:
            val artistExtracted = nlpExtractor.checkArtists(resultsNLP.artists, matchName, reqLangCode)
            val libMatchId = nlpMatcher.matchLibrary("artist", artistExtracted)

            if (libMatchId > -1) {
                Log.d(TAG, "PLAY -> Artist Top Tracks")
                //Build playable:
                val itemInfo = libUtils.getLibItemById(libMatchId)
                playable.id = spotifyUtils.getSpotifyID(itemInfo.url)
                playable.artist = SpotifyArtist(
                    id = playable.id,
                    name = itemInfo.name
                )
            }

        //B) PLAYLIST:
        } else if (playType == "playlist") {
            //Check playlist in library:
            val nlpMatcher = NLPMatcher(context)
            val libMatchId = nlpMatcher.matchLibrary(playType, matchName)

            if (libMatchId > -1) {
                //Build playable:
                Log.d(TAG, "PLAY -> Playlist in lib")
                val itemInfo = libUtils.getLibItemById(libMatchId)
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
            playable = search.searchPlayable(extractorInfo)
        }

        //PLAY:
        //A) EMPTY QUERY RESULT:
        if (playable.id == "") {
            Log.d(TAG, "PLAY -> Artist / Playlist not found!")
            return fulfillmentUtils.fallback(notUnderstood=true)

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

            //dispatcherInfo:
            dispatcherInfo.aiReplies = aiReplies
            dispatcherInfo.actionType = ActionType.PLAY

            //Update message:
            extractorInfo.reqLanguage = reqLangCode
            lastAiMessage.actionType = dispatcherInfo.actionType
            lastAiMessage.attachments.nlpExtractor = extractorInfo
            lastAiMessage.attachments.spotifyPlay = playable
            dispatcherInfo.playable = playable
        }

        //Build return
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())
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
        var playable = SpotifyPlayable(
            type="episode",   //IMPORTANT!
        )

        //Extractor:
        val nlpMatcher = NLPMatcher(context)
        extractorInfo.matchExtracted = nlp_queryText
        extractorInfo.playType = playType
        extractorInfo.contextType = playType


        //PODCAST:
        val libMatchId = nlpMatcher.matchLibrary(playType, matchName)
        if (libMatchId > -1) {
            Log.d(TAG, "PLAY -> Podcast in lib")
            //1) Context -> Podcast:
            val itemInfo = libUtils.getLibItemById(libMatchId)

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
            return fulfillmentUtils.fallback(notUnderstood=true)

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

            //dispatcherInfo:
            dispatcherInfo.aiReplies = aiReplies
            dispatcherInfo.actionType = ActionType.PLAY

            //Update message:
            extractorInfo.reqLanguage = reqLangCode
            lastAiMessage.actionType = dispatcherInfo.actionType
            lastAiMessage.attachments.nlpExtractor = extractorInfo
            lastAiMessage.attachments.spotifyPlay = playable
            dispatcherInfo.playable = playable
        }

        //Build return
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())
        return dispatcherInfo
    }

}