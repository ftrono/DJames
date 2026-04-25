package com.ftrono.DJames.be.collections

import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.be.database.ActionType
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.Message
import com.ftrono.DJames.be.database.SpotifyAlbum
import com.ftrono.DJames.be.database.SpotifyArtist
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.database.SpotifyPlaylist
import com.ftrono.DJames.be.database.SpotifyTrack


val testMessages = listOf<Message>(
    // Intro:
    Message(
        id = 0,
        timestamp = System.currentTimeMillis() - (31*60*1000),
        starterId = System.currentTimeMillis() - (31*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = true,
        fromUser = false,
        text = "Tell me, Sir!",
        isStart =  true,
    ),

    // Call:
    Message(
        id = 1,
        timestamp = System.currentTimeMillis() - (30*60*1000),
        starterId = System.currentTimeMillis() - (31*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = true,
        fromUser = true,
        text = "call Ricky",
        requestIntent = "CallRequest",
    ),

    Message(
        id = 2,
        timestamp = System.currentTimeMillis() - (29*60*1000),
        starterId = System.currentTimeMillis() - (31*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = true,
        fromUser = false,
        text = "Calling Ricky!",
        requestIntent = "CallRequest",
        actionType = ActionType.CALL,
        attachments = Attachments(
            usable = LibraryItem(
                matchScore = 100,
                type = "contact",
                name = "Ricky"
            )
        )
    ),

    // Message (SMS):
    Message(
        id = 3,
        timestamp = System.currentTimeMillis() - (25*60*1000),
        starterId = System.currentTimeMillis() - (25*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = true,
        fromUser = true,
        text = "send a message to Amal",
        requestIntent = "MessageRequest",
        isStart =  true,
    ),

    Message(
        id = 4,
        timestamp = System.currentTimeMillis() - (24*60*1000),
        starterId = System.currentTimeMillis() - (25*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = true,
        fromUser = false,
        text = "Please, dictate the SMS for Amal in Italian!",
        requestIntent = "MessageRequest",
        actionType = ActionType.SMS,
        attachments = Attachments(
            usable = LibraryItem(
                matchScore = 100,
                type = "contact",
                name = "Amal"
            )
        )
    ),

    // Place:
    Message(
        id = 5,
        timestamp = System.currentTimeMillis() - (20*60*1000),
        starterId = System.currentTimeMillis() - (20*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = false,
        fromUser = true,
        text = "drive me to a place! aeroporto di brindisi",
        requestIntent = "DriveRequest",
        isStart =  true,
    ),

    Message(
        id = 6,
        timestamp = System.currentTimeMillis() - (19*60*1000),
        starterId = System.currentTimeMillis() - (20*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = false,
        fromUser = false,
        text = "Here's the route to: Aeroporto di Brindisi, Brindisi, Contrada Baroncino!",
        requestIntent = "DriveRequest",
        actionType = ActionType.OPEN_URL,
        attachments = Attachments(
            usable = LibraryItem(
                matchScore = 100,
                type = "place",
                name = "Aeroporto di Brindisi",
                detail = "Brindisi, Contrada Baroncino"
            )
        )
    ),

    // PLAY: Track from Album:
    Message(
        id = 7,
        timestamp = System.currentTimeMillis() - (15*60*1000),
        starterId = System.currentTimeMillis() - (15*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = false,
        fromUser = true,
        text = "play lost by linkin park",
        requestIntent = "PlaySong",
        isStart =  true,
    ),

    Message(
        id = 8,
        timestamp = System.currentTimeMillis() - (14*60*1000),
        starterId = System.currentTimeMillis() - (15*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = false,
        fromUser = false,
        text = "Playing the track: Lost, by Linkin Park!",
        requestIntent = "PlaySong",
        actionType = ActionType.PLAY,
        attachments = Attachments(
            spotifyPlay = SpotifyPlayable(
                matchScore = 100,
                type = "track",
                track = SpotifyTrack(
                    name = "Lost",
                    artists = mutableListOf(
                        SpotifyArtist(
                            name = "Linkin Park"
                        )
                    ),
                    album = SpotifyAlbum(
                        type = "single",
                        name = "Lost",
                    ),
                ),
                //Context Type -> only if "playlist" or "podcast"
            )
        )
    ),

    // PLAY: Playlist:
    Message(
        id = 9,
        timestamp = System.currentTimeMillis() - (10*60*1000),
        starterId = System.currentTimeMillis() - (10*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = true,
        fromUser = true,
        text = "play my playlist francis ford",
        requestIntent = "PlayPlaylist",
        isStart =  true,
    ),

    Message(
        id = 10,
        timestamp = System.currentTimeMillis() - (9*60*1000),
        starterId = System.currentTimeMillis() - (10*60*1000) - 1,
        appVersion = appVersion,
        fromVoice = true,
        fromUser = false,
        text = "Playing your playlist: Francis Ford!",
        requestIntent = "PlayPlaylist",
        actionType = ActionType.PLAY,
        attachments = Attachments(
            spotifyPlay = SpotifyPlayable(
                matchScore = 100,
                type = "playlist",
                playlist = SpotifyPlaylist(
                    name = "Francis Ford",
                    owner = "francesco_trono"
                ),
            )
        )
    ),
)
