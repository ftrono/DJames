package com.ftrono.DJames.be.database

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.curLibrarySize
import com.ftrono.DJames.application.gMapsLinkFormat
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.libCats
import com.ftrono.DJames.application.libraryBox
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.sourceToCatMap
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.be.collections.defaultCollection
import com.ftrono.DJames.be.collections.testLibrary
import io.objectbox.Property
import io.objectbox.query.QueryBuilder.StringOrder
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import me.xdrop.fuzzywuzzy.FuzzySearch
import okhttp3.internal.toImmutableList
import java.io.File
import kotlin.math.roundToInt


class LibraryUtils {
    private val TAG = this::class.java.simpleName

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

    // GET ALL ITEMS:
    fun getAll(cat: String = "", subcat: String = "", preview: Boolean = false): List<LibraryItem> {
        try {
            var libraryItems = mutableListOf<LibraryItem>()
            if (cat == "spotify" && (subcat == "" || subcat == "playlist")) {
                libraryItems.add(defaultCollection)
            }
            if (preview) {
                if (subcat == "") {
                    libraryItems.addAll(testLibrary.filter { it.source == cat })
                } else {
                    libraryItems.addAll(testLibrary.filter { it.source == cat && it.type == subcat })
                }
            } else {
                if (cat == "" && subcat == "") {
                    libraryItems.addAll(
                        libraryBox!!.query().build().find()
                    )
                } else if (cat == "") {
                    libraryItems.addAll(
                        libraryBox!!.query(LibraryItem_.type.equal(subcat))
                        .build().find()
                    )
                } else if (subcat == "") {
                    libraryItems.addAll(
                        libraryBox!!.query(LibraryItem_.source.equal(cat))
                        .build().find()
                    )
                } else {
                    libraryItems.addAll(
                        libraryBox!!.query(LibraryItem_.source.equal(cat).and(LibraryItem_.type.equal(subcat)))
                        .build().find()
                    )
                }
            }
            return libraryItems.sortedBy { it.name.uppercase() }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot get All items with cat: $cat and subcat: $subcat! ", e)
            return listOf()
        }
    }

