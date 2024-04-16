package com.ftrono.DJames.application

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.ftrono.DJames.R
import com.ftrono.DJames.service.FloatingViewService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation


class SettingsActivity : AppCompatActivity() {

    private val TAG = SettingsActivity::class.java.simpleName

    //Settings views:
    private var userNameView: TextView? = null   //eventReceiver(login settings)
    private var userEMailView: TextView? = null   //eventReceiver(login settings)
    private var userIcon: ImageView? = null   //eventReceiver(login settings)
    private var login_mini_button: Button? = null   //eventReceiver(login settings)

    //Views:
    private var user_container: View? = null
    private var divider: View? = null
    private var header_preferences: TextView? = null
    private var text_rec_timeout: TextView? = null
    private var text_mess_timeout: TextView? = null
    private var text_clock_timeout: TextView? = null
    //New values:
    private var newRecTimeout: String = ""
    private var newMessTimeout: String = ""
    private var newClockTimeout: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        acts_active.add(TAG)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Preferences"

        //Views:
        user_container = findViewById<View>(R.id.user_container)
        divider = findViewById<View>(R.id.divider)
        header_preferences = findViewById<TextView>(R.id.header_preferences)
        var version = findViewById<TextView>(R.id.version)
        version.text = "Version $appVersion"

