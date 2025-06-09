package com.ftrono.DJames.be.database

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.artistBox
import com.ftrono.DJames.application.contactBox
import com.ftrono.DJames.application.curLibrarySize
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.playlistBox
import com.ftrono.DJames.application.podcastBox
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.routeBox
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.vocHeads
import com.ftrono.DJames.application.vocSectionIdentifier
import com.ftrono.DJames.be.samples.testArtists
import com.ftrono.DJames.be.samples.testContacts
import com.ftrono.DJames.be.samples.testPlaylists
import com.ftrono.DJames.be.database.Artist_
import com.ftrono.DJames.be.database.Contact_
import com.ftrono.DJames.be.database.Playlist_
import com.ftrono.DJames.be.database.Route_
import com.ftrono.DJames.be.samples.testPodcasts
import com.ftrono.DJames.be.samples.testRoutes
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.internal.toImmutableList
import java.io.File


class LibraryUtils {
    private val TAG = LibraryUtils::class.java.simpleName

    //GET ALL:
    //Get List of ItemInfoView items:
    fun refreshLibrary(filter: String, preview: Boolean = false, addHeaders: Boolean = true): List<String> {
        //1) Load library:
        var library = listOf<String>()
        try {
            when (filter) {
                "artist" -> {
                    library = if (preview) {
                        testArtists
                    } else {
                        artistBox!!.query().order(Artist_.name).build().find()
                    }.map { item ->
                        //Cast value to String to allow storing into MutableState:
                        Json.encodeToString(
                            ItemInfoView(
                                id = item.id,
                                name = item.name,
                                imageUrl = item.imageUrl,
                                aliases = item.aliases
                            )
                        )
                    }
                }

                "playlist" -> {
                    library = if (preview) {
                        testPlaylists
                    } else {
                        playlistBox!!.query().order(Playlist_.name).build().find()
                    }.map { item ->
                        Json.encodeToString(
                            ItemInfoView(
                                id = item.id,
                                name = item.name,
                                imageUrl = item.imageUrl,
                                aliases = item.aliases
                            )
                        )
                    }
                }

                "podcast" -> {
                    library = if (preview) {
                        testPodcasts
                    } else {
                        podcastBox!!.query().order(Podcast_.name).build().find()
                    }.map { item ->
                        Json.encodeToString(
                            ItemInfoView(
                                id = item.id,
                                name = item.name,
                                imageUrl = item.imageUrl,
                                aliases = item.aliases,
                                detail = item.publisher
                            )
                        )
                    }
                }

                "contact" -> {
                    library = if (preview) {
                        testContacts
                    } else {
                        contactBox!!.query().order(Contact_.name).build().find()
                    }.map { item ->
                        Json.encodeToString(
                            ItemInfoView(
                                id = item.id,
                                name = item.name,
                                imageUrl = "",
                                aliases = item.aliases,
                                detail = item.phoneSets["personal"]!!.phone
                            )
                        )
                    }
                }

                "route" -> {
                    library = if (preview) {
                        testRoutes
                    } else {
                        routeBox!!.query().order(Route_.name).build().find()
                    }.map { item ->
                        Json.encodeToString(
                            ItemInfoView(
                                id = item.id,
                                name = item.name,
                                imageUrl = "",
                                aliases = item.aliases,
                                detail = item.destination.town
                            )
                        )
                    }
                }
            }

            //2) Update Library size (IMPORTANT - for signs):
            curLibrarySize.postValue(library.size)

            //3) Build library map & add headers:
            if (addHeaders) {
                return addLetterHeaders(library)
            } else {
                return library
            }

        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot refresh Library for $filter! ", e)
            curLibrarySize.postValue(0)
            return library
        }
    }


    //Route: build subtitle:
    fun buildRouteSubtitle(item: Route, viewLanguage: String = "en"): String {
        var subtitle = ""
        if (item.via.placeName == "" && item.via.address == "") {
            if (item.destination.town != "" && item.destination.address != "") {
                subtitle = item.destination.town + ", " + item.destination.address + " " + item.destination.number
            } else {
                subtitle = ""
            }
        } else {
            val introStr = if (viewLanguage == "it") "Da " else "By "
            subtitle = introStr + if (item.via.placeName != "") item.via.placeName else item.via.address + " " + item.via.number
        }
        return subtitle
    }


    //Get key List with headers:
    fun addLetterHeaders(library: List<String>): List<String> {
        val listWithHeaders = mutableListOf<String>()
        //Add Letter headers:
        var letter = ""
        for (itemJson in library) {
            //get first alias:
            val item = Json.decodeFromString<ItemInfoView>(itemJson)
            val curFirst = item.name.uppercase().first().toString()
            //extend List with letter header:
            if (curFirst != letter) {
                letter = if (utils.isLetters(curFirst)) curFirst.uppercase() else "#"
                listWithHeaders.add("$vocSectionIdentifier$letter")
            }
            listWithHeaders.add(itemJson)
        }
        return listWithHeaders.toImmutableList()
    }


