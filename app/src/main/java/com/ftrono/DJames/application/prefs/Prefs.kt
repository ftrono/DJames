package com.ftrono.DJames.application.prefs

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ftrono.DJames.BuildConfig

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
        set(value) = sharedPrefs.edit().putString(KEY_USER_NICKNAME, value).apply()

    var userGender: String
        get() = sharedPrefs.getString(KEY_USER_GENDER, "Sir") as String
        set(value) = sharedPrefs.edit().putString(KEY_USER_GENDER, value).apply()

    // Experimental:
    //Enable v3:
    var enableV3: Boolean
        get() = sharedPrefs.getBoolean(KEY_ENABLE_V3, false) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_ENABLE_V3, value).apply()

    //Test: use source MIC:
    var useSourceMic: Boolean
        get() = sharedPrefs.getBoolean(KEY_USE_SOURCE_MIC, false) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_USE_SOURCE_MIC, value).apply()

    //Enable noise suppression:
    var enableNoiseSuppression: Boolean
        get() = sharedPrefs.getBoolean(KEY_ENABLE_NOISE_SUPPRESSION, true) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_ENABLE_NOISE_SUPPRESSION, value).apply()

    //Rec min freq:
    var recMinFreq: String
        get() = sharedPrefs.getString(KEY_REC_MIN_FREQ, "500") as String
        set(value) = sharedPrefs.edit().putString(KEY_REC_MIN_FREQ, value).apply()

    //Rec max freq:
    var recMaxFreq: String
        get() = sharedPrefs.getString(KEY_REC_MAX_FREQ, "2900") as String
        set(value) = sharedPrefs.edit().putString(KEY_REC_MAX_FREQ, value).apply()

    //Save to downloads:
    var recToDownloads: Boolean
        get() = sharedPrefs.getBoolean(KEY_REC_TO_DOWNLOADS, false) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_REC_TO_DOWNLOADS, value).apply()

    //Prefs:
    //Auto start-up:
    var autoStartup: Boolean
        get() = sharedPrefs.getBoolean(KEY_AUTO_STARTUP, false) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_AUTO_STARTUP, value).apply()

    //Overlay position:
    var overlayPosition: String
        //0 -> Left; 1 -> Right
        get() = sharedPrefs.getString(KEY_OVERLAY_POSITION, "Right") as String
        set(value) = sharedPrefs.edit().putString(KEY_OVERLAY_POSITION, value).apply()

    //Voice queries: Default language:
    var queryLanguage: String
        //"en-US" -> English; "it" -> Italian
        get() = sharedPrefs.getString(KEY_QUERY_LANGUAGE, "en") as String
        set(value) = sharedPrefs.edit().putString(KEY_QUERY_LANGUAGE, value).apply()

    //Voice queries: Rec timeout:
    var recTimeout: String
        get() = sharedPrefs.getString(KEY_REC_TIMEOUT, "10") as String
        set(value) = sharedPrefs.edit().putString(KEY_REC_TIMEOUT, value).apply()

    //Voice queries: Silence enabled:
    var silenceEnabledQueries: Boolean
        get() = sharedPrefs.getBoolean(KEY_SILENCE_ENABLED_QUERIES, true) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_SILENCE_ENABLED_QUERIES, value).apply()

    //Messages: Default language:
    var messageLanguage: String
        //"en-US" -> English; "it" -> Italian
        get() = sharedPrefs.getString(KEY_MESSAGE_LANGUAGE, "it") as String
        set(value) = sharedPrefs.edit().putString(KEY_MESSAGE_LANGUAGE, value).apply()

    //Messages: Rec timeout:
    var messageTimeout: String
        get() = sharedPrefs.getString(KEY_MESSAGE_TIMEOUT, "20") as String
        set(value) = sharedPrefs.edit().putString(KEY_MESSAGE_TIMEOUT, value).apply()

    //Messages: Silence enabled:
    var silenceEnabledMess: Boolean
        get() = sharedPrefs.getBoolean(KEY_SILENCE_ENABLED_MESS, true) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_SILENCE_ENABLED_MESS, value).apply()

    //Places: Default language:
    var placeLanguage: String
        //"en-US" -> English; "it" -> Italian
        get() = sharedPrefs.getString(KEY_PLACE_LANGUAGE, "it") as String
        set(value) = sharedPrefs.edit().putString(KEY_PLACE_LANGUAGE, value).apply()

    //Auto Clock:
    var autoClock: Boolean
        get() = sharedPrefs.getBoolean(KEY_AUTO_CLOCK, true) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_AUTO_CLOCK, value).apply()

    //Clock Screen enabled:
    var clockRedirectEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_CLOCK_REDIRECT_ENABLED, true) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_CLOCK_REDIRECT_ENABLED, value).apply()

    //Clock timeout:
    var clockTimeout: String
        get() = sharedPrefs.getString(KEY_CLOCK_TIMEOUT, "10") as String
        set(value) = sharedPrefs.edit().putString(KEY_CLOCK_TIMEOUT, value).apply()

    //VolumeUp enabled:
    var volumeUpEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_VOLUME_UP_ENABLED, true) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_VOLUME_UP_ENABLED, value).apply()

    //User profile:
    //Spotify user ID:
    var spotUserId: String
        get() = sharedPrefs.getString(KEY_SPOTIFY_USER_ID, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_SPOTIFY_USER_ID, value).apply()

    //Spotify user name:
    var spotUserName: String
        get() = sharedPrefs.getString(KEY_SPOTIFY_USER_NAME, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_SPOTIFY_USER_NAME, value).apply()

    //Spotify user email:
    var spotUserEMail: String
        get() = sharedPrefs.getString(KEY_SPOTIFY_USER_EMAIL, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_SPOTIFY_USER_EMAIL, value).apply()

    //Spotify user image:
    var spotUserImage: String
        get() = sharedPrefs.getString(KEY_SPOTIFY_USER_IMAGE, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_SPOTIFY_USER_IMAGE, value).apply()

    //Spotify country:
    var spotCountry: String
        get() = sharedPrefs.getString(KEY_SPOTIFY_COUNTRY, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_SPOTIFY_COUNTRY, value).apply()

    //NLP user ID:
    var nlpUserId: String
        get() = sharedPrefs.getString(KEY_NLP_USER_ID, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_NLP_USER_ID, value).apply()


    //ENCRYPTED PREFS:
    //(Encrypted) Spotify auth token:
    var spotifyToken: String
        get() = encryptedPrefs.getString(KEY_SPOTIFY_TOKEN, "") as String
        set(value) = encryptedPrefs.edit().putString(KEY_SPOTIFY_TOKEN, value).apply()

    //(Encrypted) Spotify refresh token:
    var refreshToken: String
        get() = encryptedPrefs.getString(KEY_SPOTIFY_REFRESH_TOKEN, "") as String
        set(value) = encryptedPrefs.edit().putString(KEY_SPOTIFY_REFRESH_TOKEN, value).apply()


    companion object {
        //KEYS:
        //Shared prefs:
        const val KEY_USER_NICKNAME = ".key.user_nickname"
        const val KEY_USER_GENDER = ".key.user_gender"
        const val KEY_ENABLE_V3 = ".key.enable_v3"
        const val KEY_USE_SOURCE_MIC = ".key.use_source_mic"
        const val KEY_ENABLE_NOISE_SUPPRESSION = ".key.enable_noise_suppression"
        const val KEY_REC_MIN_FREQ = ".key.rec_min_freq"
        const val KEY_REC_MAX_FREQ = ".key.rec_max_freq"
        const val KEY_REC_TO_DOWNLOADS = ".key.rec_to_downloads"
        const val KEY_AUTO_STARTUP = ".key.auto_startup"
        const val KEY_OVERLAY_POSITION = ".key.overlay_position"
        const val KEY_QUERY_LANGUAGE = ".key.query_language"
        const val KEY_REC_TIMEOUT = ".key.rec_timeout"
        const val KEY_SILENCE_ENABLED_QUERIES = ".key.enable_silence_queries"
        const val KEY_MESSAGE_LANGUAGE = ".key.message_language"
        const val KEY_MESSAGE_TIMEOUT = ".key.message_timeout"
        const val KEY_SILENCE_ENABLED_MESS = ".key.enable_silence_mess"
        const val KEY_PLACE_LANGUAGE = ".key.place_language"
        const val KEY_AUTO_CLOCK = ".key.auto_clock"
        const val KEY_CLOCK_REDIRECT_ENABLED = ".key.clock_redirect_enabled"
        const val KEY_CLOCK_TIMEOUT = ".key.clock_timeout"
        const val KEY_VOLUME_UP_ENABLED = ".key.volume_up_enabled"
        const val KEY_SPOTIFY_USER_ID = ".key.spotify_user_id"
        const val KEY_SPOTIFY_USER_NAME = ".key.spotify_user_name"
        const val KEY_SPOTIFY_USER_EMAIL = ".key.spotify_user_email"
        const val KEY_SPOTIFY_USER_IMAGE = ".key.spotify_user_image"
        const val KEY_SPOTIFY_COUNTRY = ".key.spotify_country"
        const val KEY_NLP_USER_ID = ".key.nlp_user_id"

        //Encrypted prefs:
        const val KEY_SPOTIFY_TOKEN = ".key.spotify_token"
        const val KEY_SPOTIFY_REFRESH_TOKEN = ".key.spotify_refresh_token"
    }
}