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
data class LogViewInfo(
    var id: Long = 0,
    var intentName: String = "",
    var head: String = "",
    var main: String = "",
    var detail: String = ""
)


@Serializable
data class KeyLogInfo(
    var intentName: String = "",
    var queryText: String = "",
    var libScore: Int = 0,
    var bestScore: Int = 0,
    var playedExternally: Boolean = false,
    var contextError: Boolean = false,
)


@Serializable
data class Message(
    var datetime: String = "",
    var type: String = "",   // Either: "ai", "user", "tool"
    var text: String = "",
    var langCode: String = "",
)


@Serializable
data class NlpQueryModel(
    var language: String = "en",   // AudioLanguage
    var queryText: String = "",
    var intentName: String = "Fallback",
    var artists: MutableList<String> = mutableListOf<String>(),
    var genre: String = "",
    var reqLanguage: String = "",
)


@Serializable
data class ExtractorInfo(
    var reqLanguage: String = "",
    var playType: String = "",
    var matchExtracted: String = "",   //main text
    var matchConfirmed: String = "",   //after match (for contacts / routes)
    var artistExtracted: String = "",   //original
    var artistConfirmed: String = "",   //after match (store to search Spotify)
    var contextType: String = "",
    var contextExtracted: String = "",
    var contextConfirmed: String = ""
)


@Serializable
data class SpotifyPlayable(
    //Main:
    var type: String = "",
    var id: String = "",
    var name: String = "",
    //Artists:
    var artistsIds: MutableList<String> = mutableListOf<String>(),
    var artistsNames: MutableList<String> = mutableListOf<String>(),
    //Albums:
    var albumType: String = "",
    var albumName: String = "",
    var albumId: String = "",
    //Podcasts or custom:
    var contextType: String = "",   // only if "playlist" or "podcast"
    var contextUri: String = "",
    var contextName: String = "",
    //Episodes:
    var publisher: String = "",
    var releaseDate: String = "",
    var languages: MutableList<String> = mutableListOf(""),
    var fullyPlayed: Boolean = false,
    var resumePositionMs: Int = 0,
    //Various:
    var owner: String = "",
    var saved: Boolean = false,
)


@Serializable
data class SpotifyMatchModel(
    var pos: Int = 0,
    var score: Int = 0,
    var nameSetSimilarity: Int = 0,
    var namePartialSimilarity: Int = 0,
    var nameFullSimilarity: Int = 0,
    var artistSetSimilarity: Int = 0,
    var artistPartialSimilarity: Int = 0,
    @Convert(converter = SpotifyPlayableConverter::class, dbType = String::class)
    var playable: SpotifyPlayable = SpotifyPlayable()
)


@Serializable
data class SpotifyQueryModel(
    var type: String = "",
    var url: String = "",
    var numItems: Int = 0,
    @Convert(converter = SpotifyMatchModelConverter::class, dbType = String::class)
    var spotifyMatches: MutableList<SpotifyMatchModel> = mutableListOf<SpotifyMatchModel>()
)

class SpotifyMatchModelConverter : PropertyConverter<MutableList<SpotifyMatchModel>, String> {
    override fun convertToEntityProperty(databaseValue: String?): MutableList<SpotifyMatchModel> {
        return Json.decodeFromString(databaseValue ?: "[]")
    }

    override fun convertToDatabaseValue(entityProperty: MutableList<SpotifyMatchModel>?): String {
        return Json.encodeToString(entityProperty ?: mutableListOf(SpotifyMatchModel()))
    }
}


