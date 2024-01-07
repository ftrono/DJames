package com.ftrono.djeenoforspotify.application

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.ftrono.djeenoforspotify.R
import com.ftrono.djeenoforspotify.service.FloatingViewService
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {
    //Views:
    private var checkbox_nav: CheckBox? = null
    private var descr_use: TextView? = null
    private var descr_login_status: TextView? = null
    private var toolbar: Toolbar? = null
    private var loginButton: MenuItem? = null
    private var fab: FloatingActionButton? = null
    //Statuses:
    private var overlay_active : Boolean = false
    private var loggedIn: Boolean = false
    var fab_status: Boolean = false

    //Service status checker:
    val checkThread = Thread {
        try {
            while (true) {
                synchronized(this) {
                    Thread.sleep(2000)
                    if (!isMyServiceRunning(FloatingViewService::class.java)) {
                        fab_status = setOverlayInactive(exec=false)
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

        //Load views:
        toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        fab = findViewById<FloatingActionButton>(R.id.fab) as FloatingActionButton
        descr_login_status = findViewById<TextView>(R.id.descr_login_status)
        descr_use = findViewById<TextView>(R.id.descr_use)

        //Check Login status:
        if (prefs.spotifyToken == "") {
            loggedIn = false
            descr_login_status!!.text = getString(R.string.str_status_not_logged)
            descr_use!!.text = getString(R.string.str_use_not_logged)
        } else {
            loggedIn = true
            descr_login_status!!.text = getString(R.string.str_status_logged)
            descr_use!!.text = getString(R.string.str_use_logged)
        }

        //Check NavEnabled:
        checkbox_nav = findViewById<CheckBox>(R.id.check_nav)
        checkbox_nav!!.setChecked(prefs.navEnabled)
        checkbox_nav!!.setOnClickListener(View.OnClickListener {
            if (checkbox_nav!!.isChecked()) {
                prefs.navEnabled = true
                Toast.makeText(applicationContext, "Redirect to Google Maps enabled!", Toast.LENGTH_SHORT).show()
            } else {
                prefs.navEnabled = false
                Toast.makeText(applicationContext, "Redirect to Google Maps disabled.", Toast.LENGTH_SHORT).show()
            }
        })

        //Set FAB listener:
        fab!!.setOnClickListener {
            if (!loggedIn) {
                loggedIn = login()
            } else if (!fab_status) {
                //Start overlay service:
                fab_status = setOverlayActive(exec=true)
            } else {
                fab_status = setOverlayInactive(exec=true)
            }
        }

        //Thread check:
        if (!checkThread.isAlive()){
            checkThread.start()
        }

        //ON CREATE() ONLY:
        //Check login status:
        if (!loggedIn) {
            setOverlayLoggedOut()
        } else {
            // Start overlay service automatically:
            fab_status = setOverlayActive(exec=true)
        }
    }


    override fun onResume() {

        super.onResume()

        //ON RESUME() ONLY:
        //Check login & service status:
        if (!loggedIn) {
            setOverlayLoggedOut()
        } else if (!Settings.canDrawOverlays(this)) {
            fab_status = setOverlayInactive(exec=false)
        } else if (isMyServiceRunning(FloatingViewService::class.java)) {
            fab_status = setOverlayActive(exec=false)
        } else {
            fab_status = setOverlayInactive(exec=false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        loginButton = menu.findItem(R.id.action_login)
        //Spotify login:
        if (prefs.spotifyToken != "") {
            loginButton!!.setTitle("Logout")
        } else {
            loginButton!!.setTitle("Login")
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here
        val id = item.itemId
        descr_use = findViewById<TextView>(R.id.descr_use)
        fab = findViewById<FloatingActionButton>(R.id.fab) as FloatingActionButton
        //Login / logout:
        if (id == R.id.action_login) {
            if (!loggedIn) {
                loggedIn = login()
            } else {
                loggedIn = logout()
            }
            return true
            //Settings:
        } else if (id == R.id.action_settings) {
            val intent1 = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent1)
            return true
            //Set app preferences:
        } else if (id == R.id.action_permissions) {
            val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent1.setData(uri)
            startActivity(intent1)
            return true
        } else {
            return super.onOptionsItemSelected(item)
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

    fun setOverlayActive(exec: Boolean): Boolean {
        if (exec) {
            startService(Intent(this, FloatingViewService::class.java))
        }
        if (Settings.canDrawOverlays(this)) {
            fab!!.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorStop)
            fab!!.setImageResource(R.drawable.stop_icon)
        }
        return true
    }

    fun setOverlayInactive(exec: Boolean): Boolean {
        fab!!.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorAccent)
        fab!!.setImageResource(R.drawable.add_icon)
        if (exec) {
            stopService(Intent(this, FloatingViewService::class.java))
        }
        return false
    }

    fun setOverlayLoggedOut(): Boolean {
        fab!!.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorLogin)
        fab!!.setImageResource(R.drawable.login_icon)
        return true
    }

    //Login user:
    fun login(): Boolean {
        //CALL SPOTIFY AUTHENTICATION HERE
        //store token:
        prefs.spotifyToken = "ciaoneciaone"
        //Set Logged-In UI:
        loginButton!!.setTitle("Logout")
        descr_login_status!!.text = getString(R.string.str_status_logged)
        descr_use!!.text = getString(R.string.str_use_logged)
        //Start overlay service:
        if (!isMyServiceRunning(FloatingViewService::class.java)) {
            fab_status = setOverlayActive(exec=true)
        }
        Toast.makeText(applicationContext, "App authorized! Token: "+prefs.spotifyToken, Toast.LENGTH_SHORT).show()
        return true
    }

    //Logout user:
    fun logout(): Boolean {
        //delete token:
        prefs.spotifyToken = ""
        //Set NOT Logged-In UI:
        loginButton!!.setTitle("Login")
        descr_login_status!!.text = getString(R.string.str_status_not_logged)
        descr_use!!.text = getString(R.string.str_use_not_logged)
        //Stop overlay service:
        stopService(Intent(this, FloatingViewService::class.java))
        setOverlayLoggedOut()
        Toast.makeText(applicationContext, "App authorization removed.", Toast.LENGTH_SHORT).show()
        return false
    }

    companion object {
        private val TAG: String = MainActivity::class.java.getSimpleName()
    }
}
