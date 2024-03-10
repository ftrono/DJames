package com.ftrono.DJames.adapter

import android.content.Context
import android.content.Intent
import android.telephony.PhoneNumberUtils
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
        var prevDetail = ""
        //POPOLA:
        if (filter == "contact") {
            holder.item_icon.setImageResource(R.drawable.icon_phone)
            holder.detail_intro.text = "PHONE: "
            holder.edit_detail.hint = "Write the main phone number here..."
            holder.detail_intro.visibility= View.VISIBLE
            holder.edit_detail.visibility= View.VISIBLE
        } else if (filter == "playlist") {
            holder.item_icon.setImageResource(R.drawable.icon_album)
            holder.detail_intro.text = "LINK: "
            holder.edit_detail.hint = "Paste playlist link here..."
            holder.detail_intro.visibility= View.VISIBLE
            holder.edit_detail.visibility= View.VISIBLE
        } else {
            holder.item_icon.setImageResource(R.drawable.icon_music)
        }

        holder.item_type.text = filter.uppercase()
        if (prevText == "") {
            //A) NEW ITEM:
            //Edit mode already on:
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevDetail=prevDetail, editDetail=holder.edit_detail)
            //Delete button:
            holder.delete_button.setOnClickListener { view ->
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_VOC_REFRESH)
                    context.sendBroadcast(intent)
                }
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, editDetail=holder.edit_detail)
            }
        } else {
            //B) EXISTING ITEMS:
            if (filter == "playlist" || filter == "contact") {
                //Split playlist name from URL:
                var temp = prevText.split(" %%% ")
                prevText = temp[0].strip()
                if (temp.size > 1) {
                    prevDetail = temp[1].strip()
                    holder.edit_detail.text = prevDetail
                }
            }
            //No mode:
            holder.edit_text.text = prevText.strip()
            holder.edit_text.clearFocus()
            //Delete button:
            holder.delete_button.setOnClickListener { view ->
                deleteAction(prevText=prevText, prevDetail=prevDetail)
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, editDetail=holder.edit_detail)
            }
        }
        //Edit mode:
        //a) from editText:
        holder.edit_text.setOnClickListener { view ->
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevDetail=prevDetail, editDetail=holder.edit_detail)
        }
        holder.edit_text.setOnFocusChangeListener() { view: View, hasFocus: Boolean ->
            if (hasFocus) {
                editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevDetail=prevDetail, editDetail=holder.edit_detail)
            } else {
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, editDetail=holder.edit_detail)
            }
        }
        //b) from editButton:
        holder.edit_button.setOnClickListener { view ->
            holder.edit_text.requestFocus()
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevDetail=prevDetail, editDetail=holder.edit_detail)
        }
        //c) from editDetail:
        holder.edit_detail.setOnClickListener { view ->
            editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevDetail=prevDetail, editDetail=holder.edit_detail)
        }
        holder.edit_detail.setOnFocusChangeListener() { view: View, hasFocus: Boolean ->
            if (hasFocus) {
                editMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, prevText=prevText, prevDetail=prevDetail, editDetail=holder.edit_detail)
            } else {
                noMode(editText=holder.edit_text, editButton=holder.edit_button, doneButton=holder.done_button, editDetail=holder.edit_detail)
            }
        }
        //LENGTH:
//        if (position == (vocItems.size()-1)) {
//            holder.card.layoutParams.height = (150 * density).roundToInt()
//        }
    }

    private fun editMode(editText: TextView, editButton: ImageView, doneButton: ImageView, prevText: String, prevDetail: String, editDetail: TextView) {
        editModeOn = true
        //EditText in edit mode:
        editText.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.dark_grey)
        editDetail.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.dark_grey)
        editDetail.setTextColor(AppCompatResources.getColorStateList(context, R.color.light_grey))
        //Replace EditButton with DoneButton:
        editButton.visibility = View.GONE
        doneButton.visibility = View.VISIBLE
        //End editMode with keyboard Enter key:
        editText.setOnEditorActionListener { v, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                doneAction(prevText=prevText, editText=editText, editButton=editButton, doneButton=doneButton, prevDetail=prevDetail, editDetail=editDetail)
                true
            } else {
                false
            }
        }
        //Done button:
        doneButton.setOnClickListener { view ->
            doneAction(prevText=prevText, editText=editText, editButton=editButton, doneButton=doneButton, prevDetail=prevDetail, editDetail=editDetail)
        }
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun noMode(editText: TextView, editButton: ImageView, doneButton: ImageView, editDetail: TextView) {
        //Restore default visibility:
        editModeOn = false
        editButton.visibility = View.VISIBLE
        doneButton.visibility = View.GONE
        editText.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorPrimaryDark)
        editDetail.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorPrimaryDark)
        editDetail.setTextColor(AppCompatResources.getColorStateList(context, R.color.mid_grey))
        editText.clearFocus()
        editDetail.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.hideSoftInputFromWindow(editButton.getWindowToken(), 0)
    }

    private fun doneAction(prevText: String, editText: TextView, editButton: ImageView, doneButton: ImageView, prevDetail: String, editDetail: TextView) {
        var newText = editText.text.toString().lowercase().strip()
        var newDetail = editDetail.text.toString().replace(" ", "")
        var urlTest = URLUtil.isValidUrl(newDetail) && Patterns.WEB_URL.matcher(newDetail).matches()
        var phoneTest = PhoneNumberUtils.isGlobalPhoneNumber(newDetail)
        //Request valid detail (i.e. no URL, no phone number, no international prefix in phone number):
        if ((filter == "playlist" && (!urlTest || !newDetail.contains("open.spotify.com")))
            || (filter == "contact" && (!phoneTest || !newDetail.contains("+")))) {
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_REQUEST_DETAIL)
                context.sendBroadcast(intent)
            }
        } else {
            if (newText != "") {
                //Save to file:
                if (newText != prevText
                    || (filter == "playlist" && newDetail != prevDetail)
                    || (filter == "contact" && newDetail != prevDetail)) {
                    var ret = 0
                    if (filter == "playlist" || filter == "contact") {
                        ret = utils.editVocFile(prevText = "$prevText %%% $prevDetail", newText = "$newText %%% $newDetail")
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
                deleteAction(prevText=prevText, prevDetail=prevDetail)
            }
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_VOC_REFRESH)
                context.sendBroadcast(intent)
            }
            noMode(editText=editText, editButton=editButton, doneButton=doneButton, editDetail=editDetail)
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