//ENTITIES:
@Serializable
@Entity
data class HistoryLog(
    //Primary key:
    @Id var id: Long = 0,
    var datetime: String = "",
    var appVersion: String = "",

    @Convert(converter = KeyLogInfoConverter::class, dbType = String::class)
    var keyInfo: KeyLogInfo = KeyLogInfo(),

    @Convert(converter = MessageConverter::class, dbType = String::class)
    var messages: MutableList<Message> = mutableListOf<Message>(),

    @Convert(converter = NlpQueryModelConverter::class, dbType = String::class)
    var nlpQueries: MutableList<NlpQueryModel> = mutableListOf<NlpQueryModel>(),

    @Convert(converter = ExtractorInfoConverter::class, dbType = String::class)
    var nlpExtractor: ExtractorInfo = ExtractorInfo(),

    @Convert(converter = SpotifyQueryModelConverter::class, dbType = String::class)
    var spotifyQueries: MutableList<SpotifyQueryModel> = mutableListOf<SpotifyQueryModel>(),

    @Convert(converter = SpotifyPlayableConverter::class, dbType = String::class)
    var spotifyPlay: SpotifyPlayable = SpotifyPlayable(),

    @Convert(converter = ItemInfoUseConverter::class, dbType = String::class)
    var usable: ItemInfoUse = ItemInfoUse(),
)

class KeyLogInfoConverter : PropertyConverter<KeyLogInfo, String> {
    override fun convertToEntityProperty(databaseValue: String?): KeyLogInfo {
        return Json.decodeFromString(databaseValue ?: "{}")
    }

    override fun convertToDatabaseValue(entityProperty: KeyLogInfo?): String {
        return Json.encodeToString(entityProperty ?: KeyLogInfo())
    }
}

class MessageConverter : PropertyConverter<MutableList<Message>, String> {
    override fun convertToEntityProperty(databaseValue: String?): MutableList<Message> {
        return Json.decodeFromString(databaseValue ?: "[]")
    }

    override fun convertToDatabaseValue(entityProperty: MutableList<Message>?): String {
        return Json.encodeToString(entityProperty ?: mutableListOf(Message()))
    }
}

class NlpQueryModelConverter : PropertyConverter<MutableList<NlpQueryModel>, String> {
    override fun convertToEntityProperty(databaseValue: String?): MutableList<NlpQueryModel> {
        return Json.decodeFromString(databaseValue ?: "[]")
    }

    override fun convertToDatabaseValue(entityProperty: MutableList<NlpQueryModel>?): String {
        return Json.encodeToString(entityProperty ?: mutableListOf(NlpQueryModel()))
    }
}

class ExtractorInfoConverter : PropertyConverter<ExtractorInfo, String> {
    override fun convertToEntityProperty(databaseValue: String?): ExtractorInfo {
        return Json.decodeFromString(databaseValue ?: "{}")
    }

    override fun convertToDatabaseValue(entityProperty: ExtractorInfo?): String {
        return Json.encodeToString(entityProperty ?: ExtractorInfo())
    }
}

class SpotifyQueryModelConverter : PropertyConverter<MutableList<SpotifyQueryModel>, String> {
    override fun convertToEntityProperty(databaseValue: String?): MutableList<SpotifyQueryModel> {
        return Json.decodeFromString(databaseValue ?: "[]")
    }

    override fun convertToDatabaseValue(entityProperty: MutableList<SpotifyQueryModel>?): String {
        return Json.encodeToString(entityProperty ?: mutableListOf(SpotifyQueryModel()))
    }
}

class SpotifyPlayableConverter : PropertyConverter<SpotifyPlayable, String> {
    override fun convertToEntityProperty(databaseValue: String?): SpotifyPlayable {
        return Json.decodeFromString(databaseValue ?: "{}")
    }

    override fun convertToDatabaseValue(entityProperty: SpotifyPlayable?): String {
        return Json.encodeToString(entityProperty ?: SpotifyPlayable())
    }
}

class ItemInfoUseConverter : PropertyConverter<ItemInfoUse, String> {
    override fun convertToEntityProperty(databaseValue: String?): ItemInfoUse {
        return Json.decodeFromString(databaseValue ?: "{}")
    }

    override fun convertToDatabaseValue(entityProperty: ItemInfoUse?): String {
        return Json.encodeToString(entityProperty ?: ItemInfoUse())
    }
}