    // GET SIZE:
    fun getCollectionSize(filter: String, subcat: String = ""): Long {
        try {
            if (subcat == "") {
                return libraryBox!!.query(LibraryItem_.source.equal(filter)).build().count() + if (filter == "spotify") 1 else 0
            } else {
                return libraryBox!!.query(LibraryItem_.source.equal(filter).and(LibraryItem_.type.equal(subcat))).build().count() + if (filter == "spotify" && subcat == "playlist") 1 else 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot count ${filter}s! ", e)
            return 0
        }
    }

    //GET SUBCATS PRESENT IN DB:
    fun getSubcats(cat: String, preview: Boolean = false): List<String> {
        if (preview) {
            return sourceToCatMap[cat]!!
        } else {
            var libCats = mutableListOf<String>()
            libCats.addAll(libraryBox!!.query(LibraryItem_.source.equal(cat))
                .build()
                .property(LibraryItem_.type)
                .distinct()
                .findStrings())
            if (!libCats.contains("playlist")) libCats.add("playlist")
            return libCats.sorted()
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
    fun refreshLibrary(cat: String, subcat: String = "", preview: Boolean = false): List<LibraryItem> {
        try {
            //1) LOAD LIBRARY:
            var libraryItems = getAll(cat, subcat, preview)

            //2) UPDATE LIBRARY SIZE (IMPORTANT - for signs):
            curLibrarySize.postValue(libraryItems.size)

            //3) ADD HEADERS & CONVERT TO IDS LIST:
            return addLetterHeaders(libraryItems)

        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot refresh Library for items with cat: $cat and subcat: $subcat! ", e)
            curLibrarySize.postValue(0)
            return listOf<LibraryItem>()
        }
    }

    //Get key List with headers:
    fun addLetterHeaders(libraryItems: List<LibraryItem>): List<LibraryItem> {
        val fullList = mutableListOf<LibraryItem>()
        // 1) PARTITION MAP: separate items that start
        val regex = Regex("^[a-zA-Z].*")   // Regex: matches if the first character is a-z or A-Z
        var (alphaList, nonAlphaList) = libraryItems.partition { it.name.uppercase().matches(regex) }
        alphaList.sortedBy { it.name }
        nonAlphaList.sortedBy { it.name }

        // 2) ADD HEADERS:
        var letter = ""
        // First A-Z:
        for (item in alphaList) {
            val curFirst = item.name.uppercase().first().toString()
            // store letter header:
            if (curFirst != letter) {
                letter = curFirst
                fullList.add(
                    LibraryItem(
                        type = "header",
                        name = letter
                    )
                )
            }
            // store item:
            fullList.add(item)
        }
        // Then, non A-Z:
        if (nonAlphaList.isNotEmpty()) {
            fullList.add(
                LibraryItem(
                    type = "header",
                    name = "#"
                )
            )
            fullList.addAll(nonAlphaList )
        }
        return fullList.toImmutableList()
    }

    //Get aliases Map in format {"id": ["aliases", ...]}:
    fun getAliasesMap(filter: String): Map<Long, List<String>> {
        return getAll(subcat=filter).associate { item ->
            item.id to (item.aliases ?: emptyList()) // Ensures no null values
        }
    }

    // Search if URL already exists -> get item's DB ID:
    fun getLibIDWithUrl(url: String): Long {
        val res = libraryBox!!.query(LibraryItem_.url.equal(url))
            .build()
            .property(LibraryItem_.id)
            .findLong()
        return res ?: -1L
    }

    //Get detail:
    fun getDetail(item: LibraryItem): String {
        //Init aliases:
        val itemAliases = item.aliases.toMutableList()
        itemAliases.removeAt(0)

        return if (item.source == "place") {
            if (item.address!!.province != "") {
                "(${item.address!!.province}) ${item.address!!.town}"
            } else {
                item.address!!.town
            }
        } else if (itemAliases.isNotEmpty()) {
            "\"" + itemAliases.joinToString("\", \"") + "\""
        } else if (item.source == "contact") {
            item.phoneSet!!.phone
        } else if (item.type != "playlist" && item.type != "artist") {
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


    // MODEL CONVERTER:
    // TODO: WIP!
    fun libItemToPlayable(libItem: LibraryItem): SpotifyPlayable {
        if (libItem.source != "spotify") {
            return SpotifyPlayable()
        } else {
            var playable = SpotifyPlayable(
                id = spotifyUtils.getSpotifyID(libItem.url),
                type = libItem.type
            )
            if (libItem.type == "artist") {
                playable.artist == SpotifyArtist(
                    id = spotifyUtils.getSpotifyID(libItem.url),
                    name = libItem.name
                )
            } else if (libItem.type == "playlist") {
                playable.playlist == SpotifyPlaylist(
                    id = spotifyUtils.getSpotifyID(libItem.url),
                    name = libItem.name,
                    owner = libItem.detail
                )
            } else {
                // TODO
            }
            return playable
        }
    }

    // MATCHER:
    //Match item from user query against user library:
    fun matchLibrary(filter: String, text: String, threshold: Int = midThreshold): LibMatch {
        var libMatch = LibMatch()
        var matchId = -1L
        val libMap = getAliasesMap(filter)
        if (text != "" && libMap.isNotEmpty()) {
            //Init:
            var score = 0
            val listEvalued = text.split(", ")
            val listConfirmed = mutableListOf<Long>()
            val scoresMap = mutableMapOf<Long, Int>()

            //Check each evaluated item:
            for (eval in listEvalued) {
                //Check each artist id:
                for (curId in libMap.keys) {
                    val aliasScores = mutableListOf<Int>()
                    //Check each alias:
                    for (curAlias in libMap[curId]!!) {
                        if (filter == "playlist") {
                            val namePartial = FuzzySearch.partialRatio(curAlias, eval.lowercase())
                            val nameFull = FuzzySearch.ratio(curAlias, eval.lowercase())
                            score = listOf<Int>(namePartial, nameFull).average().roundToInt()
                        } else {
                            score = FuzzySearch.ratio(curAlias, eval.lowercase())
                        }
                        // Log.d(TAG, "LIB CONFIRMATION: COMPARING $curAlias WITH ${eval.lowercase()}, MATCH: $score")
                        aliasScores.add(score)
                    }
                    //Get Max alias score and add globally only if high enough:
                    val maxScore = aliasScores.max()
                    if (!scoresMap.keys.contains(curId) && maxScore >= threshold) {
                        scoresMap[curId] = maxScore
                    }
                }
                if (scoresMap.isNotEmpty()) {
                    //Sort and get highest match:
                    val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap()
                    Log.d(TAG, "SORTED MAP FOR $eval: $sortedScores")
                    listConfirmed.add(sortedScores.keys.toList()[0])
                    val matchScore = sortedScores.values.toList()[0]
                    libMatch.matchScore = matchScore
                }
            }
            //Final:
            if (listConfirmed.isNotEmpty()) {
                Log.d(TAG, "listConfirmed: $listConfirmed")
                matchId = listConfirmed[0]
                Log.d(TAG, "LIBRARY MATCH ID: $matchId, ALIASES: ${libMap[matchId]!!}")
            }
        }
        libMatch.matchId = matchId
        return libMatch
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
            // Keep these queries separate from getAll (don't need default Collection or preview items):
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
        return if (cat == "" && subcat == "") {
            "library.json"
        } else if (cat == "spotify") {
            if (subcat == "") {
                "library_${cat}.json"
            } else if (cat != subcat) {
                "library_${cat}_${subcat}s.json"
            } else {
                "library_${cat}s.json"
            }
        } else {
            "library_${cat}s.json"
        }
    }

    //Prepare Library JSON string for export:
    fun serializeLibrary(cat: String = "", subcat: String = ""): String {
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
                    val subcats = sourceToCatMap[cat]!!
                    for (subcat in subcats) {
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

    //TODO: Use only for DB updates!
    fun updateExistingLibrary() {
        var libItems = libraryBox!!.all
        for (item in libItems) {
            //TODO: update as needed!
            // item.lastUpdated = utils.getCurrentTimestamp()
            libraryBox!!.put(item)
        }
        Log.d(TAG, "Library DB updated!")
    }

}