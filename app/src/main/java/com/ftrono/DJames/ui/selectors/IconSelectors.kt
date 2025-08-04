package com.ftrono.DJames.ui.selectors

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.ftrono.DJames.R

//ICONS SELECTORS:

//LIBRARY:
@Composable
fun libIconSelector(
    cat: String
): Painter {
    if (cat == "contact") {
        return painterResource(id = R.drawable.sign_phone)
    } else if (cat == "playlist") {
        return painterResource(id = R.drawable.sign_disc)
    } else if (cat == "podcast") {
        return painterResource(id = R.drawable.sign_headphones)
    } else if (cat == "route") {
        return painterResource(id = R.drawable.sign_place)
    } else {
        return painterResource(id = R.drawable.sign_people)
    }
}


//GUIDE:
@Composable
fun guideIconSelector(
    cat: String
): Painter {
    if (cat == "calls") {
        return painterResource(id = R.drawable.sign_phone)
    } else if (cat == "messages") {
        return painterResource(id = R.drawable.sign_message)
    } else if (cat == "routes") {
        return painterResource(id = R.drawable.sign_place)
    } else if (cat == "car") {
        return painterResource(id = R.drawable.icon_car)
    } else {
        return painterResource(id = R.drawable.sign_headphones)
    }
}


//MESSAGES:
@Composable
fun messagesIconSelector(
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
