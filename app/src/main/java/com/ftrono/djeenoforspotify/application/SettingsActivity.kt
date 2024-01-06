package com.ftrono.djeenoforspotify.application

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.ftrono.djeenoforspotify.R


class SettingsActivity : AppCompatActivity() {
    //Text views:
    private var text_rec_timeout: TextView? = null
    private var text_maps_timeout: TextView? = null
    private var text_maps_address: TextView? = null
    private var loggedIn = false
    //New values:
    private var newRecTimeout: String = ""
    private var newMapsTimeout: String = ""
    private var newMapsAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Settings"

        //RecTimeout:
        text_rec_timeout = findViewById<TextView>(R.id.val_rec_timeout)
        text_rec_timeout!!.text = prefs.recTimeout

        //MapsTimeout:
        text_maps_timeout = findViewById<TextView>(R.id.val_maps_timeout)
        text_maps_timeout!!.text = prefs.mapsTimeout

        //Spotify login:
        val loginButton = findViewById<Button>(R.id.login_button)
        if (prefs.spotifyToken != "") {
            loginButton!!.text = "Logout"
            loginButton.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorStop)
            loggedIn = true
        }

        loginButton.setOnClickListener(View.OnClickListener {
            if (!loggedIn) {
                //CALL SPOTIFY AUTHENTICATION HERE
                //User is logged in: store token and set button to "LOGOUT":
                prefs.spotifyToken = "ciaoneciaone"
                loginButton!!.text = "Logout"
                loginButton.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorStop)
                loggedIn = true
                Toast.makeText(applicationContext, "App authorized! Token: "+prefs.spotifyToken, Toast.LENGTH_SHORT).show()
            } else {
                //User is logged out: erase token and set button to "LOGIN":
                prefs.spotifyToken = ""
                loginButton!!.text = "Login"
                loginButton.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorAccent)
                loggedIn = false
                Toast.makeText(applicationContext, "App authorization removed.", Toast.LENGTH_SHORT).show()
            }
        })

        //GMaps address:
        text_maps_address = findViewById<TextView>(R.id.val_maps_address)
        text_maps_address!!.text = prefs.mapsAddress

        //Save:
        val saveButton = findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener(View.OnClickListener {
            //Get new values:
            newRecTimeout = text_rec_timeout!!.text.toString()
            newMapsTimeout = text_maps_timeout!!.text.toString()
            newMapsAddress = text_maps_address!!.text.toString()
            //Save all:
            saveAll(newRecTimeout, newMapsTimeout, newMapsAddress)
            Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        })
    }

    override fun onBackPressed() {
        //Get new values:
        newRecTimeout = text_rec_timeout!!.text.toString()
        newMapsTimeout = text_maps_timeout!!.text.toString()
        newMapsAddress = text_maps_address!!.text.toString()

        //If changes made: show alert dialog:
        if (prefs.recTimeout != newRecTimeout || prefs.mapsTimeout != newMapsTimeout || prefs.mapsAddress != newMapsAddress) {
            val alertDialog = AlertDialog.Builder(this)
            //Save all:
            alertDialog.setPositiveButton("Yes", object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    saveAll(newRecTimeout, newMapsTimeout, newMapsAddress)
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

    private fun saveAll(newRecTimeout: String, newMapsTimeout: String, newMapsAddress: String) {
        //RecTimeout:
        if (newRecTimeout.isNotEmpty()) {
            //validate & overwrite:
            prefs.recTimeout = validateTimeout(newVal = newRecTimeout, origVal = prefs.recTimeout)
        }
        //MapsTimeout:
        if (newMapsTimeout.isNotEmpty()) {
            //validate & overwrite:
            prefs.mapsTimeout = validateTimeout(newVal = newMapsTimeout, origVal = prefs.mapsTimeout)
        }
        //GMaps address:
        if (newMapsAddress.isNotEmpty()) {
            //validate & overwrite:
            prefs.mapsAddress = newMapsAddress
        }
    }
}