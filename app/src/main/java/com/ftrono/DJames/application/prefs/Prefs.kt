package com.ftrono.DJames.application.prefs

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ftrono.DJames.BuildConfig
import androidx.core.content.edit

class Prefs (context: Context) {

    //LOAD PREFERENCES:
    //Shared preferences:
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

    //Encrypted preferences:
    // This is equivalent to using deprecated MasterKeys.AES256_GCM_SPEC
    private val key_spec = KeyGenParameterSpec.Builder(
        MasterKey.DEFAULT_MASTER_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()

    private val masterKey = MasterKey.Builder(context)
        .setKeyGenParameterSpec(key_spec)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "encrypted_preferences",
        masterKey, // masterKey created above
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)


    //GETTERS & SETTERS:
    //SHARED PREFS:
    //User profile:
    var userNickname: String
        get() = sharedPrefs.getString(KEY_USER_NICKNAME, "") as String
        set(value) = sharedPrefs.edit { putString(KEY_USER_NICKNAME, value) }

    var userGender: String
        get() = sharedPrefs.getString(KEY_USER_GENDER, "Sir") as String
        set(value) = sharedPrefs.edit { putString(KEY_USER_GENDER, value) }

    //Prefs:
    //Auto start-up:
    var autoStartup: Boolean
        get() = sharedPrefs.getBoolean(KEY_AUTO_STARTUP, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_AUTO_STARTUP, value) }

    //Overlay position:
    var overlayPosition: String
        //0 -> Left; 1 -> Right
        get() = sharedPrefs.getString(KEY_OVERLAY_POSITION, "Right") as String
        set(value) = sharedPrefs.edit { putString(KEY_OVERLAY_POSITION, value) }

    //Voice settings: Default language:
    var queryLanguage: String
        //"en-US" -> English; "it" -> Italian
        get() = sharedPrefs.getString(KEY_QUERY_LANGUAGE, "en") as String
        set(value) = sharedPrefs.edit { putString(KEY_QUERY_LANGUAGE, value) }

    //Voice settings: Voice accent:
    var voiceAccent: String
        get() = sharedPrefs.getString(KEY_VOICE_ACCENT, "British") as String
        set(value) = sharedPrefs.edit { putString(KEY_VOICE_ACCENT, value) }

    //Voice queries: Enable intro:
    var enableIntro: Boolean
        get() = sharedPrefs.getBoolean(KEY_ENABLE_INTRO, false)
        set(value) = sharedPrefs.edit { putBoolean(KEY_ENABLE_INTRO, value) }

    //Voice queries: Rec timeout:
    var recTimeout: Int
        get() = sharedPrefs.getInt(KEY_REC_TIMEOUT, 10)
        set(value) = sharedPrefs.edit { putInt(KEY_REC_TIMEOUT, value) }

    //Voice queries: Silence enabled:
    var silenceEnabledQueries: Boolean
        get() = sharedPrefs.getBoolean(KEY_SILENCE_ENABLED_QUERIES, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_SILENCE_ENABLED_QUERIES, value) }

    // Voice message recording:
    //Messages: Rec timeout:
    var messageTimeout: Int
        get() = sharedPrefs.getInt(KEY_MESSAGE_TIMEOUT, 120)
        set(value) = sharedPrefs.edit { putInt(KEY_MESSAGE_TIMEOUT, value) }

    //Voice messages: Silence enabled:
    var silenceEnabledMess: Boolean
        get() = sharedPrefs.getBoolean(KEY_SILENCE_ENABLED_MESS, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_SILENCE_ENABLED_MESS, value) }

    //Noise filtering:
    //Enable noise suppression:
    var enableNoiseSuppression: Boolean
        get() = sharedPrefs.getBoolean(KEY_ENABLE_NOISE_SUPPRESSION, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_ENABLE_NOISE_SUPPRESSION, value) }

    //Rec min freq:
    var recMinFreq: Int
        get() = sharedPrefs.getInt(KEY_REC_MIN_FREQ, 900)
        set(value) = sharedPrefs.edit { putInt(KEY_REC_MIN_FREQ, value) }

    //Rec max freq:
    var recMaxFreq: Int
        get() = sharedPrefs.getInt(KEY_REC_MAX_FREQ, 3100)
        set(value) = sharedPrefs.edit { putInt(KEY_REC_MAX_FREQ, value) }

    //Enable second noise suppression:
    var enableSecondNoiseSuppression: Boolean
        get() = sharedPrefs.getBoolean(KEY_ENABLE_SECOND_NOISE_SUPPRESSION, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_ENABLE_SECOND_NOISE_SUPPRESSION, value) }

    //Delta Hz for second noise suppression:
    var secondNoiseDelta: Int
        get() = sharedPrefs.getInt(KEY_SECOND_NOISE_DELTA, 500)
        set(value) = sharedPrefs.edit { putInt(KEY_SECOND_NOISE_DELTA, value) }

    //Auto Clock:
    var autoClock: Boolean
        get() = sharedPrefs.getBoolean(KEY_AUTO_CLOCK, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_AUTO_CLOCK, value) }

    //Clock Screen enabled:
    var clockRedirectEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_CLOCK_REDIRECT_ENABLED, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_CLOCK_REDIRECT_ENABLED, value) }

    //Clock timeout:
    var clockTimeout: Int
        get() = sharedPrefs.getInt(KEY_CLOCK_TIMEOUT, 10)
        set(value) = sharedPrefs.edit { putInt(KEY_CLOCK_TIMEOUT, value) }

    //VolumeUp enabled:
    var volumeUpEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_VOLUME_UP_ENABLED, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_VOLUME_UP_ENABLED, value) }

    //Save to downloads:
    var recToDownloads: Boolean
        get() = sharedPrefs.getBoolean(KEY_REC_TO_DOWNLOADS, false)
        set(value) = sharedPrefs.edit { putBoolean(KEY_REC_TO_DOWNLOADS, value) }

    //User profile:
    //Spotify user ID:
    var spotUserId: String
        get() = sharedPrefs.getString(KEY_SPOTIFY_USER_ID, "") as String
        set(value) = sharedPrefs.edit { putString(KEY_SPOTIFY_USER_ID, value) }

    //Spotify user name:
    var spotUserName: String
        get() = sharedPrefs.getString(KEY_SPOTIFY_USER_NAME, "") as String
        set(value) = sharedPrefs.edit { putString(KEY_SPOTIFY_USER_NAME, value) }

    //Spotify user image:
    var spotUserImage: String
        get() = sharedPrefs.getString(KEY_SPOTIFY_USER_IMAGE, "") as String
        set(value) = sharedPrefs.edit { putString(KEY_SPOTIFY_USER_IMAGE, value) }


    //ENCRYPTED PREFS:
    //(Encrypted) Spotify auth token:
    var spotifyToken: String
        get() = encryptedPrefs.getString(KEY_SPOTIFY_TOKEN, "") as String
        set(value) = encryptedPrefs.edit { putString(KEY_SPOTIFY_TOKEN, value) }

    //(Encrypted) Spotify refresh token:
    var spotRefreshToken: String
        get() = encryptedPrefs.getString(KEY_SPOTIFY_REFRESH_TOKEN, "") as String
        set(value) = encryptedPrefs.edit { putString(KEY_SPOTIFY_REFRESH_TOKEN, value) }


    companion object {
        //KEYS:
        //Shared prefs:
        const val KEY_USER_NICKNAME = ".key.user_nickname"
        const val KEY_USER_GENDER = ".key.user_gender"
        const val KEY_AUTO_STARTUP = ".key.auto_startup"
        const val KEY_OVERLAY_POSITION = ".key.overlay_position"
        const val KEY_QUERY_LANGUAGE = ".key.query_language"
        const val KEY_VOICE_ACCENT = ".key.voice_accent"
        const val KEY_ENABLE_INTRO = ".key.enable_intro"
        const val KEY_REC_TIMEOUT = ".key.rec_timeout"
        const val KEY_SILENCE_ENABLED_QUERIES = ".key.enable_silence_queries"
        const val KEY_SILENCE_ENABLED_MESS = ".key.enable_silence_mess"
        const val KEY_MESSAGE_TIMEOUT = ".key.message_timeout"
        const val KEY_ENABLE_NOISE_SUPPRESSION = ".key.enable_noise_suppression"
        const val KEY_REC_MIN_FREQ = ".key.rec_min_freq"
        const val KEY_REC_MAX_FREQ = ".key.rec_max_freq"
        const val KEY_ENABLE_SECOND_NOISE_SUPPRESSION = ".key.enable_second_noise_suppression"
        const val KEY_SECOND_NOISE_DELTA = ".key.second_noise_delta"
        const val KEY_AUTO_CLOCK = ".key.auto_clock"
        const val KEY_CLOCK_REDIRECT_ENABLED = ".key.clock_redirect_enabled"
        const val KEY_CLOCK_TIMEOUT = ".key.clock_timeout"
        const val KEY_VOLUME_UP_ENABLED = ".key.volume_up_enabled"
        const val KEY_REC_TO_DOWNLOADS = ".key.rec_to_downloads"
        const val KEY_SPOTIFY_USER_ID = ".key.spotify_user_id"
        const val KEY_SPOTIFY_USER_NAME = ".key.spotify_user_name"
        const val KEY_SPOTIFY_USER_IMAGE = ".key.spotify_user_image"

        //Encrypted prefs:
        const val KEY_SPOTIFY_TOKEN = ".key.spotify_token"
        const val KEY_SPOTIFY_REFRESH_TOKEN = ".key.spotify_refresh_token"
    }
}