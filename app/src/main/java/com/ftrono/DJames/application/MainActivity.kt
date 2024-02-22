package com.ftrono.DJames.application

import android.app.ActivityManager
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.ftrono.DJames.R
import com.ftrono.DJames.service.FloatingViewService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.roundToInt
import android.content.BroadcastReceiver
import android.content.Context
import androidx.core.content.FileProvider
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.getSimpleName()

    //Views:
    private var toolbar: Toolbar? = null
    private var baloon: View? = null
    private var baloon_arrow: View? = null
    private var mega_face: ImageView? = null
    private var density: Float = 0F

    //View resources:
    private var mainActionBar: ActionBar? = null    //eventReceiver (login)
    private var descr_login_status: TextView? = null    //eventReceiver (login)
    private var descr_main: TextView? = null    //eventReceiver (login), setOverlayActive(utilities)
    private var descr_use: TextView? = null    //eventReceiver (login, volumeSettings, utilities)
    private var face_cover: View? = null    //eventReceiver (login)
    private var startButton: Button? = null    //eventReceiver (login), setOverlayActive(utilities)
    private var loginButton: MenuItem? = null    //eventReceiver (login)


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        acts_active.add(TAG)

        //Screen density:
        density = applicationContext.resources.displayMetrics.density

        //Load Main views:
        toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        mainActionBar = supportActionBar

        descr_login_status = findViewById<TextView>(R.id.descr_login_status)
        baloon = findViewById<View>(R.id.baloon)
        baloon_arrow = findViewById<View>(R.id.baloon_arrow)
        descr_main = findViewById<TextView>(R.id.descr_main)
        descr_use = findViewById<TextView>(R.id.descr_use)
        mega_face = findViewById<ImageView>(R.id.DJames_face)
        face_cover = findViewById<View>(R.id.face_cover)
        startButton = findViewById<Button>(R.id.start_button)

        //Check initial orientation:
        var config = getResources().getConfiguration()
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //HORIZONTAL:
            baloon!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToRight = R.id.DJames_face
                bottomToTop = R.id.start_button
            }
            mega_face!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = R.id.descr_login_status
                rightToLeft = R.id.baloon
                setMargins(0, 0, (50*density).roundToInt(),0)   //marginRight
            }
            baloon_arrow!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = R.id.baloon
                bottomToBottom = R.id.baloon
                rightToLeft = R.id.baloon
                setMargins(0, 0,(-25*density).roundToInt(),0)   //marginRight
            }
        }

        //Check Login status:
        if (prefs.spotifyToken == "") {
            loggedIn = false
            setViewLoggedOut()
        } else {
            loggedIn = true
            supportActionBar!!.subtitle = "for ${prefs.userName}"
            descr_login_status!!.text = getString(R.string.str_status_logged)
            if (prefs.volumeUpEnabled) {
                descr_use!!.text = getString(R.string.str_use_logged)
            } else {
                descr_use!!.text = getString(R.string.str_use_logged_no_vol)
            }
            face_cover!!.visibility = View.INVISIBLE
            if (overlay_active) {
                setOverlayActive()
            } else {
                setOverlayInactive()
            }
        }

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_MAIN_LOGGED_IN)
        actFilter.addAction(ACTION_SETTINGS_VOL_UP)
        actFilter.addAction(ACTION_OVERLAY_ACTIVATED)
        actFilter.addAction(ACTION_OVERLAY_DEACTIVATED)
        actFilter.addAction(ACTION_FINISH_MAIN)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(mainActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "MainActReceiver started.")

        //Start:
        startButton!!.setOnClickListener(View.OnClickListener {
            if (!loggedIn) {
                //Login user -> Open WebView:
                val intent1 = Intent(this@MainActivity, WebAuth::class.java)
                startActivity(intent1)
            } else if (!overlay_active) {
                //START:
                if (!isMyServiceRunning(FloatingViewService::class.java)) {
                    startService(Intent(this, FloatingViewService::class.java))
                }
                //Start fake lock screen:
                val intent1 = Intent(this@MainActivity, FakeLockScreen::class.java)
                startActivity(intent1)
                Toast.makeText(applicationContext, "Ask me to play a Spotify Song!", Toast.LENGTH_LONG).show()
                finish()
//                Snackbar.make(findViewById(R.id.content_main), getString(R.string.str_use_logged), Snackbar.LENGTH_LONG)
//                    .setAction("CLOSE") { }
//                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
//                    .show()
            } else {
                //STOP:
                if (isMyServiceRunning(FloatingViewService::class.java)) {
                    stopService(Intent(this, FloatingViewService::class.java))
                }
            }

        })

        //Init log directory:
        logDir = File(cacheDir, "log_requests")
        //logDir!!.deleteRecursively()
        if (!logDir!!.exists()) {
            logDir!!.mkdir()
        }
        //delete older logs:
        utils.deleteOldLogs()
    }


    override fun onResume() {
        super.onResume()
        //ON RESUME() ONLY:
        //Check Login status:
        if (!loggedIn) {
            overlay_active = false
            setViewLoggedOut()
        } else if (!Settings.canDrawOverlays(this)) {
            overlay_active = setOverlayInactive()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        unregisterReceiver(mainActReceiver)
        //empty views:
        mainActionBar = null
        descr_login_status = null
        descr_main = null
        descr_use = null
        face_cover = null
        loginButton = null
        startButton = null
        acts_active.remove(TAG)
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
        //History:
        } else if (id == R.id.action_history) {
            val intent1 = Intent(this@MainActivity, HistoryActivity::class.java)
            startActivity(intent1)
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //VERTICAL:
            baloon!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToTop = R.id.DJames_face
            }
            mega_face!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = R.id.baloon
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                setMargins(0, (20*density).roundToInt(),0,0)   //marginTop
            }
            baloon_arrow!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToTop = ConstraintLayout.LayoutParams.UNSET   //clear
                bottomToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToLeft = R.id.baloon
                rightToRight = R.id.baloon
                topToBottom = R.id.baloon
                setMargins(0, (-40*density).roundToInt(),0,0)   //marginTop
            }
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //HORIZONTAL:
            baloon!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToRight = R.id.DJames_face
                bottomToTop = R.id.start_button
            }
            mega_face!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = R.id.descr_login_status
                rightToLeft = R.id.baloon
                setMargins(0,0, (50*density).roundToInt(),0)   //marginRight
            }
            baloon_arrow!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = R.id.baloon
                bottomToBottom = R.id.baloon
                rightToLeft = R.id.baloon
                setMargins(0, 0,(-25*density).roundToInt(),0)   //marginRight
            }
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

    //Logout user:
    fun logout() {
        val alertDialog = MaterialAlertDialogBuilder(this)
        //Save all:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //LOG OUT:
                //Delete tokens & user details:
                loggedIn = false
                prefs.spotifyToken = ""
                prefs.refreshToken = ""
                prefs.userName = ""
                prefs.userEMail = ""
                prefs.userImage = ""
                prefs.userId = ""
                //Stop overlay service:
                if (isMyServiceRunning(FloatingViewService::class.java)) {
                    stopService(Intent(applicationContext, FloatingViewService::class.java))
                }
                setViewLoggedOut()
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

    //Set Overlay Active view in Main:
    fun setOverlayActive(): Boolean {
        try {
            if (Settings.canDrawOverlays(applicationContext)) {
                startButton!!.text = "S T O P"
                startButton!!.backgroundTintList = AppCompatResources.getColorStateList(applicationContext, R.color.colorStop)
                descr_main!!.setTextColor(AppCompatResources.getColorStateList(applicationContext, R.color.colorHeader))
                descr_main!!.setTypeface(null, Typeface.BOLD_ITALIC)
                descr_main!!.text = getString(R.string.str_main_stop)
                if (prefs.volumeUpEnabled) {
                    descr_use!!.text = getString(R.string.str_use_logged)
                } else {
                    descr_use!!.text = getString(R.string.str_use_logged_no_vol)
                }
                descr_use!!.setTextColor(AppCompatResources.getColorStateList(applicationContext, R.color.light_grey))
            }
            Log.d(TAG, "SetOverlayActive()")
        } catch (e: Exception) {
            Log.d(TAG, "SetOverlayActive(): resources not available.")
        }
        return true
    }

    //Set Overlay Inactive view in Main:
    fun setOverlayInactive(): Boolean {
        try {
            startButton!!.text = "S T A R T"
            startButton!!.backgroundTintList = AppCompatResources.getColorStateList(applicationContext, R.color.colorAccent)
            descr_main!!.setTextColor(AppCompatResources.getColorStateList(applicationContext, R.color.light_grey))
            descr_main!!.setTypeface(null, Typeface.ITALIC)
            descr_main!!.text = getString(R.string.str_main_start)
            if (prefs.volumeUpEnabled) {
                descr_use!!.text = getString(R.string.str_use_logged)
            } else {
                descr_use!!.text = getString(R.string.str_use_logged_no_vol)
            }
            descr_use!!.setTextColor(AppCompatResources.getColorStateList(applicationContext, R.color.mid_grey))
            Log.d(TAG, "SetOverlayInactive()")
        } catch (e: Exception) {
            Log.d(TAG, "SetOverlayInactive(): resources not available.")
        }
        return false
    }


    //PERSONAL RECEIVER:
    private var mainActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //When logged in:
            if (intent!!.action == ACTION_MAIN_LOGGED_IN) {
                Log.d(TAG, "MAIN: ACTION_MAIN_LOGGED_IN.")

                loggedIn = true
                try {
                    //Set Logged-In UI:
                    if (loginButton != null) {
                        loginButton!!.setTitle("Logout")
                    }
                    mainActionBar!!.subtitle = "for ${prefs.userName}"
                    descr_login_status!!.text = context!!.getString(R.string.str_status_logged)
                    face_cover!!.visibility = View.INVISIBLE
                    startButton!!.text = "S T A R T"
                    startButton!!.backgroundTintList =
                        AppCompatResources.getColorStateList(context, R.color.colorAccent)
                    descr_main!!.setTextColor(
                        AppCompatResources.getColorStateList(
                            context,
                            R.color.light_grey
                        )
                    )
                    descr_main!!.setTypeface(null, Typeface.ITALIC)
                    descr_main!!.text = getString(R.string.str_main_start)
                    if (prefs.volumeUpEnabled) {
                        descr_use!!.text = getString(R.string.str_use_logged)
                    } else {
                        descr_use!!.text = getString(R.string.str_use_logged_no_vol)
                    }
                    descr_use!!.setTextColor(
                        AppCompatResources.getColorStateList(
                            context,
                            R.color.mid_grey
                        )
                    )
                } catch (e: Exception) {
                    Log.d(TAG, "MAIN: ACTION_MAIN_LOGGED_IN: resources not available.")
                }

                //TOAST:
                try {
                    Toast.makeText(context, "SUCCESS: DJames is now LOGGED IN to your Spotify!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.d(TAG, "MAIN: ACTION_MAIN_LOGGED_IN: cannot toast.")
                }
            }

            //When Settings VOLUME-UP is changed:
            if (intent.action == ACTION_SETTINGS_VOL_UP) {
                Log.d(TAG, "MAIN: ACTION_SETTINGS_VOL_UP.")
                try {
                    if (prefs.volumeUpEnabled) {
                        descr_use!!.text = context!!.resources.getString(R.string.str_use_logged)
                    } else {
                        descr_use!!.text = context!!.resources.getString(R.string.str_use_logged_no_vol)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "MAIN: ACTION_SETTINGS_VOL_UP: resources not available.")
                }
            }


            //When Overlay is activated:
            if (intent.action == ACTION_OVERLAY_ACTIVATED) {
                Log.d(TAG, "MAIN: ACTION_OVERLAY_ACTIVATED.")
                setOverlayActive()
            }


            //When Overlay is deactivated:
            if (intent.action == ACTION_OVERLAY_DEACTIVATED) {
                Log.d(TAG, "MAIN: ACTION_OVERLAY_DEACTIVATED.")
                setOverlayInactive()
            }

            //Finish activity:
            if (intent.action == ACTION_FINISH_MAIN) {
                Log.d(TAG, "MAIN: ACTION_FINISH_MAIN.")
                finishAndRemoveTask()
            }
        }
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
