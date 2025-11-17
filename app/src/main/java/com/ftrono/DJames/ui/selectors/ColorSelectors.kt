package com.ftrono.DJames.ui.selectors

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.ftrono.DJames.R

//COLORS SELECTORS:

//Switch colors:
@Composable
fun getSwitchColors(
    color: Color
): SwitchColors {
    return SwitchDefaults.colors(
        checkedThumbColor = colorResource(id = R.color.light_grey),
        checkedTrackColor = color,
        uncheckedThumbColor = colorResource(id = R.color.mid_grey),
        uncheckedTrackColor = colorResource(id = R.color.faded_grey),
    )
}

//TextField colors:
@Composable
fun getTextFieldColors(
    colorLight: Color,
    colorDark: Color,
    fullyTransparent: Boolean = false,
): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colorLight,
        focusedTextColor = colorResource(id = R.color.light_grey),
        focusedContainerColor = colorResource(id = R.color.windowBackground),
        focusedPlaceholderColor = colorResource(id = R.color.mid_grey),
        unfocusedContainerColor = colorResource(id = if (fullyTransparent) R.color.transparent_full else R.color.dark_grey),
        unfocusedBorderColor = colorResource(id = if (fullyTransparent) R.color.transparent_full else R.color.faded_grey),
        unfocusedTextColor = colorResource(id = R.color.light_grey),
        unfocusedPlaceholderColor = colorResource(id = R.color.mid_grey),
        disabledTextColor = colorResource(id = if (fullyTransparent) R.color.light_grey else R.color.midfaded_grey),
        disabledPlaceholderColor = colorResource(id = R.color.midfaded_grey),
        disabledContainerColor = colorResource(id = if (fullyTransparent) R.color.transparent_full else R.color.dark_grey),
        focusedPrefixColor = colorResource(id = R.color.mid_grey),
        unfocusedPrefixColor = colorResource(id = R.color.mid_grey),
        disabledPrefixColor = colorResource(id = R.color.midfaded_grey),
        focusedSuffixColor = colorResource(id = R.color.mid_grey),
        unfocusedSuffixColor = colorResource(id = R.color.mid_grey),
        disabledSuffixColor = colorResource(id = R.color.midfaded_grey),
        focusedSupportingTextColor = colorLight,
        unfocusedSupportingTextColor = colorResource(id = R.color.mid_grey),
        unfocusedLeadingIconColor = colorResource(id = R.color.mid_grey),
        focusedLeadingIconColor = colorLight,
        unfocusedTrailingIconColor = colorResource(id = R.color.mid_grey),
        focusedTrailingIconColor = colorLight,
        cursorColor = colorLight,
        selectionColors = TextSelectionColors(
            handleColor = colorLight,
            backgroundColor = colorDark
        )
    )
}


//LIBRARY:
@Composable
fun libColorSelector(
    cat: String
): Color {
    return when (cat) {
        "spotify" -> colorResource(id = R.color.colorAccentMid)
        "artist" -> colorResource(id = R.color.blueSign)
        "album" -> colorResource(id = R.color.violetSign)
        "playlist" -> colorResource(id = R.color.yellowSign)
        "podcast" -> colorResource(id = R.color.redSign)
        "contact" -> colorResource(id = R.color.greenSign)
        "place" -> colorResource(id = R.color.brownSign)
        else -> colorResource(id = R.color.faded_grey)
    }
}


@Composable
fun libColorSelectorLight(
    cat: String
): Color {
    return when (cat) {
        "spotify" -> colorResource(id = R.color.colorAccentMid)
        "artist" -> colorResource(id = R.color.blueSignLight)
        "album" -> colorResource(id = R.color.violetSignLight)
        "playlist" -> colorResource(id = R.color.yellowSignLight)
        "podcast" -> colorResource(id = R.color.redSignLight)
        "contact" -> colorResource(id = R.color.greenSignLight)
        "place" -> colorResource(id = R.color.brownSignLight)
        else -> colorResource(id = R.color.mid_grey)
    }
}


//GUIDE:
@Composable
fun guideColorSelector(
    cat: String
): Color {
    return when (cat) {
        "calls" -> colorResource(id = R.color.greenSign)
        "messages" -> colorResource(id = R.color.blueSign)
        "places" -> colorResource(id = R.color.brownSign)
        "car" -> colorResource(id = R.color.brownSign)
        "play" -> colorResource(id = R.color.yellowSign)
        else -> colorResource(id = R.color.midfaded_grey)
    }
}


@Composable
fun guideColorSelectorLight(
    cat: String
): Color {
    return when (cat) {
        "calls" -> colorResource(id = R.color.greenSignLight)
        "messages" -> colorResource(id = R.color.blueSignLight)
        "places" -> colorResource(id = R.color.brownSignLight)
        "car" -> colorResource(id = R.color.brownSignLight)
        "play" -> colorResource(id = R.color.yellowSignLight)
        else -> colorResource(id = R.color.mid_grey)
    }
}


//MESSAGES:
@Composable
fun messagesColorSelector(
    cat: String
): Color {
    return when {
        cat.contains("Call") -> colorResource(id = R.color.greenSign)
        cat.contains("Message") -> colorResource(id = R.color.blueSign)
        cat.contains("Drive") -> colorResource(id = R.color.brownSign)
        cat.contains("Play") -> colorResource(id = R.color.yellowSign)
        else -> colorResource(id = R.color.dark_grey)
    }
}


@Composable
fun messagesColorSelectorLight(
    cat: String
): Color {
    return when {
        cat.contains("Call") -> colorResource(id = R.color.greenSignLight)
        cat.contains("Message") -> colorResource(id = R.color.blueSignLight)
        cat.contains("Drive") -> colorResource(id = R.color.brownSignLight)
        cat.contains("Play") -> colorResource(id = R.color.yellowSignLight)
        else -> colorResource(id = R.color.mid_grey)
    }
}