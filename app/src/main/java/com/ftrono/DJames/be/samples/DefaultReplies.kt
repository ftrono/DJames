package com.ftrono.DJames.be.samples

import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.ItemInfoUse
import com.ftrono.DJames.be.database.SpotifyPlayable


class DefaultReplies() {
    //INTRO:
    fun speakIntro(): String {
        val defaultSents = listOf(
            "Tell me ${prefs.userGender}!",
            "Ask me ${prefs.userGender}!",
            "Hi ${prefs.userGender}! Tell me.",
            "${prefs.userGender}? Tell me!",
            "Ready ${prefs.userGender}!",
            "Listening ${prefs.userGender}!",
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

    fun replyNoPermission(): String {
        val defaultSents = listOf(
            "Sorry ${prefs.userGender}, you first need to enable me in Settings! Don't do it while driving.",
            "I'm sorry ${prefs.userGender}, you first need to enable me in Settings! Don't do it while driving.",
        )
        return defaultSents.random()
    }

    fun replyNotLoggedIn(): String {
        val defaultSents = listOf(
            "Sorry ${prefs.userGender}, you need to be logged in to Spotify!",
            "I'm sorry ${prefs.userGender}, you need to be logged in to Spotify first!",
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

    // CONTACTS:
    fun replyCalling(contactName: String): String {
        return "Calling $contactName..."
    }

    fun replyMessageRecord(contactName: String): String {
        return "Please, record the voice message for $contactName!"
    }

    fun replyMessageDictate(contactName: String, msgType: String = "", reqLangName: String): String {
        return "Please, dictate the $msgType message for $contactName in $reqLangName!".replace("  ", " ")
    }

    fun replySmsSent(contactName: String): String {
        return "SMS sent to $contactName!"
    }

    fun replyWATextSent(contactName: String): String {
        return "Message for $contactName ready: please, click on Send in Whatsapp!"
    }

    fun replyWAVoiceSent(contactName: String): String {
        return "Voice message for $contactName ready: please, select the contact in Whatsapp and Send it!"
    }

    // ROUTES:
    fun replyRouteRequest(reqLangName: String): String {
        return "Tell me the route you need in ${reqLangName}!"
    }

    fun replyRouteShowIntro(): String {
        return "Here's the route to: "
    }

    fun replyRouteShowDetail(itemInfo: ItemInfoUse): String {
        var ttsToRead = ""
        if (itemInfo.detail == "") {
            ttsToRead = "${itemInfo.name}!"
        } else {
            ttsToRead = "${itemInfo.name}, ${itemInfo.detail}!"
        }
        return ttsToRead
    }

    // PLAY:
    fun replyPlayRequest(intentName: String, reqLangName: String, contextType: String = ""): String {
        var ttsToRead = ""
        if (intentName == "PlayPodcast") {
            ttsToRead = "Tell me the name of the podcast in ${reqLangName}!"

        } else if (intentName == "PlayPlaylist") {
            ttsToRead = "Tell me the name of the playlist in ${reqLangName}!"

        } else if (intentName == "PlayArtist") {
            ttsToRead = "Tell me the name of the artist in ${reqLangName}!"

        } else if (intentName == "PlayAlbum") {
            if (reqLangName == "english") {
                ttsToRead = "Tell me in ${reqLangName}: name of the album, by, name of the artist!"
            } else {
                ttsToRead = "Tell me in ${reqLangName} the name of the album and the name of the artist!"
            }

        } else {
            if (contextType == "playlist") {
                ttsToRead = "Tell me in ${reqLangName}: name of the song, by, name of the artist, from playlist, name of the playlist!"
            } else {
                if (reqLangName == "english") {
                    ttsToRead =
                        "Tell me in ${reqLangName}: name of the song, by, name of the artist!"
                } else {
                    ttsToRead =
                        "Tell me in ${reqLangName} the name of the song and the name of the artist!"
                }
            }
        }
        return ttsToRead
    }
    
    fun replyPlayIntro(playable: SpotifyPlayable): String {
        var ttsToRead = ""
        if (playable.type == "collection") {
            ttsToRead = "Playing your Liked Songs collection!"

        } else if (playable.type == "episode" || playable.type == "podcast") {
            if (playable.releaseDate != "") {
                ttsToRead = "Playing the latest episode dated ${playable.releaseDate}: "
            } else {
                ttsToRead = "Playing the latest episode: "
            }

        } else if (playable.owner == prefs.spotUserName) {
            ttsToRead = "Playing your playlist: "

        } else if (playable.type == "artist") {
            ttsToRead = "Playing top tracks for the artist: "

        } else {
            ttsToRead = "Playing the ${playable.type}: "
        }
        return ttsToRead
    }

    fun replyPlayDetail(playable: SpotifyPlayable): String {
        var ttsToRead = ""
        if (playable.type == "episode" || playable.type == "podcast") {
            ttsToRead = utils.cleanString(playable.name, emojiOnly = true)

        } else if (playable.type == "playlist") {
            if (playable.owner == prefs.spotUserName) {
                ttsToRead = utils.cleanString(playable.name, emojiOnly = true)
            } else {
                val ownerString = if (playable.owner == "") "" else ", by ${playable.owner}"
                ttsToRead = utils.cleanString("${playable.name}${ownerString}.", emojiOnly = true)
            }

        } else if (playable.type == "artist") {
            ttsToRead = utils.cleanString(playable.name, emojiOnly = true)

        } else {
            var artist = playable.artistsNames.joinToString(" feat. ")
            ttsToRead = "${playable.name}, by $artist!"
        }
        return ttsToRead
    }


}