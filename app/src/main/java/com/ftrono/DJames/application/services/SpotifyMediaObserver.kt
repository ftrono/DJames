package com.ftrono.DJames.application.services

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.ftrono.DJames.application.currentArtistPlaying
import com.ftrono.DJames.application.currentPlayerColor
import com.ftrono.DJames.application.currentPlayerImage
import com.ftrono.DJames.application.currentSongPlaying
import com.ftrono.DJames.application.lastPlaybackInfo
import com.ftrono.DJames.application.defaultArtistInfoFallback
import com.ftrono.DJames.application.defaultPlayerColor
import com.ftrono.DJames.application.defaultSongInfoFallback
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.models.PlaybackInfo

class SpotifyMediaObserver(
    private val context: Context
) {
    private val TAG = this::class.java.simpleName
    var isPlaying = false

    private val manager =
        context.getSystemService(
            Context.MEDIA_SESSION_SERVICE
        ) as MediaSessionManager

    private val component =
        ComponentName(
            context,
            DJamesNotificationListener::class.java
        )

    private var controller: MediaController? = null

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateSpotifyController(controllers)
        }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            /*
            Useful metadata found:
              - android.media.metadata.MEDIA_ID (String - Media URI!)
              - android.media.metadata.TITLE (String)
              - android.media.metadata.ARTIST (String - ", " separated)
              - android.media.metadata.ALBUM (String)
              - android.media.metadata.ALBUM_ARTIST (String - ", " separated)
              - android.media.metadata.ALBUM_ART (Bitmap)
              - android.media.metadata.ALBUM_ART_URI (String - "content://" format)
              - com.spotify.music.extra.ART_HTTPS_URI (String)
              - com.spotify.music.extra.CONTEXT_URI (String - NOTE: don't use if context type is artist!!!)
              - com.spotify.music.extra.CONTEXT_TITLE (String - NOTE: don't use if context type is artist!!!)
              - com.spotify.music.extra.CONTEXT_SHARE_URL (String - NOTE: don't use if context type is artist!!!)

              If context is artist, the context URI format is, i.e.:
              "spotify:list:popular-release-segments-main-roles:artist_1jGACwVpRJvsOfg29pM5L7"
            */
            metadata ?: return
            val newUri = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: ""
            val newImageUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) ?: ""

            if (newUri != lastPlaybackInfo.uri || newImageUri != lastPlaybackInfo.imageLocalUri) {
                Log.d(TAG, "onMetadataChanged triggered")
                if (newUri == "") {
                    lastPlaybackInfo = PlaybackInfo()
                } else {
                    // Store new playback info:
                    val comps = newUri.replace("spotify:", "").split(":")
                    lastPlaybackInfo = PlaybackInfo(
                        id = comps.last(),
                        type = comps.first(),
                        uri = newUri,
                        name = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
                        artists = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
                        album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
                        albumArtists = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                            ?: "",
                        imageBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
                        imageLocalUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                            ?: "",
                        imageUrl = metadata.getString("com.spotify.music.extra.ART_HTTPS_URI")
                            ?: "",
                        contextUri = metadata.getString("com.spotify.music.extra.CONTEXT_URI")
                            ?: "",
                        contextName = metadata.getString("com.spotify.music.extra.CONTEXT_TITLE")
                            ?: "",
                        contextUrl = metadata.getString("com.spotify.music.extra.CONTEXT_SHARE_URL")
                            ?: "",
                    )
                }

                //Update player:
                currentSongPlaying.postValue(
                    if (lastPlaybackInfo.name != "") {
                        utils.trimString(lastPlaybackInfo.name, 25)
                    } else defaultSongInfoFallback
                )
                currentArtistPlaying.postValue(
                    if (lastPlaybackInfo.artists != "") {
                        utils.trimString(lastPlaybackInfo.artists, 25)
                    } else defaultArtistInfoFallback
                )
                currentPlayerImage.postValue(
                    if (lastPlaybackInfo.imageLocalUri != "") {
                        lastPlaybackInfo.imageLocalUri
                    } else ""
                )
                currentPlayerColor.postValue(
                    if (lastPlaybackInfo.imageLocalUri != "") {
                        utils.getDominantColor(context, lastPlaybackInfo.imageLocalUri)
                    } else defaultPlayerColor
                )
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            val newState = state?.state == PlaybackState.STATE_PLAYING
            if (newState != isPlaying) {
                isPlaying = newState
                Log.d(TAG, "Spotify playback state changed: $isPlaying")
            }
        }
    }

    fun start() {
        // START observer:
        manager.addOnActiveSessionsChangedListener(
            sessionsChangedListener,
            component
        )
        // Check Spotify session already active:
        updateSpotifyController(
            manager.getActiveSessions(component)
        )
        Log.d(TAG, "Spotify media observer STARTED")
    }

    fun stop() {
        // STOP observer:
        controller?.unregisterCallback(callback)
        controller = null
        Log.d(TAG, "Spotify media observer STOPPED")
    }

    private fun updateSpotifyController(
        controllers: List<MediaController>?
    ) {
        // Update controller:
        val newController =
            controllers?.firstOrNull {
                it.packageName == "com.spotify.music"
            }

        // Same session as before: nothing to change:
        if (newController?.sessionToken == controller?.sessionToken) {
            return
        }

        // Detach from previous Spotify session:
        controller?.unregisterCallback(callback)
        controller = newController

        if (controller != null) {
            Log.d(TAG, "Spotify media session found")
            controller!!.registerCallback(callback)

            // Important: immediately retrieve the current states:
            callback.onMetadataChanged(
                controller!!.metadata
            )
            callback.onPlaybackStateChanged(
                controller!!.playbackState
            )

        } else {
            Log.d(TAG, "Spotify media session ended")
            isPlaying = false
        }
    }
}