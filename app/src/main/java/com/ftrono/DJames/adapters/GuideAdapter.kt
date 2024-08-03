package com.ftrono.DJames.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.google.gson.JsonArray


class GuideAdapter(
        private val context: Context,
        private var guideItems: JsonArray)
    : RecyclerView.Adapter<GuideViewHolder>() {

    private val TAG = GuideAdapter::class.java.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.guide_card_layout, parent, false)
        return GuideViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        var item = guideItems[position].asJsonObject

        if (item.has("header")) {
            //HEADER:
            //Views:
            holder.guide_card.cardElevation = 0F
            holder.guide_header.visibility = View.VISIBLE
            holder.guide_icon.visibility = View.GONE
            holder.guide_info.visibility = View.GONE
            holder.guide_intro.visibility = View.GONE
            holder.guide_text_1_intro.visibility = View.GONE
            holder.guide_text_1.visibility = View.GONE
            holder.guide_text_2_intro.visibility = View.GONE
            holder.guide_text_2.visibility = View.GONE
            holder.guide_header.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomToTop = ConstraintLayout.LayoutParams.UNSET   //clear
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
            //Popola:
            holder.guide_header.text = item.get("header").asString

        } else {
            //CONTENT:
            //Views:
            holder.guide_card.cardElevation = 2 * density
            holder.guide_header.visibility = View.GONE
            holder.guide_icon.visibility = View.VISIBLE
            holder.guide_info.visibility = View.VISIBLE
            holder.guide_intro.visibility = View.VISIBLE
            holder.guide_text_1_intro.visibility = View.VISIBLE
            holder.guide_text_1.visibility = View.VISIBLE
            holder.guide_intro.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
            //Popola:
            holder.guide_intro.text = item.get("intro").asString
            holder.guide_text_1_intro.text = item.get("sentence_1_intro").asString
            holder.guide_text_1.text = item.get("sentence_1").asString
            var text_2 = item.get("sentence_2").asString
            if (text_2 == "") {
                holder.guide_text_2_intro.visibility = View.GONE
                holder.guide_text_2.visibility = View.GONE
            } else {
                holder.guide_text_2_intro.visibility = View.VISIBLE
                holder.guide_text_2.visibility = View.VISIBLE
                holder.guide_text_2_intro.text = item.get("sentence_2_intro").asString
                holder.guide_text_2.text = text_2
            }
            //Listener:
            holder.guide_card.setOnClickListener { view ->
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_GUIDE_POPUP)
                    intent.putExtra("index", position.toString())
                    context.sendBroadcast(intent)
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return guideItems.size()
    }

}

