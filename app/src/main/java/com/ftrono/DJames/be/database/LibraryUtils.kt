package com.ftrono.DJames.be.database

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.curLibrarySize
import com.ftrono.DJames.application.gMapsLinkFormat
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.libHeads
import com.ftrono.DJames.application.libSectionIdentifier
import com.ftrono.DJames.application.libraryBox
import com.ftrono.DJames.be.samples.testLibrary
import io.objectbox.Property
import io.objectbox.query.QueryBuilder.StringOrder
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.internal.toImmutableList
import java.io.File


class LibraryUtils {
    private val TAG = LibraryUtils::class.java.simpleName

    // COMMON:
    fun getAll(filter: String): List<LibraryItem> {
        try {
            // TODO: Add Source filter:
            return libraryBox!!.query(LibraryItem_.type.equal(filter)).order(LibraryItem_.name).build().find()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot get All ${filter}s! ", e)
            return listOf()
        }
    }

    fun getCollectionSize(filter: String): Long {
        try {
            // TODO: Add Source filter:
            return libraryBox!!.query(LibraryItem_.type.equal(filter)).build().count()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot count ${filter}s! ", e)
            return 0
        }
    }


    //GET SINGLE:
    //Get single by ID:
    fun getLibItemById(id: Long): LibraryItem {
        return libraryBox!!.get(id)
    }

    //Get single item name:
    fun getLibItemName(id: Long): String {
        return libraryBox!!.query()
            .equal(LibraryItem_.id, id)
            .build()
            .property(LibraryItem_.name)
            .findString()
    }


    // REFRESH:
    //Get List of ItemInfoView items:
    fun refreshLibrary(filter: String, preview: Boolean = false, addHeaders: Boolean = true): List<String> {
        //1) Load library:
        var library = listOf<String>()
        try {
            library = if (preview) {
                testLibrary.filter { it.type == filter }.sortedBy { it.name }
            } else {
                getAll(filter)
            }.map { item ->
                //Cast value to String to allow storing into MutableState:
                Json.encodeToString(
                    ItemInfoView(
                        id = item.id,
                        name = item.name,
                        imageUrl = item.imageUrl,
                        aliases = item.aliases,
                        detail = if (filter == "contact") {
                            item.phoneSet!!.phone
                        } else if (filter == "place") {
                            item.address!!.town
                        } else ""
                    )
                )
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
                listWithHeaders.add("$libSectionIdentifier$letter")
            }
            listWithHeaders.add(itemJson)
        }
        return listWithHeaders.toImmutableList()
    }

    //Get aliases Map in format {"id": ["aliases", ...]}:
    fun getAliasesMap(filter: String): Map<Long, List<String>> {
        return getAll(filter).associate { item ->
            item.id to (item.aliases ?: emptyList()) // Ensures no null values
        }
    }

    //Get URL Map in format {"url": "id"}:
    fun getUrlMap(filter: String): Map<String, Long> {
        return getAll(filter).associate { item ->
            item.url.toString() to item.id // Ensures no null values
        }
    }

    //Place: build navigation URL from Library Place item:
    fun buildPlaceUrlFromLibraryItem(item: Address?): String {
        var url = gMapsLinkFormat
        if (item != null) {
            //Destination:
            var fullDestination = listOf(
                item.placeName,
                item.street,
                item.number,
                item.zip,
                item.town,
                item.province
            ).joinToString(" ").trim()
            return url + fullDestination.replace(" ", "+")
        } else return ""
    }

    //Place: build navigation URL from temp ItemInfo item:
    fun buildPlaceUrlFromItemInfo(item: LibraryItem): String {
        var url = gMapsLinkFormat
        url = url + item.name.replace(" ", "+").trim() + "/"
        return url
    }


