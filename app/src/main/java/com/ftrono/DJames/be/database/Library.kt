package com.ftrono.DJames.be.database

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Convert
import io.objectbox.converter.PropertyConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString


//SUPPORT CLASSES:
@Serializable
data class PlayLink(
    var name: String,
    var owner: String,
    var spotifyUrl: String
)

@Serializable
data class PhoneSet(
    var prefix: String,
    var phone: String
)

@Serializable
data class Address(
    var address: String = "",
    var number: String = "",
    var placeName: String = "",
    var town: String = "",
    var zip: String = "",
    var province: String = "",
    var country: String = "Italy",
)

//ITEM INFO:
@Serializable
data class ItemInfoView(
    var name: String = "",
    var imageUrl: String = "",
    var aliases: MutableList<String> = mutableListOf(),
    var detail: String = "",
)

@Serializable
data class ItemInfoUse(
    var name: String,
    var detail: String = "",
    var language: String = "",
    var url: String = "",
    var defaultKey: String = "",
    var playLinks: MutableMap<String, PlayLink> = mutableMapOf(),
    var phoneSets: MutableMap<String, PhoneSet> = mutableMapOf(),
)

//ENTITIES:
@Serializable
@Entity
data class Artist(
    //Primary key:
    @Id var id: Long = 0,
    var name: String = "",
    var aliases: MutableList<String> = mutableListOf(""),
    var spotifyUrl: String = "",
    var country: String = "",
    var genres: MutableList<String> = mutableListOf(""),
    var imageUrl: String = "",
    var defaultPlay: String = "artist",
    var playLinksJson: String = Json.encodeToString(
        mutableMapOf(
            "spotify_this_is" to PlayLink(
                name = "",
                owner = "Spotify",
                spotifyUrl = ""
            )
        ),
    ),
) {
    var playLinks: MutableMap<String, PlayLink>
        get() = Json.decodeFromString<Map<String, PlayLink>>(playLinksJson).toMutableMap()
        set(value) {
            playLinksJson = Json.encodeToString(value)
        }
}

@Serializable
@Entity
data class Playlist(
    //Primary key:
    @Id var id: Long = 0,
    var name: String = "",
    var aliases: MutableList<String> = mutableListOf(""),
    var owner: String = "",
    var imageUrl: String = "",
    var spotifyUrl: String = "",
)

@Serializable
@Entity
data class Contact(
    //Primary key:
    @Id var id: Long = 0,
    var name: String = "",
    var aliases: MutableList<String> = mutableListOf(""),
    var language: String = "",
    var defaultPhone: String = "personal",
    var phoneSetsJson: String = Json.encodeToString(
        mutableMapOf(
            "personal" to PhoneSet(
                prefix = "",
                phone = ""
            )
        ),
    ),
) {
    var phoneSets: MutableMap<String, PhoneSet>
        get() = Json.decodeFromString<Map<String, PhoneSet>>(phoneSetsJson).toMutableMap()
        set(value) {
            phoneSetsJson = Json.encodeToString(value)
        }
}

@Serializable
@Entity
data class Route(
    //Primary key:
    @Id var id: Long = 0,
    var name: String = "",
    var aliases: MutableList<String> = mutableListOf(""),

    @Convert(converter = AddressConverter::class, dbType = String::class)
    var destination: Address = Address(),

    @Convert(converter = AddressConverter::class, dbType = String::class)
    var via: Address = Address(),
)


class AddressConverter : PropertyConverter<Address, String> {
    override fun convertToEntityProperty(databaseValue: String?): Address {
        return Json.decodeFromString(databaseValue ?: "{}")
    }

    override fun convertToDatabaseValue(entityProperty: Address?): String {
        return Json.encodeToString(entityProperty ?: Address())
    }
}
