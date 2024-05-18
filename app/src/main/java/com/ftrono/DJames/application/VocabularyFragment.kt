package com.ftrono.DJames.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.webkit.URLUtil
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ftrono.DJames.R
import com.ftrono.DJames.adapter.VocabularyAdapter
import com.ftrono.DJames.utilities.Utilities
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import java.io.File


class VocabularyFragment : Fragment(R.layout.fragment_vocabulary) {

    private val TAG: String = VocabularyFragment::class.java.getSimpleName()
    private var utils = Utilities()

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var textNoData: TextView? = null
    private var refreshList: RecyclerView? = null
    private var vocItems = JsonObject()
    private var listItems = listOf<String>()

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
        actFilter.addAction(ACTION_VOC_DELETE)
        actFilter.addAction(ACTION_VOC_EDIT)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        requireActivity().registerReceiver(vocabularyActReceiver, actFilter, AppCompatActivity.RECEIVER_EXPORTED)
        Log.d(TAG, "VocabularyActReceiver started.")

        //Load data:
        updateRecyclerView()

        //Delete vocabulary button:
        var deleteVocButton = requireActivity().findViewById<ImageView>(R.id.voc_delete_all)
        deleteVocButton.setOnClickListener { view ->
            deleteVocabulary()
        }

        //FAB:
        var fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        if (!loggedIn) {
            fab.visibility = View.GONE
        }
        fab.setOnClickListener { view ->
            showEditDialog("")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        requireActivity().unregisterReceiver(vocabularyActReceiver)
    }

