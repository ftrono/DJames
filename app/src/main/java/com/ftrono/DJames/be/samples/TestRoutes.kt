package com.ftrono.DJames.be.samples

import com.ftrono.DJames.be.database.Address
import com.ftrono.DJames.be.database.Route


val testRoutes = listOf<Route>(
    Route(
        id = 0,
        name = "Casa Amal",
        aliases = mutableListOf("casa amal", "via alcide de gasperi lecce"),
        destination = Address(
            address = "Via Alcide de Gasperi",
            number = "37 A",
            placeName = "",
            town = "Lecce",
            zip = "73100",
            province = "LE",
            country = "Italy"
        ),
        via = Address()
    ),
    Route(
        id = 1,
        name = "Links Academy",
        aliases = mutableListOf("links academy", "via masseria caldare lecce"),
        destination = Address(
            address = "Via Masseria Caldare",
            number = "",
            placeName = "Links Academy",
            town = "Lecce",
            zip = "73100",
            province = "LE",
            country = "Italy"
        ),
        via = Address(
            address = "Tangenziale Ovest di Lecce",
            number = "",
            placeName = "",
            town = "Lecce",
            zip = "73100",
            province = "LE",
            country = "Italy"
        ),
    ),
    Route(
        id = 2,
        name = "Links Scotellaro",
        aliases = mutableListOf("links scotellaro", "via rocco scotellaro 55 lecce"),
        destination = Address(
            address = "Via Alcide de Gasperi",
            number = "37 A",
            placeName = "",
            town = "Lecce",
            zip = "73100",
            province = "LE",
            country = "Italy"
        ),
        via = Address(
            address = "Via Alcide de Gasperi",
            number = "37 A",
            placeName = "",
            town = "Lecce",
            zip = "73100",
            province = "LE",
            country = "Italy"
        ),
    )
)