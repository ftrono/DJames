package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.defaultChatWait
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.minSpeechPct
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.kaigraph.data.StateInfo
import com.ftrono.DJames.kaigraph.graph.Graph
import com.ftrono.DJames.kaigraph.data.ChatMessage
import com.ftrono.DJames.be.agents.nodes.CallIntentNode
import com.ftrono.DJames.be.agents.nodes.DriverIntentNode
import com.ftrono.DJames.be.agents.nodes.IntentRouterNode
import com.ftrono.DJames.be.agents.nodes.MessageIntentNode
import com.ftrono.DJames.be.agents.nodes.PlayerIntentNode
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.database.NlpQueryModel
import com.ftrono.DJames.be.agents.fulfillment.NLPQuery
import com.ftrono.DJames.be.models.RecDetails


// INTENTS GRAPH:
class IntentsGraph(
    private val context: Context,
): Graph(context) {

    override val name = this::class.java.simpleName
    override var TAG = name

    override fun build() {
        // Define the graph structure:
        addNodes(
            nodes = mutableListOf(
                // MAIN ROUTER:
                IntentRouterNode(
                    context = context,
                    nextOptions = listOf(
                        "PlayerIntent",
                        "CallIntent",
                        "MessageIntent",
                        "DriverIntent",
                        END
                    ),
                ),

                // NODES:
                PlayerIntentNode(
                    context = context,
                    onComplete = END,
                ),

                CallIntentNode(
                    context = context,
                    onComplete = END,
                ),

                MessageIntentNode(
                    context = context,
                    onComplete = END,
                ),

                DriverIntentNode(
                    context = context,
                    onComplete = END,
                ),
            )
        )
        Log.d(TAG, "Graph built!")
    }

    // Process new user message:
    override fun processUserMessage(
        prevState: StateInfo,
        recDetails: RecDetails?,
        inMessage: String,
    ): StateInfo {

        // Status vars:
        var updState = prevState
        var fromVoice = recDetails != null
        updState.fromVoice = fromVoice
        updState.noSave = false
        var isStart = updState.next == START
        var resultsNlp: NlpQueryModel? = null
        val attachments = if (updState.messageMode) updState.attachments else Attachments()
        Log.d(TAG, "Processing new user message...")

        // Parse new message:
        // Intent version: always call NLP Intent classifier:
        var transcription = ""

        if (fromVoice && recDetails!!.speechPct <= minSpeechPct) {
            // Empty audio:
            Log.d(TAG, "Empty audio.")
            updState.isSilence = true
            updState.fail = true
            updState.noSave = true

        } else if (updState.messageMode && updState.messageType == "voice" && fromVoice) {
            //Whatsapp audio message -> no NLP query!
            transcription = "(voice message recorded)"
            Log.d(TAG, "Voice message recorded - no STT transcription.")

        } else {
            if (fromVoice) updState.lastRecording = recDetails!!.recName
            // (Intent) STT + Intent Classifier:
            Log.d(TAG, "NLPQuery activated")
            val nlpQuery = NLPQuery(context)
            resultsNlp = nlpQuery.queryNLP(
                text = inMessage,
                recDetails = recDetails,
                messageMode = updState.messageMode,
                reqLanguage = updState.reqLangCode
            )

            // Process:
            transcription = fulfillmentUtils.replaceNums(resultsNlp.queryText)
            updState.isSilence = (transcription == "")
            updState.fail = (
                transcription == "" || (isStart && resultsNlp.intentName == "Fallback")
            )
            // Keep previous intent in case of follow-ups:
            if (isStart) updState.intentName = resultsNlp.intentName
            if (resultsNlp.intentName == "Cancel") {
                // Cancel:
                updState = fulfillmentUtils.fallback(updState, nevermind = true)
                updState.next == END
            }

            // ReqLanguage selection:
            val defaultLang = if (resultsNlp.intentName == "DriveRequest") {
                    prefs.placeLanguage
                } else if (resultsNlp.intentName == "MessageRequest") {
                    prefs.messageLanguage
                } else {
                    prefs.queryLanguage
                }
            if (isStart && resultsNlp.reqLanguage != "") {
                updState.reqLangCode = utils.getLanguageCode(
                    language = resultsNlp.reqLanguage,   // Explicitly requested language by voice
                    default = defaultLang,
                )
            } else if (isStart) {
                updState.reqLangCode = defaultLang   // Intent default
            } else {
                updState.reqLangCode = resultsNlp.language   // Audio language
            }
            updState.reqLangName = utils.getLanguageName(updState.reqLangCode)
            Log.d(TAG, "LANGUAGES: detLanguage: ${resultsNlp.reqLanguage}, reqLangCode: ${updState.reqLangCode}, reqLanguageName: ${updState.reqLangName}")

            attachments.entityArtists = resultsNlp.artists
            attachments.nlpQueries = mutableListOf<NlpQueryModel>()
            attachments.nlpQueries!!.add(resultsNlp)

            if (updState.isSilence) {
                updState.noSave = true
                Log.d(TAG, "Empty NLP query text.")
            } else {
                Log.d(TAG, "NLQ QUERY TEXT: $transcription")
            }
        }

        if (!updState.isSilence) {
            // STATE: Append last user message (original) to conversation:
            updState.messages.add(
                ChatMessage(role = "user", content = transcription)
            )
            updState.attachments = attachments   // Reset attachments
            // DB: Store user message (anonymized):
            val storedText = if (updState.messageMode && updState.messageType != "voice") "(private message)" else transcription
            updState.lastUserMsgId = messageUtils.storeMessage(
                context = context,
                langCode = if (updState.reqLangCode != "") updState.reqLangCode else "en",
                fromUser = true,
                fromVoice = fromVoice,
                isStart = isStart,
                text = storedText,
                intent = updState.intentName,
                attachments = attachments,
            )
        } else {
            // Cancel:
            updState = fulfillmentUtils.fallback(updState, notUnderstood = true)
            updState.noSave = true
            updState.messageMode = false
            updState.actionType = null
            updState.next = END
        }

        // Typing delay:
        if (inMessage != "") {
            Thread.sleep(defaultChatWait)
        }
        return updState
    }

    override fun invoke(
        prevState: StateInfo,
        recDetails: RecDetails?,
        inMessage: String,
    ): StateInfo {

        // Status vars:
        var updState = prevState
        Log.d(TAG, "Invoking Graph...")

        // Process new message:
        updState = processUserMessage(
            prevState = updState,
            recDetails = recDetails,
            inMessage = inMessage,
        )

        Log.d(TAG, "Messages size (input): ${updState.messages.size} items.")

        // STREAMING LOOP:
        updState = stream(updState, routerNodes = listOf("IntentRouter"))
        Log.d(TAG, "Messages size (output): ${updState.messages.size} items, fail: ${updState.fail}")

        updState.fullReply = updState.aiReplies.joinToString(" ") { it.text }
        return updState
    }
}