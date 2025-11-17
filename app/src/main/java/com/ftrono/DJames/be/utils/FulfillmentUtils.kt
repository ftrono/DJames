package com.ftrono.DJames.be.utils

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.data.StateInfo
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader


class FulfillmentUtils {
    private val TAG = this::class.java.simpleName

    //FALLBACK:
    fun fallback(
        notUnderstood: Boolean = false,
        notLoggedIn: Boolean = false,
        noPermission: Boolean = false,
        nevermind: Boolean = false,
        cannotRecordWAVoice: Boolean = false,
        notAvailable: Boolean = false,
        noSave: Boolean = false,
    ): StateInfo {
        //Build fallback response:
        return StateInfo(
            lastRecording = lastRecordingName,
            fail = true,
            noSave = noSave,
            aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = if (notLoggedIn)
                        defaultReplies.replyNotLoggedIn()
                    else if (noPermission)
                        defaultReplies.replyNoPermission()
                    else if (notAvailable)
                        defaultReplies.replyNotAvailable()
                    else if (notUnderstood)
                        defaultReplies.replyFallback()
                    else if (nevermind)
                        defaultReplies.replyNevermind()
                    else if (cannotRecordWAVoice)
                        defaultReplies.replyMessageCannotRecord()
                    else defaultReplies.replyError()
                )
            )
        )
    }


    //Join replies:
    fun joinReplies(replies: List<AiReply>): String {
        var fullText = ""
        for (reply in replies) {
            fullText = fullText + reply.text
        }
        return fullText
    }


    //Replace nums with strings (& opposite):
    fun replaceNums(text: String): String {
        //TODO: Replace words with numbers too.
        val regexMap = mapOf(
            "\\d*000\\b" to "thousand",
            "\\d*,000\\b" to "thousand",
            "\\d*00\\b" to "hundred",
            "thousand" to "1000",
            "hundred" to "100"
        )

        var number = false
        var newText = text
        val textSplits = text.lowercase().split(" ")
        for (tok in textSplits) {
            if (tok == "number") {
                number = true
            }
            //tok -> entire word:
            for (regStr in regexMap.keys) {
                val regex = Regex(regStr)
                //match -> only fixed portion:
                val match = regex.find(tok)?.value
                if (match != null) {
                    Log.d(TAG, "REGEX MATCHED: $match")
                    //If match: if "number", remove the word "number" and
                    if (number) {
                        newText = newText.replace("number", "")
                    } else {
                        //val cleaned = tok.replace(match, "").replace(",", "")
                        newText = newText.replace(tok, regexMap[regStr]!!)
                    }
                    break
                }
            }
        }
        return newText
    }


    //Emoji replacer:
    fun replaceEmojis(context: Context, text: String, reqLanguage: String): String {
        var textReplaced = text
        var reader: BufferedReader? = null
        //Query language:
        var messLanguage = prefs.messageLanguage
        if (reqLanguage != "") {
            messLanguage = reqLanguage
        }
        //language:
        if (messLanguage == "it") {
            reader =
                BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_ita)))   //"ita"
        } else {
            reader =
                BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_eng)))   //"eng"
        }

        //Load map:
        val sourceSents = JsonParser.parseReader(reader).asJsonObject
        val emojiMap = sourceSents.get("emojiMap").asJsonObject

        //Replace:
        textReplaced = textReplaced.replace("Emoji", "emoji")
        textReplaced = textReplaced.replace("emoji,", "emoji")
        textReplaced = textReplaced.replace("emoji.", "emoji")
        textReplaced = textReplaced.replace("emoji?", "emoji")

        for (sent in emojiMap.keySet()) {
            textReplaced = textReplaced.replace("emoji $sent", emojiMap.get(sent).asString, ignoreCase=true)
        }
        return textReplaced
    }

}