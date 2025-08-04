package com.ftrono.DJames.be.database

import kotlinx.serialization.Serializable


// CLASSES:
@Serializable
data class GuideCategory(
    var category: String = "",
    var header: String = "",
    var requests: List<GuideRequest> = listOf<GuideRequest>(),
)

@Serializable
data class GuideRequest(
    var intro: String = "",
    var sentence: String = "",
    var description: String = "",
)


// --------------------------------

// DATA:
val fullGuide = listOf<GuideCategory>(
    // CALLS:
    GuideCategory(
        category = "calls",
        header = "Calls",
        requests = listOf(
            GuideRequest(
                intro = "Call a contact*",
                sentence = "\uD83D\uDDE3\uFE0F  \"Call <contact name>\"",
                description = "Starts a phone call to a contact of yours*.\n\n*NOTE: the contact must be listed in your Saved Items, in the Contacts tab!"
            ),
        )
    ),

    //MESSAGES:
    GuideCategory(
        category = "messages",
        header = "Messages",
        requests = listOf(
            GuideRequest(
                intro = "Send SMS to a contact*",
                sentence = "\uD83D\uDDE3\uFE0F  \"Send a message to <contact name> in <language>\"",
                description = "Sends an SMS to a contact of yours*. Messaging language MUST be told if it's different from the contact's default.\n\n*NOTE: the contact must be listed in your Saved Items, in the Contacts tab! Set there also the default messaging language for each contact.\n\nTo use emojis, say:\n\n- \uD83C\uDDEC\uD83C\uDDE7: \"EMOJI\" + smile / happy / grin / kiss / sunglasses / heart / sad / worried.\n\n- \uD83C\uDDEE\uD83C\uDDF9: \"EMOJI\" + sorriso / felice / sorrisino / bacio / occhiali da sole / cuore / triste / preoccupato."
            ),
        )
    ),

    //ROUTES:
    GuideCategory(
        category = "routes",
        header = "Routes",
        requests = listOf(
            GuideRequest(
                intro = "Show a Google Maps route",
                sentence = "\uD83D\uDDE3\uFE0F  \"Drive me to a place in <language>\"",
                description = "Shows any navigation route in Google Maps. Language MUST be told if the name is not in the default routes language (edit in Settings).\n\n*NOTE: list your favourite routes in your Saved Items, in the Routes tab!"
            ),
        )
    ),

    //SPOTIFY:
    GuideCategory(
        category = "spotify",
        header = "Spotify",
        requests = listOf(

            GuideRequest(
                intro = "Play song",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play a song in <language>\"",
                description = "Plays a song in Spotify from its album. Language MUST be told if the name is not in English. Preference is given to your saved tracks and to albums over singles."
            ),

            GuideRequest(
                intro = "Play song within a playlist*",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play a song in <language> from a playlist\"",
                description = "Plays a song in Spotify from a playlist of yours*. Language MUST be told if the name is not in English.\n\n*NOTE: the playlist must be listed in your Saved Items, in the Playlists tab!"
            ),

            GuideRequest(
                intro = "Play song within Liked Songs*",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play a song in <language> from my liked songs / saved tracks / collection\"",
                description = "Plays a song in Spotify from your playlist Liked Songs*. Language MUST be told if the name is not in English.\n\n*NOTE: this will work only for your most recently saved tracks!"
            ),

            GuideRequest(
                intro = "Play album",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play an album in <language>\"",
                description = "Search & play an album in Spotify. Language MUST be told if the name is not in English."
            ),

            GuideRequest(
                intro = "Play artist collection",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play an artist in <language>\"",
                description = "Search & play top tracks for the requested artist, or the default playlist \"This is <artist>\" in Spotify (only if saved in your Saved Items). Language MUST be told if the name is not in English.\n\nIf you add the word \"Radio\" or \"Mix\" after the artist name, DJames will play these playlist instead (only if they're saved in your Saved Items)."
            ),

            GuideRequest(
                intro = "Play playlist",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play a playlist in <language>\"",
                description = "Plays any playlist* in Spotify. Language MUST be told if the name is not in English.\n\n*NOTE: list your favourite playlists in your Saved Items, in the Playlists tab! Otherwise, DJames will search for playlist with that name across Spotify."
            ),

            GuideRequest(
                intro = "Play Liked Songs collection",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play my liked songs / saved tracks / collection\"",
                description = "Plays your Liked Songs collection in Spotify."
            ),

            GuideRequest(
                intro = "Play podcast episode*",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play a podcast (episode) in <language>\"",
                description = "Plays the latest episode for a podcast* in Spotify. Language MUST be told if the name is not in English.\n\n*NOTE: the podcast MUST be saved in your Saved Items!"
            ),

        )
    ),

)
