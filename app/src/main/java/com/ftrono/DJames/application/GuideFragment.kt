package com.ftrono.DJames.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ftrono.DJames.R
import com.ftrono.DJames.adapter.GuideAdapter
import com.ftrono.DJames.utilities.Utilities
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject


class GuideFragment : Fragment(R.layout.fragment_guide) {

    private val TAG: String = GuideFragment::class.java.getSimpleName()
    private var utils = Utilities()

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var refreshList: RecyclerView? = null
    private var guideItems = JsonArray()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Filters buttons:
        var guideButton = requireActivity().findViewById<Button>(R.id.guide_button)

        //Filters listeners:
        guideButton.setOnClickListener(View.OnClickListener {
            val intent1 = Intent(requireActivity(), SettingsActivity::class.java)
            startActivity(intent1)
        })

        //SwipeRefreshLayout:
        swipeRefreshLayout = requireActivity().findViewById<SwipeRefreshLayout>(R.id.guide_refresh)
        swipeRefreshLayout!!.setOnRefreshListener {
            Log.d(TAG, "onRefresh called from SwipeRefreshLayout")
            swipeRefreshLayout!!.setRefreshing(false)
            // setRefreshing(false) when it finishes.
            updateRecyclerView()
        }

        //Views
        refreshList = requireActivity().findViewById(R.id.guide_list)
        refreshList!!.layoutManager = LinearLayoutManager(requireActivity())
        refreshList!!.setHasFixedSize( true )

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_GUIDE_POPUP)
        actFilter.addAction(ACTION_GUIDE_REFRESH)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        requireActivity().registerReceiver(guideActReceiver, actFilter, AppCompatActivity.RECEIVER_EXPORTED)
        Log.d(TAG, "GuideActReceiver started.")

        //Load data:
        updateRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        requireActivity().unregisterReceiver(guideActReceiver)
    }

    fun updateRecyclerView() {
        //Load data:
        guideItems = utils.getGuideArray(requireActivity())
        //Set updated adapter:
        val mAdapter = GuideAdapter(requireActivity(), guideItems)
        refreshList!!.adapter = mAdapter
    }


    //Show Guide Popup:
    fun showGuidePopup(item: JsonObject) {
        val inflater = LayoutInflater.from(requireActivity())
        val subView: View = inflater.inflate(R.layout.guide_popup, null)
        //Load:
        val alertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
        alertDialogBuilder.setTitle("🔍  Guide")
        alertDialogBuilder.setView(subView)

        //Views:
        var popup_text_1 = subView.findViewById<TextView>(R.id.guide_popup_text_1)
        var popup_text_2 = subView.findViewById<TextView>(R.id.guide_popup_text_2)
        var popup_descr = subView.findViewById<TextView>(R.id.guide_popup_descr)
        var popup_alts = subView.findViewById<TextView>(R.id.guide_popup_alts)

        //Populate:
        //Main:
        popup_text_1.text = item.get("sentence_1").asString
        popup_descr.text = item.get("description").asString
        //Text_2:
        var text_2 = item.get("sentence_2").asString
        if (text_2 == "") {
            popup_text_2.visibility = View.GONE
        } else {
            popup_text_2.visibility = View.VISIBLE
            popup_text_2.text = "\n$text_2"
        }
        //Alts:
        var cur = ""
        var alts = ""
        for (curRaw in item.get("alternatives").asJsonArray) {
            cur = curRaw.asString
            if (alts == "") {
                alts = "$cur\n"
            } else {
                alts = "$alts\n$cur\n"
            }

        }
        popup_alts.text = alts

        alertDialogBuilder.setPositiveButton("Ok", null)
        val alertDialog = alertDialogBuilder.create()

        alertDialog.setOnShowListener(DialogInterface.OnShowListener { dialog ->
            val positiveButton: Button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                //CLOSE THE DIALOG:
                dialog.dismiss()
            }
        })
        alertDialog.show()
    }


    //PERSONAL RECEIVER:
    private var guideActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Show guide popup:
            if (intent!!.action == ACTION_GUIDE_POPUP) {
                Log.d(TAG, "GUIDE: ACTION_GUIDE_POPUP.")
                Log.d(TAG, "$intent")
                try {
                    var index = intent.getStringExtra("index")
                    Log.d(TAG, index!!)
                    var item = guideItems[index.toInt()].asJsonObject
                    Log.d(TAG, item.toString())
                    showGuidePopup(item=item)
                } catch (e: Exception) {
                    Log.d(TAG, "GUIDE: ACTION_GUIDE_POPUP: ERROR: ", e)
                }

            }

            //Refresh guide:
            if (intent.action == ACTION_GUIDE_REFRESH) {
                Log.d(TAG, "GUIDE: ACTION_GUIDE_REFRESH.")
                updateRecyclerView()
            }

        }
    }

}