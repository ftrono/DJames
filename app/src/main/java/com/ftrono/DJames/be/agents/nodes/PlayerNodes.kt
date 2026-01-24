package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.mistralLlmModelSmall
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.llm.LlmAgent
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.agents.tools.*
import com.ftrono.DJames.be.agents.fulfillment.SpotifyFulfillment


// (LLM-based) ReAct agent node:
class PlayerAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val onComplete: String,
    override val onFallback: String,
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")
    val model = mistralLlmModelSmall

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            Help the user find music or podcasts they want to play. You are connected to Spotify and you can play songs, artists, albums, playlists, podcast episodes or their liked songs collection. Consider the context in the conversation and **use the available tools** to search and play the item the user is requesting **before replying** to them.
            
            **General rules**: 
            - You need to understand what the user wants to play from the context of the conversation, and retrieve the needed Spotify ID from 'tool_retrieve', in order to play it via 'tool_play'.
            
            ## TOOLS:
            You can use the following tools:
                * **tool_handoff**: use this tool if the user either: 
                    (i) wants to end, stop or restart the conversation; 
                    (ii) is requesting guidance or info about your capabilities; 
                    (iii) in **any case* the user makes a request outside your tasks scope.
                * **tool_retrieve**: get the Spotify ID of the requested item to play, if it exists in the knowledge base. **Always use this tool to retrieve the Spotify ID** for songs, artists, albums, playlists, podcast episodes or liked songs collection from your knowledge base before playing them!
                * **tool_play**: play the requested item in Spotify. Use this tool only **AFTER you retrieved from 'tool_retrieve' the Spotify ID** for the specific item to play.
            
            ## FURTHER INFO:
            - If the user's request is unclear, ask for clarification before proceeding with any tools. 
            - If the user's request includes additional information that is not relevant to the task, focus on the primary request and ignore the additional information.
            - **Always follow the indications you receive from the tools!**
        """.trimIndent()
        var inMessages = prepareInMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            model = model,
            agentName = name,
            onComplete = onComplete,
            onFallback = onFallback,
            tools = mapOf<String, Tool>(
                ToolHandoff().name to ToolHandoff(),
                ToolRetrievePlayer().name to ToolRetrievePlayer(),
                ToolPlay().name to ToolPlay(),
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        updState = updateStateFlow(updState, llmReturn)
        return updState
    }
}


// (Intent-based) Fulfillment node:
class PlayerIntentNode (
    private val context: Context,
    override val onComplete: String = "",
    override val onFallback: String = "",
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState
        var intentName = updState.intentName

        // Fork:
        var spotify = SpotifyFulfillment(context)
        if (!spotifyLoggedIn.value!!) {
            updState = fulfillmentUtils.fallback(updState, notLoggedIn=true)
        } else {
            when (intentName) {
                "PlaySong" -> {
                    updState = if (updState.isStart) spotify.playItem1(updState) else spotify.playSongAlbum2(updState)
                    updState.next = if (updState.isStart) name else END
                }
                "PlayAlbum" -> {
                    updState = if (updState.isStart) spotify.playItem1(updState) else spotify.playSongAlbum2(updState)
                    updState.next = if (updState.isStart) name else END
                }
                "PlayArtist" -> {
                    updState = if (updState.isStart) spotify.playItem1(updState) else spotify.playArtistPlaylist2(updState)
                    updState.next = if (updState.isStart) name else END
                }
                "PlayPlaylist" -> {
                    updState = if (updState.isStart) spotify.playItem1(updState) else spotify.playArtistPlaylist2(updState)
                    updState.next = if (updState.isStart) name else END
                }
                "PlayPodcast" -> {
                    updState = if (updState.isStart) spotify.playItem1(updState) else spotify.playPodcast2(updState)
                    updState.next = if (updState.isStart) name else END
                }
                "PlayCollection" -> {
                    updState = spotify.playCollection(updState)   // Mono
                    updState.next = END
                }
                else -> fulfillmentUtils.fallback(updState)
            }
        }

        // Update messages:
        if (updState.aiReplies.isNotEmpty()) {
            updState.messages.add(
                ChatMessage(
                    role = "assistant",
                    content = updState.aiReplies.joinToString(" ") { it.text },
                )
            )
        }
        return updState
    }
}