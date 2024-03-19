package com.ftrono.DJames.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*


class VocabularyAdapter(
        private val context: Context,
        private var listItems: List<String>)
    : RecyclerView.Adapter<VocabularyViewHolder>() {

    private val TAG = VocabularyAdapter::class.java.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabularyViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.vocabulary_card_layout, parent, false)
        return VocabularyViewHolder(view)
    }

    override fun onBindViewHolder(holder: VocabularyViewHolder, position: Int) {
        var prevText = listItems[position]
        //POPOLA:
        if (filter == "contact") {
            holder.item_icon.setImageResource(R.drawable.icon_contact)
        } else if (filter == "playlist") {
            holder.item_icon.setImageResource(R.drawable.icon_album)
        } else {
            holder.item_icon.setImageResource(R.drawable.icon_music)
        }
        holder.item_text.text = prevText
        //EDIT LISTENERS:
        holder.edit_button.setOnClickListener { view ->
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_EDIT)
                intent.putExtra("prevText", prevText)
                context.sendBroadcast(intent)
            }
        }
        holder.item_icon.setOnClickListener { view ->
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_EDIT)
                intent.putExtra("prevText", prevText)
                context.sendBroadcast(intent)
            }
        }
        holder.item_text.setOnClickListener { view ->
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_EDIT)
                intent.putExtra("prevText", prevText)
                context.sendBroadcast(intent)
            }
        }
        //DELETE LISTENER:
        holder.delete_button.setOnClickListener { view ->
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_DELETE)
                intent.putExtra("toDelete", prevText)
                context.sendBroadcast(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

}