    fun updateRecyclerView() {
        //Load updated data:
        vocItems = utils.getVocabulary(filter=filter)
        listItems = vocItems.keySet().toList()
        if (listItems.isNotEmpty()) {
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
                    "Your contacts vocabulary is empty!\n\nLet DJames know your favourite contacts'\nnames and phone numbers by\nwriting them here.\n\n✏️"
            } else if (filter == "playlist") {
                textNoData!!.text =
                    "Your playlists vocabulary is empty!\n\nLet DJames know your playlists by\nwriting their names & links here.\n\n✏️"
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
                var ret = utils.editVocFile(prevText=toDelete)
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
    fun deleteVocabulary() {
        val alertDialog = MaterialAlertDialogBuilder(requireActivity())
        //Exec:
        alertDialog.setPositiveButton("Yes", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //Yes
                File(vocDir, "voc_${filter}s.json").delete()
                Log.d(TAG, "Deleted ${filter}s vocabulary.")
                Toast.makeText(requireActivity(), "${filter.replaceFirstChar { it.uppercase() }}s vocabulary deleted!", Toast.LENGTH_SHORT).show()
                updateRecyclerView()
            }
        })
        //Exit:
        alertDialog.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                //No
            }
        })
        alertDialog.setTitle("Delete ${filter}s vocabulary")
        alertDialog.setMessage("Do you want to delete your ${filter}s vocabulary?")
        alertDialog.show()
    }

    //Request detail:
    fun requestDetail() {
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


    //Edit item:
    private fun editItem(prevText: String, newText: String, newDetails: JsonObject): Int {
        if (newText != "") {
            if (filter == "playlist") {
                var newURL = newDetails.get("playlist_URL").asString
                var urlTest = URLUtil.isValidUrl(newURL) && Patterns.WEB_URL.matcher(newURL).matches()
                if (!urlTest || !newURL.contains(playlistUrlIntro)) {
                    //Request enter valid URL:
                    requestDetail()
                    return -1
                } else {
                    newURL = newURL.split("?")[0]
                    newDetails.addProperty("playlist_URL", newURL)
                }
            } else if (filter == "contact") {
                var newPrefix = newDetails.get("prefix").asString
                var newPhone = newDetails.get("phone").asString
                var phoneTest = PhoneNumberUtils.isGlobalPhoneNumber(newPhone)
                //Request valid detail (i.e. no URL, no phone number, no international prefix in phone number):
                if (!phoneTest || (!newPrefix.contains("+") && newPrefix.length != 3) || (newPhone.length != 10 && newPhone.length != 11)) {
                    //(Phone numbers length is 10 digits (Italy) or 11 digits (UK), + international prefix (3 digits))
                    requestDetail()
                    return -1
                }
            }
            var prevDetails = JsonObject()
            if (vocItems.has(prevText)) {
                prevDetails = vocItems.get(prevText).asJsonObject
            }
            //Save to file:
            if (newText != prevText || newDetails != prevDetails) {
                var ret = utils.editVocFile(prevText=prevText, newText=newText, newDetails=newDetails)
                if (ret == 0) {
                    Toast.makeText(context, "Saved!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "ERROR: Vocabulary not updated!", Toast.LENGTH_LONG).show()
                }
            }
            updateRecyclerView()
        }
        return 0
    }


    //Show Edit Dialog:
    fun showEditDialog(prevText: String) {
        val inflater = LayoutInflater.from(activity)
        val subView: View = inflater.inflate(R.layout.vocabulary_edit_layout, null)
        //Load:
        val alertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
        alertDialogBuilder.setTitle("✏️  ${filter.replaceFirstChar { it.uppercase() }}")
        alertDialogBuilder.setView(subView)

        //Views:
        var enterName = subView.findViewById<TextView>(R.id.enter_name)
        var introDetail = subView.findViewById<TextView>(R.id.intro_detail)
        var enterDetail = subView.findViewById<TextView>(R.id.enter_detail)
        var introPhone = subView.findViewById<TextView>(R.id.intro_phone)
        var enterPrefix = subView.findViewById<TextView>(R.id.enter_prefix)
        var enterPhone = subView.findViewById<TextView>(R.id.enter_phone)

        //Load data:
        var prevDetails = JsonObject()
        if (prevText != "") {
            prevDetails = vocItems.get(prevText).asJsonObject
        }

        //PRESET:
        if (filter == "contact") {
            //CONTACT:
            //Visibility:
            introDetail.visibility = View.GONE
            enterDetail.visibility = View.GONE
            //Text:
            enterName.text = prevText
            if (prevDetails.isEmpty) {
                enterPrefix.text = "+39"
                enterPhone.text = ""
            } else {
                enterPrefix.text = prevDetails.get("prefix").asString
                enterPhone.text = prevDetails.get("phone").asString
            }

        } else if (filter == "playlist") {
            //PLAYLIST:
            //Visibility:
            introPhone.visibility = View.GONE
            enterPrefix.visibility = View.GONE
            enterPhone.visibility = View.GONE
            //Text:
            enterName.text = prevText
            if (prevDetails.isEmpty) {
                enterDetail.text = ""
            } else {
                enterDetail.text = prevDetails.get("playlist_URL").asString
            }
        } else {
            //ARTIST:
            //Visibility:
            introDetail.visibility = View.GONE
            enterDetail.visibility = View.GONE
            introPhone.visibility = View.GONE
            enterPrefix.visibility = View.GONE
            enterPhone.visibility = View.GONE
            //Text:
            enterName.text = prevText
        }
        alertDialogBuilder.setPositiveButton("Ok", null)
        alertDialogBuilder.setNegativeButton("Cancel", null)
        val alertDialog = alertDialogBuilder.create()

        alertDialog.setOnShowListener(OnShowListener { dialog ->
            val positiveButton: Button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                var ret = 0
                var newText = enterName.text.toString().lowercase().strip()
                var newDetails = JsonObject()
                //Get new info:
                if (filter == "playlist") {
                    newDetails.addProperty("playlist_URL", enterDetail.text.toString().replace(" ", ""))
                } else if (filter == "contact") {
                    newDetails.addProperty("prefix", enterPrefix.text.toString().replace(" ", ""))
                    newDetails.addProperty("phone", enterPhone.text.toString().replace(" ", ""))
                }
                //Edit:
                ret = editItem(prevText=prevText, newText=newText, newDetails=newDetails)
                if (ret == 0) {
                    //CLOSE THE DIALOG:
                    dialog.dismiss()
                }
            }
            val negativeButton: Button = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                //CLOSE THE DIALOG:
                dialog.dismiss()
            }
        })
        alertDialog.setCancelable(false)
        alertDialog.show()
    }


    //PERSONAL RECEIVER:
    private var vocabularyActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Delete items:
            if (intent!!.action == ACTION_VOC_DELETE) {
                Log.d(TAG, "VOCABULARY: ACTION_VOC_DELETE.")
                var toDelete = intent.getStringExtra("toDelete")
                deleteItem(toDelete!!)
            }

            //Dialog for voc Edit:
            if (intent.action == ACTION_VOC_EDIT) {
                Log.d(TAG, "VOCABULARY: ACTION_VOC_EDIT.")
                val prevText = intent.getStringExtra("prevText")
                showEditDialog(prevText!!)
            }

        }
    }

}