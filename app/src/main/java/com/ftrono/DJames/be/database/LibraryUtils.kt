package com.ftrono.DJames.be.database

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.curLibrarySize
import com.ftrono.DJames.application.gMapsLinkFormat
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.libCats
import com.ftrono.DJames.application.libSectionIdentifier
import com.ftrono.DJames.application.libSubcats
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
    // Get library name:
    fun getLibName(cat: String, subcat: String = "", plural: Boolean = false, lowercase: Boolean = false): String {
        return if (cat == "spotify") {
            if (subcat == "") {
                if (plural) "Spotify links" else "Spotify link"
            } else {
                if (plural) "Spotify ${subcat}s" else "Spotify ${subcat}"
            }
        } else if (lowercase) {
            if (plural) "${cat}s" else cat
        } else {
            if (plural) "${utils.capitalizeWords(cat)}s" else utils.capitalizeWords(cat)
        }
    }

    fun getAll(cat: String = "", subcat: String = "", preview: Boolean = false): List<LibraryItem> {
        try {
            if (preview) {
                if (subcat == "") {
                    return testLibrary.filter { it.source == cat }.sortedBy { it.name }
                } else {
                    return testLibrary.filter { it.source == cat && it.type == subcat }.sortedBy { it.name }
                }
            } else {
                if (cat == "") {
                    return libraryBox!!.query(LibraryItem_.type.equal(subcat))
                        .order(LibraryItem_.name)
                        .build().find()
                } else if (subcat == "") {
                    return libraryBox!!.query(LibraryItem_.source.equal(cat))
                        .order(LibraryItem_.name)
                        .build().find()
                } else {
                    return libraryBox!!.query(LibraryItem_.source.equal(cat).and(LibraryItem_.type.equal(subcat)))
                        .order(LibraryItem_.name)
                        .build().find()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot get All items with cat: $cat and subcat: $subcat! ", e)
            return listOf()
        }
    }

    fun getCollectionSize(filter: String, subcat: String = ""): Long {
        try {
            if (subcat == "") {
                return libraryBox!!.query(LibraryItem_.source.equal(filter)).build().count()
            } else {
                return libraryBox!!.query(LibraryItem_.source.equal(filter).and(LibraryItem_.type.equal(subcat))).build().count()
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot count ${filter}s! ", e)
            return 0
        }
    }


    //GET SINGLE:
    //Get single by ID:
    fun getLibItemById(id: Long, preview: Boolean = false): LibraryItem {
        if (preview) {
            return testLibrary.filter { it.id == id }[0]
        } else {
            return libraryBox!!.get(id)
        }
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
    fun refreshLibrary(cat: String, subcat: String = "", preview: Boolean = false): List<String> {
        try {
            //1) LOAD LIBRARY:
            var idsMap = mapOf<Long, String>()
            var filtered = listOf<LibraryItem>()
            var ids = listOf<Long>()
            var names = listOf<String>()

            //Load library IDs:
            if (preview) {
                if (subcat == "") {
                    filtered = testLibrary.filter { it.source == cat }.sortedBy { it.name }
                } else {
                    filtered = testLibrary.filter { it.source == cat && it.type == subcat }.sortedBy { it.name }
                }
                ids = filtered.map { it.id }
                names = filtered.map { it.name }

            } else {
                // TODO (TEMP): Cannot sort in ObjectBox directly in PropertyQuery!
                // SO: First map ids to names, then sort and finally extract sorted ids only:
                val queryCond = if (subcat == "") {
                    LibraryItem_.source.equal(cat)
                } else {
                    LibraryItem_.source.equal(cat).and(LibraryItem_.type.equal(subcat))
                }

                ids = libraryBox!!.query(queryCond)
                    .build()
                    .property(LibraryItem_.id)
                    .findLongs()
                    .toList()
                names = libraryBox!!.query(queryCond)
                    .build()
                    .property(LibraryItem_.name)
                    .findStrings()
                    .toList()
            }
            // Sort:
            idsMap = ids.zip(names).sortedBy { it.second }.toMap()

            //2) UPDATE LIBRARY SIZE (IMPORTANT - for signs):
            curLibrarySize.postValue(idsMap.size)

            //3) ADD HEADERS & CONVERT TO IDS LIST:
            return addLetterHeaders(idsMap)

        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot refresh Library for items with cat: $cat and subcat: $subcat! ", e)
            curLibrarySize.postValue(0)
            return listOf<String>()
        }
    }

    //Get key List with headers:
    fun addLetterHeaders(idsMap: Map<Long, String>): List<String> {
        val fullStringList = mutableListOf<String>()
        // 1) PARTITION MAP: separate items that start
        val regex = Regex("^[a-zA-Z].*")   // Regex: matches if the first character is a-z or A-Z
        val (alphaPairs, nonAlphaPairs) = idsMap.entries.partition { it.value.matches(regex) }
        val alphaMap = alphaPairs
            .sortedBy { it.value.lowercase() } // sort alphabetically (case-insensitive)
            .associate { it.toPair() }   // Contains names whose first character is a-z or A-Z
        val nonAlphaMap = nonAlphaPairs
            .sortedBy { it.value.lowercase() } // sort alphabetically (case-insensitive)
            .associate { it.toPair() }   // Contains all the remaining items

        // 2) ADD HEADERS:
        var letter = ""
        // First A-Z:
        for (item in alphaMap) {
            val curFirst = item.value.uppercase().first().toString()
            // store letter header:
            if (curFirst != letter) {
                letter = if (utils.isLetters(curFirst)) curFirst.uppercase() else "#"
                fullStringList.add(libSectionIdentifier + letter)
            }
            // store item:
            fullStringList.add(item.key.toString())
        }
        // Then, non A-Z:
        if (nonAlphaMap.isNotEmpty()) {
            fullStringList.add(libSectionIdentifier + "#")
            fullStringList.addAll(nonAlphaMap.keys.map { it.toString() } )
        }
        return fullStringList.toImmutableList()
    }

    //Get aliases Map in format {"id": ["aliases", ...]}:
    fun getAliasesMap(filter: String): Map<Long, List<String>> {
        return getAll(subcat=filter).associate { item ->
            item.id to (item.aliases ?: emptyList()) // Ensures no null values
        }
    }

    //Get URL Map in format {"url": "id"}:
    fun getUrlMap(filter: String): Map<String, Long> {
        return getAll(subcat=filter).associate { item ->
            item.url.toString() to item.id // Ensures no null values
        }
    }

    //Get detail:
    fun getDetail(item: LibraryItem): String {
        //Init aliases:
        val itemAliases = item.aliases.toMutableList()
        itemAliases.removeAt(0)

        return if (item.source == "place") {
            item.address!!.town
        } else if (itemAliases.isNotEmpty()) {
            "\"" + itemAliases.joinToString("\", \"") + "\""
        } else if (item.source == "contact") {
            item.phoneSet!!.phone
        } else if (item.type == "podcast") {
            item.detail
        } else ""
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
    fun deleteLibItem(context: Context, id: Long) {
        try {
            libraryBox!!.remove(id)
            Log.d(TAG, "Deleted item: $id!")
            Toast.makeText(context, "Item deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ERROR in deleting item!", Toast.LENGTH_LONG).show()
            Log.w(TAG, "ERROR in deleting item: $id. ", e)
        }
    }

    //Delete all:
    fun deleteLibrary(context: Context, cat: String, subcat: String = "") {
        val detailStr = getLibName(cat, subcat, plural=true)
        try {
            var itemsToDelete = listOf<LibraryItem>()
            if (subcat == "") {
                itemsToDelete = libraryBox!!
                    .query(LibraryItem_.source.equal(cat))
                    .build()
                    .find()
            } else {
                itemsToDelete = libraryBox!!
                    .query(LibraryItem_.source.equal(cat).and(LibraryItem_.type.equal(subcat)))
                    .build()
                    .find()
            }
            libraryBox!!.remove(itemsToDelete)
            Log.d(TAG, "Deleted $detailStr library!")
            Toast.makeText(context, "$detailStr library deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR in deleting $detailStr library. ", e)
            Toast.makeText(context, "ERROR in deleting $detailStr library!", Toast.LENGTH_LONG).show()
        }
    }


    // SEND:
    //Get export file name:
    fun getExportFileName(cat: String, subcat: String = ""): String {
        return if (cat == "spotify") {
            if (subcat == "") {
                "library_${cat}.json"
            } else {
                "library_${cat}s.json"
            }
        } else {
            "library_${cat}s.json"
        }
    }

    //Prepare Library JSON string for export:
    fun serializeLibrary(cat: String, subcat: String): String {
        var jsonContent = ""
        try {
            //Populate cached array & store to cached file:
            jsonContent = Json.encodeToString(getAll(cat, subcat))
            Log.d(TAG, "Successfully serialized Library for cat: $cat, subcat: $subcat.")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot serialize Library for cat: $cat, subcat: $subcat! ", e)
        }
        return jsonContent
    }

    //Prepare cached Library file to send:
    fun buildFileToSend(context: Context, cat: String, subcat: String = "", jsonContent: String): String {
        var filename = ""
        try {
            filename = getExportFileName(cat, subcat)
            val cachedFile = File(context.cacheDir, filename)
            cachedFile.writeText(jsonContent)
            Log.d(TAG, "Successfully prepared and cached Library for cat: $cat, subcat: $subcat.")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot prepare or cache Library for cat: $cat, subcat: $subcat! ", e)
        }
        return filename
    }

    //Clean consolidated export cache (from directory, no DB):
    fun cleanLibraryCache(context: Context) {
        for (cat in libCats) {
            try {
                if (cat == "spotify") {
                    File(context.cacheDir, getExportFileName(cat)).delete()
                    for (subcat in libSubcats) {
                        File(context.cacheDir, getExportFileName(cat, subcat)).delete()
                    }
                } else {
                    File(context.cacheDir, getExportFileName(cat)).delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "No cached library $cat.json file to delete!")
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
    fun importLibrary(context: Context, jsonContent: String) {
        try {
            var c = 0
            var tot = 0
            val items = Json.decodeFromString<List<LibraryItem>>(jsonContent)
            tot = items.size
            c = importItems(items, LibraryItem_.name, TAG)
            Log.d(TAG, "Imported $c / $tot items!")
            Toast.makeText(context, "Imported $c / $tot items!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Invalid file! ", e)
            Toast.makeText(context, "ERROR: Invalid file!", Toast.LENGTH_LONG).show()
        }
    }
}