    //Get aliases Map in format {"id": ["aliases", ...]}:
    fun getAliasesMap(filter: String): Map<Long, List<String>> {
        var vocMap = mapOf<Long, List<String>>()
        when (filter) {
            "artist" -> {
                vocMap =
                    artistBox!!.query().order(Artist_.name).build().find().associate { artist ->
                        artist.id to (artist.aliases ?: emptyList()) // Ensures no null values
                    }
            }

            "playlist" -> {
                vocMap = playlistBox!!.query().order(Playlist_.name).build().find()
                    .associate { playlist ->
                        playlist.id to (playlist.aliases ?: emptyList()) // Ensures no null values
                    }
            }

            "podcast" -> {
                vocMap = podcastBox!!.query().order(Podcast_.name).build().find()
                    .associate { podcast ->
                        podcast.id to (podcast.aliases ?: emptyList()) // Ensures no null values
                    }
            }

            "contact" -> {
                vocMap =
                    contactBox!!.query().order(Contact_.name).build().find().associate { contact ->
                        contact.id to (contact.aliases ?: emptyList()) // Ensures no null values
                    }
            }

            "route" -> {
                vocMap =
                    routeBox!!.query().order(Route_.name).build().find().associate { route ->
                        route.id to (route.aliases ?: emptyList()) // Ensures no null values
                    }
            }
        }
        return vocMap
    }


    //Get URL Map in format {"url": "id"}:
    fun getUrlMap(filter: String): Map<String, Long> {
        var urlMap = mapOf<String, Long>()
        when (filter) {
            "artist" -> {
                urlMap =
                    artistBox!!.query().order(Artist_.name).build().find().associate { artist ->
                        artist.spotifyUrl.toString() to artist.id // Ensures no null values
                    }
            }

            "playlist" -> {
                urlMap = playlistBox!!.query().order(Playlist_.name).build().find()
                    .associate { playlist ->
                        playlist.spotifyUrl.toString() to playlist.id // Ensures no null values
                    }
            }

            "podcast" -> {
                urlMap = podcastBox!!.query().order(Podcast_.name).build().find()
                    .associate { podcast ->
                        podcast.spotifyUrl.toString() to podcast.id // Ensures no null values
                    }
            }

            else -> { }
        }
        return urlMap
    }


    //GET SINGLE:
    //Get entire item:
    fun getArtist(id: Long): Artist {
        return artistBox!!.get(id)
    }

    fun getPlaylist(id: Long): Playlist {
        return playlistBox!!.get(id)
    }

    fun getPodcast(id: Long): Podcast {
        return podcastBox!!.get(id)
    }

    fun getContact(id: Long): Contact {
        return contactBox!!.get(id)
    }

    fun getRoute(id: Long): Route {
        return routeBox!!.get(id)
    }

    //Get single item name:
    fun getItemName(filter: String, id: Long): String {
        when (filter) {
            "artist" -> {
                return artistBox!!.query()
                    .equal(Artist_.id, id)
                    .build()
                    .property(Artist_.name)
                    .findString()
            }
            "playlist" -> {
                return playlistBox!!.query()
                    .equal(Playlist_.id, id)
                    .build()
                    .property(Playlist_.name)
                    .findString()
            }
            "podcast" -> {
                return podcastBox!!.query()
                    .equal(Podcast_.id, id)
                    .build()
                    .property(Podcast_.name)
                    .findString()
            }
            "contact" -> {
                return contactBox!!.query()
                    .equal(Contact_.id, id)
                    .build()
                    .property(Contact_.name)
                    .findString()
            }
            "route" -> {
                return routeBox!!.query()
                    .equal(Route_.id, id)
                    .build()
                    .property(Route_.name)
                    .findString()
            }
            else -> return ""
        }
    }


