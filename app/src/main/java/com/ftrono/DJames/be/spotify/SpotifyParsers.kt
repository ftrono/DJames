package com.ftrono.DJames.be.spotify

import android.util.Log
import com.ftrono.DJames.application.spotifyParsers
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.SpotifyAlbum
import com.ftrono.DJames.be.database.SpotifyArtist
import com.ftrono.DJames.be.database.SpotifyEpisode
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.database.SpotifyPlaylist
import com.ftrono.DJames.be.database.SpotifyPodcast
import com.ftrono.DJames.be.database.SpotifyTrack
import com.google.gson.JsonArray
import com.google.gson.JsonObject



class SpotifyParsers() {
    private val TAG = this::class.java.simpleName

    fun extractImageUrl(itemJson: JsonObject): String {
        return try {
            itemJson.get("images").asJsonArray.get(0).asJsonObject.get("url").asString
        } catch (e: Exception) {
            ""
        }
    }

    fun extractSingleArtist(itemJson: JsonObject): SpotifyArtist {
        return SpotifyArtist(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString,
            imageUrl = extractImageUrl(itemJson)
        )
    }

    fun extractAllArtists(arrayJsonArray: JsonArray): MutableList<SpotifyArtist> {
        var artists = mutableListOf<SpotifyArtist>()
        for (artist in arrayJsonArray) {
            artists.add(extractSingleArtist(artist.asJsonObject))
        }
        return artists
    }

    fun extractAlbum(itemJson: JsonObject): SpotifyAlbum {
        return SpotifyAlbum(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString,
            type = itemJson.get("album_type").asString,
            artists = extractAllArtists(itemJson.getAsJsonArray("artists")),
            imageUrl = extractImageUrl(itemJson)
        )
    }

    fun extractTrack(itemJson: JsonObject): SpotifyTrack {
        return SpotifyTrack(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString,
            artists = extractAllArtists(itemJson.getAsJsonArray("artists")),
            album = extractAlbum(itemJson.get("album").asJsonObject),
        )
    }

    fun extractPlaylist(itemJson: JsonObject): SpotifyPlaylist {
        return SpotifyPlaylist(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString,
            owner = itemJson.get("owner").asJsonObject.get("display_name").asString,
            imageUrl = extractImageUrl(itemJson)
        )
    }

    fun extractPodcast(itemJson: JsonObject): SpotifyPodcast {
        return SpotifyPodcast(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString,
            imageUrl = extractImageUrl(itemJson)
        )
    }
    
    fun extractEpisodeFromLibItem(itemJson: JsonObject, podcastId: String, podcastName: String): SpotifyEpisode {
        var episode = SpotifyEpisode(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString.replace(" - Ep. ", ". Ep "),
            releaseDate = itemJson.get("release_date").asString,
            podcast = SpotifyPodcast(
                id = podcastId,
                name = podcastName,
            )
        )
        try {
            for (obj in itemJson.get("languages").asJsonArray) {
                episode.languages.add(obj.asString)
            }
        } catch (e: Exception) {
            Log.d(TAG, "No languages info in current Episode!")
        }
        //ResumePoint info:
        try {
            val itemResume = itemJson.get("resume_point").asJsonObject
            episode.fullyPlayed = itemResume.get("fully_played").asBoolean
            if (!episode.fullyPlayed) {
                episode.resumePositionMs = itemResume.get("resume_position_ms").asInt
            }
        } catch (e: Exception) {
            Log.d(TAG, "No ResumePoint info in current Episode!")
        }
        return episode
    }

    fun extractEpisodeFromPodcast(itemJson: JsonObject): SpotifyEpisode {
        var episode = SpotifyEpisode(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString.replace(" - Ep. ", ". Ep "),
            releaseDate = itemJson.get("release_date").asString,
            podcast = extractPodcast(itemJson.get("show").asJsonObject)
        )
        try {
            for (obj in itemJson.get("languages").asJsonArray) {
                episode.languages.add(obj.asString)
            }
        } catch (e: Exception) {
            Log.d(TAG, "No languages info in current Episode!")
        }
        //ResumePoint info:
        try {
            val itemResume = itemJson.get("resume_point").asJsonObject
            episode.fullyPlayed = itemResume.get("fully_played").asBoolean
            if (!episode.fullyPlayed) {
                episode.resumePositionMs = itemResume.get("resume_position_ms").asInt
            }
        } catch (e: Exception) {
            Log.d(TAG, "No ResumePoint info in current Episode!")
        }
        return episode
    }

    //MAIN: API-to-Playable converter:
    fun extractPlayableFromJson(playType: String, itemJson: JsonObject): SpotifyPlayable {
        var playable = SpotifyPlayable(
            id = itemJson.get("id").asString,
            type = playType
        )
        // Extract key info:
        when (playType) {
            "artist" -> {
                playable.artist = spotifyParsers.extractSingleArtist(itemJson)
                playable.imageUrl = playable.artist!!.imageUrl
            }
            "album" -> {
                playable.album = spotifyParsers.extractAlbum(itemJson)
                playable.imageUrl = playable.album!!.imageUrl
            }
            "track" -> {
                playable.track = spotifyParsers.extractTrack(itemJson)
                playable.imageUrl = playable.track!!.album!!.imageUrl
            }
            "playlist" -> {
                playable.playlist = spotifyParsers.extractPlaylist(itemJson)
                playable.imageUrl = playable.playlist!!.imageUrl
            }
            "podcast" -> {
                playable.podcast = spotifyParsers.extractPodcast(itemJson)
                playable.imageUrl = playable.podcast!!.imageUrl
            }
            "episode" -> {
                playable.episode = spotifyParsers.extractEpisodeFromPodcast(itemJson)
                playable.imageUrl = playable.episode!!.podcast!!.imageUrl
            }
        }
        return playable
    }

}