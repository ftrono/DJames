package com.ftrono.DJames.be.chat

import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.LibraryItem
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

    fun replyMessageCannotRecord(): String {
        return "I'm sorry, I can't record voice messages via chat. Please, enable DRIVE Mode and ask again by voice!"
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

    // PLACES:
    fun replyPlaceRequest(reqLangName: String): String {
        return "Tell me the place you need in ${reqLangName}!"
    }

    fun replyPlaceShowIntro(): String {
        return "Here's the route to: "
    }

    fun replyPlaceShowDetail(itemInfo: LibraryItem): String {
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
        if (playable.type == "episode" && playable.episode != null) {
            if (playable.episode!!.releaseDate != "") {
                ttsToRead = "Playing the latest episode dated ${playable.episode!!.releaseDate}: "
            } else {
                ttsToRead = "Playing the latest episode: "
            }

        } else if (playable.type == "playlist" && playable.playlist != null) {
            if (playable.id == "collection") {
                ttsToRead = "Playing your Liked Songs collection!"
            } else if (playable.playlist!!.owner == prefs.spotUserName) {
                ttsToRead = "Playing your playlist: "
            } else {
                ttsToRead = "Playing the playlist: "
            }
        } else if (playable.type == "artist") {
            ttsToRead = "Playing top tracks for the artist: "

        } else {
            ttsToRead = "Playing the ${playable.type}: "
        }
        return ttsToRead
    }

    fun replyPlayDetail(playable: SpotifyPlayable): String {
        var ttsToRead = ""
        if (playable.type == "episode" && playable.episode != null) {
            ttsToRead = utils.cleanString(playable.episode!!.name, emojiOnly = true)

        } else if (playable.type == "playlist" && playable.playlist != null) {
            if (playable.playlist!!.owner == prefs.spotUserName) {
                ttsToRead = utils.cleanString(playable.playlist!!.name, emojiOnly = true)
            } else {
                val ownerString = if (playable.playlist!!.owner == "") "" else ", by ${playable.playlist!!.owner}"
                ttsToRead = utils.cleanString("${playable.playlist!!.name}${ownerString}.", emojiOnly = true)
            }

        } else if (playable.type == "artist" && playable.artist != null) {
            ttsToRead = utils.cleanString(playable.artist!!.name, emojiOnly = true)

        } else if (playable.type == "album" && playable.album != null && playable.album!!.artists.isNotEmpty()) {
            var artist = playable.album!!.artists.joinToString(" feat. ") { it.name }
            ttsToRead = "${playable.album!!.name}, by $artist!"

        } else if (playable.type == "track" && playable.track != null && playable.track!!.artists.isNotEmpty()) {
            var artist = playable.track!!.artists.joinToString(" feat. ") { it.name }
            ttsToRead = "${playable.track!!.name}, by $artist!"
        }
        return ttsToRead
    }

}