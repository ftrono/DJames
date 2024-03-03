package com.ftrono.DJames.adapter

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.google.gson.JsonArray
import kotlin.math.roundToInt


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
        val prevText = vocItems[position].asString
        //POPOLA:
        var itemIcon = "🧑‍🎤"
        if (filter == "album") {
            itemIcon = "💿"
        } else if (filter == "playlist") {
            itemIcon = "▶️"
        }
        holder.item_type.text = "$itemIcon   ${filter.uppercase()}"
        if (prevText == "") {
            //A) NEW ITEM:
            //Edit mode already on:
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText)
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
            //No mode:
            holder.edit_text.text = prevText.strip()
            holder.edit_text.clearFocus()
            //Delete button:
            holder.delete_button.setOnClickListener { view ->
                deleteAction(prevText = prevText)
            }
        }
        //Edit mode:
        //a) from editText:
        holder.edit_text.setOnClickListener { view ->
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText)
        }
        holder.edit_text.setOnFocusChangeListener() { view: View, hasFocus: Boolean ->
            if (hasFocus) {
                editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText)
            } else {
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button)
            }
        }
        //b) from editButton:
        holder.edit_button.setOnClickListener { view ->
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText)
        }
        //LENGTH:
        if (position == (vocItems.size()-1)) {
            holder.card.layoutParams.height = (150 * density).roundToInt()
        }
    }

    private fun editMode(editText: TextView, editButton: ImageView, doneButton: ImageView, prevText: String) {
        //EditText in edit mode:
        editText.requestFocus()
        editText.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.dark_grey)
        //Replace EditButton with DoneButton:
        editButton.visibility = View.GONE
        doneButton.visibility = View.VISIBLE
        //End editMode with keyboard Enter key:
        editText.setOnEditorActionListener { v, actionId, event ->
            if (event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                doneAction(prevText=prevText, editText=editText, editButton=editButton, doneButton=doneButton)
                true
            } else {
                false
            }
        }
        //Done button:
        doneButton.setOnClickListener { view ->
            doneAction(prevText=prevText, editText=editText, editButton=editButton, doneButton=doneButton)
        }
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun noMode(editText: TextView, editButton: ImageView, doneButton: ImageView) {
        //Restore default visibility:
        editButton.visibility = View.VISIBLE
        doneButton.visibility = View.GONE
        editText.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorPrimaryDark)
        editText.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.hideSoftInputFromWindow(editButton.getWindowToken(), 0)
    }

    private fun doneAction(prevText: String, editText: TextView, editButton: ImageView, doneButton: ImageView) {
        var newText = editText.text.toString().lowercase().strip()
        if (newText != "") {
            //Save to file:
            if (newText != prevText) {
                var ret = utils.editVocFile(prevText = prevText, newText = newText)
                if (ret == 0) {
                    Toast.makeText(context, "Saved!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "ERROR: Vocabulary not updated!", Toast.LENGTH_LONG).show()
                }
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_VOC_REFRESH)
                    context.sendBroadcast(intent)
                }
            }
        }
        //Restore default visibility:
        noMode(editText=editText, editButton=editButton, doneButton=doneButton)
    }

    private fun deleteAction(prevText: String) {
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_VOC_DELETE)
            intent.putExtra("prevText", prevText)
            context.sendBroadcast(intent)
        }
    }

    override fun getItemCount(): Int {
        return vocItems.size()
    }

}

