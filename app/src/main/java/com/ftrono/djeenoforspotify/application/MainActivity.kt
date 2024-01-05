package com.ftrono.djeenoforspotify.application

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ftrono.djeenoforspotify.R
import com.ftrono.djeenoforspotify.service.FloatingViewService
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {
    private var text_status: TextView? = null
    private var checkbox_nav: CheckBox? = null
    private var overlay_active : Boolean = false

    val checkThread = Thread {
        try {
            while (true) {
                synchronized(this) {
                    Thread.sleep(2000)
                    if (!isMyServiceRunning(FloatingViewService::class.java) && overlay_active) {
                        val fab1 = findViewById<FloatingActionButton>(R.id.fab) as FloatingActionButton
                        setOverlayInactive(fab1, false)
                    }
                }
            }
        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SHARED WITH ON RESUME():
        //Load views:
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab) as FloatingActionButton

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

        //(Encrypted) Spotify token:
        text_status = findViewById<TextView>(R.id.val_status)
        var spotifyToken = encryptedPrefs.getString(SettingsActivity.KEY_SPOTIFY_TOKEN, "") as String
        if (spotifyToken != "") {
            text_status!!.text = "Logged in"
            text_status!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.colorAccent))
        }

        // Load preferences:
        val sharedPrefs = applicationContext.getSharedPreferences(SettingsActivity.SETTINGS_STORAGE, MODE_PRIVATE)

        //Check NavEnabled:
        checkbox_nav = findViewById<CheckBox>(R.id.check_nav)
        var navEnabled = sharedPrefs.getBoolean(KEY_NAV_ENABLED, false)
        checkbox_nav!!.setChecked(navEnabled)

        checkbox_nav!!.setOnClickListener(View.OnClickListener {
            if (checkbox_nav!!.isChecked()) {
                navEnabled = true
                Toast.makeText(applicationContext, "Redirect to Google Maps enabled!", Toast.LENGTH_SHORT).show()
            } else {
                navEnabled = false
                Toast.makeText(applicationContext, "Redirect to Google Maps disabled.", Toast.LENGTH_SHORT).show()
            }
            sharedPrefs.edit().putBoolean(KEY_NAV_ENABLED, navEnabled).apply()
        })

        //ON CREATE() ONLY:
        // Start overlay service automatically:
        var fab_status = setOverlayActive(fab, true) as Boolean

        fab.setOnClickListener {
            if (!fab_status) {
                fab_status = setOverlayActive(fab, true)
            } else {
                fab_status = setOverlayInactive(fab, true)
            }
        }
        //Thread check:
        if (!checkThread.isAlive()){
            checkThread.start();
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent1 = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent1)
            return true
        } else if (id == R.id.action_permissions) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.setData(uri)
            startActivity(intent)
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {

        super.onResume()
        setContentView(R.layout.activity_main)

        // SHARED WITH ON CREATE():
        //Load views:
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab) as FloatingActionButton

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

        //(Encrypted) Spotify token:
        text_status = findViewById<TextView>(R.id.val_status)
        var spotifyToken = encryptedPrefs.getString(SettingsActivity.KEY_SPOTIFY_TOKEN, "") as String
        if (spotifyToken != "") {
            text_status!!.text = "Logged in"
            text_status!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.colorAccent))
        }

        // Load preferences:
        val sharedPrefs = applicationContext.getSharedPreferences(SettingsActivity.SETTINGS_STORAGE, MODE_PRIVATE)

        //Check NavEnabled:
        checkbox_nav = findViewById<CheckBox>(R.id.check_nav)
        var navEnabled = sharedPrefs.getBoolean(KEY_NAV_ENABLED, false)
        checkbox_nav!!.setChecked(navEnabled)

        checkbox_nav!!.setOnClickListener(View.OnClickListener {
            if (checkbox_nav!!.isChecked()) {
                navEnabled = true
                Toast.makeText(applicationContext, "Redirect to Google Maps enabled!", Toast.LENGTH_SHORT).show()
            } else {
                navEnabled = false
                Toast.makeText(applicationContext, "Redirect to Google Maps disabled.", Toast.LENGTH_SHORT).show()
            }
            sharedPrefs.edit().putBoolean(KEY_NAV_ENABLED, navEnabled).apply()
        })

        //ON RESUME() ONLY:
        //Check if service is active:
        var fab_status = false as Boolean
        if (isMyServiceRunning(FloatingViewService::class.java)) {
            fab_status = setOverlayActive(fab, false)
        } else {
            fab_status = setOverlayInactive(fab, false)
        }

        fab.setOnClickListener {
            if (!fab_status) {
                fab_status = setOverlayActive(fab, true)
            } else {
                fab_status = setOverlayInactive(fab, true)
            }
        }
        //Thread check:
        if (!checkThread.isAlive()){
            checkThread.start();
        }
    }

    fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun setOverlayActive(fab: FloatingActionButton, exec: Boolean): Boolean {
        fab.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorStop)
        fab.setImageResource(R.drawable.stop_icon)
        if (exec) {
            startService(Intent(this, FloatingViewService::class.java))
            overlay_active = true
        }
        return true
    }

    fun setOverlayInactive(fab: FloatingActionButton, exec: Boolean): Boolean {
        fab.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorAccent)
        fab.setImageResource(R.drawable.add_icon)
        if (exec) {
            stopService(Intent(this, FloatingViewService::class.java))
            overlay_active = false
        }
        return false
    }

    companion object {
        private val TAG: String = MainActivity::class.java.getSimpleName()
        const val KEY_NAV_ENABLED = ".key.nav_enabled"
    }
}
