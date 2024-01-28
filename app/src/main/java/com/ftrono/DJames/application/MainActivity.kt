package com.ftrono.DJames.application

import android.app.ActivityManager
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
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

        descr_login_status = findViewById<TextView>(R.id.descr_login_status)
        descr_use = findViewById<TextView>(R.id.descr_use)

        //Load Options views:
        mapsView = findViewById<View>(R.id.maps_mode_view)
        mapsTitle = findViewById<TextView>(R.id.maps_title)
        mapsDescr = findViewById<TextView>(R.id.maps_descr)
        clockView = findViewById<View>(R.id.clock_mode_view)
        clockTitle = findViewById<TextView>(R.id.clock_title)
        clockDescr = findViewById<TextView>(R.id.clock_descr)

        //Check Login status:
        if (prefs.spotifyToken == "") {
            loggedIn = false
            descr_login_status!!.text = getString(R.string.str_status_not_logged)
            descr_use!!.text = getString(R.string.str_use_not_logged)
            setViewLoggedOut()
        } else {
            loggedIn = true
            descr_login_status!!.text = getString(R.string.str_status_logged)
            descr_use!!.text = getString(R.string.str_use_logged)
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
        filter.addAction(ACTION_MODE_CHANGED)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(eventReceiver, filter, RECEIVER_EXPORTED)
        Log.d(TAG, "Receiver started.")

        //Check NavEnabled:
        mapsView!!.setOnClickListener(View.OnClickListener {
            if (overlay_active && prefs.navEnabled) {
                overlay_active = setOverlayInactive(exec = true)
            }
            else {
                //MAPS ON:
                prefs.navEnabled = true
                if (isMyServiceRunning(FloatingViewService::class.java)) {
                    overlay_active = setOverlayInactive(exec=true)
                }
                overlay_active = setOverlayActive(exec = true)
                Toast.makeText(applicationContext, "Maps mode enabled! Use the overlay button to record a voice request.", Toast.LENGTH_SHORT).show()
            }
        })

        clockView!!.setOnClickListener(View.OnClickListener {
            if (overlay_active && !prefs.navEnabled) {
                overlay_active = setOverlayInactive(exec = true)
            }
            else {
                //CLOCK ON:
                prefs.navEnabled = false
                if (isMyServiceRunning(FloatingViewService::class.java)) {
                    overlay_active = setOverlayInactive(exec=true)
                }
                overlay_active = setOverlayActive(exec=true)
                Toast.makeText(applicationContext, "Clock mode enabled! Use the overlay button to record a voice request.", Toast.LENGTH_SHORT).show()
            }
        })

        //(TEST) Fake Lock Screen:
        val testButton = findViewById<Button>(R.id.fake_lock_button)
        testButton.setOnClickListener(View.OnClickListener {
            //Open fake lock screen:
            val intent1 = Intent(this@MainActivity, FakeLockScreen::class.java)
            startActivity(intent1)
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
        descr_use = null
        mapsView = null
        mapsTitle = null
        mapsDescr = null
        clockView = null
        clockTitle = null
        clockDescr = null
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
            if (prefs.navEnabled) {
                //MAPS ON:
                mapsView!!.setBackgroundResource(R.drawable.rounded_option_sel)
                mapsTitle!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.colorHeader))
                clockView!!.setBackgroundResource(R.drawable.rounded_option)
                clockTitle!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.mid_grey))
            } else {
                //CLOCK ON:
                mapsView!!.setBackgroundResource(R.drawable.rounded_option)
                mapsTitle!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.mid_grey))
                clockView!!.setBackgroundResource(R.drawable.rounded_option_sel)
                clockTitle!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.colorHeader))
            }
        }
        return true
    }

    fun setOverlayInactive(exec: Boolean): Boolean {
        //ALL MODES OFF:
        mapsView!!.setBackgroundResource(R.drawable.rounded_option)
        mapsTitle!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.mid_grey))
        clockView!!.setBackgroundResource(R.drawable.rounded_option)
        clockTitle!!.setTextColor(AppCompatResources.getColorStateList(this, R.color.mid_grey))
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
        descr_login_status!!.text = getString(R.string.str_status_not_logged)
        descr_use!!.text = getString(R.string.str_use_not_logged)
        //Hide views:
        mapsView!!.visibility = View.GONE
        mapsDescr!!.visibility = TextView.GONE
        mapsTitle!!.visibility = TextView.GONE
        clockView!!.visibility = View.GONE
        clockDescr!!.visibility = TextView.GONE
        clockTitle!!.visibility = TextView.GONE
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
