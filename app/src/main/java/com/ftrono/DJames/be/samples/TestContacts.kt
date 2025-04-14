package com.ftrono.DJames.be.samples

import com.ftrono.DJames.be.database.Contact
import com.ftrono.DJames.be.database.PhoneSet
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString


val testContacts = listOf<Contact>(
    Contact(
        id = 0,
        name = "Amal",
        aliases = mutableListOf("amal"),
        language = "",
        defaultPhone = "personal",
        phoneSetsJson = Json.encodeToString(
            mutableMapOf(
                "personal" to PhoneSet(
                    prefix = "+39",
                    phone = "3331122333"
                )
            ),
        ),
    ),
    Contact(
        id = 1,
        name = "Mammut",
        aliases = mutableListOf("mammut", "mammood"),
        language = "it",
        defaultPhone = "personal",
        phoneSetsJson = Json.encodeToString(
            mutableMapOf(
                "personal" to PhoneSet(
                    prefix = "+39",
                    phone = "3320011234"
                )
            ),
        ),
    ),
    Contact(
        id = 2,
        name = "Rick",
        aliases = mutableListOf("rick"),
        language = "en",
        defaultPhone = "personal",
        phoneSetsJson = Json.encodeToString(
            mutableMapOf(
                "personal" to PhoneSet(
                    prefix = "+39",
                    phone = "3325678912"
                )
            ),
        ),
    ),
)