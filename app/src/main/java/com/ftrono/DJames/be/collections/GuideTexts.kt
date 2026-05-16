package com.ftrono.DJames.be.collections

import com.ftrono.DJames.application.guidePosPlaceholder
import com.ftrono.DJames.be.models.GuideText


val guideTexts = mapOf<String, GuideText>(
    "info" to GuideText(
        intro = "",
        content = "Ask me to play music,\nget driving directions,\ncall or message your contacts!",   //TODO: add "or YouTube"
        outro = "Tap on the button $guidePosPlaceholder, or press Volume Up, or use a Bluetooth remote!"
    ),
    "music" to GuideText(
        intro = "Try saying:",
        content = "\"Play Lost by Linkin Park\"\n\"Play my liked songs\"",
        outro = "I can play via Spotify any song, album, artist, playlist or podcast!"
    ),
    "phone" to GuideText(
        intro = "Try saying:",
        content = "\"Call Ricky\"\n\"Make a phone call to mom\"",
        outro = "I can call any contact of yours - just save them to Library first!"
    ),
    "messages" to GuideText(
        intro = "Try saying:",
        content = "\"Send a message to Ricky\"\n\"Send a voice message to Frank\"",
        outro = "I can help you draft and send SMS, WhatApp text / voice messages!"
    ),
    "gmaps" to GuideText(
        intro = "Try saying:",
        content = "\"Drive me to Copertino Castle\"\n\"Drive me to work\"",
        outro = "I can open Google Maps for you and show you the route to any place you need!"
    ),
)