    //Get single item, only key ItemInfoUse info:
    fun getItemInfoUse(filter: String, id: Long): ItemInfoUse {
        when (filter) {
            "artist" -> {
                val item = artistBox!!.get(id)
                return ItemInfoUse(
                    type = filter,
                    name = item.name,
                    url = item.spotifyUrl,
                    defaultKey = item.defaultPlay,
                    playLinks = item.playLinks,
                )
            }

            "playlist" -> {
                val item = playlistBox!!.get(id)
                return ItemInfoUse(
                    type = filter,
                    name = item.name,
                    detail = item.owner,
                    url = item.spotifyUrl
                )
            }

            "podcast" -> {
                val item = podcastBox!!.get(id)
                return ItemInfoUse(
                    type = filter,
                    name = item.name,
                    detail = item.publisher,
                    url = item.spotifyUrl,
                    language = try {
                        item.languages[0]
                    } catch (e: Exception) {
                        "en"
                    },
                )
            }

            "contact" -> {
                val item = contactBox!!.get(id)
                return ItemInfoUse(
                    type = filter,
                    name = item.name,
                    language = item.language,
                    defaultKey = item.defaultPhone,
                    phoneSets = item.phoneSets
                )
            }

            "route" -> {
                val item = routeBox!!.get(id)
                val language = prefs.routeLanguage   //TODO: Default only!
                return ItemInfoUse(
                    type = filter,
                    name = item.name,
                    detail = buildRouteSubtitle(item, viewLanguage = language),
                    language = language,
                    url = fulfillmentUtils.buildRouteUrlFromLibraryItem(item)
                )
            }

            else -> return ItemInfoUse()
        }
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

    //Update / store Podcast to DB:
    fun storePodcast(context: Context, item: Podcast) {
        try {
            podcastBox!!.put(item)
            Log.d(TAG, "Podcast ${item.id} saved!")
            Toast.makeText(context, "Podcast saved!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Podcast ${item.id} not saved!", e)
            Toast.makeText(context, "ERROR: Podcast not saved!", Toast.LENGTH_LONG).show()
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

    //Update / store Route to DB:
    fun storeRoute(context: Context, item: Route) {
        try {
            routeBox!!.put(item)
            Log.d(TAG, "Route ${item.id} saved!")
            Toast.makeText(context, "Route saved!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Route ${item.id} not saved!", e)
            Toast.makeText(context, "ERROR: Route not saved!", Toast.LENGTH_LONG).show()
        }
    }


    //UTILS:
    fun getCollectionSize(filter: String): Long {
        return when (filter) {
            "artist" -> artistBox!!.count()
            "playlist" -> playlistBox!!.count()
            "podcast" -> podcastBox!!.count()
            "contact" -> contactBox!!.count()
            "route" -> routeBox!!.count()
            else -> 0
        }
    }


    //DB DELETE:
    //Delete single item:
    fun deleteLibraryItem(context: Context, filter: String, id: Long) {
        try {
            when (filter) {
                "artist" -> artistBox!!.remove(id)
                "playlist" -> playlistBox!!.remove(id)
                "podcast" -> podcastBox!!.remove(id)
                "contact" -> contactBox!!.remove(id)
                "route" -> routeBox!!.remove(id)
            }
            Log.d(TAG, "Deleted $filter item $id!")
            Toast.makeText(context, "${filter.replaceFirstChar { it.uppercase() }} deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ERROR in deleting $filter item!", Toast.LENGTH_LONG).show()
            Log.w(TAG, "ERROR in deleting ${filter.replaceFirstChar { it.uppercase() }}: $id. ", e)
        }
    }

    //Delete all:
    fun deleteLibrary(context: Context, filter: String) {
        try {
            when (filter) {
                "artist" -> artistBox!!.removeAll()
                "playlist" -> playlistBox!!.removeAll()
                "podcast" -> podcastBox!!.removeAll()
                "contact" -> contactBox!!.removeAll()
                "route" -> routeBox!!.removeAll()
            }
            Log.d(TAG, "Deleted ${filter} library!")
            Toast.makeText(context, "${filter.replaceFirstChar { it.uppercase() }} library deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR in deleting $filter library. ", e)
            Toast.makeText(context, "ERROR in deleting ${filter.replaceFirstChar { it.uppercase() }} library!", Toast.LENGTH_LONG).show()
        }
    }


    //Prepare cached Library file to send:
    fun buildLibraryToSend(context: Context, filter: String): String {
        var filename = ""
        try {
            filename = "library_${filter}s.json"
            val cachedFile = File(context.cacheDir, filename)
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
                "podcast" -> {
                    val libArray = podcastBox!!.query().order(Podcast_.name).build().find().toList()
                    cachedFile.writeText(Json.encodeToString(libArray))
                }
                "contact" -> {
                    val libArray = contactBox!!.query().order(Contact_.name).build().find().toList()
                    cachedFile.writeText(Json.encodeToString(libArray))
                }
                "route" -> {
                    val libArray = routeBox!!.query().order(Route_.name).build().find().toList()
                    cachedFile.writeText(Json.encodeToString(libArray))
                }
            }
            Log.d(TAG, "Successfully prepared and cached consolidated Library for ${filter}s.")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot prepare or cache consolidated Library for ${filter}s!, ", e)
        }
        return filename
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