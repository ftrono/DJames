package com.ftrono.DJames.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import com.ftrono.DJames.R


//COLORS & ICONS SELECTORS:

//VOCABULARY:
@Composable
fun vocIconSelector(
    cat: String
): Painter {
    if (cat == "contact") {
        return painterResource(id = R.drawable.sign_phone)
    } else if (cat == "playlist") {
        return painterResource(id = R.drawable.sign_headphones)
    } else {
        return painterResource(id = R.drawable.sign_note)
    }
}


@Composable
fun vocColorSelector(
    cat: String
): Color {
    if (cat == "contact") {
        return colorResource(id = R.color.greenSign)
    } else if (cat == "playlist") {
        return colorResource(id = R.color.yellowSign)
    } else {
        return colorResource(id = R.color.blueSign)
    }
}


@Composable
fun vocColorSelectorLight(
    cat: String
): Color {
    if (cat == "contact") {
        return colorResource(id = R.color.greenSignLight)
    } else if (cat == "playlist") {
        return colorResource(id = R.color.yellowSignLight)
    } else {
        return colorResource(id = R.color.blueSignLight)
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
    } else {
        return painterResource(id = R.drawable.sign_headphones)
    }
}


@Composable
fun guideColorSelector(
    cat: String
): Color {
    if (cat == "calls") {
        return colorResource(id = R.color.greenSign)
    } else if (cat == "messages") {
        return colorResource(id = R.color.blueSign)
    } else {
        return colorResource(id = R.color.yellowSign)
    }
}


@Composable
fun guideColorSelectorLight(
    cat: String
): Color {
    if (cat == "calls") {
        return colorResource(id = R.color.greenSignLight)
    } else if (cat == "messages") {
        return colorResource(id = R.color.blueSignLight)
    } else {
        return colorResource(id = R.color.yellowSignLight)
    }
}


//HISTORY:
@Composable
fun historyColorSelectorLight(
    cat: String
): Color {
    if (cat == "CallRequest") {
        return colorResource(id = R.color.greenSignLight)
    } else if (cat == "MessageRequest") {
        return colorResource(id = R.color.blueSignLight)
    } else if (cat.contains("Play")) {
        return colorResource(id = R.color.yellowSignLight)
    } else {
        return colorResource(id = R.color.mid_grey)
    }
}


@Composable
fun historyIconSelector(
    cat: String
): Painter {
    if (cat == "CallRequest") {
        return painterResource(id = R.drawable.sign_phone)
    } else if (cat == "MessageRequest") {
        return painterResource(id = R.drawable.sign_message)
    } else if (cat.contains("Play")) {
        return painterResource(id = R.drawable.sign_headphones)
    } else {
        return painterResource(id = R.drawable.sign_help)
    }
}