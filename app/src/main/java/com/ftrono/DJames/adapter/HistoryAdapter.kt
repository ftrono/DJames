package com.ftrono.DJames.adapter

import android.util.Log
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlin.math.roundToInt
import java.io.File


class HistoryAdapter(
        private val context: Context,
        private var logItems: JsonArray)
    : RecyclerView.Adapter<HistoryViewHolder>() {

    private val TAG = HistoryAdapter::class.java.simpleName
    private var density: Float = 0F
    //private var toDelete = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)
        //Screen density:
        density = context.resources.displayMetrics.density
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val logItem: JsonObject = logItems[position].asJsonObject
        //POPOLA:
        var datetime = logItem.get("datetime").asString
        var filename = "$datetime.json"
        //result check:
        try {
            if (logItem.get("best_score").asInt > (matchDoubleThreshold)) {
                holder.datetime.text = "${context.getString(R.string.result_good)}   $datetime"
            } else {
                holder.datetime.text = "${context.getString(R.string.result_not_good)}   $datetime"
            }
        } catch (e: Exception) {
            holder.datetime.text = "${context.getString(R.string.result_fail)}   $datetime"
        }
        //NLP:
        try {
            holder.nlp_text.text =
                logItem.get("nlp").asJsonObject.get("query_text").asString
        } catch (e: Exception) {
            holder.nlp_text.text = "Not understood"
        }
        //SPOTIFY:
        try {
            holder.match_name.text =
                logItem.get("spotify_play").asJsonObject.get("song_name").asString
            holder.match_artist.text =
                logItem.get("spotify_play").asJsonObject.get("artist_name").asString
            holder.match_context.text =
                logItem.get("spotify_play").asJsonObject.get("context_name").asString
        } catch (e: Exception) {
            holder.match_name.text = "No match"
            holder.match_name.setPadding(
                (8 * density).roundToInt(),
                (8 * density).roundToInt(),
                0,
                (8 * density).roundToInt()
            )
            holder.match_name_intro.setPadding(
                (8 * density).roundToInt(),
                (8 * density).roundToInt(),
                0,
                (8 * density).roundToInt()
            )
            holder.match_artist_intro.visibility = View.GONE
            holder.match_artist.visibility = View.GONE
            holder.match_context_intro.visibility = View.GONE
            holder.match_context.visibility = View.GONE
        }
        //Button listeners:
        holder.send_button.setOnClickListener { view -> sendFile(filename) }
        holder.lookup_button.setOnClickListener { view -> openFile(filename) }
        holder.delete_button.setOnClickListener { view -> deleteAction(filename) }
    }

    private fun sendFile(filename: String) {
        //Send the current file:
        val file = File(logDir, filename)
        val uriToFile = FileProvider.getUriForFile(context, "com.ftrono.DJames.provider", file)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uriToFile)
            type = "image/jpeg"
        }
        var chooserIntent = Intent.createChooser(sendIntent, null)
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        chooserIntent.putExtra("fromwhere", "ser")
        startActivity(context, chooserIntent, null)
    }

    private fun openFile(filename: String) {
        try {
            // Get URI and MIME type of file
            val file = File(logDir, filename)
            val uri = FileProvider.getUriForFile(context, "com.ftrono.DJames.provider", file)
            val mime = context.contentResolver.getType(uri)

            // Open file with user selected app
            val intent1 = Intent()
            intent1.setAction(Intent.ACTION_VIEW)
            intent1.setDataAndType(uri, mime)
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent1.putExtra("fromwhere", "ser")
            startActivity(context, intent1, null)
        } catch (e: Exception) {
            Log.d(TAG, "OpenLogFile(): viewer app not found!")
            Toast.makeText(context, "No app to open the selected file!", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteAction(filename: String) {
        //Compose list of items to delete:
        var singleToDelete = ArrayList<String>()
        singleToDelete.add(filename)
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_LOG_DELETE)
            intent.putExtra("toDeleteStr", singleToDelete.joinToString(",", "", ""))
            context.sendBroadcast(intent)
        }
    }

    override fun getItemCount(): Int {
        return logItems.size()
    }

}

