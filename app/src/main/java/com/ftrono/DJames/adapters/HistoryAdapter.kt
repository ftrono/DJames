package com.ftrono.DJames.adapters

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
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlin.math.roundToInt
import java.io.File
import java.util.Locale


class HistoryAdapter(
        private val context: Context,
        private var logItems: JsonArray)
    : RecyclerView.Adapter<HistoryViewHolder>() {

    private val TAG = HistoryAdapter::class.java.simpleName
    private var utils = Utilities()
    private var trimLength = 40
    //private var toDelete = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.history_card_layout, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val logItem: JsonObject = logItems[position].asJsonObject
        //POPOLA:
        var datetime = logItem.get("datetime").asString
        var filename = "$datetime.json"
        var type_icon = ""
        var context_error = false
        var play_externally = false
        var intentName = ""
        try {
            intentName = logItem.get("intent_name").asString
        } catch (e: Exception) {
            //TODO: Temporary (remove after September 10, 2024)
            intentName = logItem.get("nlp").asJsonObject.get("intent_response").asString
        }
        var playType = "track"
        try {
            playType = logItem.get("spotify_play").asJsonObject.get("play_type").asString
        } catch (e: Exception) {
            Log.w(TAG, "No play_type in spotify_play!")
        }
        //Call / message requests:
        if (intentName == "CallRequest" || intentName == "MessageRequest") {
            //type icon:
            if (intentName == "CallRequest") {
                type_icon = "📞"
            } else {
                type_icon = "💬"
            }
            //result check:
            try {
                if (logItem.get("voc_score").asInt > midThreshold) {
                    holder.datetime.text = "$type_icon  ${context.getString(R.string.result_good)}  $intentName: $datetime"
                } else {
                    holder.datetime.text = "$type_icon  ${context.getString(R.string.result_not_good)}  $intentName: $datetime"
                }
            } catch (e: Exception) {
                //Generic request:
                holder.datetime.text = "${context.getString(R.string.result_question)}   $intentName: $datetime"
            }
        } else {
            //Play requests:
            try {
                type_icon = "🎧"
                try {
                    context_error = logItem.get("context_error").asBoolean
                } catch (e: Exception) {
                    context_error = false
                }
                if (logItem.has("voc_score")) {
                    if (logItem.get("voc_score").asInt > midThreshold) {
                        holder.datetime.text = "$type_icon  ${context.getString(R.string.result_good)}  $intentName: $datetime"
                    } else {
                        holder.datetime.text = "$type_icon  ${context.getString(R.string.result_not_good)}  $intentName: $datetime"
                    }
                } else {
                    var bestScore = logItem.get("best_score").asInt
                    if (!context_error && bestScore >= matchDoubleThreshold) {
                        //TODO: TEMPORARY ONLY (remove after August 28, 2024):
                        holder.datetime.text = "$type_icon  ${context.getString(R.string.result_good)}  $intentName: $datetime"
                    } else if (!context_error && bestScore <= 100 && bestScore >= playThreshold) {
                        holder.datetime.text = "$type_icon  ${context.getString(R.string.result_good)}  $intentName: $datetime"
                    } else {
                        holder.datetime.text = "$type_icon  ${context.getString(R.string.result_not_good)}  $intentName: $datetime"
                    }
                }
            } catch (e: Exception) {
                //Generic request:
                holder.datetime.text = "${context.getString(R.string.result_question)}   $intentName: $datetime"
            }
        }
        //NLP:
        try {
            var queryText = logItem.get("nlp").asJsonObject.get("query_text").asString
            if (intentName.contains("Play") && !queryText.contains("play ")) {
                holder.nlp_text.text = "play: $queryText"
            } else {
                holder.nlp_text.text = queryText
            }
        } catch (e: Exception) {
            holder.nlp_text.text = "Not understood"
        }
        //MATCH:
        try {
            //Reset:
            holder.match_name.setPadding(
                (8 * density).roundToInt(),
                (8 * density).roundToInt(),
                0,
                0
            )
            holder.match_name_intro.setPadding(
                (8 * density).roundToInt(),
                (8 * density).roundToInt(),
                0,
                0
            )
            holder.match_artist_intro.visibility = View.VISIBLE
            holder.match_artist.visibility = View.VISIBLE
            holder.match_context_intro.visibility = View.VISIBLE
            holder.match_context.visibility = View.VISIBLE
            //SPOTIFY:
            holder.match_name_intro.text = "MATCH:     "
            holder.match_name.text =
                utils.trimString(logItem.get("spotify_play").asJsonObject.get("match_name").asString, trimLength)
            holder.match_artist.text =
                utils.trimString(logItem.get("spotify_play").asJsonObject.get("artist_name").asString, trimLength)
            //Context:
            try {
                play_externally = logItem.get("play_externally").asBoolean
            } catch (e: Exception) {
                play_externally = false
            }
            var contextType = logItem.get("spotify_play").asJsonObject.get("context_type").asString.replaceFirstChar { it.uppercase() }
            var contextName = ""
            if (contextType == "Playlist" && !context_error && !play_externally) {
                //Use Playlist:
                contextName = logItem.get("spotify_play").asJsonObject.get("context_name").asString
            } else {
                //Default to Album type:
                try {
                    contextType = logItem.get("spotify_play").asJsonObject.get("album_type").asString.replaceFirstChar { it.uppercase() }
                } catch (e: Exception) {
                    contextType = "Album"
                }
                contextName = logItem.get("spotify_play").asJsonObject.get("album_name").asString
            }
            contextName = contextName.split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
            var contextFull = "($contextType) $contextName"
            if (play_externally) {
                contextFull = "[EXT] $contextFull"
            }
            holder.match_context.text = utils.trimString(contextFull, trimLength)

        } catch (e: Exception) {
            //Log.w(TAG, "ADAPTER ERROR: ", e)
            try {
                if (playType != "track") {
                    //PLAY PLAYLIST:
                    holder.match_name_intro.text = "MATCH: "
                    var playTypeCaps = playType.replaceFirstChar { it.uppercase() }
                    var matchName = logItem.get("spotify_play").asJsonObject.get("context_name").asString.split(" ").map { it.lowercase().capitalize(Locale.getDefault()) }.joinToString(" ")
                    holder.match_name.text = "($playTypeCaps) $matchName"

                } else {
                    //CONTACT CALL / MESSAGE:
                    var contacted = logItem.get("contact_extractor").asJsonObject.get("contact_confirmed").asString.replaceFirstChar { it.uppercase() }
                    if (contacted != "") {
                        holder.match_name_intro.text = "CONTACTED: "
                        holder.match_name.text = contacted
                    } else {
                        holder.match_name_intro.text = "TYPE: "
                        try {
                            holder.match_name.text = logItem.get("nlp").asJsonObject.get("intent_name").asString
                        } catch (e: Exception) {
                            holder.match_name.text = "Unknown request"
                        }
                    }
                }

            } catch (e:Exception) {
                //GENERIC REQUEST:
                holder.match_name_intro.text = "TYPE: "
                try {
                    holder.match_name.text = logItem.get("nlp").asJsonObject.get("intent_name").asString
                } catch (e: Exception) {
                    holder.match_name.text = "Unknown request"
                }
            }
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
        holder.history_card.setOnClickListener { view -> openFile(filename) }
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

