package com.ftrono.DJames.be.samples

import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.database.ItemInfoUse
import com.ftrono.DJames.be.database.Message
import com.ftrono.DJames.be.database.SpotifyPlayable


val testMessages = listOf<Message>(
    // Intro:
    Message(
        timestamp = System.currentTimeMillis() - (31*60*1000) - 1,
        appVersion = appVersion,
        type = "starter",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (31*60*1000),
        appVersion = appVersion,
        type = "ai",
        text = "Tell me, Sir!",
    ),

    // Call:
    Message(
        timestamp = System.currentTimeMillis() - (30*60*1000),
        appVersion = appVersion,
        type = "user",
        text = "call Ricky",
        requestIntent = "CallRequest",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (29*60*1000),
        appVersion = appVersion,
        type = "ai",
        text = "Calling Ricky!",
        requestIntent = "CallRequest",
        attachments = Attachments(
            matchScore = 100,
            usable = ItemInfoUse(
                type = "contact",
                name = "Ricky"
            )
        )
    ),

    // Message (SMS):
    Message(
        timestamp = System.currentTimeMillis() - (25*60*1000) - 1,
        appVersion = appVersion,
        type = "starter",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (25*60*1000),
        appVersion = appVersion,
        type = "user",
        text = "send a message to Amal",
        requestIntent = "MessageRequest",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (24*60*1000),
        appVersion = appVersion,
        type = "ai",
        text = "Please, dictate the SMS for Amal in Italian!",
        requestIntent = "MessageRequest",
        attachments = Attachments(
            matchScore = 100,
            usable = ItemInfoUse(
                type = "contact",
                name = "Amal"
            )
        )
    ),

    // Route:
    Message(
        timestamp = System.currentTimeMillis() - (20*60*1000) - 1,
        appVersion = appVersion,
        type = "starter",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (20*60*1000),
        appVersion = appVersion,
        type = "user",
        text = "show me a route! aeroporto di brindisi",
        requestIntent = "DriveRequest",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (19*60*1000),
        appVersion = appVersion,
        type = "ai",
        text = "Here's the route to: Aeroporto di Brindisi, Brindisi, Contrada Baroncino!",
        requestIntent = "DriveRequest",
        attachments = Attachments(
            matchScore = 100,
            usable = ItemInfoUse(
                type = "route",
                name = "Aeroporto di Brindisi",
                detail = "Brindisi, Contrada Baroncino"
            )
        )
    ),

    // PLAY: Track from Album:
    Message(
        timestamp = System.currentTimeMillis() - (15*60*1000) - 1,
        appVersion = appVersion,
        type = "starter",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (15*60*1000),
        appVersion = appVersion,
        type = "user",
        text = "play lost by linkin park",
        requestIntent = "PlaySong",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (14*60*1000),
        appVersion = appVersion,
        type = "ai",
        text = "Playing the track: Lost, by Linkin Park!",
        requestIntent = "PlaySong",
        attachments = Attachments(
            matchScore = 100,
            spotifyPlay = SpotifyPlayable(
                type = "track",
                name = "Lost",
                artistsNames = mutableListOf("Linkin Park"),
                albumType = "single",
                albumName = "Lost",
                //Context Type -> only if "playlist" or "podcast"
            )
        )
    ),

    // PLAY: Playlist:
    Message(
        timestamp = System.currentTimeMillis() - (10*60*1000) - 1,
        appVersion = appVersion,
        type = "starter",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (10*60*1000),
        appVersion = appVersion,
        type = "user",
        text = "play my playlist francis ford",
        requestIntent = "PlayPlaylist",
    ),

    Message(
        timestamp = System.currentTimeMillis() - (9*60*1000),
        appVersion = appVersion,
        type = "ai",
        text = "Playing your playlist: Francis Ford!",
        requestIntent = "PlayPlaylist",
        attachments = Attachments(
            matchScore = 100,
            spotifyPlay = SpotifyPlayable(
                type = "playlist",
                name = "Francis Ford",
                owner = "francesco_trono"
                //Context Type -> only if "playlist" or "podcast"
            )
        )
    ),

)
