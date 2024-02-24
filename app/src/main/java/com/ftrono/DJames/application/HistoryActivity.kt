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
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.widget.TextView
import com.ftrono.DJames.R
import com.ftrono.DJames.adapter.HistoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import java.io.File


class HistoryActivity : AppCompatActivity() {

    private val TAG = HistoryActivity::class.java.simpleName

    private val logConsName = "requests_log.json"
    private var textNoData: TextView? = null
    private var historyList: RecyclerView? = null
    private var logItems = JsonArray()
    private var subtitle = "0 requests (last 30 days)"

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
        supportActionBar!!.subtitle = subtitle

        //SwipeRefreshLayout:
        var swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "onRefresh called from SwipeRefreshLayout")
            swipeRefreshLayout.setRefreshing(false)
            // setRefreshing(false) when it finishes.
            updateRecyclerView()
        }

        //Views:
        textNoData = findViewById(R.id.text_no_data)
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
        subtitle = "${logItems.size()} requests (last 30 days)"
        supportActionBar!!.subtitle = subtitle
        if (logItems.size() > 0) {
            //Update visibility:
            historyList!!.visibility = View.VISIBLE
            textNoData!!.visibility = View.GONE
            //Set updated adapter:
            val mAdapter = HistoryAdapter(applicationContext, logItems)
            historyList!!.adapter = mAdapter
        } else {
            //No data:
            historyList!!.visibility = View.GONE
            textNoData!!.visibility = View.VISIBLE
        }
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
            val uriToFile = FileProvider.getUriForFile(
                applicationContext,
                "com.ftrono.DJames.provider",
                logCons
            )
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uriToFile)
                type = "image/jpeg"
            }
            startActivity(Intent.createChooser(sendIntent, null))
            //Toast.makeText(applicationContext, "Preparing logs...", Toast.LENGTH_SHORT).show()
            return true
        } else if (id == R.id.action_clean_successful) {
            //Delete successful logs:
            deleteSuccessful()
            return true
        } else if (id == R.id.action_clean) {
            //Delete all history:
            deleteAll()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }


    //Prepare consolidated Log file:
    private fun prepareLogCons(): File {
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

    //Delete selected items in RecyclerView:
    fun deleteItems(toDeleteStr: String) {
        var toDelete = toDeleteStr.split(", ")
        val alertDialog = MaterialAlertDialogBuilder(this@HistoryActivity)
        //Exec:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                for (f in toDelete) {
                    File(logDir, f).delete()
                    Log.d(TAG, "Deleted file: $f")
                }
                Toast.makeText(applicationContext, "Deleted!", Toast.LENGTH_SHORT).show()
                updateRecyclerView()
            }
        })
        //Exit:
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

    //Delete successful requests only;
    fun deleteSuccessful() {
        val alertDialog = MaterialAlertDialogBuilder(this@HistoryActivity)
        //Exec:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                for (logItem in logItems) {
                    //get result status:
                    var result = 0
                    try {
                        result = logItem.asJsonObject.get("result").asInt
                    } catch (e: Exception) {
                        result = -1
                    }
                    if (result > 0) {
                        var datetime = logItem.asJsonObject.get("datetime").asString
                        var filename = "$datetime.json"
                        File(logDir, filename).delete()
                        Log.d(TAG, "Deleted file: $filename")
                    }
                }
                Toast.makeText(applicationContext, "All successful requests deleted!", Toast.LENGTH_SHORT).show()
                updateRecyclerView()
            }
        })
        //Exit:
        alertDialog.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //No
            }
        })
        alertDialog.setTitle("Delete all successful requests")
        alertDialog.setMessage("Do you want to delete all successful requests from history?")
        alertDialog.show()
    }

    //Delete ALL logs:
    fun deleteAll() {
        val alertDialog = MaterialAlertDialogBuilder(this@HistoryActivity)
        //Exec:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                logDir!!.deleteRecursively()
                Log.d(TAG, "Deleted ALL logs.")
                Toast.makeText(applicationContext, "History deleted!", Toast.LENGTH_SHORT).show()
                updateRecyclerView()
            }
        })
        //Exit:
        alertDialog.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //No
            }
        })
        alertDialog.setTitle("Delete history")
        alertDialog.setMessage("Do you want to delete all history?")
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