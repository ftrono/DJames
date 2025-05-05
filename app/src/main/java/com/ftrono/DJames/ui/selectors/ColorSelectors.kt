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
    colorDark: Color
): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colorLight,
        unfocusedContainerColor = colorResource(id = R.color.dark_grey),
        unfocusedBorderColor = colorResource(id = R.color.faded_grey),
        focusedTextColor = colorResource(id = R.color.light_grey),
        unfocusedTextColor = colorResource(id = R.color.light_grey),
        focusedPlaceholderColor = colorResource(id = R.color.mid_grey),
        unfocusedPlaceholderColor = colorResource(id = R.color.mid_grey),
        focusedPrefixColor = colorResource(id = R.color.mid_grey),
        unfocusedPrefixColor = colorResource(id = R.color.mid_grey),
        focusedSuffixColor = colorResource(id = R.color.mid_grey),
        unfocusedSuffixColor = colorResource(id = R.color.mid_grey),
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


//VOCABULARY:
@Composable
fun vocColorSelector(
    cat: String
): Color {
    if (cat == "contact") {
        return colorResource(id = R.color.greenSign)
    } else if (cat == "playlist") {
        return colorResource(id = R.color.yellowSign)
    } else if (cat == "podcast") {
        return colorResource(id = R.color.redSign)
    } else if (cat == "route") {
        return colorResource(id = R.color.brownSign)
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
    } else if (cat == "podcast") {
        return colorResource(id = R.color.redSignLight)
    } else if (cat == "route") {
        return colorResource(id = R.color.brownSignLight)
    } else {
        return colorResource(id = R.color.blueSignLight)
    }
}


//GUIDE:
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
    } else if (cat == "DriveRequest") {
        return colorResource(id = R.color.brownSignLight)
    } else if (cat.contains("Play")) {
        return colorResource(id = R.color.yellowSignLight)
    } else {
        return colorResource(id = R.color.mid_grey)
    }
}