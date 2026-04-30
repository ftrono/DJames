package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.mistralLlmModelMedium
import com.ftrono.DJames.kaigraph.LlmAgent
import com.ftrono.DJames.kaigraph.StateInfo
import com.ftrono.DJames.be.agents.handoffDescription
import com.ftrono.DJames.be.agents.tools.*
import com.ftrono.DJames.kaigraph.Node
import com.ftrono.DJames.kaigraph.Tool


// ReAct agent node:
class PlayerAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val onComplete: String,
    override val onFallback: String,
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")
    val model = mistralLlmModelMedium

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        val corePrompt = """
            ## TASK:
            You're in charge of every request regarding playing music or podcasts.
            Help the user find music or podcasts they want to play. You are connected to Spotify and you can play songs, artists, albums, playlists, podcast episodes or their liked songs collection. Consider the context in the conversation and **use the available tools** to search and play the item the user is requesting **before replying** to them.
            
            **General rules**: 
            - You need to understand what the user wants to play from the context of the conversation, and retrieve the needed Spotify ID from 'tool_retrieve', in order to play it via 'tool_play'.
            - If no info is available from the conversation or the user is requesting if you can play some music / podcast, ask the user directly what he'd like to listen to.
            
            ## TOOLS:
            You can use the following tools:
                * **tool_handoff**: use this tool if either: 
                    (i) the user is asking to end, stop or restart the conversation; 
                    (ii) the user is requesting guidance or info about your capabilities; 
                    (iii) in **any case* the user makes a request outside your tasks scope.
                * **tool_retrieve**: get the Spotify ID of the requested item to play, if it exists in the knowledge base. **Always use this tool to retrieve the Spotify ID** for songs, artists, albums, playlists, podcast episodes or liked songs collection from your knowledge base before playing them!
                * **tool_play**: play the requested item in Spotify. Use this tool only **AFTER you retrieved from 'tool_retrieve' the Spotify ID** for the specific item to play.
            
            ## FURTHER INFO:
            - If the user's request is unclear, ask for clarification before proceeding with any tools. 
            - If the user's request includes additional information that is not relevant to the task, focus on the primary request and ignore the additional information.
            - **Reply in the same language in which the user is speaking!** 
            - **Always follow the indications you receive from the tools!**
        """.trimIndent()

        val inMessages = prepareInMessages(
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
                ToolHandoff(handoffDescription).name to ToolHandoff(handoffDescription),
                ToolRetrievePlayer(context).name to ToolRetrievePlayer(context),
                ToolPlay(context).name to ToolPlay(context),
            ),
        )

        val llmReturn = llmAgent.invoke(
            llmMessages = inMessages,
            attachments = updState.attachments
        )

        updState = updateStateFromNode(updState, llmReturn)
        return updState
    }
}