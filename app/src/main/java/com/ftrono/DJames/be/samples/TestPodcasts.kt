package com.ftrono.DJames.be.samples

import com.ftrono.DJames.be.database.Podcast


val testPodcasts = listOf<Podcast>(
    Podcast(
        id = 0,
        name = "3 Fattori",
        aliases = mutableListOf("3 fattori"),
        publisher = "Sky Tg24",
        imageUrl = "https://i.scdn.co/image/ab6765630000ba8aaaea81d4152fb611170f0f7f",
        spotifyUrl = "https://open.spotify.com/show/2lrRJEThluTsQVgEqCeR9X",
        description = "3 Fattori è il podcast nato dalla rubrica di Mariangela Pira, giornalista di Sky Tg24, su Linkedin. Ogni venerdì esce un nuovo episodio. Il proposito è quello di raccontare a chiunque, anche al lattaio dell'Ohio, ciò che accade in finanza, economia e in geopolitica economica. In ogni puntata, con un linguaggio chiaro e semplice, vengono snocciolate le tematiche di attualità: dall'energia all'inflazione, dalle banche centrali ai programmi economici del governo, dal commercio globale a ciò che banalmente accade alle nostre imprese e nel nostro quotidiano. Vi aspettiamo.",
        languages = mutableListOf("it")
    )
)