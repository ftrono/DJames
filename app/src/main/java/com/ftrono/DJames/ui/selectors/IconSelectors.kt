package com.ftrono.DJames.ui.selectors

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.ftrono.DJames.R

//ICONS SELECTORS:

//VOCABULARY:
@Composable
fun vocIconSelector(
    cat: String
): Painter {
    if (cat == "contact") {
        return painterResource(id = R.drawable.sign_phone)
    } else if (cat == "playlist") {
        return painterResource(id = R.drawable.sign_disc)
    } else if (cat == "route") {
        return painterResource(id = R.drawable.sign_place)
    } else {
        return painterResource(id = R.drawable.sign_people)
    }
}


//GUIDE:
//TODO: Update!
@Composable
fun guideIconSelector(
    cat: String
): Painter {
    if (cat == "calls") {
        return painterResource(id = R.drawable.sign_phone)
    } else if (cat == "messages") {
        return painterResource(id = R.drawable.sign_message)
    } else {
        return painterResource(id = R.drawable.sign_headphones)
    }
}


//HISTORY:
@Composable
fun historyIconSelector(
    cat: String
): Painter {
    if (cat == "CallRequest") {
        return painterResource(id = R.drawable.sign_phone)
    } else if (cat == "MessageRequest") {
        return painterResource(id = R.drawable.sign_message)
    } else if (cat == "DriveRequest") {
        return painterResource(id = R.drawable.sign_place)
    } else if (cat.contains("Play")) {
        return painterResource(id = R.drawable.sign_headphones)
    } else {
        return painterResource(id = R.drawable.sign_help)
    }
}
