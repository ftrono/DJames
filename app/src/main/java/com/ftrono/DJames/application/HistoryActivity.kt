package com.ftrono.DJames.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ftrono.DJames.R
import com.ftrono.DJames.adapter.HistoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import java.io.File


class HistoryActivity : AppCompatActivity() {

    private val TAG = HistoryActivity::class.java.simpleName

    private val logConsName = "requests_log.json"
    private var historyList: RecyclerView? = null
    private var logItems = JsonArray()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        acts_active.add(TAG)

        //Load Main views:
        var toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Load Main views:
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "History"

        //SwipeRefreshLayout:
        var swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "onRefresh called from SwipeRefreshLayout")
            swipeRefreshLayout.setRefreshing(false)
            // setRefreshing(false) when it finishes.
            updateRecyclerView()
        }

        //Views:
        historyList = findViewById(R.id.history_list)
        historyList!!.layoutManager = LinearLayoutManager(this)
        historyList!!.setHasFixedSize( true )


        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_LOG_REFRESH)
        actFilter.addAction(ACTION_LOG_DELETE)
        actFilter.addAction(ACTION_FINISH_HISTORY)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(historyActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "HistoryActReceiver started.")

        //Load data:
        updateRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        unregisterReceiver(historyActReceiver)
        acts_active.remove(TAG)
    }

    fun updateRecyclerView() {
        //Load updated data:
        logItems = utils.getLogArray()
        //Set updated adapter:
        val mAdapter = HistoryAdapter(applicationContext, this, logItems)
        historyList!!.adapter = mAdapter
        //mAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_history, menu)

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here
        val id = item.itemId
        //Login / logout:
        if (id == R.id.action_send_logs) {
            val logCons = prepareLogCons()
            val uriToFile = FileProvider.getUriForFile(applicationContext, "com.ftrono.DJames.provider", logCons)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uriToFile)
                type="image/jpeg"
            }
            startActivity(Intent.createChooser(sendIntent, null))
            //Toast.makeText(applicationContext, "Preparing logs...", Toast.LENGTH_SHORT).show()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }


    //Prepare consolidated Log file:
    fun prepareLogCons(): File {
        val logArray = utils.getLogArray()
        var consFile = File(cacheDir, logConsName)
        if (consFile.exists()) {
            consFile.delete()
        }
        consFile.createNewFile()
        consFile.writeText(logArray.toString())
        return consFile
    }

    override fun onBackPressed() {
        //Finish and launch Main activity:
        finish()
        val intent1 = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent1)
    }

    //Delete items in RecyclerView:
    fun deleteItems(toDeleteStr: String) {
        var toDelete = toDeleteStr.split(", ")
        //Refresh
        val alertDialog = MaterialAlertDialogBuilder(this@HistoryActivity)
        //Save all:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                updateRecyclerView()
            }
        })
        //Exit without saving:
        alertDialog.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //No
                updateRecyclerView()
            }
        })
        if (toDelete.size == 1) {
            alertDialog.setTitle("Delete item")
            alertDialog.setMessage("Do you want to delete this log item?\n\n${toDelete[0].replace(".json", "")}")
        } else {
            alertDialog.setTitle("Delete items")
            alertDialog.setMessage("Do you want to delete ${toDelete.size} log items?")
        }
        alertDialog.show()
    }


    //PERSONAL RECEIVER:
    private var historyActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Refresh RecycleView:
            if (intent!!.action == ACTION_LOG_REFRESH) {
                Log.d(TAG, "HISTORY: ACTION_LOG_REFRESH.")
                updateRecyclerView()
            }

            //Delete items:
            if (intent.action == ACTION_LOG_DELETE) {
                Log.d(TAG, "HISTORY: ACTION_LOG_DELETE.")
                var toDeleteStr = intent.getStringExtra("toDeleteStr")
                deleteItems(toDeleteStr!!)
            }

            //Finish activity:
            if (intent.action == ACTION_FINISH_HISTORY) {
                Log.d(TAG, "HISTORY: ACTION_FINISH_SETTINGS.")
                finishAndRemoveTask()
            }
        }
    }

}