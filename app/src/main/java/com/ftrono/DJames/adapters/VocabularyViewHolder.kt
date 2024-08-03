package com.ftrono.DJames.application

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R
import com.google.android.material.card.MaterialCardView

class VocabularyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @JvmField
    var voc_card: MaterialCardView = itemView.findViewById(R.id.voc_card)

    @JvmField
    var item_icon: ImageView = itemView.findViewById(R.id.voc_icon)

    @JvmField
    var item_text: TextView = itemView.findViewById(R.id.voc_text)

    @JvmField
    var item_updated: TextView = itemView.findViewById(R.id.voc_updated)

    @JvmField
    var download_button: ImageView = itemView.findViewById(R.id.voc_download_button)

    @JvmField
    var edit_button: ImageView = itemView.findViewById(R.id.voc_edit_button)

    @JvmField
    var delete_button: ImageView = itemView.findViewById(R.id.voc_delete_button)
}