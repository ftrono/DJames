package com.ftrono.DJames.database

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.artistBox
import com.ftrono.DJames.application.contactBox
import com.ftrono.DJames.application.playlistBox
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.vocHeads
import com.ftrono.DJames.application.vocSectionIdentifier
import com.ftrono.DJames.test_objects.testArtists
import com.ftrono.DJames.test_objects.testContacts
import com.ftrono.DJames.test_objects.testPlaylists
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File


class LibraryUtils {
    private val TAG = LibraryUtils::class.java.simpleName

    //GET ALL:
    //Get Map in format {"id": ["aliases", ...]}:
    fun getLibraryMap(filter: String): Map<String, List<String>> {
        var vocMap = mapOf<String, List<String>>()
        when (filter) {
            "artist" -> {
                vocMap =
                    artistBox!!.query().order(Artist_.name).build().find().associate { artist ->
                        artist.id.toString() to (artist.aliases ?: emptyList()) // Ensures no null values
                    }
            }

            "playlist" -> {
                vocMap = playlistBox!!.query().order(Playlist_.name).build().find()
                    .associate { playlist ->
                        playlist.id.toString() to (playlist.aliases ?: emptyList()) // Ensures no null values
                    }
            }

            "contact" -> {
                vocMap =
                    contactBox!!.query().order(Contact_.name).build().find().associate { contact ->
                        contact.id.toString() to (contact.aliases ?: emptyList()) // Ensures no null values
                    }
            }
        }
        return vocMap
    }

    //GET SINGLE:
    //Get entire item:
    fun getArtist(id: String): Artist {
        return artistBox!!.get(id.toLong())
    }

    fun getPlaylist(id: String): Playlist {
        return playlistBox!!.get(id.toLong())
    }

    fun getContact(id: String): Contact {
        return contactBox!!.get(id.toLong())
    }

    //Get single item name:
    fun getItemName(filter: String, id: String): String {
        when (filter) {
            "artist" -> {
                return artistBox!!.query()
                    .equal(Artist_.id, id.toLong())
                    .build()
                    .property(Artist_.name)
                    .findString()
            }
            "playlist" -> {
                return playlistBox!!.query()
                    .equal(Playlist_.id, id.toLong())
                    .build()
                    .property(Playlist_.name)
                    .findString()
            }
            "contact" -> {
                return contactBox!!.query()
                    .equal(Contact_.id, id.toLong())
                    .build()
                    .property(Contact_.name)
                    .findString()
            }
            else -> return ""
        }
    }

    //Get Map in format {"id": {"key": "...", "aliases": "...", "phone": "..."}}:
    fun getItemInfoView(filter: String, id: String, preview: Boolean = false): ItemInfoView {
        when (filter) {
            "artist" -> {
                val item = if (preview) testArtists[id.toInt()] else artistBox!!.get(id.toLong())
                return ItemInfoView(
                    name = item.name,
                    aliases = item.aliases
                )
            }

            "playlist" -> {
                val item = if (preview) testPlaylists[id.toInt()] else playlistBox!!.get(id.toLong())
                return ItemInfoView(
                    name = item.name,
                    aliases = item.aliases
                )
            }

            "contact" -> {
                val item = if (preview) testContacts[id.toInt()] else contactBox!!.get(id.toLong())
                return ItemInfoView(
                    name = item.name,
                    aliases = item.aliases,
                    phone = item.phoneSets["personal"]!!.phone
                )
            }
            else -> return ItemInfoView(
                name = "",
                aliases = mutableListOf<String>()
            )
        }
    }


    //Get Map in format {"id": {"key": "...", "aliases": "...", "phone": "..."}}:
    fun getItemInfoUse(filter: String, id: String): ItemInfoUse {
        when (filter) {
            "artist" -> {
                val item = artistBox!!.get(id.toLong())
                return ItemInfoUse(
                    name = item.name,
                    spotifyUrl = item.spotifyUrl,
                    defaultKey = item.defaultPlay,
                    playLinks = item.playLinks,
                )
            }

            "playlist" -> {
                val item = playlistBox!!.get(id.toLong())
                return ItemInfoUse(
                    name = item.name,
                    owner = item.owner,
                    spotifyUrl = item.spotifyUrl
                )
            }

            "contact" -> {
                val item = contactBox!!.get(id.toLong())
                return ItemInfoUse(
                    name = item.name,
                    language = item.language,
                    defaultKey = item.defaultPhone,
                    phoneSets = item.phoneSets
                )
            }
            else -> return ItemInfoUse(
                name = ""
            )
        }
    }


