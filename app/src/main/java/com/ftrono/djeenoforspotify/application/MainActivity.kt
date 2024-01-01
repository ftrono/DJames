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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import com.ftrono.djeenoforspotify.R
import com.ftrono.djeenoforspotify.service.FloatingViewService
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {

    var notificationID = 1
    private val checkMaps: CheckBox? = null

    //fun checkRunning(fab: FloatingActionButton) {}
    val checkThread = Thread {
        try {
            while (true) {
                synchronized(this) {
                    Thread.sleep(2000)
                    if (!isMyServiceRunning(FloatingViewService::class.java)) {
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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab = findViewById<FloatingActionButton>(R.id.fab) as FloatingActionButton

        val checkMaps = findViewById<CheckBox>(R.id.check_maps)
        checkMaps.setOnClickListener(View.OnClickListener {
            val yes1: Boolean
            /*
            if (checkMaps.isChecked()) {
                info = getString(R.string.you_added) + ingr1
                Toast.makeText(applicationContext, info, Toast.LENGTH_SHORT).show()
                yes1 = true
            } else {
                info = getString(R.string.you_removed) + ingr1
                Toast.makeText(applicationContext, info, Toast.LENGTH_SHORT).show()
                yes1 = false
            }
            val sharedPrefs = getSharedPreferences(SETTINGS_STORAGE, MODE_PRIVATE)
            sharedPrefs.edit().putString(INGR1V_KEY, ingr1).apply()
            sharedPrefs.edit().putBoolean(INGR1YN_KEY, yes1).apply()

             */
        })

        //if (isMyServiceRunning(FloatingViewService::class.java)) {}
        // Start overlay service automatically
        var fab_status = setOverlayActive(fab, true) as Boolean

        fab.setOnClickListener {
            /*
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
            */
            if (!fab_status) {
                fab_status = setOverlayActive(fab, true)
            } else {
                fab_status = setOverlayInactive(fab, true)
            }
        }
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab = findViewById<FloatingActionButton>(R.id.fab) as FloatingActionButton
        var fab_status = false as Boolean

        if (isMyServiceRunning(FloatingViewService::class.java)) {
            fab_status = setOverlayActive(fab, false)
        } else {
            fab_status = setOverlayInactive(fab, false)
        }

        fab.setOnClickListener {
            /*
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
            */
            //Log.e(TAG, "Notification ID: $notificationID")
            if (!fab_status) {
                fab_status = setOverlayActive(fab, true)
            } else {
                fab_status = setOverlayInactive(fab, true)
            }
        }
        if (!checkThread.isAlive()){
            checkThread.start();
        }
    }

    companion object {
        private val TAG: String = MainActivity::class.java.getSimpleName()
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
        }
        return true
    }

    fun setOverlayInactive(fab: FloatingActionButton, exec: Boolean): Boolean {
        fab.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorAccent)
        fab.setImageResource(R.drawable.add_icon)
        if (exec) {
            stopService(Intent(this, FloatingViewService::class.java))
        }
        return false
    }
}
