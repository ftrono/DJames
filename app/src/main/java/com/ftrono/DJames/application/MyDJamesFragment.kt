package com.ftrono.DJames.application

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ftrono.DJames.R
import com.ftrono.DJames.utilities.Utilities
import com.google.android.material.card.MaterialCardView


class MyDJamesFragment : Fragment(R.layout.fragment_mydjames) {

    private val TAG: String = MyDJamesFragment::class.java.getSimpleName()
    private var utils = Utilities()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filter = "artist"

        //VOCABULARY:
        //Sizes:
        val myArtistsSub = requireActivity().findViewById<TextView>(R.id.md_artists_sub)
        myArtistsSub.text = updateSub("artist", "item")

        val myPlaylistsSub = requireActivity().findViewById<TextView>(R.id.md_playlists_sub)
        myPlaylistsSub.text = updateSub("playlist", "item")

        val myContactsSub = requireActivity().findViewById<TextView>(R.id.md_contacts_sub)
        myContactsSub.text = updateSub("contact", "item")

        //Cards:
        val myArtists = requireActivity().findViewById<MaterialCardView>(R.id.md_card_artists)
        myArtists.setOnClickListener(View.OnClickListener {
            filter = "artist"
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_SWITCH_FRAGMENT)
                intent.putExtra("mdId", 1)
                requireActivity().sendBroadcast(intent)
            }
        })

        val myPlaylists = requireActivity().findViewById<MaterialCardView>(R.id.md_card_playlists)
        myPlaylists.setOnClickListener(View.OnClickListener {
            filter = "playlist"
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_SWITCH_FRAGMENT)
                intent.putExtra("mdId", 1)
                requireActivity().sendBroadcast(intent)
            }
        })

        val myContacts = requireActivity().findViewById<MaterialCardView>(R.id.md_card_contacts)
        myContacts.setOnClickListener(View.OnClickListener {
            filter = "contact"
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_SWITCH_FRAGMENT)
                intent.putExtra("mdId", 1)
                requireActivity().sendBroadcast(intent)
            }
        })

    }


    fun updateSub(filter: String, singularWord: String): String {
        val size = utils.getVocSize(filter)
        if (size == 1) {
            return "1 ${singularWord}"
        } else {
            return "$size ${singularWord}s"
        }
    }

}