package com.ftrono.DJames.be.samples

import com.ftrono.DJames.application.spotCollectionName
import com.ftrono.DJames.application.spotCollectionUrl
import com.ftrono.DJames.be.database.Address
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.PhoneSet
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString


// DEFAULT: COLLECTION:
// type -> "playlist"
// <LibraryItem> -> id = -2L
// <SpotifyPlayable> -> id = "collection"
val defaultCollection = LibraryItem(
    id = -2L,
    source = "spotify",
    type = "playlist",
    name = spotCollectionName,
    aliases = mutableListOf("liked songs", "saved tracks", "collection"),
    detail = "Spotify",
    url = spotCollectionUrl,
    imageUrl = "",
)


// TEST LIBRARY:
val testLibrary = listOf<LibraryItem>(
    // SPOTIFY - ARTISTS:
    LibraryItem(
        id = 0,
        source = "spotify",
        type = "artist",
        name = "Aimer",
        aliases = mutableListOf("aimer", "amor", "amer"),
        url = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
    ),
    LibraryItem(
        id = 1,
        source = "spotify",
        type = "artist",
        name = "Alex Goot",
        aliases = mutableListOf("alex goot", "alex good"),
        url = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
    ),
    LibraryItem(
        id = 3,
        source = "spotify",
        type = "artist",
        name = "Deaf Havana",
        aliases = mutableListOf("deaf havana"),
        url = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
    ),
    LibraryItem(
        id = 4,
        source = "spotify",
        type = "artist",
        name = "Dean Lewis",
        aliases = mutableListOf("dean lewis"),
        url = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
    ),
    LibraryItem(
        id = 5,
        source = "spotify",
        type = "artist",
        name = "The Script",
        aliases = mutableListOf("the script"),
        url = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
    ),

    // SPOTIFY - PLAYLISTS:
    LibraryItem(
        id = 6,
        source = "spotify",
        type = "playlist",
        name = "80 Ricky & Classics",
        aliases = mutableListOf("80 ricky & classics", "80 ricky classics"),
        detail = "djames_test",
        url = "https://open.spotify.com/playlist/4RHMRttTD9Cz7ZxQXjZViI",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
    ),
    LibraryItem(
        id = 7,
        source = "spotify",
        type = "playlist",
        name = "Acoustic & Slow",
        aliases = mutableListOf("acoustic & slow", "acoustic and slow"),
        detail = "djames_test",
        url = "https://open.spotify.com/playlist/4RHMRttTD9Cz7ZxQXjZViI",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
    ),
    LibraryItem(
        id = 8,
        source = "spotify",
        type = "playlist",
        name = "British & Alternative Mood",
        aliases = mutableListOf("british & alternative mood", "british alternative"),
        detail = "djames_test",
        url = "https://open.spotify.com/playlist/4RHMRttTD9Cz7ZxQXjZViI",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
    ),

    // SPOTIFY - PODCASTS:
    LibraryItem(
        id = 9,
        source = "spotify",
        type = "podcast",
        name = "3 Fattori",
        aliases = mutableListOf("3 fattori"),
        detail = "Sky Tg24",
        url = "https://open.spotify.com/show/2lrRJEThluTsQVgEqCeR9X",
        imageUrl = "https://i.scdn.co/image/ab6765630000ba8aaaea81d4152fb611170f0f7f",
        language = "it",
    ),

    // SPOTIFY - ALBUMS:
    LibraryItem(
        id = 10,
        source = "spotify",
        type = "album",
        name = "No Sound Without Silence",
        aliases = mutableListOf("no sound without silence"),
        detail = "The Script",
        url = "https://open.spotify.com/show/2lrRJEThluTsQVgEqCeR9X",
        imageUrl = "https://i.scdn.co/image/ab6765630000ba8aaaea81d4152fb611170f0f7f",
    ),

    // SPOTIFY - TRACKS:
    LibraryItem(
        id = 11,
        source = "spotify",
        type = "track",
        name = "Listen",
        aliases = mutableListOf("listen"),
        detail = "Tears For Fears",
        url = "https://open.spotify.com/show/2lrRJEThluTsQVgEqCeR9X",
        imageUrl = "https://i.scdn.co/image/ab6765630000ba8aaaea81d4152fb611170f0f7f",
    ),

    // SPOTIFY - EPISODES:
    LibraryItem(
        id = 12,
        source = "spotify",
        type = "episode",
        name = "Notizie: la BCE e i tassi - Ep 10",
        aliases = mutableListOf("3 fattori"),
        detail = "3 Fattori",
        url = "https://open.spotify.com/show/2lrRJEThluTsQVgEqCeR9X",
        imageUrl = "https://i.scdn.co/image/ab6765630000ba8aaaea81d4152fb611170f0f7f",
        language = "it",
    ),

    // LOCAL - CONTACTS:
    LibraryItem(
        id = 13,
        source = "contact",
        type = "contact",
        name = "Amal",
        aliases = mutableListOf("amal"),
        language = "",
        phoneSet = PhoneSet(
            prefix = "+39",
            phone = "3331122333"
        ),
    ),
    LibraryItem(
        id = 14,
        source = "contact",
        type = "contact",
        name = "Mammut",
        aliases = mutableListOf("mammut", "mammood"),
        language = "it",
        phoneSet = PhoneSet(
            prefix = "+39",
            phone = "3320011234"
        ),
    ),
    LibraryItem(
        id = 15,
        source = "contact",
        type = "contact",
        name = "Rick",
        aliases = mutableListOf("rick"),
        language = "en",
        phoneSet = PhoneSet(
            prefix = "+39",
            phone = "3325678912"
        ),
    ),

    // MAPS - PLACES:
    LibraryItem(
        id = 16,
        source = "place",
        type = "place",
        name = "Casa Amal",
        aliases = mutableListOf("casa amal", "via alcide de gasperi lecce"),
        address = Address(
            street = "Via Alcide de Gasperi",
            number = "37 A",
            placeName = "",
            town = "Lecce",
            zip = "73100",
            province = "LE",
            country = "Italy"
        ),
    ),
    LibraryItem(
        id = 17,
        source = "place",
        type = "place",
        name = "Links Academy",
        aliases = mutableListOf("links academy", "via masseria caldare lecce"),
        address = Address(
            street = "Via Masseria Caldare",
            number = "",
            placeName = "Links Academy",
            town = "Lecce",
            zip = "73100",
            province = "LE",
            country = "Italy"
        ),
    ),
    LibraryItem(
        id = 18,
        source = "place",
        type = "place",
        name = "Links Scotellaro",
        aliases = mutableListOf("links scotellaro", "via rocco scotellaro 55 lecce"),
        address = Address(
            street = "Via Alcide de Gasperi",
            number = "37 A",
            placeName = "",
            town = "Lecce",
            zip = "73100",
            province = "LE",
            country = "Italy"
        ),
    ),
)