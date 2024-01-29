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

    //Maps timeout:
    var mapsTimeout: String
        get() = sharedPrefs.getString(KEY_MAPS_TIMEOUT, "3") as String
        set(value) = sharedPrefs.edit().putString(KEY_MAPS_TIMEOUT, value).apply()

    //Clock timeout:
    var clockTimeout: String
        get() = sharedPrefs.getString(KEY_CLOCK_TIMEOUT, "10") as String
        set(value) = sharedPrefs.edit().putString(KEY_CLOCK_TIMEOUT, value).apply()

    //GMaps address:
    var mapsAddress: String
        get() = sharedPrefs.getString(KEY_MAPS_ADDRESS, "https://www.google.com/maps/") as String
        set(value) = sharedPrefs.edit().putString(KEY_MAPS_ADDRESS, value).apply()

    //Nav enabled:
    var navEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_NAV_ENABLED, false) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_NAV_ENABLED, value).apply()

    //Clock Screen enabled:
    var clockEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_CLOCK_ENABLED, false) as Boolean
        set(value) = sharedPrefs.edit().putBoolean(KEY_CLOCK_ENABLED, value).apply()

    //User profile:
    var userName: String
        get() = sharedPrefs.getString(KEY_USER_NAME, "") as String
        set(value) = sharedPrefs.edit().putString(KEY_USER_NAME, value).apply()


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
        const val KEY_MAPS_TIMEOUT = ".key.maps_timeout"
        const val KEY_CLOCK_TIMEOUT = ".key.clock_timeout"
        const val KEY_MAPS_ADDRESS = ".key.maps_address"
        const val KEY_NAV_ENABLED = ".key.nav_enabled"
        const val KEY_CLOCK_ENABLED = ".key.clock_enabled"
        const val KEY_USER_NAME = ".key.user_name"

        //Encrypted prefs:
        const val KEY_SPOTIFY_TOKEN = ".key.spotify_token"
        const val KEY_REFRESH_TOKEN = ".key.refresh_token"
    }
}