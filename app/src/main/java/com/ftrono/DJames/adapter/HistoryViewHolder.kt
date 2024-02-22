package com.ftrono.DJames.application

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R

class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @JvmField
    var datetime: TextView = itemView.findViewById(R.id.datetime)

    @JvmField
    var nlp_text: TextView = itemView.findViewById(R.id.nlp_text)

    @JvmField
    var match_name_intro: TextView = itemView.findViewById(R.id.match_name_intro)

    @JvmField
    var match_name: TextView = itemView.findViewById(R.id.match_name)

    @JvmField
    var match_artist_intro: TextView = itemView.findViewById(R.id.match_artist_intro)

    @JvmField
    var match_artist: TextView = itemView.findViewById(R.id.match_artist)

    @JvmField
    var match_context_intro: TextView = itemView.findViewById(R.id.match_context_intro)

    @JvmField
    var match_context: TextView = itemView.findViewById(R.id.match_context)

    @JvmField
    var send_button: ImageView = itemView.findViewById(R.id.send_button)

    @JvmField
    var lookup_button: ImageView = itemView.findViewById(R.id.lookup_button)

    @JvmField
    var delete_button: ImageView = itemView.findViewById(R.id.delete_button)
}