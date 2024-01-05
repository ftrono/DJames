package com.ftrono.djeenoforspotify.application

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.SharedPreferences
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ftrono.djeenoforspotify.BuildConfig
import com.ftrono.djeenoforspotify.R


class SettingsActivity : AppCompatActivity() {
    //Text views:
    private var text_rec_timeout: TextView? = null
    private var text_maps_timeout: TextView? = null
    private var text_spotify_token: TextView? = null
    private var text_maps_address: TextView? = null

    //Shared preferences:
    private var origRecTimeout: String? = null
    private var origMapsTimeout: String? = null
    private var origSpotifyToken: String? = null
    private var origMapsAddress: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Settings"

        // Load preferences:
        val sharedPrefs = applicationContext.getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)

        //Encrypted preferences:
        // This is equivalent to using deprecated MasterKeys.AES256_GCM_SPEC
        val key_spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyGenParameterSpec(key_spec)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            "encrypted_preferences",
            masterKey, // masterKey created above
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        //RecTimeout:
        text_rec_timeout = findViewById<TextView>(R.id.val_rec_timeout)
        origRecTimeout = sharedPrefs.getString(KEY_REC_TIMEOUT, "5") as String
        text_rec_timeout!!.text = origRecTimeout

        //MapsTimeout:
        text_maps_timeout = findViewById<TextView>(R.id.val_maps_timeout)
        origMapsTimeout = sharedPrefs.getString(KEY_MAPS_TIMEOUT, "3") as String
        text_maps_timeout!!.text = origMapsTimeout

        //(Encrypted) Spotify token:
        text_spotify_token = findViewById<TextView>(R.id.val_spotify_token)
        origSpotifyToken = encryptedPrefs.getString(KEY_SPOTIFY_TOKEN, "") as String
        text_spotify_token!!.text = origSpotifyToken

        //GMaps address:
        text_maps_address = findViewById<TextView>(R.id.val_maps_address)
        origMapsAddress =
            sharedPrefs.getString(KEY_MAPS_ADDRESS, "https://www.google.com/maps/") as String
        text_maps_address!!.text = origMapsAddress

        //Save:
        val saveButton = findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener(View.OnClickListener {
            //RecTimeout:
            val newRecTimeout = text_rec_timeout!!.text.toString()
            if (newRecTimeout.isNotEmpty()) {
                sharedPrefs.edit().putString(
                    KEY_REC_TIMEOUT,
                    validateTimeout(newVal = newRecTimeout, origVal = origRecTimeout!!)
                ).apply()
            }
            //MapsTimeout:
            val newMapsTimeout = text_maps_timeout!!.text.toString()
            if (newMapsTimeout.isNotEmpty()) {
                sharedPrefs.edit().putString(
                    KEY_MAPS_TIMEOUT,
                    validateTimeout(newVal = newMapsTimeout, origVal = origMapsTimeout!!)
                ).apply()
            }
            //(Encrypted) Spotify token:
            val newSpotifyToken = text_spotify_token!!.text.toString()
            if (newSpotifyToken.isNotEmpty()) {
                encryptedPrefs.edit().putString(KEY_SPOTIFY_TOKEN, newSpotifyToken).apply()
            }
            //GMaps address:
            val newMapsAddress = text_maps_address!!.text.toString()
            if (newMapsAddress.isNotEmpty()) {
                sharedPrefs.edit().putString(KEY_MAPS_ADDRESS, newMapsAddress).apply()
            }
            Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        })
    }

    override fun onBackPressed() {
        //Load preferences:
        val sharedPrefs = applicationContext.getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)

        //Encrypted preferences:
        // This is equivalent to using deprecated MasterKeys.AES256_GCM_SPEC
        val key_spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyGenParameterSpec(key_spec)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            "encrypted_preferences",
            masterKey, // masterKey created above
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

        //New vals:
        val newRecTimeout = text_rec_timeout!!.text.toString()
        val newMapsTimeout = text_maps_timeout!!.text.toString()
        val newSpotifyToken = text_spotify_token!!.text.toString()
        val newMapsAddress = text_maps_address!!.text.toString()

        //If changes made: show alert dialog:
        if (origRecTimeout != newRecTimeout || origMapsTimeout != newMapsTimeout || origSpotifyToken != newSpotifyToken || origMapsAddress != newMapsAddress) {
            val alertDialog = AlertDialog.Builder(
                this
            )
            //Save:
            alertDialog.setPositiveButton("Yes", object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    //RecTimeout:
                    if (newRecTimeout.isNotEmpty()) {
                        sharedPrefs.edit().putString(
                            KEY_REC_TIMEOUT,
                            validateTimeout(newVal = newRecTimeout, origVal = origRecTimeout!!)
                        ).apply()
                    }
                    //MapsTimeout:
                    if (newMapsTimeout.isNotEmpty()) {
                        sharedPrefs.edit().putString(
                            KEY_MAPS_TIMEOUT,
                            validateTimeout(newVal = newMapsTimeout, origVal = origMapsTimeout!!)
                        ).apply()
                    }
                    //(Encrypted) Spotify token:
                    if (newSpotifyToken.isNotEmpty()) {
                        encryptedPrefs.edit().putString(KEY_SPOTIFY_TOKEN, newSpotifyToken).apply()
                    }
                    //GMaps address:
                    if (newMapsAddress.isNotEmpty()) {
                        sharedPrefs.edit().putString(KEY_MAPS_ADDRESS, newMapsAddress).apply()
                    }
                    Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
            //Exit without saving:
            alertDialog.setNegativeButton("No", object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    Toast.makeText(applicationContext, "Settings NOT saved.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            })
            alertDialog.setMessage("Save before exit?")
            alertDialog.setTitle("Warning")
            alertDialog.show()
        } else {
            finish()
        }
    }


    private fun validateTimeout(newVal: String, origVal: String) : String {
        val newInt = newVal.toInt()
        return if (newInt in 3..10) {
            newVal
        } else {
            origVal
        }
    }

    companion object {
        val SETTINGS_STORAGE: String = BuildConfig.APPLICATION_ID
        const val KEY_REC_TIMEOUT = ".key.rec_timeout"
        const val KEY_MAPS_TIMEOUT = ".key.maps_timeout"
        const val KEY_SPOTIFY_TOKEN = ".key.spotify_token"
        const val KEY_MAPS_ADDRESS = ".key.maps_address"
    }
}