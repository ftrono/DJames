package com.ftrono.DJames.be.database

import com.ftrono.DJames.be.models.ActionType
import com.ftrono.DJames.be.models.ActionTypeConverter
import com.ftrono.DJames.be.models.JsonConverter
import com.ftrono.DJames.be.models.JsonListConverter
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Convert
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString


//SUPPORT CLASSES:
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
    var matchConfirmed: String = "",   //after match (for contacts / places)
    var artistExtracted: String = "",   //original
    var artistConfirmed: String = "",   //after match (store to search Spotify)
    var contextType: String = "",
    var contextExtracted: String = "",
    var contextConfirmed: String = ""
)


// SPOTIFY PLAYABLES:
@Serializable
data class SpotifyContext(
    var type: String = "",      // Populate only if type is not the original album!
    var id: String = "",
    var name: String = "",
)

@Serializable
data class SpotifyArtist(
    var id: String = "",
    var name: String = "",
)

@Serializable
data class SpotifyAlbum(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var artists: MutableList<SpotifyArtist> = mutableListOf<SpotifyArtist>(),
)

@Serializable
data class SpotifyPlaylist(
    var id: String = "",
    var name: String = "",
    var owner: String = "",
)

@Serializable
data class SpotifyTrack(
    var id: String = "",
    var name: String = "",
    var artists: MutableList<SpotifyArtist> = mutableListOf<SpotifyArtist>(),
    var album: SpotifyAlbum? = null,
    var saved: Boolean = false,   // Only for search
    var context: SpotifyContext? = null,
)

@Serializable
data class SpotifyPodcast(
    var id: String = "",
    var name: String = "",
    var publisher: String = "",
)

@Serializable
data class SpotifyEpisode(
    var id: String = "",
    var name: String = "",
    var releaseDate: String = "",
    var fullyPlayed: Boolean = false,
    var resumePositionMs: Int = 0,
    var languages: MutableList<String> = mutableListOf(),
    var podcast: SpotifyPodcast? = null,
)


@Serializable
data class SpotifyPlayable(
    var id: String = "",
    var type: String = "",
    var track: SpotifyTrack? = null,
    var artist: SpotifyArtist? = null,
    var artists: MutableList<SpotifyArtist> = mutableListOf<SpotifyArtist>(),
    var album: SpotifyAlbum? = null,
    var playlist: SpotifyPlaylist? = null,
    var episode: SpotifyEpisode? = null,
    var podcast: SpotifyPodcast? = null,
)

class SpotifyTrackConverter : JsonConverter<SpotifyTrack>(SpotifyTrack.serializer())
class SpotifyArtistConverter : JsonListConverter<SpotifyArtist>(SpotifyArtist.serializer())
class SpotifyAlbumConverter : JsonConverter<SpotifyAlbum>(SpotifyAlbum.serializer())
class SpotifyPlaylistConverter : JsonConverter<SpotifyPlaylist>(SpotifyPlaylist.serializer())
class SpotifyPodcastConverter : JsonConverter<SpotifyPodcast>(SpotifyPodcast.serializer())
class SpotifyEpisodeConverter : JsonConverter<SpotifyEpisode>(SpotifyEpisode.serializer())
class SpotifyContextConverter : JsonConverter<SpotifyContext>(SpotifyContext.serializer())


@Serializable
data class SpotifyMatchModel(
    var pos: Int = 0,
    var score: Int = 0,
    var nameSetSimilarity: Int = 0,
    var namePartialSimilarity: Int = 0,
    var nameFullSimilarity: Int = 0,
    var detailSetSimilarity: Int = 0,
    var detailPartialSimilarity: Int = 0,
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

class SpotifyMatchModelConverter : JsonListConverter<SpotifyMatchModel>(SpotifyMatchModel.serializer())


//ENTITIES:
/*
************************************
   ### GOLDEN RULE: ###
   - User messages are saved by Dispatcher (need NLP results for language recognition!)
   - AI messages are saved by:
      - TTSReader (if voice)
      - ChatManager (if chat)
*/
@Serializable
@Entity
data class Message(
    //Primary key:
    @Id var id: Long = 0,
    var timestamp: Long = 0,
    var datetime: String = "",   // NOTE: for export only!
    var appVersion: String = "",
    var fromVoice: Boolean = false,
    var fromUser: Boolean = false,
    var text: String = "",
    var langCode: String = "",
    var requestIntent: String = "",   // IntentName from the original request
    var isStart: Boolean = false,
    var starterId: Long = 0,
    //Attachments management:
    // - requestIntent -> in user message;
    // - all other -> in the AI reply
    @Convert(converter = ActionTypeConverter::class, dbType = String::class)
    var actionType: ActionType? = null,   //"call", ""
    @Convert(converter = AttachmentsConverter::class, dbType = String::class)
    var attachments: Attachments = Attachments()
)


@Serializable
data class Attachments(
    var matchScore: Int = 0,
    var playedExternally: Boolean = false,
    var contextError: Boolean = false,

    @Convert(converter = NlpQueryModelConverter::class, dbType = String::class)
    var nlpQueries: MutableList<NlpQueryModel>? = null,

    @Convert(converter = ExtractorInfoConverter::class, dbType = String::class)
    var nlpExtractor: ExtractorInfo? = null,

    @Convert(converter = SpotifyQueryModelConverter::class, dbType = String::class)
    var spotifyQueries: MutableList<SpotifyQueryModel>? = null,

    @Convert(converter = SpotifyPlayableConverter::class, dbType = String::class)
    var spotifyPlay: SpotifyPlayable? = null,

    @Convert(converter = LibraryItemConverter::class, dbType = String::class)
    var usable: LibraryItem? = null,
)


class AttachmentsConverter : JsonConverter<Attachments>(Attachments.serializer())
class ExtractorInfoConverter : JsonConverter<ExtractorInfo>(ExtractorInfo.serializer())
class SpotifyPlayableConverter : JsonConverter<SpotifyPlayable>(SpotifyPlayable.serializer())
class NlpQueryModelConverter : JsonListConverter<NlpQueryModel>(NlpQueryModel.serializer())
class SpotifyQueryModelConverter : JsonListConverter<SpotifyQueryModel>(SpotifyQueryModel.serializer())
