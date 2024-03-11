package com.ftrono.DJames.application

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R

class VocabularyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @JvmField
    var item_icon: ImageView = itemView.findViewById(R.id.voc_icon)

    @JvmField
    var item_type: TextView = itemView.findViewById(R.id.item_type)

    @JvmField
    var edit_text: TextView = itemView.findViewById(R.id.item_edit_text)

    @JvmField
    var edit_detail: TextView = itemView.findViewById(R.id.voc_detail)

    @JvmField
    var edit_button: ImageView = itemView.findViewById(R.id.voc_edit_button)

    @JvmField
    var done_button: ImageView = itemView.findViewById(R.id.voc_done_button)

    @JvmField
    var delete_button: ImageView = itemView.findViewById(R.id.voc_delete_button)
}