package com.ftrono.djeenoforspotify.application

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ftrono.djeenoforspotify.R
import com.ftrono.djeenoforspotify.service.FloatingViewService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.content.res.AppCompatResources


class MainActivity : AppCompatActivity() {

    var notificationID = 1

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        var fab_status = 0

        if (isMyServiceRunning(FloatingViewService::class.java)) {
            fab_status = 1
            fab.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorStop)
            fab.setImageResource(R.drawable.stop_icon)
        }

        fab.setOnClickListener {
            /*
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
            */
            Log.e(TAG, "Notification ID: $notificationID")
            if (fab_status == 0) {
                fab_status = 1
                fab.setImageResource(R.drawable.stop_icon)
                fab.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorStop)
                startService(Intent(this, FloatingViewService::class.java))
            } else {
                fab_status = 0
                fab.setImageResource(R.drawable.add_icon)
                fab.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.colorAccent)
                stopService(Intent(this, FloatingViewService::class.java))
            }
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
            val intent1 = Intent(this@MainActivity, Pem::class.java)
            startActivity(intent1)
            return true
        } else if (id == R.id.action_accessibility) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private val TAG: String = Pem::class.java.getSimpleName()
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