    //Get key List with headers:
    fun addLetterHeaders(vocMap: Map<String, List<String>>): List<String> {
        val vocKeysWithHeaders = mutableListOf<String>()
        //Cast ids to string and add Letter headers:
        var letter = ""
        for (id in vocMap.keys) {
            //get first alias:
            val aliases = vocMap[id]
            val cur = aliases!![0].first().toString()
            //extend List with letter header:
            if (cur != letter) {
                letter = if (utils.isLetters(cur)) cur else "#"
                vocKeysWithHeaders.add("$vocSectionIdentifier$letter")
            }
            vocKeysWithHeaders.add(id)
        }
        return vocKeysWithHeaders
    }


    //DB UPDATE:
    //Update / store Artist to DB:
    fun storeArtist(context: Context, item: Artist) {
        try {
            artistBox!!.put(item)
            Log.d(TAG, "Artist ${item.id} saved!")
            Toast.makeText(context, "Artist saved!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Artist ${item.id} not saved!", e)
            Toast.makeText(context, "ERROR: Artist not saved!", Toast.LENGTH_LONG).show()
        }
    }

    //Update / store Playlist to DB:
    fun storePlaylist(context: Context, item: Playlist) {
        try {
            playlistBox!!.put(item)
            Log.d(TAG, "Playlist ${item.id} saved!")
            Toast.makeText(context, "Playlist saved!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Playlist ${item.id} not saved!", e)
            Toast.makeText(context, "ERROR: Playlist not saved!", Toast.LENGTH_LONG).show()
        }
    }

    //Update / store Contact to DB:
    fun storeContact(context: Context, item: Contact) {
        try {
            contactBox!!.put(item)
            Log.d(TAG, "Contact ${item.id} saved!")
            Toast.makeText(context, "Contact saved!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Contact ${item.id} not saved!", e)
            Toast.makeText(context, "ERROR: Contact not saved!", Toast.LENGTH_LONG).show()
        }
    }


    //UTILS:
    fun getCollectionSize(filter: String): Long {
        return when (filter) {
            "artist" -> artistBox!!.count()
            "playlist" -> playlistBox!!.count()
            "contact" -> contactBox!!.count()
            else -> 0
        }
    }


    //DB DELETE:
    //Delete single item:
    fun deleteLibraryItem(context: Context, filter: String, id: String) {
        try {
            when (filter) {
                "artist" -> artistBox!!.remove(id.toLong())
                "playlist" -> playlistBox!!.remove(id.toLong())
                "contact" -> contactBox!!.remove(id.toLong())
            }
            Log.d(TAG, "Deleted $filter item $id!")
            Toast.makeText(context, "${filter.replaceFirstChar { it.uppercase() }} deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ERROR in deleting $filter item: $id}!", Toast.LENGTH_LONG).show()
            Log.w(TAG, "ERROR in deleting ${filter.replaceFirstChar { it.uppercase() }}: $id. ", e)
        }
    }

    //Delete all:
    fun deleteLibrary(context: Context, filter: String) {
        try {
            when (filter) {
                "artist" -> artistBox!!.removeAll()
                "playlist" -> playlistBox!!.removeAll()
                "contact" -> contactBox!!.removeAll()
            }
            artistBox!!.removeAll()
            Log.d(TAG, "Deleted ${filter} library!")
            Toast.makeText(context, "${filter.replaceFirstChar { it.uppercase() }} library deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR in deleting $filter library. ", e)
            Toast.makeText(context, "ERROR in deleting ${filter.replaceFirstChar { it.uppercase() }} library!", Toast.LENGTH_LONG).show()
        }
    }


    //Prepare and send cached Library file:
    fun buildLibraryToSend(context: Context, filter: String): String {
        var fileName = ""
        try {
            fileName = "library_${filter}s.json"
            val cachedFile = File(context.cacheDir, fileName)
            //Populate cached array & store to cached file:
            when (filter) {
                "artist" -> {
                    val libArray = artistBox!!.query().order(Artist_.name).build().find().toList()
                    cachedFile.writeText(Json.encodeToString(libArray))
                }
                "playlist" -> {
                    val libArray = playlistBox!!.query().order(Playlist_.name).build().find().toList()
                    cachedFile.writeText(Json.encodeToString(libArray))
                }
                "contact" -> {
                    val libArray = contactBox!!.query().order(Contact_.name).build().find().toList()
                    cachedFile.writeText(Json.encodeToString(libArray))
                }
            }
            Log.d(TAG, "Successfully prepared and cached consolidated Library for ${filter}s.")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot prepare or cache consolidated Library for ${filter}s!, ", e)
        }
        return fileName
    }


    //Clean consolidated cache (from directory, no DB):
    fun cleanLibraryCache(context: Context) {
        for (head in vocHeads) {
            try {
                File(context.cacheDir, "library_${head}s.json").delete()
            } catch (e: Exception) {
                Log.w(TAG, "No cached library_$head.json file to delete!")
            }
        }
    }
}