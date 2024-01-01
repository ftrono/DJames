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
import android.content.SharedPreferences


class SettingsActivity : AppCompatActivity() {
    private var val_timeout: TextView? = null
    private var val_spotify_token: TextView? = null
    private var val_maps_address: TextView? = null
    //var sharedPrefs = PreferenceManager
        //.getDefaultSharedPreferences(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Settings"

        val sharedPrefs = applicationContext.getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)
        val saveButton = findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener(View.OnClickListener {
            saveAll(sharedPrefs)
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
        val sharedPrefs = applicationContext.getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)
        val alertDialog = AlertDialog.Builder(
            this
        )
        alertDialog.setPositiveButton("Yes", object : OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                saveAll(sharedPrefs)
                finish()
            }
        })
        alertDialog.setNegativeButton("No", object: OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                finish()
            }
        })
        alertDialog.setMessage("Save before exit?")
        alertDialog.setTitle("Warning")
        alertDialog.show()
    }

    private fun saveAll(sharedPrefs: SharedPreferences) {
        // Load preferences:
        val strValTimeout = sharedPrefs.getString(KEY_TIMEOUT, "5")
        //Load text views:
        val val_timeout = findViewById<TextView>(R.id.val_timeout)
        val strTimeoutNew = val_timeout.text.toString()

        if (strTimeoutNew.isNotEmpty()) {
            val intTimeout = strTimeoutNew.toInt()
            if (intTimeout > 0 && intTimeout <= 15) {
                sharedPrefs.edit().putString(KEY_TIMEOUT, strTimeoutNew).commit()   //apply()?
            } else {
                val_timeout!!.text = strValTimeout
            }
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