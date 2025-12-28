package com.ftrono.DJames.ui.theme

import com.ftrono.DJames.R

sealed class NavigationItem(
    var route: String,
    var icon: Int,
    var title: String) {

    //MAIN NAV:
    object Home : NavigationItem("home", R.drawable.icon_home, "Home")
    object Guide : NavigationItem("guide", R.drawable.icon_info, "Guide")
    object Library : NavigationItem("library", R.drawable.icon_star, "Saved")
    object Messages : NavigationItem("messages", R.drawable.icon_message, "Messages")
    //SETTINGS:
    object Accounts : NavigationItem("accounts", R.drawable.icon_user, "Accounts")
    object Settings : NavigationItem("settings", R.drawable.icon_settings, "Preferences")
}