        //User details:
        userNameView = findViewById<TextView>(R.id.user_name)
        userEMailView = findViewById<TextView>(R.id.user_email)
        userIcon = findViewById<ImageView>(R.id.user_icon)
        login_mini_button = findViewById<Button>(R.id.login_mini_button)

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_SETTINGS_LOGGED_IN)
        actFilter.addAction(ACTION_FINISH_SETTINGS)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(settingsActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "SettingsActReceiver started.")

        //Check initial orientation:
        var config = getResources().getConfiguration()
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //HORIZONTAL:
            user_container!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToLeft = R.id.divider
            }
            divider!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToRight = R.id.user_container
            }
            header_preferences!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }

        //Set login details:
        if (prefs.userName != "") {
            login_mini_button!!.text = "LOGOUT"
            userNameView!!.text = prefs.userName
            userEMailView!!.visibility = View.VISIBLE
            userEMailView!!.text = prefs.userEMail
            if (prefs.userImage != "") {
                Picasso.get().load(prefs.userImage)
                    .transform(CropCircleTransformation())
                    .into(userIcon)
            }
        } else {
            setViewLoggedOut()
        }

        login_mini_button!!.setOnClickListener {
            logout()
        }


        //SECTION: OVERLAY:
        //Auto Start-up:
        var checkbox_startup = findViewById<CheckBox>(R.id.checkbox_startup)
        checkbox_startup!!.setChecked(prefs.autoStartup)

        checkbox_startup.setOnClickListener {
            if (checkbox_startup.isChecked) {
                prefs.autoStartup = true
            } else {
                prefs.autoStartup = false
            }
        }

        //RecTimeout:
        text_rec_timeout = findViewById<TextView>(R.id.val_rec_timeout)
        text_rec_timeout!!.text = prefs.recTimeout

        //Overlay Position:
        var c = 0
        var spinner_overlay_pos = findViewById<Spinner>(R.id.spinner_overlay_pos)
        spinner_overlay_pos!!.setSelection(prefs.overlayPosition.toInt())
        spinner_overlay_pos.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapter: AdapterView<*>?, view: View, pos: Int, id: Long) {
                prefs.overlayPosition = pos.toString()
                if (c > 0) {
                    //Restart overlay service:
                    restartOverlay()
                }
                c += 1
            }
            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        })


        //SECTION: OVERLAY:
        //Messages Timeout:
        text_mess_timeout = findViewById<TextView>(R.id.val_mess_timeout)
        text_mess_timeout!!.text = prefs.messageTimeout

        //Messages default language:
        var spinner_mess_language = findViewById<Spinner>(R.id.spinner_mess_language)
        spinner_mess_language!!.setSelection(prefs.messageLanguage.toInt())
        spinner_mess_language.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapter: AdapterView<*>?, view: View, pos: Int, id: Long) {
                prefs.messageLanguage = pos.toString()
            }
            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        })


        //SECTION: CLOCK:
        //Auto Clock:
        var checkbox_auto_clock = findViewById<CheckBox>(R.id.checkbox_auto_clock)
        checkbox_auto_clock!!.setChecked(prefs.autoClock)

        checkbox_auto_clock.setOnClickListener {
            if (checkbox_auto_clock.isChecked) {
                prefs.autoClock = true
            } else {
                prefs.autoClock = false
            }
        }

        //ClockTimeout:
        var text_clock_after = findViewById<TextView>(R.id.descr_clock_after)
        var text_clock_descr = findViewById<TextView>(R.id.descr_clock_timeout)
        var checkbox_timeout = findViewById<CheckBox>(R.id.checkbox_clock_redirect)
        checkbox_timeout!!.setChecked(prefs.clockRedirectEnabled)
        text_clock_timeout = findViewById<TextView>(R.id.val_clock_timeout)
        text_clock_timeout!!.text = prefs.clockTimeout

        //Initial clock timeout view:
        if (!checkbox_timeout.isChecked) {
            text_clock_after!!.visibility = View.GONE
            text_clock_descr!!.visibility = View.GONE
            text_clock_timeout!!.visibility = View.GONE
        }

        checkbox_timeout.setOnClickListener {
            if (checkbox_timeout.isChecked) {
                prefs.clockRedirectEnabled = true
                text_clock_after!!.visibility = View.VISIBLE
                text_clock_descr!!.visibility = View.VISIBLE
                text_clock_timeout!!.visibility = View.VISIBLE
            } else {
                prefs.clockRedirectEnabled = false
                text_clock_after!!.visibility = View.GONE
                text_clock_descr!!.visibility = View.GONE
                text_clock_timeout!!.visibility = View.GONE
            }
        }


        //SECTION: ADVANCED:
        //Volume Up:
        var checkbox_volume_up = findViewById<CheckBox>(R.id.checkbox_volume_receiver)
        checkbox_volume_up!!.setChecked(prefs.volumeUpEnabled)
        checkbox_volume_up.setOnClickListener {
            if (checkbox_volume_up.isChecked) {
                prefs.volumeUpEnabled = true
                //Restart overlay service:
                restartOverlay()
            } else {
                prefs.volumeUpEnabled = false
                //Restart overlay service:
                restartOverlay()
            }
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_SETTINGS_VOL_UP)
                sendBroadcast(intent)
            }
        }

        //Hide bars in Clock screen:
        var checkbox_hide_bars = findViewById<CheckBox>(R.id.checkbox_hide_bars)
        checkbox_hide_bars!!.setChecked(prefs.hideBars)

        checkbox_hide_bars.setOnClickListener {
            if (checkbox_hide_bars.isChecked) {
                prefs.hideBars = true
            } else {
                prefs.hideBars = false
            }
        }

        //Mic type:
        var spinner_mic_type = findViewById<Spinner>(R.id.spinner_mic)
        spinner_mic_type!!.setSelection(prefs.micType.toInt())
        spinner_mic_type.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapter: AdapterView<*>?, view: View, pos: Int, id: Long) {
                prefs.micType = pos.toString()
            }
            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        })

        //Save:
        val saveButton = findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener(View.OnClickListener {
            //Get new values:
            newRecTimeout = text_rec_timeout!!.text.toString()
            newMessTimeout = text_mess_timeout!!.text.toString()
            newClockTimeout = text_clock_timeout!!.text.toString()
            //Save all:
            saveAll(newRecTimeout, newMessTimeout, newClockTimeout)
            Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        unregisterReceiver(settingsActReceiver)
        userNameView = null
        userEMailView = null
        userIcon = null
        login_mini_button = null
        acts_active.remove(TAG)
    }

    override fun onBackPressed() {
        //Get new values:
        newRecTimeout = text_rec_timeout!!.text.toString()
        newMessTimeout = text_mess_timeout!!.text.toString()
        newClockTimeout = text_clock_timeout!!.text.toString()

        //If changes made: show alert dialog:
        if (prefs.recTimeout != newRecTimeout || prefs.messageTimeout != newMessTimeout || prefs.clockTimeout != newClockTimeout) {
            val alertDialog = MaterialAlertDialogBuilder(this)
            //Save all:
            alertDialog.setPositiveButton("Yes", object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    saveAll(newRecTimeout, newMessTimeout, newClockTimeout)
                    Toast.makeText(applicationContext, "Settings saved!", Toast.LENGTH_SHORT).show()
                    finish()
                    val intent1 = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent1)
                }
            })
            //Exit without saving:
            alertDialog.setNegativeButton("No", object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    Toast.makeText(applicationContext, "Settings NOT saved.", Toast.LENGTH_SHORT).show()
                    finish()
                    val intent1 = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent1)
                }
            })
            alertDialog.setTitle("Warning")
            alertDialog.setMessage("Save before exit?")
            alertDialog.show()
        } else {
            finish()
            val intent1 = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent1)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //VERTICAL:
            user_container!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            }
            divider!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            }
            header_preferences!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToTop = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = R.id.user_container
            }
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //HORIZONTAL:
            user_container!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToLeft = R.id.divider
            }
            divider!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToRight = R.id.user_container
            }
            header_preferences!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
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

    private fun saveAll(newRecTimeout: String, newMessTimeout: String, newClockTimeout: String) {
        //RecTimeout:
        if (newRecTimeout.isNotEmpty()) {
            //validate & overwrite:
            prefs.recTimeout = validateTimeout(newVal = newRecTimeout, origVal = prefs.recTimeout, min_val = 5, max_val = 15)
        }
        //MessageTimeout:
        if (newMessTimeout.isNotEmpty()) {
            //validate & overwrite:
            prefs.messageTimeout = validateTimeout(newVal = newMessTimeout, origVal = prefs.messageTimeout, min_val = 10, max_val = 60)
        }
        //ClockTimeout:
        if (newClockTimeout.isNotEmpty()) {
            //validate & overwrite:
            prefs.clockTimeout = validateTimeout(newVal = newClockTimeout, origVal = prefs.clockTimeout, min_val = 5, max_val = 30)
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

    private fun restartOverlay() {
        //Restart overlay service:
        if (isMyServiceRunning(FloatingViewService::class.java)) {
            stopService(Intent(applicationContext, FloatingViewService::class.java))
            if (!isMyServiceRunning(FloatingViewService::class.java)) {
                var intentOS = Intent(applicationContext, FloatingViewService::class.java)
                intentOS.putExtra("faded", false)
                applicationContext.startService(intentOS)
            }
        }
    }

    fun logout() {
        if (!loggedIn) {
            //Login user -> Open WebView:
            val intent1 = Intent(this, WebAuth::class.java)
            startActivity(intent1)
        } else {
            val alertDialog = MaterialAlertDialogBuilder(this)
            //Save all:
            alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    //LOG OUT:
                    //Delete tokens & user details:
                    loggedIn = false
                    prefs.spotifyToken = ""
                    prefs.refreshToken = ""
                    prefs.spotUserId = ""
                    prefs.userName = ""
                    prefs.userEMail = ""
                    prefs.userImage = ""
                    prefs.nlpUserId = ""
                    //Stop overlay service:
                    if (isMyServiceRunning(FloatingViewService::class.java)) {
                        stopService(Intent(applicationContext, FloatingViewService::class.java))
                    }
                    //utils.deleteUserCache()
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
            alertDialog.setMessage("You will need to login again to Spotify to use DJames and you'll lose your saved vocabulary & history.\n\nDo you want to log out?")
            alertDialog.show()
        }
    }

    fun setViewLoggedOut() {
        //User NOT logged in:
        login_mini_button!!.text = "LOGIN"
        userNameView!!.text = "Not logged in"
        userEMailView!!.visibility = View.GONE
        Picasso.get().load(R.drawable.user_icon)
            .transform(CropCircleTransformation())
            .into(userIcon)
    }


    //PERSONAL RECEIVER:
    private var settingsActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //When logged in:
            if (intent!!.action == ACTION_SETTINGS_LOGGED_IN) {
                Log.d(TAG, "SETTINGS: ACTION_SETTINGS_LOGGED_IN.")

                loggedIn = true
                //SETTINGS ACTIVITY:
                try {
                    //Set Logged-In UI:
                    login_mini_button!!.text = "LOGOUT"
                    userNameView!!.text = prefs.userName
                    userEMailView!!.visibility = View.VISIBLE
                    userEMailView!!.text = prefs.userEMail
                    if (prefs.userImage != "") {
                        Picasso.get().load(prefs.userImage)
                            .transform(CropCircleTransformation())
                            .into(userIcon)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "SETTINGS: ACTION_SETTINGS_LOGGED_IN: resources not available.")
                }
            }

            //Finish activity:
            if (intent.action == ACTION_FINISH_SETTINGS) {
                Log.d(TAG, "SETTINGS: ACTION_FINISH_SETTINGS.")
                finishAndRemoveTask()
            }
        }
    }

}