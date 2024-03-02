package com.ftrono.DJames.adapter

import android.util.Log
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File


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
        val vocItem: JsonObject = vocItems[position].asJsonObject
        //POPOLA:
        var itemType = vocItem.get("item_type").asString
        var itemText = vocItem.get("item_text").asString
        var itemIcon = "🧑‍🎤"
        if (itemType == "album") {
            itemIcon = "💿"
        } else if (itemType == "playlist") {
            itemIcon = "▶️"
        }
        holder.item_type.text = "$itemIcon   ${itemType.uppercase()}"
        //A) NEW ITEM:
        if (itemText == "") {
            //Show EditText:
            holder.item_text.visibility = View.GONE
            holder.item_edit_text.visibility = View.VISIBLE
            holder.item_edit_text.requestFocus()
            //Replace EditButton with DoneButton:
            holder.edit_button.visibility = View.GONE
            holder.done_button.visibility = View.VISIBLE
            //Done button:
            holder.done_button.setOnClickListener { view ->
                doneAction(itemType=itemType, editTextView=holder.item_edit_text, itemTextView=holder.item_text, editButton=holder.edit_button, doneButton=holder.done_button, createNew = true)
            }
            //Delete button:
            holder.delete_button.setOnClickListener { view ->
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_VOC_REFRESH)
                    context.sendBroadcast(intent)
                }
            }

        } else {
            //B) ESISTING ITEMS:
            holder.item_text.text = itemText
            holder.item_text.visibility = View.VISIBLE
            holder.item_edit_text.visibility = View.GONE
            //Edit button:
            holder.edit_button.setOnClickListener { view ->
                //Show EditText:
                holder.item_text.visibility = View.GONE
                holder.item_edit_text.visibility = View.VISIBLE
                holder.item_edit_text.text = itemText.strip()
                holder.item_edit_text.requestFocus()
                //Replace EditButton with DoneButton:
                holder.edit_button.visibility = View.GONE
                holder.done_button.visibility = View.VISIBLE
                //Done button:
                holder.done_button.setOnClickListener { view ->
                    doneAction(itemType=itemType, editTextView=holder.item_edit_text, itemTextView=holder.item_text, editButton=holder.edit_button, doneButton=holder.done_button, createNew = false)
                }
            }
            //Delete button:
            holder.delete_button.setOnClickListener { view -> deleteAction("${itemType}_${itemText}.json") }
        }
    }

    private fun doneAction(itemType: String, editTextView: TextView, itemTextView: TextView, editButton: ImageView, doneButton: ImageView, createNew: Boolean) {
        var newText = editTextView.text.toString().lowercase().strip()
        if (newText != "") {
            //Save to file:
            if (newText != itemTextView.text) {
                //Pack JSON:
                var newJSON = JsonObject()
                newJSON.addProperty("item_type", itemType)
                newJSON.addProperty("item_text", newText)
                //Save:
                var newFile = File(vocDir, "${itemType}_${newText}.json")
                if (!createNew) {
                    var oldFileName = "${itemType}_${itemTextView.text}.json"
                    File(vocDir, oldFileName).delete()
                    Log.d(TAG, "Deleted file: $oldFileName")
                }
                newFile.createNewFile()
                newFile.writeText(newJSON.toString())
                itemTextView.text = newText
                //Toast:
                Toast.makeText(context, "Saved!", Toast.LENGTH_LONG).show()
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_VOC_REFRESH)
                    context.sendBroadcast(intent)
                }
            }
        }
        //Restore default visibility:
        itemTextView.visibility = View.VISIBLE
        editTextView.visibility = View.GONE
        editButton.visibility = View.VISIBLE
        doneButton.visibility = View.GONE
    }

    private fun deleteAction(filename: String) {
        //Compose list of items to delete:
        var singleToDelete = ArrayList<String>()
        singleToDelete.add(filename)
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_VOC_DELETE)
            intent.putExtra("toDeleteStr", singleToDelete.joinToString(",", "", ""))
            context.sendBroadcast(intent)
        }
    }

    override fun getItemCount(): Int {
        return vocItems.size()
    }

}

