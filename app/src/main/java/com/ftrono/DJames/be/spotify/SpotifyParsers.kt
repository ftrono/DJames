package com.ftrono.DJames.be.spotify

import android.util.Log
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
    private val TAG = SpotifyParsers::class.java.simpleName

    fun extractSingleArtist(itemJson: JsonObject): SpotifyArtist {
        return SpotifyArtist(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString
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
        )
    }

    fun extractPodcast(itemJson: JsonObject): SpotifyPodcast {
        return SpotifyPodcast(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString,
            publisher = itemJson.get("publisher").asString,
        )
    }
    
    fun extractEpisodeFromLibItem(itemJson: JsonObject, libItem: LibraryItem): SpotifyEpisode {
        var episode = SpotifyEpisode(
            id = itemJson.get("id").asString,
            name = itemJson.get("name").asString.replace(" - Ep. ", ". Ep "),
            releaseDate = itemJson.get("release_date").asString,
            podcast = SpotifyPodcast(
                id = libItem.url.split("/").last(),   //getSpotifyID()
                name = libItem.name,
                publisher = libItem.detail,
            )
        )
        try {
            for (obj in itemJson.get("languages").asJsonArray) {
                val lang = obj.asString
                if (lang != "") {
                    episode.languages.add(lang)
                }
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

}