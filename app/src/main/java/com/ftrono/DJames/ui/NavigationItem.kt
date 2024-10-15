package com.ftrono.DJames.ui.theme

import com.ftrono.DJames.R

sealed class NavigationItem(
    var route: String,
    var icon: Int,
    var title: String) {

    //MAIN NAV:
    object Home : NavigationItem("home", R.drawable.nav_home, "Home")
    object Guide : NavigationItem("guide", R.drawable.nav_help, "Guide")
    object MyDJames : NavigationItem("myDjames", R.drawable.nav_vocabulary, "My Djames")
    object History : NavigationItem("history", R.drawable.nav_history, "History")
    //SETTINGS:
    object Settings : NavigationItem("settings", R.drawable.item_settings, "Preferences")
}
