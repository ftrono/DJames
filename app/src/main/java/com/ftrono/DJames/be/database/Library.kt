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

//ITEM INFO:
@Serializable
data class ItemInfoView(
    var id: Long = 0,
    var name: String = "",
    var imageUrl: String = "",
    var aliases: MutableList<String> = mutableListOf<String>(),
    var detail: String = "",
)

//ENTITIES:
@Serializable
@Entity
data class LibraryItem(
    //Primary key:
    @Id var id: Long = 0,
    var name: String = "",
    var aliases: MutableList<String> = mutableListOf(""),
    var source: String = "",   // i.e.: "local", "spotify", "youtube", "maps", ...
    var type: String = "",   // i.e.: "artist", "playlist", "podcast", "episode", ...
    var url: String = "",
    var imageUrl: String = "",
    var detail: String = "",   // i.e. detail, publisher, ...
    var language: String = "",
    @Convert(converter = PhoneSetConverter::class, dbType = String::class)
    var phoneSet: PhoneSet? = null,
    @Convert(converter = AddressConverter::class, dbType = String::class)
    var address: Address? = null,
)

class PhoneSetConverter : PropertyConverter<PhoneSet?, String> {
    override fun convertToEntityProperty(databaseValue: String?): PhoneSet? {
        return databaseValue?.let { Json.decodeFromString(it) }
    }

    override fun convertToDatabaseValue(entityProperty: PhoneSet?): String? {
        return entityProperty?.let { Json.encodeToString(it) }
    }
}

class AddressConverter : PropertyConverter<Address?, String> {
    override fun convertToEntityProperty(databaseValue: String?): Address? {
        return databaseValue?.let { Json.decodeFromString(it) }
    }

    override fun convertToDatabaseValue(entityProperty: Address?): String? {
        return entityProperty?.let { Json.encodeToString(it) }
    }
}
