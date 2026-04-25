package com.ftrono.DJames.be.agents.tools

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.maxSearchMatches
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.kaigraph.data.ToolDefinition
import com.ftrono.DJames.kaigraph.data.ToolFunction
import com.ftrono.DJames.kaigraph.data.ToolParameters
import com.ftrono.DJames.kaigraph.data.ToolProperty
import com.ftrono.DJames.kaigraph.data.ToolResponse
import com.ftrono.DJames.kaigraph.data.ToolType
import com.ftrono.DJames.be.database.ActionType
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.database.LibMatch
import com.ftrono.DJames.be.database.PlayRequest
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.spotify.SpotifySearch
import com.ftrono.DJames.kaigraph.tool.Tool
import kotlinx.serialization.json.*

data class QueryAPIReturn(
    var candidates: MutableList<SpotifyPlayable> = mutableListOf(),
    var attachments: Attachments = Attachments()
)


class ToolRetrievePlayer(
    private val context: Context,
): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_retrieve"
    override val type: ToolType = ToolType.INTERMEDIATE
    val spotSearch = SpotifySearch(context)

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                    Get the Spotify ID of the requested item to play. You must pass here ALL the parameters you collect from the user conversation (i.e. artist name, track name, ...). 
                    **Always use this tool to retrieve the Spotify ID** for songs, artists, albums, playlists, podcast episodes or liked songs collection from your knowledge base before playing them!""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "artist" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name(s) of the requested music artist(s) / band(s) (if any). If multiple artists, separate them by a comma."   // Arg description
                        ),
                        "track" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested music track / song (if any)."   // Arg description
                        ),
                        "album" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested music album (if any)."   // Arg description
                        ),
                        "playlist" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested music playlist or collection (if any)."   // Arg description
                        ),
                        "podcast" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested music podcast (if any)."   // Arg description
                        ),
                    ),
                )
            )
        )
    }

    // Prepare original query + add Library matches (avoiding duplicates):
    fun loadCandidates(queryType: String, query: String, maxMatches: Int = maxSearchMatches): MutableList<LibMatch> {
        val selMatches = mutableListOf<LibMatch>()

        // Get library matches:
        val libMatches = if (queryType == "track") {
            mutableListOf()
        } else libUtils.matchLibrary(
            filter = queryType.lowercase(),
            text = query.lowercase(),
            threshold = midThreshold
        )

        // If collection:
        if (libMatches.isNotEmpty() && libMatches[0].matchId == -2L) {
            return mutableListOf(libMatches[0])
        }

        // If query not in library matches -> Add as first empty match:
        if (!libMatches.map{ it.matchName.lowercase() }.contains(query)) {
            selMatches.add(
                LibMatch(
                    matchId = -1L,
                    matchName = query
                )
            )
        }

        // Add sorted library matches up to maxMatches:
        for (match in libMatches) {
            if (selMatches.size < maxMatches) {
                selMatches.add(match)
            } else {
                break
            }
        }
        return selMatches
    }

    // Query API and extract results:
    fun queryAPI(searchType: String, attachments: Attachments, matchName: String, detailName: String = ""): QueryAPIReturn {
        var updAttachments = attachments
        val candidates = mutableListOf<SpotifyPlayable>()
        val curQuery = spotSearch.searchPlayable(
            playType = searchType,
            matchName = matchName.lowercase(),
            detailName = detailName.lowercase(),
        )
        if (curQuery.spotifyMatches.isNotEmpty()) {
            curQuery.spotifyMatches = spotSearch.rescoreResults(searchType, curQuery.spotifyMatches)
        }
        updAttachments.spotifyQueries!!.add(curQuery)

        // Select N best:
        if (curQuery.spotifyMatches.size <= maxSearchMatches) {
            candidates.addAll(curQuery.spotifyMatches)
        } else {
            candidates.addAll(curQuery.spotifyMatches.slice(0..maxSearchMatches))
        }

        return QueryAPIReturn(
            candidates = candidates,
            attachments = updAttachments
        )
    }

    // MAIN: INVOKE:
    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        // INIT:
        var retString = ""
        var updAttachments = attachments
        updAttachments.spotifyQueries = mutableListOf()   // Reset

        val playRequest = PlayRequest(
            source = "spotify",   // TODO
            track = (args["track"]?.jsonPrimitive?.content ?: "").lowercase().trim(),
            artist = (args["artist"]?.jsonPrimitive?.content ?: "").lowercase().trim(),
            album = (args["album"]?.jsonPrimitive?.content ?: "").lowercase().trim(),
            playlist = (args["playlist"]?.jsonPrimitive?.content ?: "").lowercase().trim(),
            podcast = (args["podcast"]?.jsonPrimitive?.content ?: "").lowercase().trim(),
        )

        // Queries (max 2 items per list):
        val matchNames = mutableListOf<LibMatch>()   // all
        val matchDetails = mutableListOf<LibMatch>()   // artist only
        val matchContexts = mutableListOf<LibMatch>()   // playlist or collection

        // 1) PLAY HIERARCHY:
        if (playRequest.podcast != "") {
            // PLAY PODCAST:
            playRequest.type = "podcast"
            matchNames.addAll(loadCandidates("podcast", playRequest.podcast))

        } else if (playRequest.album != "") {
            // PLAY ALBUM:
            playRequest.type = "album"
            matchNames.addAll(loadCandidates("album", playRequest.album))
            if (playRequest.artist != "") {
                matchDetails.addAll(loadCandidates("artist", playRequest.artist))
            }

        } else if (playRequest.track != "") {
            // PLAY TRACK:
            playRequest.type = "track"
            matchNames.addAll(loadCandidates("track", playRequest.track))
            if (playRequest.artist != "") {
                matchDetails.addAll(loadCandidates("artist", playRequest.artist))
            }
            if (playRequest.playlist != "") {
                matchContexts.addAll(loadCandidates("playlist", playRequest.playlist))
                playRequest.context = "playlist"
            }

        } else if (playRequest.playlist != "") {
            // PLAY PLAYLIST:
            playRequest.type = "playlist"
            matchNames.addAll(loadCandidates("playlist", playRequest.playlist))

        } else if (playRequest.artist != "") {
            // PLAY ARTIST:
            playRequest.type = "artist"
            matchNames.addAll(loadCandidates("artist", playRequest.artist))
        }

        Log.d(TAG, "PLAY TYPE: ${playRequest.type}")
        Log.d(TAG, "MATCH NAMES: $matchNames")
        Log.d(TAG, "MATCH DETAILS: $matchDetails")
        Log.d(TAG, "MATCH CONTEXTS: $matchContexts")

        // Fallback:
        if (playRequest.type == "" || matchNames.isEmpty()) {
            retString = "This tool was called with no input args: try again passing the correct input information."

        } else {
            // 2) QUERY SPOTIFY:
            updAttachments.playCandidates = mutableListOf<SpotifyPlayable>()
            for (nameMatch in matchNames) {
                if (nameMatch.matchId != -1L) {
                    // Add from library (no API search):
                    updAttachments.playCandidates!!.add(
                        libUtils.libItemToPlayable(
                            libItem = libUtils.getLibItemById(nameMatch.matchId),
                            matchScore = nameMatch.matchScore
                        )
                    )
                } else {
                    // API search:
                    if (matchDetails.isEmpty()) {
                        // Query name only:
                        val queryAPIReturn = queryAPI(playRequest.type, updAttachments, nameMatch.matchName)
                        updAttachments = queryAPIReturn.attachments
                        updAttachments.playCandidates!!.addAll(queryAPIReturn.candidates)
                    } else {
                        // Add artist filter:
                        for (detailMatch in matchDetails) {
                            val queryAPIReturn = queryAPI(playRequest.type, updAttachments, nameMatch.matchName, detailMatch.matchName)
                            updAttachments = queryAPIReturn.attachments
                            updAttachments.playCandidates!!.addAll(queryAPIReturn.candidates)
                        }
                    }
                }
            }
            // TODO: match context!

            Log.d(TAG, "PLAY CANDIDATES: ${updAttachments.playCandidates}!!")

            // 3) PREPARE TOOL RESPONSE:
            if (updAttachments.playCandidates!!.isEmpty()) {
                // Fallback:
                retString = "Reply that you could not find any results. Ask the user if he/she wants to search for something else or to specify better what to search for."

            } else if (updAttachments.playCandidates!!.size == 1) {
                // Success -> one match:
                retString = """
                ${utils.capitalizeWords(playRequest.type)} found! Spotify ID: ${updAttachments.playCandidates!![0].id}.
                Call tool 'tool_play' with this ID.
                """.trimMargin()

            } else {
                var candidateStr = ""
                for (playable in updAttachments.playCandidates!!) {
                    when (playable.type) {
                        "artist" -> {
                            candidateStr += "\n- spotifyId: ${playable.id}, name: ${playable.artist!!.name}"
                        }

                        "podcast" -> {
                            candidateStr += "\n- spotifyId: ${playable.id}, name: ${playable.podcast!!.name}"
                        }

                        "playlist" -> {
                            candidateStr += "\n- spotifyId: ${playable.id}, name: ${playable.playlist!!.name}, by: ${playable.playlist!!.owner}"   // TODO: Collection!!!
                        }

                        "album" -> {
                            candidateStr += "\n- spotifyId: ${playable.id}, name: ${playable.album!!.name}, by: ${playable.album!!.artists.joinToString { it.name }}"
                        }

                        "track" -> {
                            candidateStr += "\n- spotifyId: ${playable.id}, name: ${playable.track!!.name}, by: ${playable.track!!.artists.joinToString { it.name }}"
                        }
                    }
                }
                if (candidateStr == "") {
                    // Fallback:
                    retString =
                        "Reply that you could not find any results. Ask the user if he/she wants to search for something else or to specify better what to search for."
                } else {
                    // Success -> multiple matches:
                    retString = """
                        ${utils.capitalizeWords(playRequest.type)}s found:
                        (don't read Spotify IDs to the user):
                        [CANDIDATES]
                       
                        If you can clearly identify what the user is requesting among them, just call tool 'tool_play' with that result's Spotify ID.
                        Otherwise, read to the user the most relevant results based on the query (in plain text, no markdown) and ask them which of these results they want to play.
                        """.trimIndent()
                    retString = retString.replace("[CANDIDATES]", candidateStr)
                }
            }
        }

        // Return:
        return ToolResponse(
            message = retString,
            attachments = updAttachments,
        )
    }
}


