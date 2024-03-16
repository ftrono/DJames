package com.ftrono.DJames.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.google.gson.JsonArray


class VocabularyAdapter(
        private val context: Context,
        private var vocItems: JsonArray)
    : RecyclerView.Adapter<VocabularyViewHolder>() {

    private val TAG = VocabularyAdapter::class.java.simpleName
    //private var toDelete = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabularyViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.vocabulary_card_layout, parent, false)
        return VocabularyViewHolder(view)
    }

    override fun onBindViewHolder(holder: VocabularyViewHolder, position: Int) {
        var prevText = vocItems[position].asString
        var prevDetail = ""
        //POPOLA:
        if (filter == "contact") {
            holder.item_icon.setImageResource(R.drawable.icon_contact)
        } else if (filter == "playlist") {
            holder.item_icon.setImageResource(R.drawable.icon_album)
        } else {
            holder.item_icon.setImageResource(R.drawable.icon_music)
        }

        if (filter == "playlist" || filter == "contact") {
            //Split playlist name from URL:
            var temp = prevText.split(" %%% ")
            prevText = temp[0].strip()
            if (temp.size > 1) {
                prevDetail = temp[1].strip()
            }
        }
        holder.item_text.text = prevText.strip()
        //EDIT LISTENERS:
        holder.edit_button.setOnClickListener { view ->
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_EDIT)
                intent.putExtra("prevText", prevText)
                intent.putExtra("prevDetail", prevDetail)
                context.sendBroadcast(intent)
            }
        }
        holder.item_icon.setOnClickListener { view ->
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_EDIT)
                intent.putExtra("prevText", prevText)
                intent.putExtra("prevDetail", prevDetail)
                context.sendBroadcast(intent)
            }
        }
        holder.item_text.setOnClickListener { view ->
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_EDIT)
                intent.putExtra("prevText", prevText)
                intent.putExtra("prevDetail", prevDetail)
                context.sendBroadcast(intent)
            }
        }
        //DELETE LISTENER:
        holder.delete_button.setOnClickListener { view ->
            deleteAction(prevText=prevText, prevDetail=prevDetail)
        }
    }

    private fun deleteAction(prevText: String, prevDetail: String) {
        var toDelete = prevText
        if (filter == "playlist" || filter == "contact") {
            toDelete = "$toDelete %%% $prevDetail"
        }
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_VOC_DELETE)
            intent.putExtra("toDelete", toDelete)
            context.sendBroadcast(intent)
        }
    }

    override fun getItemCount(): Int {
        return vocItems.size()
    }

}

