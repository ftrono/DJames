package com.ftrono.djeenoforspotify.application

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.djeenoforspotify.BuildConfig
import com.ftrono.djeenoforspotify.R
import android.content.DialogInterface.OnClickListener
import android.widget.Toast
import android.content.SharedPreferences


class SettingsActivity : AppCompatActivity() {
    private var text_timeout: TextView? = null
    private var text_spotify_token: TextView? = null
    private var text_maps_address: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Settings"

        // Load preferences:
        val sharedPrefs = applicationContext.getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)

        //Timeout:
        text_timeout = findViewById<TextView>(R.id.val_timeout)
        val origTimeout = sharedPrefs.getString(KEY_TIMEOUT, "5") as String
        text_timeout!!.text = origTimeout

        //Spotify token:
        text_spotify_token = findViewById<TextView>(R.id.val_spotify_token)
        val origSpotifyToken = sharedPrefs.getString(KEY_SPOTIFY_TOKEN, "") as String
        text_spotify_token!!.text = origSpotifyToken

        //GMaps address:
        text_maps_address = findViewById<TextView>(R.id.val_maps_address)
        val origMapsAddress = sharedPrefs.getString(KEY_MAPS_ADDRESS, "") as String
        text_maps_address!!.text = origMapsAddress

        //Save:
        val saveButton = findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener(View.OnClickListener {
            //Timeout:
            val newTimeout = text_timeout!!.text.toString()
            if (newTimeout.isNotEmpty()) {
                sharedPrefs.edit().putString(KEY_TIMEOUT, validateTimeout(newVal=newTimeout, origVal=origTimeout)).apply()
            }
            //Spotify token:
            val newSpotifyToken = text_spotify_token!!.text.toString()
            if (newSpotifyToken.isNotEmpty()) {
                sharedPrefs.edit().putString(KEY_SPOTIFY_TOKEN, newSpotifyToken).apply()
            }
            //GMaps address:
            val newMapsAddress = text_maps_address!!.text.toString()
            if (newMapsAddress.isNotEmpty()) {
                sharedPrefs.edit().putString(KEY_MAPS_ADDRESS, newMapsAddress).apply()
            }
            Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        })

        var info = ""
        /*
        radiobutton1.setOnClickListener(View.OnClickListener {
            info = getString(R.string.am_selected)
            checkbox1.setText(applicationContext.resources.getString(R.string.ingr1_am))
            checkbox2.setText(applicationContext.resources.getString(R.string.ingr2_am))
            checkbox3.setText(applicationContext.resources.getString(R.string.ingr3_am))
            Toast.makeText(applicationContext, info, Toast.LENGTH_SHORT).show()
            ingr1 = getString(R.string.ingr1_am)
            ingr2 = getString(R.string.ingr2_am)
            ingr3 = getString(R.string.ingr3_am)
            val sharedPrefs = getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)
            sharedPrefs.edit().putString(INGR1V_KEY, ingr1).apply()
            sharedPrefs.edit().putString(INGR2V_KEY, ingr2).apply()
            sharedPrefs.edit().putString(INGR3V_KEY, ingr3).apply()
            sharedPrefs.edit().putBoolean(AM_YN_KEY, true).apply()
            sharedPrefs.edit().putBoolean(NA_YN_KEY, false).apply()
        })
        */
    }

    override fun onBackPressed() {
        //Load preferences:
        val sharedPrefs = applicationContext.getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)
        val origTimeout = sharedPrefs.getString(KEY_TIMEOUT, "5") as String

        /*
        //Load views:
        val text_timeout = findViewById<TextView>(R.id.val_timeout)
        val text_spotify_token = findViewById<TextView>(R.id.val_spotify_token)
        val text_maps_address = findViewById<TextView>(R.id.val_maps_address)
         */

        //Alert dialog:
        val alertDialog = AlertDialog.Builder(
            this
        )
        //Save:
        alertDialog.setPositiveButton("Yes", object : OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Timeout:
                val newTimeout = text_timeout!!.text.toString()
                if (newTimeout.isNotEmpty()) {
                    sharedPrefs.edit().putString(KEY_TIMEOUT, validateTimeout(newVal=newTimeout, origVal=origTimeout)).apply()
                }
                //Spotify token:
                val newSpotifyToken = text_spotify_token!!.text.toString()
                if (newSpotifyToken.isNotEmpty()) {
                    sharedPrefs.edit().putString(KEY_SPOTIFY_TOKEN, newSpotifyToken).apply()
                }
                //GMaps address:
                val newMapsAddress = text_maps_address!!.text.toString()
                if (newMapsAddress.isNotEmpty()) {
                    sharedPrefs.edit().putString(KEY_MAPS_ADDRESS, newMapsAddress).apply()
                }
                Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
        //Exit without saving:
        alertDialog.setNegativeButton("No", object: OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                Toast.makeText(applicationContext, "Settings NOT saved.", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
        alertDialog.setMessage("Save before exit?")
        alertDialog.setTitle("Warning")
        alertDialog.show()
    }

    private fun validateTimeout(newVal: String, origVal: String) : String {
        val newInt = newVal.toInt()
        return if (newInt in 3..15) {
            newVal
        } else {
            origVal
        }
    }

    private fun showSettings() {
        val sharedPrefs = getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)
        /*
        val strValTimeout = sharedPrefs.getString(VAL_TIMEOUT, null)
        val_timeout!!.text = strValTimeout
        val am_yn = sharedPrefs.getBoolean(AM_YN_KEY, true)
        radiobutton1!!.isChecked = am_yn
        val na_yn = sharedPrefs.getBoolean(NA_YN_KEY, false)
        radiobutton2!!.isChecked = na_yn
        ingr1 = sharedPrefs.getString(
            INGR1V_KEY,
            applicationContext.resources.getString(R.string.ingr1_am)
        )
        checkbox1!!.text = ingr1
        ingr2 = sharedPrefs.getString(
            INGR2V_KEY,
            applicationContext.resources.getString(R.string.ingr2_am)
        )
        checkbox2!!.text = ingr2
        ingr3 = sharedPrefs.getString(
            INGR3V_KEY,
            applicationContext.resources.getString(R.string.ingr3_am)
        )
        checkbox3!!.text = ingr3
        val ingr1_yn = sharedPrefs.getBoolean(INGR1YN_KEY, true)
        checkbox1!!.isChecked = ingr1_yn
        val ingr2_yn = sharedPrefs.getBoolean(INGR2YN_KEY, false)
        checkbox2!!.isChecked = ingr2_yn
        val ingr3_yn = sharedPrefs.getBoolean(INGR3YN_KEY, false)
        checkbox3!!.isChecked = ingr3_yn
         */
    }

    private fun showSaveDialog() {
        //USERNAME POPUP (TO REPLACE!)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Warning: you did not save!")

        /*
        // Set up the ok buttons
        builder.setPositiveButton(
            "Save"
        ) { dialog, which ->
            val strUserName = input.text.toString()
            val sharedPrefs = getSharedPreferences(
                SETTINGS_STORAGE,
                MODE_PRIVATE
            )
            sharedPrefs.edit().putString(
                KEY_TIMEOUT,
                strUserName
            ).commit()
            val_timeout!!.text = strUserName
            finish()
        }

        // Set up the cancel buttons
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        builder.show()

         */
    }

    companion object {
        val SETTINGS_STORAGE: String = BuildConfig.APPLICATION_ID
        const val KEY_TIMEOUT = ".key.timeout"
        const val KEY_SPOTIFY_TOKEN = ".key.spotify_token"
        const val KEY_MAPS_ADDRESS = ".key.maps_address"
    }
}