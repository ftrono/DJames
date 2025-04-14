package com.ftrono.DJames.be.samples

import com.ftrono.DJames.be.database.Artist
import com.ftrono.DJames.be.database.PlayLink
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString


val testArtists = listOf<Artist>(
    Artist(
        id = 0,
        name = "Aimer",
        aliases = mutableListOf("aimer", "amor", "amer"),
        spotifyUrl = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        country = "",
        genres = mutableListOf(
            "anime",
            "j-pop",
            "j-rock",
        ),
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
        defaultPlay = "spotify_this_is",
        playLinksJson = Json.encodeToString(
            mutableMapOf(
                "spotify_this_is" to PlayLink(
                    name = "This is Aimer",
                    owner = "Spotify",
                    spotifyUrl = "https://open.spotify.com/playlist/37i9dQZF1DZ06evO02uS96"
                )
            ),
        ),
    ),
    Artist(
        id = 1,
        name = "Alex Goot",
        aliases = mutableListOf("alex goot", "alex good"),
        spotifyUrl = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        country = "",
        genres = mutableListOf(
            "cover",
            "acoustic",
            "pop",
        ),
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
        defaultPlay = "spotify_this_is",
        playLinksJson = Json.encodeToString(
            mutableMapOf(
                "spotify_this_is" to PlayLink(
                    name = "This is Alex Goot",
                    owner = "Spotify",
                    spotifyUrl = "https://open.spotify.com/playlist/37i9dQZF1DZ06evO02uS96"
                )
            ),
        ),
    ),
    Artist(
        id = 2,
        name = "Deaf Havana",
        aliases = mutableListOf("deaf havana"),
        spotifyUrl = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        country = "",
        genres = mutableListOf(
            "rock",
            "pop",
        ),
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
        defaultPlay = "spotify_this_is",
        playLinksJson = Json.encodeToString(
            mutableMapOf(
                "spotify_this_is" to PlayLink(
                    name = "This is Deaf Havana",
                    owner = "Spotify",
                    spotifyUrl = "https://open.spotify.com/playlist/37i9dQZF1DZ06evO02uS96"
                )
            ),
        ),
    ),
    Artist(
        id = 3,
        name = "The Script",
        aliases = mutableListOf("the script"),
        spotifyUrl = "https://open.spotify.com/artist/0bAsR2unSRpn6BQPEnNlZm",
        country = "",
        genres = mutableListOf(
            "rock",
            "pop",
        ),
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
        defaultPlay = "spotify_this_is",
        playLinksJson = Json.encodeToString(
            mutableMapOf(
                "spotify_this_is" to PlayLink(
                    name = "This is The Script",
                    owner = "Spotify",
                    spotifyUrl = "https://open.spotify.com/playlist/37i9dQZF1DZ06evO02uS96"
                )
            ),
        ),
    )
)