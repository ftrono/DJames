package com.ftrono.DJames.adapter

import android.content.Context
import android.content.Intent
import android.util.Patterns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
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
        var prevUrl = ""
        //POPOLA:
        if (filter == "album") {
            holder.item_icon.setImageResource(R.drawable.icon_album)
        } else if (filter == "playlist") {
            holder.item_icon.setImageResource(R.drawable.icon_playlist)
            holder.url_intro.visibility= View.VISIBLE
            holder.edit_url.visibility= View.VISIBLE
        }
        holder.item_type.text = filter.uppercase()
        if (prevText == "") {
            //A) NEW ITEM:
            //Edit mode already on:
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevUrl=prevUrl, editUrl=holder.edit_url)
            //Delete button:
            holder.delete_button.setOnClickListener { view ->
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_VOC_REFRESH)
                    context.sendBroadcast(intent)
                }
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, editUrl=holder.edit_url)
            }
        } else {
            //B) EXISTING ITEMS:
            if (filter == "playlist") {
                //Split playlist name from URL:
                var temp = prevText.split(" %%% ")
                prevText = temp[0].strip()
                if (temp.size > 1) {
                    prevUrl = temp[1].strip()
                    holder.edit_url.text = prevUrl
                }
            }
            //No mode:
            holder.edit_text.text = prevText.strip()
            holder.edit_text.clearFocus()
            //Delete button:
            holder.delete_button.setOnClickListener { view ->
                deleteAction(prevText=prevText, prevUrl=prevUrl)
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, editUrl=holder.edit_url)
            }
        }
        //Edit mode:
        //a) from editText:
        holder.edit_text.setOnClickListener { view ->
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevUrl=prevUrl, editUrl=holder.edit_url)
        }
        holder.edit_text.setOnFocusChangeListener() { view: View, hasFocus: Boolean ->
            if (hasFocus) {
                editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevUrl=prevUrl, editUrl=holder.edit_url)
            } else {
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, editUrl=holder.edit_url)
            }
        }
        //b) from editButton:
        holder.edit_button.setOnClickListener { view ->
            holder.edit_text.requestFocus()
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevUrl=prevUrl, editUrl=holder.edit_url)
        }
        //c) from editUrl:
        holder.edit_url.setOnClickListener { view ->
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevUrl=prevUrl, editUrl=holder.edit_url)
        }
        holder.edit_url.setOnFocusChangeListener() { view: View, hasFocus: Boolean ->
            if (hasFocus) {
                editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevUrl=prevUrl, editUrl=holder.edit_url)
            } else {
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, editUrl=holder.edit_url)
            }
        }
        //LENGTH:
//        if (position == (vocItems.size()-1)) {
//            holder.card.layoutParams.height = (150 * density).roundToInt()
//        }
    }

    private fun editMode(editText: TextView, editButton: ImageView, doneButton: ImageView, prevText: String, prevUrl: String, editUrl: TextView) {
        editModeOn = true
        //EditText in edit mode:
        editText.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.dark_grey)
        editUrl.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.dark_grey)
        //Replace EditButton with DoneButton:
        editButton.visibility = View.GONE
        doneButton.visibility = View.VISIBLE
        //End editMode with keyboard Enter key:
        editText.setOnEditorActionListener { v, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                doneAction(prevText=prevText, editText=editText, editButton=editButton, doneButton=doneButton, prevUrl=prevUrl, editUrl=editUrl)
                true
            } else {
                false
            }
        }
        //Done button:
        doneButton.setOnClickListener { view ->
            doneAction(prevText=prevText, editText=editText, editButton=editButton, doneButton=doneButton, prevUrl=prevUrl, editUrl=editUrl)
        }
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun noMode(editText: TextView, editButton: ImageView, doneButton: ImageView, editUrl: TextView) {
        //Restore default visibility:
        editModeOn = false
        editButton.visibility = View.VISIBLE
        doneButton.visibility = View.GONE
        editText.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorPrimaryDark)
        editUrl.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorPrimaryDark)
        editText.clearFocus()
        editUrl.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.hideSoftInputFromWindow(editButton.getWindowToken(), 0)
    }

    private fun doneAction(prevText: String, editText: TextView, editButton: ImageView, doneButton: ImageView, prevUrl: String, editUrl: TextView) {
        var newText = editText.text.toString().lowercase().strip()
        var newUrl = editUrl.text.toString().strip()
        var urlTest = URLUtil.isValidUrl(newUrl) && Patterns.WEB_URL.matcher(newUrl).matches()
        if (filter == "playlist" && !urlTest) {
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_REQUEST_URL)
                context.sendBroadcast(intent)
            }
        } else {
            if (newText != "") {
                //Save to file:
                if (newText != prevText || (filter == "playlist" && newUrl != prevUrl)) {
                    var ret = 0
                    if (filter == "playlist") {
                        ret = utils.editVocFile(prevText = "$prevText %%% $prevUrl", newText = "$newText %%% $newUrl")
                    } else {
                        ret = utils.editVocFile(prevText = prevText, newText = newText)
                    }
                    if (ret == 0) {
                        Toast.makeText(context, "Saved!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "ERROR: Vocabulary not updated!", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            } else if (prevText != "") {
                deleteAction(prevText=prevText, prevUrl=prevUrl)
            }
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_REFRESH)
                context.sendBroadcast(intent)
            }
            noMode(editText=editText, editButton=editButton, doneButton=doneButton, editUrl=editUrl)
        }
    }

    private fun deleteAction(prevText: String, prevUrl: String) {
        var toDelete = prevText
        if (filter == "playlist") {
            toDelete = "$toDelete %%% $prevUrl"
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

