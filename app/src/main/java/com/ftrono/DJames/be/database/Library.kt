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
data class Episode(
    var name: String,
    var spotifyId: String,
    var releaseDate: String,
    var languages: MutableList<String> = mutableListOf(""),
    var fullyPlayed: Boolean = false,
    var resumePositionMs: Int = 0,
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

// ARTIST:
val defaultPlayLinkStr = "{\"spotify_this_is\": {\"name\": \"\", \"owner\": \"\", \"spotifyUrl\": \"\"}}"
val defaultPlayLinkObj = mutableMapOf(
    "spotify_this_is" to PlayLink(
        name = "",
        owner = "Spotify",
        spotifyUrl = ""
    )
)

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
    @Convert(converter = PlayLinkConverter::class, dbType = String::class)
    var playLinks: MutableMap<String, PlayLink> = defaultPlayLinkObj
)

class PlayLinkConverter : PropertyConverter<MutableMap<String, PlayLink>, String> {
    override fun convertToEntityProperty(databaseValue: String?): MutableMap<String, PlayLink> {
        return Json.decodeFromString(databaseValue ?: defaultPlayLinkStr)
    }

    override fun convertToDatabaseValue(entityProperty: MutableMap<String, PlayLink>?): String {
        return Json.encodeToString(entityProperty ?: defaultPlayLinkObj)
    }
}


// PLAYLIST:
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


// PODCAST:
@Serializable
@Entity
data class Podcast(
    //Primary key:
    @Id var id: Long = 0,
    var name: String = "",
    var aliases: MutableList<String> = mutableListOf(""),
    var publisher: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var spotifyUrl: String = "",
    var languages: MutableList<String> = mutableListOf(""),
)


// CONTACT:
val defaultPhoneSetStr = "{\"personal\": {\"prefix\": \"\", \"phone\": \"\"}}"
val defaultPhoneSetObj = mutableMapOf(
    "personal" to PhoneSet(
        prefix = "+39",
        phone = ""
    )
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
    @Convert(converter = PhoneSetsConverter::class, dbType = String::class)
    var phoneSets: MutableMap<String, PhoneSet> = defaultPhoneSetObj
)

class PhoneSetsConverter : PropertyConverter<MutableMap<String, PhoneSet>, String> {
    override fun convertToEntityProperty(databaseValue: String?): MutableMap<String, PhoneSet> {
        return Json.decodeFromString(databaseValue ?: defaultPhoneSetStr)
    }

    override fun convertToDatabaseValue(entityProperty: MutableMap<String, PhoneSet>?): String {
        return Json.encodeToString(entityProperty ?: defaultPhoneSetObj)
    }
}


// ROUTE:
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
