package com.ftrono.DJames.be.agents.chat

import com.ftrono.DJames.application.dictatedNumber
import com.ftrono.DJames.application.prefs


class DefaultReplies() {
    //INTRO:
    fun speakIntro(): String {
        val defaultSents = listOf(
            "Tell me, ${prefs.userGender}!",
            "Hi ${prefs.userGender}! Tell me.",
            "${prefs.userGender}? Tell me!",
            "Ready for you, ${prefs.userGender}!",
            "Listening, ${prefs.userGender}!",
            "${prefs.userGender}, here to help!",
        )
        return defaultSents.random()
    }

    // FALLBACK:
    fun replyFallback(): String {
        val defaultSents = listOf(
            "Sorry ${prefs.userGender}, I did not understand!",
            "I'm sorry ${prefs.userGender}, I could not understand!",
            "Sorry ${prefs.userGender}, I didn't get that!"
        )
        return defaultSents.random()
    }

    fun replyNotAvailable(): String {
        val defaultSents = listOf(
            "Sorry ${prefs.userGender}, this task is not available yet!",
            "I'm sorry ${prefs.userGender}, I'm not ready for this task at the moment!",
        )
        return defaultSents.random()
    }

    fun replyNoPermission(): String {
        val defaultSents = listOf(
            "Sorry ${prefs.userGender}, you first need to enable me in Settings! Don't do it while driving.",
            "I'm sorry ${prefs.userGender}, you first need to enable me in Settings! Don't do it while driving.",
        )
        return defaultSents.random()
    }

    fun replyError(): String {
        val defaultSents = listOf(
            "There was a technical issue: I'm sorry for that!",
            "Sorry, there was a problem!",
            "My apologies, there was an issue!"
        )
        return defaultSents.random()
    }

    fun replyNevermind(): String {
        val defaultSents = listOf(
            "No problem ${prefs.userGender}!",
            "Nevermind ${prefs.userGender}!",
            "Anytime ${prefs.userGender}!"
        )
        return defaultSents.random()
    }

    fun replyMessageCannotRecord(): String {
        return "I'm sorry, I can't record voice messages via chat. Please, enable DRIVE Mode and ask again by voice!"
    }

    // CONTACTS:
    fun replyCalling(contactName: String, contactPhone: String = ""): String {
        return if (contactName == "" || contactName == dictatedNumber) {
            "Calling the number: $contactPhone..."
        } else {
            "Calling $contactName..."
        }
    }

    fun replyMessageRecord(contactName: String = ""): String {
        return if (contactName != "") {
            "Please, record the voice message for $contactName!"
        } else {
            "Please, record now the voice message to send via Whatsapp!"
        }
    }

    fun replySMSSent(contactName: String): String {
        return if (contactName == dictatedNumber) {
            "SMS sent to the number dictated!"
        } else {
            "SMS sent to $contactName!"
        }
    }

    fun replyWATextReady(contactName: String): String {
        return "Message ready for $contactName: please, click on 'Send' in Whatsapp to send it!"
    }

    fun replyWAVoiceReady(): String {
        return "Voice message ready: please, select the contact in Whatsapp and click 'Send' to send it!"
    }

}