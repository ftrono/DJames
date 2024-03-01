package com.ftrono.DJames.application

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R


class VocabularyFragment : Fragment(R.layout.fragment_vocabulary) {

    private val TAG: String = VocabularyFragment::class.java.getSimpleName()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Views:
        var textNoData = requireActivity().findViewById<TextView>(R.id.vocabulary_no_data)
        var recyclerList = requireActivity().findViewById<RecyclerView>(R.id.vocabulary_list)

        //Visibility:
        recyclerList.visibility = View.GONE
        textNoData.visibility = View.VISIBLE

    }

}