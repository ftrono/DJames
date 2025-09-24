package com.ftrono.DJames.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.queryLangFull
import com.ftrono.DJames.application.queryLangCodes
import com.ftrono.DJames.application.screens.restartOverlay
import com.ftrono.DJames.application.userGender
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime


@Composable
fun isKeyboardOpen(): Boolean {
    val ime = WindowInsets.ime
    return ime.getBottom(LocalDensity.current) > 0
}


@Composable
fun CustomCheckbox(
    modifier: Modifier = Modifier,
    checkedState: MutableState<Boolean>,
    checkedColor: Color,
    textColor: Color,
    onClickExtra: () -> Unit = {},
    text: String
){
    val checkBoxColors = CheckboxDefaults.colors(
        checkedColor = checkedColor,
        uncheckedColor = colorResource(id = R.color.mid_grey),
        checkmarkColor = colorResource(id = R.color.dark_grey_background)
    )

    Row(
        modifier = modifier
            .offset(x=-(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(
            modifier = Modifier,
            checked = checkedState.value,
            onCheckedChange = {
                checkedState.value = it
                onClickExtra()
              },
            colors = checkBoxColors
        )
        Text(
            modifier = Modifier
                .clickable {
                    checkedState.value = !checkedState.value
                    onClickExtra()
                   },
            text = text,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            color = textColor
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSpinner(
    context: Context,
    parentOptions: List<String>,
    init: String,
    state: MutableState<String>,
    focusColorLight: Color,
    focusColorDark: Color,
    optionsBackground: Color = colorResource(id = R.color.windowBackground),
    prefName: String = "",
    width: Int = 0,
    start: Int = 0
) {
    //"PREFNAME" ARG IS TO SELECT PREF TO OVERWRITE!!!
    var isExpanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(init) }
    //TODO: TEMP:
    if (init == "0" || init == "1") {
        state.value = "it"
    }

    //Full spinner:
    ExposedDropdownMenuBox(
        modifier = if (width > 0) {
            Modifier
                .padding(top = 8.dp, bottom = 20.dp, start = start.dp)
                .width(width.dp)
        } else {
            Modifier
                .padding(top = 8.dp, bottom = 20.dp, start = start.dp)
                .fillMaxWidth()
        },
        expanded = isExpanded,
        onExpandedChange = {
            isExpanded = it
        }
    ) {
        //Visualized textbox:
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            value = selectedOptionText,
            onValueChange = { },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = isExpanded
                )
            },
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            colors = getTextFieldColors(
                colorLight = focusColorLight,
                colorDark = focusColorDark
            )
//            colors = ExposedDropdownMenuDefaults.textFieldColors(
//                focusedContainerColor = colorResource(id = R.color.dark_grey_background),
//                unfocusedContainerColor = colorResource(id = R.color.transparent_full),
//                focusedTextColor = colorResource(id = R.color.light_grey),
//                unfocusedTextColor = colorResource(id = R.color.light_grey),
//                focusedIndicatorColor = focusColorLight,
//                unfocusedIndicatorColor = colorResource(id = R.color.mid_grey),
//            )
        )
        //Dropdown:
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {
                isExpanded = false
            },
            scrollState = rememberScrollState(),
            containerColor = optionsBackground
        ) {
            parentOptions.forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        //UPDATE SELECTION & STATE:
                        selectedOptionText = selectionOption
                        if (prefName == "userGender") {
                            prefs.userGender = selectionOption
                            state.value = selectionOption
                            userGender.postValue(selectionOption)
                        } else if (prefName == "overlayPosition") {
                            prefs.overlayPosition = selectionOption
                            state.value = selectionOption
                            restartOverlay(context)
                        } else if (prefName == "silenceDetector") {
                            prefs.silenceDetector = selectionOption
                            state.value = selectionOption
                        } else if (prefName == "queryLanguage") {
                            prefs.queryLanguage = queryLangCodes[queryLangFull.indexOf(selectionOption)]
                            state.value = selectionOption
                        } else if (prefName == "messageLanguage") {
                            prefs.messageLanguage = messLangCodes[messLangFull.indexOf(selectionOption)]
                            state.value = selectionOption
                        } else if (prefName == "placeLanguage") {
                            prefs.placeLanguage = messLangCodes[messLangFull.indexOf(selectionOption)]
                            state.value = selectionOption
                        } else {
                            state.value = selectionOption
                        }
                        isExpanded = false
                    },
                    text = {
                        Text(
                            text = selectionOption,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = colorResource(id = R.color.light_grey)
                        )
                    }
                )
            }
        }
    }
}