class ToolPlay(
    private val context: Context,
): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_play"
    override val type: ToolType = ToolType.ACTION

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                     Play the requested music item in Spotify. **Use this tool only after** you retrieved from 'tool_retrieve' the Spotify ID for the specific item to play.""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "spotify_id" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Spotify ID of the requested item to play."   // Arg description
                        ),
                    ),
                    required = mutableListOf("spotify_id")
                )
            )
        )
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        val spotifyID: String = (args["spotify_id"]?.jsonPrimitive?.content ?: "")

        if (spotifyID == "" || attachments.playCandidates == null) {
            Log.w(TAG, "ERROR: ToolPlay invoked with either missing spotifyID ($spotifyID) or attachments!")
            return ToolResponse(
                message = "Reply that there was a problem. Do NOT ask further questions to the user.",
                attachments = attachments,
            )

        } else {
            // Retrieve from attachments:
            val playMatches = attachments.playCandidates!!.filter { it.id == spotifyID }
            if (playMatches.isNotEmpty()) {
                val playMatch = playMatches[0]
                var detailInfo = ""

                if (playMatch.type == "podcast") {
                    // Podcast -> GET latest podcast episode:
                    //TODO: latest episode only!
                    try {
                        playMatch.episode = spotifyUtils.getPodcastEpisodes(
                            context = context,
                            podcastId = playMatch.id,
                            podcastName = playMatch.podcast!!.name,
                            latestOnly = true
                        )[0]
                        playMatch.id = playMatch.episode!!.id
                        playMatch.type = "episode"
                        detailInfo = ", latest episode dated ${utils.readDate(
                            date=playMatch.episode!!.releaseDate, inputFormat="yyyy-MM-dd")
                        } (read it)"
                        Log.d(TAG, "Episode: $playMatch")

                    } catch (e: Exception) {
                        // Episodes not found:
                        Log.d(TAG, "Podcast episodes not found! ", e)
                        return ToolResponse(
                            message = "Reply that you could not find the latest episode for the requested podcast.",
                            attachments = attachments,
                        )
                    }
                }

                attachments.spotifyPlay = playMatch
                attachments.actionType = ActionType.PLAY
                return ToolResponse(
                    message = "Playing the ${playMatch.type} with Spotify ID: $spotifyID$detailInfo. Always tell the user what you're playing and do NOT ask further questions to the user.",
                    attachments = attachments,
                )

            } else {
                return ToolResponse(
                    message = "Cannot find the requested song. Read them again to the user.",
                    attachments = attachments,
                )
            }
        }
    }
}
