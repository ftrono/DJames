package com.ftrono.DJames.be.collections

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import com.ftrono.DJames.R
import kotlinx.serialization.Serializable


// CLASSES:
data class GuideOption(
    var name: String = "",
    var text: String = "",
    var iconPainter: Painter? = null,
    var background: Color? = null,
    var tint: Color? = null,
    var onClick: () -> Unit = {}
)

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

val guideOptions = listOf("spotify", "youtube", "maps", "calls", "sms", "whatsapp")

@Composable
fun getGuideOption(name: String): GuideOption {
    val optionsMap = mapOf(
        "spotify" to GuideOption(
            name = "spotify",
            text = "Play music & podcasts via Spotify",
            background = colorResource(R.color.colorPrimaryDark),
            iconPainter = painterResource(R.drawable.logo_spotify),
        ),
        "youtube" to GuideOption(
            name = "youtube",
            text = "Play music & videos via Youtube",
            background = colorResource(R.color.redSignDark),
            iconPainter = painterResource(R.drawable.logo_youtube),
        ),
        "maps" to GuideOption(
            name = "maps",
            text = "Get driving directions via Google Maps",
            background = colorResource(R.color.yellowSignDark),
            iconPainter = painterResource(R.drawable.logo_gmaps),
        ),
        "calls" to GuideOption(
            name = "calls",
            text = "Call a contact",
            background = colorResource(R.color.colorPrimaryDark),
            iconPainter = painterResource(R.drawable.icon_phone),
            tint = colorResource(R.color.colorAccentMid),
        ),
        "sms" to GuideOption(
            name = "sms",
            text = "Send an SMS",
            background = colorResource(R.color.blueSignDark),
            iconPainter = painterResource(R.drawable.icon_message),
            tint = colorResource(R.color.blueSignMid),
        ),
        "whatsapp" to GuideOption(
            name = "whatsapp",
            text = "Send a Whatsapp text or voice",
            background = colorResource(R.color.colorPrimaryDark),
            iconPainter = painterResource(R.drawable.logo_whatsapp),
            tint = colorResource(R.color.greenSignLight),
        ),
    )
    return optionsMap[name]!!
}


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

    //PLACES:
    GuideCategory(
        category = "places",
        header = "Places",
        requests = listOf(
            GuideRequest(
                intro = "Show a Google Maps route",
                sentence = "\uD83D\uDDE3\uFE0F  \"Drive me to a place in <language>\"",
                description = "Shows the navigation route to a place in Google Maps. Language MUST be told if the name is not in the default places language (edit in Settings).\n\n*NOTE: list your favourite places in your Saved Items, in the Places tab!"
            ),
        )
    ),

    //SPOTIFY:
    GuideCategory(
        category = "play",
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
                intro = "Play artist",
                sentence = "\uD83D\uDDE3\uFE0F  \"Play an artist in <language>\"",
                description = "Search & play top tracks for the requested artist*. Language MUST be told if the name is not in English.\n\n*NOTE: list your favourite artists in your Saved Items, in the Artists tab! Otherwise, DJames will search for artists with that name across Spotify."
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
