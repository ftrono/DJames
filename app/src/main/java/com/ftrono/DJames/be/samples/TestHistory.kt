package com.ftrono.DJames.be.samples

import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.be.database.HistoryLog
import com.ftrono.DJames.be.database.ItemInfoUse
import com.ftrono.DJames.be.database.KeyLogInfo
import com.ftrono.DJames.be.database.SpotifyPlayable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val testHistory = listOf<HistoryLog>(
    // Call:
    HistoryLog(
        id = 0,
        appVersion = appVersion,
        datetime = LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        keyInfo = KeyLogInfo(
            intentName = "CallRequest",
            queryText = "call myself",
            libScore = 100,
        ),
        usable = ItemInfoUse(
            type = "contact",
            name = "Myself"
        )
    ),

    // Message:
    HistoryLog(
        id = 1,
        appVersion = appVersion,
        datetime = LocalDateTime.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        keyInfo = KeyLogInfo(
            intentName = "MessageRequest",
            queryText = "send a message to myself",
            libScore = 100,
        ),
        usable = ItemInfoUse(
            type = "contact",
            name = "Myself"
        )
    ),

    // Route:
    HistoryLog(
        id = 2,
        appVersion = appVersion,
        datetime = LocalDateTime.now().minusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        keyInfo = KeyLogInfo(
            intentName = "DriveRequest",
            queryText = "aeroporto di brindisi",
        ),
        usable = ItemInfoUse(
            type = "route",
            name = "Aeroporto di Brindisi",
            detail = "Brindisi, Contrada Baroncino"
        )
    ),

    // Track from Album:
    HistoryLog(
        id = 3,
        appVersion = appVersion,
        datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        keyInfo = KeyLogInfo(
            intentName = "PlayRequest",
            queryText = "lost by linkin park",
            bestScore = 100,
        ),
        spotifyPlay = SpotifyPlayable(
            type = "track",
            name = "Lost",
            artistsNames = mutableListOf("Linkin Park"),
            albumType = "single",
            albumName = "Lost",
            //Context Type -> only if "playlist" or "podcast"
        )
    ),

    // Playlist:
    HistoryLog(
        id = 4,
        appVersion = appVersion,
        datetime = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        keyInfo = KeyLogInfo(
            intentName = "PlayPlaylist",
            queryText = "francis ford",
            libScore = 100,
        ),
        spotifyPlay = SpotifyPlayable(
            type = "playlist",
            name = "Francis Ford",
            owner = "francesco_trono"
            //Context Type -> only if "playlist" or "podcast"
        )
    ),

)
