package com.ftrono.DJames.test_objects

import com.ftrono.DJames.database.Playlist


val testPlaylists = listOf<Playlist>(
    Playlist(
        id = 0,
        name = "80 Ricky & Classics",
        aliases = mutableListOf("80 ricky & classics", "80 ricky classics"),
        owner = "djames_test",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
        spotifyUrl = "https://open.spotify.com/playlist/4RHMRttTD9Cz7ZxQXjZViI",
    ),
    Playlist(
        id = 1,
        name = "Acoustic & Slow",
        aliases = mutableListOf("acoustic & slow", "acoustic and slow"),
        owner = "djames_test",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
        spotifyUrl = "https://open.spotify.com/playlist/4RHMRttTD9Cz7ZxQXjZViI",
    ),
    Playlist(
        id = 2,
        name = "British & Alternative Mood",
        aliases = mutableListOf("british & alternative mood", "british alternative"),
        owner = "djames_test",
        imageUrl = "https://i.scdn.co/image/ab6761610000e5eb7e58b86655f447e0ef0278b8",
        spotifyUrl = "https://open.spotify.com/playlist/4RHMRttTD9Cz7ZxQXjZViI",
    )
)