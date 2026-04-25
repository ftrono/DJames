package com.ftrono.DJames.be.database

import com.ftrono.DJames.be.models.JsonConverter
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Convert
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString


//SUPPORT CLASSES:
@Serializable
data class PhoneSet(
    var prefix: String = "",
    var phone: String = ""
)

@Serializable
data class Address(
    var street: String = "",
    var number: String = "",
    var placeName: String = "",
    var town: String = "",
    var zip: String = "",
    var province: String = "",
    var country: String = "Italy",
)

//ENTITIES:
@Serializable
@Entity
data class LibraryItem(
    //Primary key:
    @Id var id: Long = 0,
    var uniId: String = "",   // For tools
    var lastUpdated: Long = 0,   // Timestamp
    var matchScore: Int = 0,
    var name: String = "",
    var aliases: MutableList<String> = mutableListOf(),
    var source: String = "",   // i.e.: "local", "spotify", "youtube", "maps", ...
    var type: String = "",   // i.e.: "artist", "playlist", "podcast", "episode", ...
    var url: String = "",
    var imageUrl: String = "",
    var detail: String = "",   // i.e. playlist owner, artist genres, album artists, ...
    var language: String = "",
    @Convert(converter = PhoneSetConverter::class, dbType = String::class)
    var phoneSet: PhoneSet? = null,
    @Convert(converter = AddressConverter::class, dbType = String::class)
    var address: Address? = null,
)

class LibraryItemConverter : JsonConverter<LibraryItem>(LibraryItem.serializer())
class PhoneSetConverter : JsonConverter<PhoneSet>(PhoneSet.serializer())
class AddressConverter : JsonConverter<Address>(Address.serializer())


data class ExtractedItem(
    var response: Int = 400,
    var libItem: LibraryItem = LibraryItem()
)

data class LibMatch(
    var matchScore: Int = 0,
    var matchId: Long = 0L,
    var matchName: String = "",
)
