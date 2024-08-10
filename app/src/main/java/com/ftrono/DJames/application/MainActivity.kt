package com.ftrono.DJames.application

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.ftrono.DJames.R
import com.ftrono.DJames.services.FloatingViewService
import com.ftrono.DJames.utilities.Utilities
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigationrail.NavigationRailView
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.getSimpleName()
    private var utils = Utilities()

    //Views:
    private var toolbar: Toolbar? = null
    private var mainActionBar: ActionBar? = null    //eventReceiver (login)
    private var loginButton: MenuItem? = null    //eventReceiver (login)
    private var navBar: BottomNavigationView? = null
    private var navRail: NavigationRailView? = null
    private var mainFrame: FragmentContainerView? = null
    private var curFragment: Fragment? = null
    private var curNavItemId = R.id.nav_home
    private var curInd = 0
    private var prevMdFragment: Fragment = MyDJamesFragment()


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        acts_active.add(TAG)

        //Load Main views:
        toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        mainActionBar = supportActionBar
        //toolbar!!.overflowIcon = AppCompatResources.getDrawable(this, R.drawable.user_icon)

        //Screen density:
        density = resources.displayMetrics.density

        //Check Login status:
        if (prefs.spotifyToken == "") {
            spotifyLoggedIn = false
        } else {
            spotifyLoggedIn = true
            supportActionBar!!.subtitle = "for ${prefs.spotUserName}"
        }

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_MAIN_LOGGED_IN)
        actFilter.addAction(ACTION_FINISH_MAIN)
        actFilter.addAction(ACTION_SWITCH_FRAGMENT)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(mainActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "MainActReceiver started.")

        //Navigation bars:
        navBar = findViewById<BottomNavigationView>(R.id.navbar)
        navBar!!.setOnItemSelectedListener {
            selectNavItem(it)
            true
        }
        navRail = findViewById<NavigationRailView>(R.id.navrail)
        navRail!!.setOnItemSelectedListener {
            selectNavItem(it)
            true
        }

        //Check initial orientation:
        mainFrame = findViewById(R.id.main_frame)
        var config = getResources().getConfiguration()
        setOrientationLayout(config)

        //Load Home fragment:
        curFragment = HomeFragment()
        curNavItemId = R.id.nav_home
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_frame, curFragment!!)
            .commit()

        //CLEANING:
        if (!main_initialized) {
            //Init log directory:
            logDir = File(cacheDir, "log_requests")
            if (!logDir!!.exists()) {
                logDir!!.mkdir()
            }
            //Init vocabulary directory:
            vocDir = File(cacheDir, "vocabulary")
            if (!vocDir!!.exists()) {
                vocDir!!.mkdir()
            }
            //delete older logs:
            utils.deleteOldLogs()
            //delete older recFiles in cache:
            if (!overlay_active) {
                try {
                    File(cacheDir, "$recFileName.mp3").delete()
                    Log.d(TAG, "Recording mp3 deleted.")
                } catch (e: Exception) {
                    Log.d(TAG, "Recording mp3 not deleted.")
                }
                try {
                    File(cacheDir, "$recFileName.flac").delete()
                    Log.d(TAG, "Recording flac deleted.")
                } catch (e: Exception) {
                    Log.d(TAG, "Recording flac not deleted.")
                }
            }
        }

        //NLP USER ID:
        if (prefs.nlpUserId == "") {
            prefs.nlpUserId = utils.generateRandomString(12)
        }

        //AUTO START-UP:
        if (prefs.autoStartup && !main_initialized && !isMyServiceRunning(FloatingViewService::class.java)) {
            try {
                var intentOS = Intent(applicationContext, FloatingViewService::class.java)
                intentOS.putExtra("faded", false)
                applicationContext.startService(intentOS)
            } catch (e: Exception) {
                Log.d(TAG, "Cannot auto-start Overlay Service. EXCEPTION: $e")
            }

        }

        //Done:
        main_initialized = true
    }


    override fun onResume() {
        super.onResume()
        //ON RESUME() ONLY:
        //Check Login status:
        if (!spotifyLoggedIn) {
            setViewLoggedOut()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        unregisterReceiver(mainActReceiver)
        //empty views:
        mainActionBar = null
        loginButton = null
        acts_active.remove(TAG)
    }

    fun setOrientationLayout(config: Configuration){
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //HORIZONTAL:
            navBar!!.visibility = View.GONE
            navRail!!.visibility = View.VISIBLE
            //Show NavRail:
            navRail!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                if (prefs.overlayPosition == "1") {
                    //Overlay to Right -> NavRail to Left:
                    rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                    leftToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                    rightToLeft = R.id.main_frame
                } else {
                    //Overlay to Left -> NavRail to Right:
                    leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                    rightToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                    rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                    leftToRight = R.id.main_frame
                }
            }
            //Fix MainFrame:
            mainFrame!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                if (prefs.overlayPosition == "1") {
                    //Overlay to Right -> NavRail to Left:
                    leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                    rightToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                    leftToRight = R.id.navrail
                    rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                } else {
                    //Overlay to Left -> NavRail to Right:
                    rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                    leftToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                    rightToLeft = R.id.navrail
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                }
            }
            var item = navRail!!.menu.findItem(curNavItemId)
            item.isChecked = true
        } else {
            //VERTICAL:
            navRail!!.visibility = View.GONE
            navBar!!.visibility = View.VISIBLE
            var item = navBar!!.menu.findItem(curNavItemId)
            item.isChecked = true
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setOrientationLayout(newConfig)
    }

    private fun selectNavItem(item: MenuItem) {
        Log.d(TAG, "${item.itemId}")
        if (item.itemId != curNavItemId) {
            curFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_help -> GuideFragment()
                R.id.nav_myDJames -> MyDJamesFragment()
                R.id.nav_history -> HistoryFragment()
                else -> HomeFragment()
            }
            var newInd = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_help -> 1
                R.id.nav_myDJames -> 2
                R.id.nav_history -> 3
                else -> 0
            }
            var transaction = supportFragmentManager.beginTransaction()
            if (newInd > curInd) {
                transaction.setCustomAnimations(
                    R.anim.slide_in_from_right, // enter
                    R.anim.fade_out, // exit
                    R.anim.fade_in, // popEnter
                    R.anim.slide_out_from_right // popExit
                )
            } else {
                transaction.setCustomAnimations(
                    R.anim.slide_in_from_left, // enter
                    R.anim.fade_out, // exit
                    R.anim.fade_in, // popEnter
                    R.anim.slide_out_from_left // popExit
                )
            }
            //transaction.setReorderingAllowed(true)
            //transaction.addToBackStack(null)
            transaction.replace(R.id.main_frame, curFragment!!)
            transaction.commit()

            item.isChecked = true
            curNavItemId = item.itemId
            curInd = newInd
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        @SuppressLint("RestrictedApi")
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        loginButton = menu.findItem(R.id.action_login)
        //Spotify login:
        if (prefs.spotifyToken != "") {
            loginButton!!.setTitle("Logout from Spotify")
        } else {
            loginButton!!.setTitle("Login to Spotify")
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here
        val id = item.itemId

        if (id == R.id.action_login) {
            //Login / logout:
            if (!spotifyLoggedIn) {
                //Login user -> Open WebView:
                val intent1 = Intent(this@MainActivity, WebAuth::class.java)
                startActivity(intent1)
            } else {
                logout(applicationContext)
            }
            return true

        } else if (id == R.id.action_settings) {
            //Settings:
            val intent1 = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent1)
            return true

        } else if (id == R.id.action_permissions) {
            //Set app preferences:
            val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent1.setData(uri)
            startActivity(intent1)
            return true

        } else if (id == R.id.action_tts) {
            //Set app preferences:
            val intent1 = Intent("com.android.settings.TTS_SETTINGS")
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

    //Logout user:
    fun logout(context: Context) {
        val alertDialog = MaterialAlertDialogBuilder(this)
        //Save all:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //LOG OUT:
                utils.logoutCommons(context)
                setViewLoggedOut()
                //Send broadcasts:
                Intent().also { intent ->
                    intent.setAction(ACTION_HOME_LOGGED_OUT)
                    sendBroadcast(intent)
                }
            }
        })
        //Exit without saving:
        alertDialog.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                spotifyLoggedIn = true
            }
        })
        alertDialog.setTitle("Log out")
        alertDialog.setMessage("You will need to login again to Spotify to use DJames and you'll lose your saved vocabulary & history.\n\nDo you want to log out?")
        alertDialog.show()
    }

    fun setViewLoggedOut(): Boolean {
        //Set NOT Logged-In UI:
        if (loginButton != null) {
            loginButton!!.setTitle("Login to Spotify")
        }
        supportActionBar!!.subtitle = ""
        return false
    }


    //PERSONAL RECEIVER:
    private var mainActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //When logged in:
            if (intent!!.action == ACTION_MAIN_LOGGED_IN) {
                Log.d(TAG, "MAIN: ACTION_MAIN_LOGGED_IN.")
                spotifyLoggedIn = true
                try {
                    //Set Logged-In UI:
                    if (loginButton != null) {
                        loginButton!!.setTitle("Logout from Spotify")
                    }
                    mainActionBar!!.subtitle = "for ${prefs.spotUserName}"
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

            //Finish activity:
            if (intent.action == ACTION_FINISH_MAIN) {
                Log.d(TAG, "MAIN: ACTION_FINISH_MAIN.")
                finishAndRemoveTask()
            }

            //Switch fragment:
            if (intent.action == ACTION_SWITCH_FRAGMENT) {
                val mdId = intent.getIntExtra("mdId", 0)
                var mdFragment = when (mdId) {
                    0 -> MyDJamesFragment()
                    1 -> VocabularyFragment()
                    //2 -> ForYouFragment()
                    else -> MyDJamesFragment()
                }

                if (mdId > 0) {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.main_frame, mdFragment)
                        .addToBackStack("tag")
                        .commit()

                } else {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.main_frame, mdFragment)
                        .addToBackStack(null)
                        .commit()
                }

                prevMdFragment = mdFragment

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
