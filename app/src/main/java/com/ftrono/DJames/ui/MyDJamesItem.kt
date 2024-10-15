package com.ftrono.DJames.ui.theme

import com.ftrono.DJames.R

sealed class MyDJamesItem(
    var route: String,
    var icon: Int,
    var listIcon: Int,
    var background: Int,
    var title: String) {

    //MY DJAMES:
    object Artists : MyDJamesItem("artists", R.drawable.png_note, R.drawable.icon_note, R.drawable.bg_artists, "My Artists")
    object Playlists : MyDJamesItem("playlists", R.drawable.png_headphones, R.drawable.icon_headphones, R.drawable.bg_playlists,  "My Playlists")
    object Contacts : MyDJamesItem("contacts", R.drawable.png_contact, R.drawable.icon_contact, R.drawable.bg_contacts,  "My Contacts")
    object Places : MyDJamesItem("places", R.drawable.png_pin, R.drawable.icon_pin, R.drawable.bg_places,  "My Places")
    object Parking : MyDJamesItem("parking", R.drawable.png_car, R.drawable.icon_pin, R.drawable.bg_parking,  "Last Parking")
    object News : MyDJamesItem("news", R.drawable.png_news, R.drawable.icon_note, R.drawable.bg_news,  "My News")
}
