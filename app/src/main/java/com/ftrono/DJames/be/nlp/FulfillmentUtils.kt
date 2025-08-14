package com.ftrono.DJames.be.nlp

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.chatLastDispatch
import com.ftrono.DJames.application.datetimeFullFormat
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.gMapsLinkFormat
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.database.ItemInfoUse
import com.ftrono.DJames.be.database.Route
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class FulfillmentUtils {
    private val TAG = FulfillmentUtils::class.java.simpleName

    //FALLBACK:
    fun fallback(
        notUnderstood: Boolean = false,
        notLoggedIn: Boolean = false,
        noPermission: Boolean = false,
        nevermind: Boolean = false
    ): DispatcherInfo {
        //Build fallback response:
        var dispatcherInfo = DispatcherInfo()
        dispatcherInfo.fail = true
        dispatcherInfo.aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = if (notLoggedIn)
                    defaultReplies.replyNotLoggedIn()
                else if (noPermission)
                    defaultReplies.replyNoPermission()
                else if (notUnderstood)
                    defaultReplies.replyFallback()
                else if (nevermind)
                    defaultReplies.replyNevermind()
                else defaultReplies.replyError()
            )
        )
        return dispatcherInfo
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
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_ita)))   //"ita"
        } else {
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_eng)))   //"eng"
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


    //Route: build navigation URL from Library Route item:
    fun buildRouteUrlFromLibraryItem(item: Route): String {
        var url = gMapsLinkFormat
        //Via:
        var fullVia = listOf(
                item.via.placeName,
                item.via.address,
                item.via.number,
                item.via.zip,
                item.via.town,
                item.via.province
            ).joinToString(" ").trim()
        if (fullVia != "") {
            fullVia = fullVia + "/"
        }

        //Destination:
        var fullDestination = listOf(
            item.destination.placeName,
            item.destination.address,
            item.destination.number,
            item.destination.zip,
            item.destination.town,
            item.destination.province
        ).joinToString(" ").trim()

        return url + fullVia.replace(" ", "+") + fullDestination.replace(" ", "+")

    }


    //Route: build navigation URL from temp ItemInfo item:
    fun buildRouteUrlFromItemInfo(item: ItemInfoUse): String {
        var url = gMapsLinkFormat
        var viaUrl = ""
        if (item.detail != "") {
            viaUrl = item.detail.replace(" ", "+") + "/"
        }
        url = url + viaUrl + item.name.replace(" ", "+").trim() + "/"
        return url
    }

}