    //DB UPDATE:
    //Update / store item to DB:
    fun storeLibItem(context: Context, item: LibraryItem) {
        val type = utils.capitalizeWords(item.type)
        try {
            libraryBox!!.put(item)
            Log.d(TAG, "$type ${item.id} saved!")
            Toast.makeText(context, "$type saved!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: $type ${item.id} not saved!", e)
            Toast.makeText(context, "ERROR: $type not saved!", Toast.LENGTH_LONG).show()
        }
    }


    //DB DELETE:
    //Delete single item:
    fun deleteLibItem(context: Context, filter: String, id: Long) {
        try {
            libraryBox!!.remove(id)
            Log.d(TAG, "Deleted $filter item $id!")
            Toast.makeText(context, "${utils.capitalizeWords(filter)} deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ERROR in deleting $filter item!", Toast.LENGTH_LONG).show()
            Log.w(TAG, "ERROR in deleting ${utils.capitalizeWords(filter)}: $id. ", e)
        }
    }

    //Delete all:
    fun deleteLibrary(context: Context, filter: String) {
        try {
            // TODO: add Source to filter:
            var itemsToDelete = libraryBox!!
                .query(LibraryItem_.type.equal(filter))
                .build()
                .find()
            libraryBox!!.remove(itemsToDelete)
            Log.d(TAG, "Deleted ${filter} library!")
            Toast.makeText(context, "${utils.capitalizeWords(filter)} library deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR in deleting $filter library. ", e)
            Toast.makeText(context, "ERROR in deleting ${utils.capitalizeWords(filter)} library!", Toast.LENGTH_LONG).show()
        }
    }


    // SEND:
    //Prepare Library JSON string for export:
    fun serializeLibrary(filter: String): String {
        var jsonContent = ""
        try {
            //Populate cached array & store to cached file:
            jsonContent = Json.encodeToString(getAll(filter))
            Log.d(TAG, "Successfully serialized Library for ${filter}s.")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot serialize Library for ${filter}s!, ", e)
        }
        return jsonContent
    }

    //Prepare cached Library file to send:
    fun buildFileToSend(context: Context, filter: String, jsonContent: String): String {
        var filename = ""
        try {
            filename = "library_${filter}s.json"
            val cachedFile = File(context.cacheDir, filename)
            cachedFile.writeText(jsonContent)
            Log.d(TAG, "Successfully prepared and cached Library for ${filter}s.")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot prepare or cache Library for ${filter}s!, ", e)
        }
        return filename
    }

    //Clean consolidated export cache (from directory, no DB):
    fun cleanLibraryCache(context: Context) {
        for (head in libHeads) {
            try {
                File(context.cacheDir, "library_${head}s.json").delete()
            } catch (e: Exception) {
                Log.w(TAG, "No cached library_$head.json file to delete!")
            }
        }
    }


    // IMPORT:
    //Common importer:
    fun importItems(
        items: List<LibraryItem>,
        stringProperty: Property<LibraryItem>,
        tag: String
    ): Int {
        var count = 0
        for (item in items) {
            try {
                //Check duplicates to update:
                val duplicates = libraryBox!!.query(stringProperty.equal(item.name, StringOrder.CASE_INSENSITIVE)).build().find()
                if (duplicates.isNotEmpty()) {
                    item.id = duplicates[0].id
                } else {
                    item.id = 0
                }
                //Insert / update:
                libraryBox!!.put(item)
                count++
            } catch (e: Exception) {
                Log.w(tag, "Cannot import item!", e)
            }
        }
        return count
    }

    //Import Library from file:
    fun importLibrary(context: Context, filter: String, jsonContent: String) {
        try {
            var c = 0
            var tot = 0
            val items = Json.decodeFromString<List<LibraryItem>>(jsonContent)
            tot = items.size
            c = importItems(items, LibraryItem_.name, TAG)
            Log.d(TAG, "Imported $c / $tot ${filter}s!")
            Toast.makeText(context, "Imported $c / $tot ${filter}s!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Invalid file! ", e)
            Toast.makeText(context, "ERROR: Invalid file!", Toast.LENGTH_LONG).show()
        }
    }
}