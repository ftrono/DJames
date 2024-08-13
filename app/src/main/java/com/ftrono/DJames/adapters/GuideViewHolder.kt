package com.ftrono.DJames.application

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R
import com.google.android.material.card.MaterialCardView

class GuideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @JvmField
    var guide_card: MaterialCardView = itemView.findViewById(R.id.guide_card)

    @JvmField
    var guide_header: TextView = itemView.findViewById(R.id.guide_header)

    @JvmField
    var guide_icon: ImageView = itemView.findViewById(R.id.guide_icon)

    @JvmField
    var guide_intro: TextView = itemView.findViewById(R.id.guide_intro)

    @JvmField
    var guide_info: TextView = itemView.findViewById(R.id.guide_info)

    @JvmField
    var guide_text: TextView = itemView.findViewById(R.id.guide_text)
}