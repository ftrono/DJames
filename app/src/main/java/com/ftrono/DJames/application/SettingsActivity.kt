package com.ftrono.DJames.application

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.DJames.R


class SettingsActivity : AppCompatActivity() {
    //Text views:
    private var text_rec_timeout: TextView? = null
    private var text_maps_timeout: TextView? = null
    private var text_clock_timeout: TextView? = null
    private var text_maps_address: TextView? = null
    //New values:
    private var newRecTimeout: String = ""
    private var newMapsTimeout: String = ""
    private var newClockTimeout: String = ""
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

        //ClockTimeout:
        text_clock_timeout = findViewById<TextView>(R.id.val_clock_timeout)
        text_clock_timeout!!.text = prefs.clockTimeout

        //GMaps address:
        text_maps_address = findViewById<TextView>(R.id.val_maps_address)
        text_maps_address!!.text = prefs.mapsAddress

        //Save:
        val saveButton = findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener(View.OnClickListener {
            //Get new values:
            newRecTimeout = text_rec_timeout!!.text.toString()
            newMapsTimeout = text_maps_timeout!!.text.toString()
            newClockTimeout = text_clock_timeout!!.text.toString()
            newMapsAddress = text_maps_address!!.text.toString()
            //Save all:
            saveAll(newRecTimeout, newMapsTimeout, newClockTimeout, newMapsAddress)
            Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
            //Start Main:
            val intent1 = Intent(this, MainActivity::class.java)
            startActivity(intent1)
        })
    }

    override fun onBackPressed() {
        //Get new values:
        newRecTimeout = text_rec_timeout!!.text.toString()
        newMapsTimeout = text_maps_timeout!!.text.toString()
        newClockTimeout = text_clock_timeout!!.text.toString()
        newMapsAddress = text_maps_address!!.text.toString()

        //If changes made: show alert dialog:
        if (prefs.recTimeout != newRecTimeout || prefs.mapsTimeout != newMapsTimeout || prefs.clockTimeout != newClockTimeout || prefs.mapsAddress != newMapsAddress) {
            val alertDialog = MaterialAlertDialogBuilder(this)
            //Save all:
            alertDialog.setPositiveButton("Yes", object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    saveAll(newRecTimeout, newMapsTimeout, newClockTimeout, newMapsAddress)
                    Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
                    finish()
                    //Start Main:
                    val intent1 = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent1)
                }
            })
            //Exit without saving:
            alertDialog.setNegativeButton("No", object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    Toast.makeText(applicationContext, "Settings NOT saved.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                    //Start Main:
                    val intent1 = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent1)
                }
            })
            alertDialog.setTitle("Warning")
            alertDialog.setMessage("Save before exit?")
            alertDialog.show()
        } else {
            finish()
            //Start Main:
            val intent1 = Intent(this, MainActivity::class.java)
            startActivity(intent1)
        }
    }


    private fun validateTimeout(newVal: String, origVal: String, min_val: Int, max_val: Int) : String {
        val newInt = newVal.toInt()
        return if (newInt in min_val..max_val) {
            newVal
        } else {
            origVal
        }
    }

    private fun saveAll(newRecTimeout: String, newMapsTimeout: String, newClockTimeout: String, newMapsAddress: String) {
        //RecTimeout:
        if (newRecTimeout.isNotEmpty()) {
            //validate & overwrite:
            prefs.recTimeout = validateTimeout(newVal = newRecTimeout, origVal = prefs.recTimeout, min_val = 3, max_val = 10)
        }
        //MapsTimeout:
        if (newMapsTimeout.isNotEmpty()) {
            //validate & overwrite:
            prefs.mapsTimeout = validateTimeout(newVal = newMapsTimeout, origVal = prefs.mapsTimeout, min_val = 0, max_val = 10)
        }
        //ClockTimeout:
        if (newClockTimeout.isNotEmpty()) {
            //validate & overwrite:
            prefs.clockTimeout = validateTimeout(newVal = newClockTimeout, origVal = prefs.clockTimeout, min_val = 0, max_val = 60)
        }
        //GMaps address:
        if (newMapsAddress.isNotEmpty()) {
            //validate & overwrite:
            prefs.mapsAddress = newMapsAddress
        }
    }
}