package com.ftrono.DJames.utilities

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
    //Rec timeout:
    var recTimeout: String
        get() = sharedPrefs.getString(KEY_REC_TIMEOUT, "5") as String
        set(value) = sharedPrefs.edit().putString(KEY_REC_TIMEOUT, value).apply()

    var overlayPosition: String
        //0 -> Left; 1 -> Right
        get() = sharedPrefs.getString(KEY_OVERLAY_POSITION, "1") as String
        set(value) = sharedPrefs.edit().putString(KEY_OVERLAY_POSITION, value).apply()

    //Clock Screen enabled:
    var clockRedirectEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_CLOCK_REDIRECT_ENABLED, true) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_CLOCK_REDIRECT_ENABLED, value).apply()

    //Clock timeout:
    var clockTimeout: String
        get() = sharedPrefs.getString(KEY_CLOCK_TIMEOUT, "5") as String
        set(value) = sharedPrefs.edit().putString(KEY_CLOCK_TIMEOUT, value).apply()

    //VolumeUp enabled:
    var volumeUpEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_VOLUME_UP_ENABLED, true) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_VOLUME_UP_ENABLED, value).apply()

    //Preferred mic:
    var micType: String
        //0 -> Current default; 1 -> Primary device
        get() = sharedPrefs.getString(KEY_MIC_TYPE, "0") as String
        set(value) = sharedPrefs.edit().putString(KEY_MIC_TYPE, value).apply()

    //User profile:
    //Spotify user name:
    var userName: String
        get() = sharedPrefs.getString(KEY_USER_NAME, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_USER_NAME, value).apply()

    //Spotify user email:
    var userEMail: String
        get() = sharedPrefs.getString(KEY_USER_EMAIL, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_USER_EMAIL, value).apply()

    //Spotify user image:
    var userImage: String
        get() = sharedPrefs.getString(KEY_USER_IMAGE, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_USER_IMAGE, value).apply()


    //ENCRYPTED PREFS:
    //(Encrypted) Spotify auth token:
    var spotifyToken: String
        get() = encryptedPrefs.getString(KEY_SPOTIFY_TOKEN, "") as String
        set(value) = encryptedPrefs.edit().putString(KEY_SPOTIFY_TOKEN, value).apply()

    //(Encrypted) Spotify refresh token:
    var refreshToken: String
        get() = encryptedPrefs.getString(KEY_REFRESH_TOKEN, "") as String
        set(value) = encryptedPrefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()


    companion object {
        //KEYS:
        //Shared prefs:
        const val KEY_REC_TIMEOUT = ".key.rec_timeout"
        const val KEY_OVERLAY_POSITION = ".key.overlay_position"
        const val KEY_CLOCK_REDIRECT_ENABLED = ".key.clock_redirect_enabled"
        const val KEY_CLOCK_TIMEOUT = ".key.clock_timeout"
        const val KEY_VOLUME_UP_ENABLED = ".key.volume_up_enabled"
        const val KEY_MIC_TYPE = ".key.mic_type"
        const val KEY_USER_NAME = ".key.user_name"
        const val KEY_USER_EMAIL = ".key.user_email"
        const val KEY_USER_IMAGE = ".key.user_image"

        //Encrypted prefs:
        const val KEY_SPOTIFY_TOKEN = ".key.spotify_token"
        const val KEY_REFRESH_TOKEN = ".key.refresh_token"
    }
}