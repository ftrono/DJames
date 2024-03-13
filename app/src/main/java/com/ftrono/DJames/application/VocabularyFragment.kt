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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ftrono.DJames.R
import com.ftrono.DJames.adapter.VocabularyAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonArray


class VocabularyFragment : Fragment(R.layout.fragment_vocabulary) {

    private val TAG: String = VocabularyFragment::class.java.getSimpleName()

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var textNoData: TextView? = null
    private var refreshList: RecyclerView? = null
    private var listItems = JsonArray()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filter = "artist"

        //Header intro:
        var textHeader = requireActivity().findViewById<TextView>(R.id.voc_intro)
        textHeader.text = "Your hard-to-spell artists"

        //Filters buttons:
        var vocArtists = requireActivity().findViewById<Button>(R.id.voc_artists)
        var vocPlaylists = requireActivity().findViewById<Button>(R.id.voc_playlists)
        var vocContacts = requireActivity().findViewById<Button>(R.id.voc_contacts)

        //Filters listeners:
        vocArtists.setOnClickListener(View.OnClickListener {
            editModeOn = false
            filter = "artist"
            textHeader.text = "Your hard-to-spell artists"
            vocArtists.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.colorAccent)
            vocPlaylists.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.dark_grey)
            vocContacts.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.dark_grey)
            updateRecyclerView()
        })
        vocPlaylists.setOnClickListener(View.OnClickListener {
            editModeOn = false
            filter = "playlist"
            textHeader.text = "Your favourite playlists"
            vocArtists.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.dark_grey)
            vocPlaylists.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.colorAccent)
            vocContacts.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.dark_grey)
            updateRecyclerView()
        })
        vocContacts.setOnClickListener(View.OnClickListener {
            editModeOn = false
            filter = "contact"
            textHeader.text = "Your favourite contacts"
            vocArtists.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.dark_grey)
            vocPlaylists.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.dark_grey)
            vocContacts.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.colorAccent)
            updateRecyclerView()
        })

        //SwipeRefreshLayout:
        swipeRefreshLayout = requireActivity().findViewById<SwipeRefreshLayout>(R.id.vocabulary_refresh)
        swipeRefreshLayout!!.setOnRefreshListener {
            Log.d(TAG, "onRefresh called from SwipeRefreshLayout")
            swipeRefreshLayout!!.setRefreshing(false)
            // setRefreshing(false) when it finishes.
            updateRecyclerView()
        }

        //Views:
        textNoData = requireActivity().findViewById(R.id.vocabulary_no_data)
        refreshList = requireActivity().findViewById(R.id.vocabulary_list)
        refreshList!!.layoutManager = LinearLayoutManager(requireActivity())
        refreshList!!.setHasFixedSize( true )

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_VOC_REFRESH)
        actFilter.addAction(ACTION_VOC_DELETE)
        actFilter.addAction(ACTION_VOC_REQUEST_DETAIL)
        actFilter.addAction(ACTION_VOC_TEST)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        requireActivity().registerReceiver(vocabularyActReceiver, actFilter, AppCompatActivity.RECEIVER_EXPORTED)
        Log.d(TAG, "VocabularyActReceiver started.")

        //Load data:
        updateRecyclerView()

        //FAB:
        var fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        if (!loggedIn) {
            fab.visibility = View.GONE
        }
        fab.setOnClickListener { view ->
            if (!editModeOn) {
                updateRecyclerView(newItem = true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        requireActivity().unregisterReceiver(vocabularyActReceiver)
    }

    fun updateRecyclerView(newItem: Boolean = false) {
        //Load updated data:
        listItems = utils.getVocabularyArray(filter=filter, newItem=newItem)
        if (listItems.size() > 0) {
            //Update visibility:
            refreshList!!.visibility = View.VISIBLE
            textNoData!!.visibility = View.GONE
            //Set updated adapter:
            val mAdapter = VocabularyAdapter(requireActivity(), listItems)
            refreshList!!.adapter = mAdapter
        } else {
            //No data:
            refreshList!!.visibility = View.GONE
            textNoData!!.visibility = View.VISIBLE
            if (filter == "contact") {
                textNoData!!.text =
                    "Your contacts vocabulary is empty!\n\nLet DJames know your favourite contacts'\nnames and phone numbers by\nwriting writing them here.\n\n✏️"
            } else if (filter == "playlist") {
                textNoData!!.text =
                    "Your playlists vocabulary is empty!\n\nLet DJames know your playlists by\nwriting writing their names & links here.\n\n✏️"
            } else {
                textNoData!!.text =
                    "Your ${filter}s vocabulary is empty!\n\nHelp DJames understand your\n lesser known or hard-to-spell ${filter} names\nby writing them here.\n\n✏️"
            }
        }
    }


    //Delete selected item in RecyclerView:
    fun deleteItem(toDelete: String) {
        val alertDialog = MaterialAlertDialogBuilder(requireActivity())
        //Exec:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                var ret = utils.editVocFile(prevText=toDelete!!)
                if (ret == 0) {
                    Toast.makeText(context, "Deleted!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "ERROR: Vocabulary not updated!", Toast.LENGTH_LONG).show()
                }
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
        alertDialog.setTitle("Remove items")
        alertDialog.setMessage("Do you want to remove the item \"${toDelete.split(" %%% ")[0].strip()}\" from this list?")
        alertDialog.show()
    }

    //Delete ALL vocs:
    fun deleteAll() {
        val alertDialog = MaterialAlertDialogBuilder(requireActivity())
        //Exec:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                vocDir!!.deleteRecursively()
                Log.d(TAG, "Deleted ALL vocabulary.")
                Toast.makeText(requireActivity(), "Vocabulary deleted!", Toast.LENGTH_SHORT).show()
                updateRecyclerView()
            }
        })
        //Exit:
        alertDialog.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //No
            }
        })
        alertDialog.setTitle("Delete vocabulary")
        alertDialog.setMessage("Do you want to delete all vocabulary?")
        alertDialog.show()
    }


    //TEST: show Edit Dialog:
    fun showEditDialog() {
        val inflater = LayoutInflater.from(activity)
        val subView: View = inflater.inflate(R.layout.vocabulary_edit_layout, null)
        //Load:
        val alertDialog = MaterialAlertDialogBuilder(requireActivity())
        alertDialog.setTitle("✏️  ${filter.replaceFirstChar { it.uppercase() }}")
        alertDialog.setView(subView)

        //Views:
        var introDetail = subView.findViewById<TextView>(R.id.intro_detail)
        var enterDetail = subView.findViewById<EditText>(R.id.enter_detail)
        var introPhone = subView.findViewById<TextView>(R.id.intro_phone)
        var enterPrefix = subView.findViewById<EditText>(R.id.enter_prefix)
        var enterPhone = subView.findViewById<EditText>(R.id.enter_phone)
        //Set visibility:
        if (filter == "contact") {
            //Contact:
            introDetail.visibility = View.GONE
            enterDetail.visibility = View.GONE
        } else if (filter == "playlist") {
            //Playlist:
            introPhone.visibility = View.GONE
            enterPrefix.visibility = View.GONE
            enterPhone.visibility = View.GONE
        } else {
            //Artist:
            introDetail.visibility = View.GONE
            enterDetail.visibility = View.GONE
            introPhone.visibility = View.GONE
            enterPrefix.visibility = View.GONE
            enterPhone.visibility = View.GONE
        }
        //Exec:
        alertDialog.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
            }
        })
        alertDialog.show()
    }


    //PERSONAL RECEIVER:
    private var vocabularyActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Refresh RecycleView:
            if (intent!!.action == ACTION_VOC_REFRESH) {
                Log.d(TAG, "VOCABULARY: ACTION_VOC_REFRESH.")
                updateRecyclerView()
                editModeOn = false
            }

            //Delete items:
            if (intent.action == ACTION_VOC_DELETE) {
                Log.d(TAG, "VOCABULARY: ACTION_VOC_DELETE.")
                var toDelete = intent.getStringExtra("toDelete")
                deleteItem(toDelete!!)
                editModeOn = false
            }

            //Dialog for add Url:
            if (intent.action == ACTION_VOC_REQUEST_DETAIL) {
                Log.d(TAG, "VOCABULARY: ACTION_VOC_REQUEST_DETAIL.")
                val alertDialog = MaterialAlertDialogBuilder(requireActivity())
                //Exec:
                alertDialog.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                    }
                })
                if (filter == "contact") {
                    //CONTACT:
                    alertDialog.setTitle("Contact Phone Number")
                    alertDialog.setMessage("Please enter a valid phone number for the current Contact!\n\nPlease include the international prefix at the beginning (i.e. \"+39\", \"+44\", ...).")
                } else {
                    //PLAYLIST:
                    alertDialog.setTitle("Playlist URL")
                    alertDialog.setMessage("Please enter a valid URL for the current Playlist!\n\nPlease copy it from Spotify -> your playlist -> Share -> Copy link.")
                }
                alertDialog.show()
            }

            //Dialog for add Url:
            if (intent.action == ACTION_VOC_TEST) {
                Log.d(TAG, "VOCABULARY: ACTION_VOC_TEST.")
                showEditDialog()
            }

        }
    }

}