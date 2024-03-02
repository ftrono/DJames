package com.ftrono.DJames.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ftrono.DJames.R
import com.ftrono.DJames.adapter.HistoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import java.io.File


class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val TAG: String = HistoryFragment::class.java.getSimpleName()

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var textNoData: TextView? = null
    private var refreshList: RecyclerView? = null
    private var logItems = JsonArray()
    private var subtitleView: TextView? = null
    private var subtitle = "0 requests (last 30 days)"
    private var filterButton: MenuItem? = null
    private var hideSuccessful = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subtitleView = requireActivity().findViewById<TextView>(R.id.history_subtitle)
        subtitleView!!.text = subtitle

        //Options button:
        var historyMenu = requireActivity().findViewById<Button>(R.id.history_menu)

        // Setting onClick behavior to the button
        historyMenu.setOnClickListener {
            // Initializing the popup menu and giving the reference as current context
            val popupMenu = PopupMenu(requireActivity(), historyMenu)

            // Inflating popup menu from popup_menu.xml file
            popupMenu.menuInflater.inflate(R.menu.menu_history, popupMenu.menu)
            popupMenu.setForceShowIcon(true)
            filterButton = popupMenu.menu.findItem(R.id.action_filter_successful)
            if (!hideSuccessful) {
                filterButton!!.setTitle("Hide successful")
                filterButton!!.setIcon(R.drawable.filter_icon)
            } else {
                filterButton!!.setTitle("Show successful")
                filterButton!!.setIcon(R.drawable.unfilter_icon)
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                // Toast message on menu item clicked
                onPopupItemSelected(menuItem)
            }
            // Showing the popup menu
            popupMenu.show()
        }

        //SwipeRefreshLayout:
        swipeRefreshLayout = requireActivity().findViewById<SwipeRefreshLayout>(R.id.history_refresh)
        swipeRefreshLayout!!.setOnRefreshListener {
            Log.d(TAG, "onRefresh called from SwipeRefreshLayout")
            swipeRefreshLayout!!.setRefreshing(false)
            // setRefreshing(false) when it finishes.
            updateRecyclerView()
        }

        //Views:
        textNoData = requireActivity().findViewById(R.id.history_no_data)
        refreshList = requireActivity().findViewById(R.id.history_list)
        refreshList!!.layoutManager = LinearLayoutManager(requireActivity())
        refreshList!!.setHasFixedSize( true )

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_LOG_REFRESH)
        actFilter.addAction(ACTION_LOG_DELETE)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            requireActivity().registerReceiver(historyActReceiver, actFilter, AppCompatActivity.RECEIVER_EXPORTED)
        Log.d(TAG, "HistoryActReceiver started.")

        //Load data:
        updateRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
            requireActivity().unregisterReceiver(historyActReceiver)
    }

    fun updateRecyclerView() {
        //Load updated data:
        logItems = utils.getLogArray(hideSuccessful=hideSuccessful)
        subtitle = "${logItems.size()} requests (last 30 days)"
        subtitleView!!.text = subtitle
        if (logItems.size() > 0) {
            //Update visibility:
            refreshList!!.visibility = View.VISIBLE
            textNoData!!.visibility = View.GONE
            //Set updated adapter:
            val mAdapter = HistoryAdapter(requireActivity(), logItems)
            refreshList!!.adapter = mAdapter
        } else {
            //No data:
            refreshList!!.visibility = View.GONE
            textNoData!!.visibility = View.VISIBLE
        }
    }


    fun onPopupItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here
        val id = item.itemId
        //Login / logout:
        if (id == R.id.action_filter_successful) {
            if (!hideSuccessful) {
                hideSuccessful = true
                filterButton!!.setTitle("Show successful")
                filterButton!!.setIcon(R.drawable.unfilter_icon)
            } else {
                hideSuccessful = false
                filterButton!!.setTitle("Hide successful")
                filterButton!!.setIcon(R.drawable.filter_icon)
            }
            updateRecyclerView()
        }
        else if (id == R.id.action_send_logs) {
            val logCons = utils.prepareLogCons(requireActivity(), hideSuccessful=hideSuccessful)
            val uriToFile = FileProvider.getUriForFile(
                requireActivity(),
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
        } else if (id == R.id.action_clean_successful) {
            //Delete successful logs:
            deleteSuccessful()
        } else if (id == R.id.action_clean) {
            //Delete all history:
            deleteAll()
        }
        return true
    }


    //Delete selected items in RecyclerView:
    fun deleteItems(toDeleteStr: String) {
        var toDelete = toDeleteStr.split(", ")
        val alertDialog = MaterialAlertDialogBuilder(requireActivity())
        //Exec:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                for (f in toDelete) {
                    File(logDir, f).delete()
                    Log.d(TAG, "Deleted file: $f")
                }
                Toast.makeText(requireActivity(), "Deleted!", Toast.LENGTH_SHORT).show()
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
        val alertDialog = MaterialAlertDialogBuilder(requireActivity())
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
                Toast.makeText(requireActivity(), "All successful requests deleted!", Toast.LENGTH_SHORT).show()
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
        val alertDialog = MaterialAlertDialogBuilder(requireActivity())
        //Exec:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                logDir!!.deleteRecursively()
                Log.d(TAG, "Deleted ALL logs.")
                Toast.makeText(requireActivity(), "History deleted!", Toast.LENGTH_SHORT).show()
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
        }
    }

}