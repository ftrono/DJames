package com.ftrono.djeenoforspotify.application

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.djeenoforspotify.R
import com.ftrono.djeenoforspotify.BuildConfig

class SettingsActivity : AppCompatActivity() {
    private var val_timeout: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Settings"

        val val_timeout = findViewById<TextView>(R.id.val_timeout)
        val_timeout.setOnClickListener(View.OnClickListener { showSettings() })

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

    private fun showUsernameDialog() {
        //USERNAME POPUP (TO REPLACE!)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("User name")

        // Set up the input
        val input = EditText(this)

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        // Set up the ok buttons
        builder.setPositiveButton(
            "Ok"
        ) { dialog, which ->
            val strUserName = input.text.toString()
            val sharedPrefs = getSharedPreferences(
                SETTINGS_STORAGE,
                MODE_PRIVATE
            )
            sharedPrefs.edit().putString(
                VAL_TIMEOUT,
                strUserName
            ).commit()
            val_timeout!!.text = strUserName
        }

        // Set up the cancel buttons
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    companion object {
        val SETTINGS_STORAGE: String = BuildConfig.APPLICATION_ID
        const val VAL_TIMEOUT = ".key.val_timeout"
    }
}