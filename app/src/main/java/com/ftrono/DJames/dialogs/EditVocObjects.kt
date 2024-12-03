package com.ftrono.DJames.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R

@Composable
fun EditVocTextField(
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController,
    textFieldColors: TextFieldColors,
    title: String,
    placeholder: String,
    textState: MutableState<String>
) {
    Text(
        text = title,
        color = colorResource(id = R.color.light_grey),
        textAlign = TextAlign.Start,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    )
    OutlinedTextField(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 20.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .focusRequester(focusRequester),
        colors = textFieldColors,
        value = textState.value,
        onValueChange = { newText ->
            textState.value = newText.trimStart { it == '0' }
        },
        textStyle = TextStyle(
            fontSize = 16.sp
        ),
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                keyboardController.hide()
            }
        ),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic
            )
        },
    )
}