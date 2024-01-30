package com.ftrono.DJames.application

import android.app.ActivityManager
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import com.ftrono.DJames.R
import com.ftrono.DJames.receivers.EventReceiver
import com.ftrono.DJames.service.FloatingViewService
import com.google.android.material.snackbar.Snackbar





class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.getSimpleName()
    //Views:
    private var toolbar: Toolbar? = null

    //Receiver:
    var eventReceiver = EventReceiver()

    //Statuses:
    private var activity_active : Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activity_active = true

        //Load Main views:
        toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        mainActionBar = supportActionBar

        descr_login_status = findViewById<TextView>(R.id.descr_login_status)
        descr_main = findViewById<TextView>(R.id.descr_main)
        descr_use = findViewById<TextView>(R.id.descr_use)
        face_cover = findViewById<View>(R.id.face_cover)
        startButton = findViewById<Button>(R.id.start_button)

        //Check Login status:
        if (prefs.spotifyToken == "") {
            loggedIn = false
            setViewLoggedOut()
        } else {
            loggedIn = true
            supportActionBar!!.subtitle = "for ${prefs.userName}"
            descr_login_status!!.text = getString(R.string.str_status_logged)
            face_cover!!.visibility = View.INVISIBLE
            if (overlay_active) {
                setOverlayActive(exec = false)
            } else {
                setOverlayInactive(exec=false)
            }
        }

        //Start Receiver:
        val filter = IntentFilter()
        filter.addAction(ACTION_LOGGED_IN)
        filter.addAction(ACTION_OVERLAY_DEACTIVATED)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(eventReceiver, filter, RECEIVER_EXPORTED)
        Log.d(TAG, "Receiver started.")

        //Start:
        startButton!!.setOnClickListener(View.OnClickListener {
            if (!loggedIn) {
                //Login user -> Open WebView:
                val intent1 = Intent(this@MainActivity, WebAuth::class.java)
                startActivity(intent1)
            } else if (!overlay_active) {
                //START:
                overlay_active = setOverlayActive(exec = true)
                //Start fake lock screen:
                val intent1 = Intent(this@MainActivity, FakeLockScreen::class.java)
                startActivity(intent1)
                Toast.makeText(applicationContext, "Ready to get your voice requests!", Toast.LENGTH_LONG).show()
//                Snackbar.make(findViewById(R.id.content_main), getString(R.string.str_use_logged), Snackbar.LENGTH_LONG)
//                    .setAction("CLOSE") { }
//                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
//                    .show()
            } else {
                //STOP:
                overlay_active = setOverlayInactive(exec = true)
            }

        })

    }


    override fun onResume() {
        super.onResume()
        //ON RESUME() ONLY:
        //Check Login status:
        if (!loggedIn) {
            overlay_active = false
            setViewLoggedOut()
        } else if (!Settings.canDrawOverlays(this)) {
            overlay_active = setOverlayInactive(exec=false)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        activity_active = false
        //unregister receivers:
        unregisterReceiver(eventReceiver)
        //empty views:
        loginButton = null
        descr_login_status = null
        descr_main = null
        descr_use = null
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
        //Login / logout:
        if (id == R.id.action_login) {
            if (!loggedIn) {
                //Login user -> Open WebView:
                val intent1 = Intent(this@MainActivity, WebAuth::class.java)
                startActivity(intent1)
            } else {
                logout()
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
            startButton!!.text = "S T O P"
            startButton!!.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorStop)
            descr_main!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.colorHeader))
            descr_main!!.setTypeface(null, Typeface.BOLD_ITALIC)
            descr_main!!.text = getString(R.string.str_main_stop)
            descr_use!!.text = getString(R.string.str_use_logged)
            descr_use!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.light_grey))
        }
        return true
    }

    fun setOverlayInactive(exec: Boolean): Boolean {
        startButton!!.text = "S T A R T"
        startButton!!.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorAccent)
        descr_main!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.light_grey))
        descr_main!!.setTypeface(null, Typeface.ITALIC)
        descr_main!!.text = getString(R.string.str_main_start)
        descr_use!!.text = getString(R.string.str_use_logged)
        descr_use!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.mid_grey))
        if (exec) {
            stopService(Intent(this, FloatingViewService::class.java))
        }
        return false
    }

    //Logout user:
    fun logout() {
        val alertDialog = AlertDialog.Builder(this)
        //Save all:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //LOG OUT:
                //Delete tokens:
                prefs.spotifyToken = ""
                prefs.refreshToken = ""
                prefs.userName = ""
                //Stop overlay service:
                if (isMyServiceRunning(FloatingViewService::class.java)) {
                    overlay_active = setOverlayInactive(exec=true)
                }
                setViewLoggedOut()
                loggedIn = false
                Toast.makeText(applicationContext, "Djames is now LOGGED OUT from your Spotify.", Toast.LENGTH_LONG).show()
            }
        })
        //Exit without saving:
        alertDialog.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                loggedIn = true
            }
        })
        alertDialog.setTitle("Log out")
        alertDialog.setMessage("You will need to login again to Spotify to use DJames.\n\nDo you want to log out?")
        alertDialog.show()
    }

    fun setViewLoggedOut(): Boolean {
        //Set NOT Logged-In UI:
        if (loginButton != null) {
            loginButton!!.setTitle("Login")
        }
        supportActionBar!!.subtitle = ""
        descr_login_status!!.text = getString(R.string.str_status_not_logged)
        face_cover!!.visibility = View.VISIBLE
        descr_main!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.light_grey))
        descr_main!!.setTypeface(null, Typeface.ITALIC)
        descr_main!!.text = getString(R.string.str_main_not_logged)
        descr_use!!.text = getString(R.string.str_use_not_logged)
        descr_use!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.mid_grey))
        startButton!!.text = "L O G I N"
        startButton!!.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.faded_grey)
        return false
    }

//    //Manage volume up keyEvent in Main Activity:
//    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
//        val keyCode = event.keyCode
//        val action = event.action
//        val source = event.source
//        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && action == 0) {
//            Log.d(
//                TAG,
//                "KEY BUTTON PRESSED, KEYCODE: ${keyCode}, ACTION: ${action}, SOURCE: ${source}"
//            )
//            Toast.makeText(
//                applicationContext,
//                "KEY BUTTON PRESSED, KEYCODE: ${keyCode}, ACTION: ${action}, SOURCE: ${source}",
//                Toast.LENGTH_LONG
//            ).show()
//        }
//        return super.dispatchKeyEvent(event)
//    }

}
