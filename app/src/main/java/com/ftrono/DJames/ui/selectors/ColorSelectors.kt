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


//MAIN:
@Composable
fun colorSelector(
    cat: String
): Color {
    return when (cat) {
        // Modes:
        "mobile" -> colorResource(R.color.colorAccentMid)
        "car" -> colorResource(R.color.colorAccentMid)
        // Guide:
        "info" -> colorResource(id = R.color.colorPrimary)
        "music" -> colorResource(R.color.yellowSign)
        "phone" -> colorResource(R.color.greenSign)
        "messages" -> colorResource(R.color.blueSign)
        "gmaps" -> colorResource(R.color.brownSign)
        // Library:
        "spotify" -> colorResource(R.color.colorAccentMid)
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
fun colorSelectorLight(
    cat: String
): Color {
    return when (cat) {
        // Modes:
        "mobile" -> colorResource(R.color.colorAccentMid)
        "car" -> colorResource(R.color.colorAccentMid)
        // Guide:
        "info" -> colorResource(R.color.light_grey)
        "music" -> colorResource(R.color.greenSignLight)
        "phone" -> colorResource(R.color.colorAccentMid)
        "messages" -> colorResource(R.color.blueSignLight)
        "gmaps" -> colorResource(R.color.yellowSignLight)
        // Library:
        "spotify" -> colorResource(R.color.greenSignLight)
        "artist" -> colorResource(id = R.color.blueSignLight)
        "album" -> colorResource(id = R.color.violetSignLight)
        "playlist" -> colorResource(id = R.color.yellowSignLight)
        "podcast" -> colorResource(id = R.color.redSignLight)
        "contact" -> colorResource(id = R.color.greenSignLight)
        "place" -> colorResource(id = R.color.brownSignLight)
        else -> colorResource(id = R.color.mid_grey)
    }
}


@Composable
fun colorSelectorDark(
    cat: String
): Color {
    return when (cat) {
        // Modes:
        "mobile" -> colorResource(R.color.greenSignDark)
        "car" -> colorResource(R.color.greenSignDark)
        // Guide:
        "info" -> colorResource(R.color.dark_grey)
        "music" -> colorResource(R.color.greenSignDark)
        "phone" -> colorResource(R.color.greenSignDark)
        "messages" -> colorResource(R.color.blueSignDark)
        "gmaps" -> colorResource(R.color.yellowSignDark)
        // Library:
        "spotify" -> colorResource(R.color.greenSignDark)
        "artist" -> colorResource(id = R.color.blueSignDark)
        "album" -> colorResource(id = R.color.violetSignDark)
        "playlist" -> colorResource(id = R.color.yellowSignDark)
        "podcast" -> colorResource(id = R.color.redSignDark)
        "contact" -> colorResource(id = R.color.greenSignDark)
        "place" -> colorResource(id = R.color.brownSignDark)
        else -> colorResource(id = R.color.dark_grey)
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