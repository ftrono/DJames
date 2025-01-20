package com.ftrono.DJames.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ftrono.DJames.R
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.ui.vocIconSelector


//EDIT VOC COMPONENTS:

@Preview
@Composable
fun DialogRequestDetailPreview() {
    DialogRequestDetail(
        dialogOnState = remember {
            mutableStateOf(true)
        }
    )
}


//Request missing detail:
@Composable
fun DialogRequestDetail(
    dialogOnState: MutableState<Boolean>,
    title: String = "Title",
    message: String = "Custom message"
) {
    //REQUEST DETAIL DIALOG:
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = title,
        content = {
            Text(
                text = message,
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "Ok"
    )
}


@Preview
@Composable
fun DialogEditVocPreview() {
    Dialog (
        onDismissRequest = {}
    ) {
        EditVocDialog(filter = "artist")
    }
}


//Main Container: EditVocDialog:
@Composable
fun EditVocDialog(
    modifier: Modifier = Modifier,
    filter: String,
    onDismiss: () -> Unit = {},
    onSave: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    //EDIT DIALOG CONTAINER:
    Card(
        modifier = modifier
            .padding(
                top = 30.dp,
                bottom = 30.dp,
                start = if (isLandscape) 80.dp else 40.dp,
                end = if (isLandscape) 80.dp else 40.dp
            )
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(20.dp),
        //border = BorderStroke(2.dp, colorResource(id = R.color.faded_grey)),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {

        Column(
            modifier = Modifier
                .padding(20.dp)
                .wrapContentWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {

            //TITLE:
            EditVocHeader(
                filter = filter,
                onCancel = { onDismiss() },
                onSave = { onSave() }
            )

            //CONTENT:
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {

                content()

                //END PADDING:
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
        }
    }
}


@Preview
@Composable
fun EditVocHeaderPreview() {
    EditVocHeader(
        filter = "artist",
        onCancel = {},
        onSave = {}
    )
}


//EditVoc Header:
@Composable
fun EditVocHeader(
    filter: String,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    //HEADER:
    Box(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        //HEADER CONTENT:
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //ICON:
            Icon(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(36.dp),
                painter = vocIconSelector(cat = filter),
                contentDescription = filter,
                tint = vocColorSelectorLight(cat = filter)
            )
            //TITLE:
            Text(
                text = "${filter.replaceFirstChar { it.uppercase() }}",
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f),
                color = colorResource(id = R.color.light_grey),
                textAlign = TextAlign.Start,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            //BACK BUTTON:
            Icon(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(35.dp)
                    .clickable {
                        onCancel()
                    },
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancel",
                tint = vocColorSelectorLight(cat = filter)
            )
            //SAVE BUTTON:
            Icon(
                modifier = Modifier
                    .size(35.dp)
                    .clickable {
                        onSave()
                    },
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                tint = vocColorSelectorLight(cat = filter)
            )
        }
    }
}


//Text Field with title:
@Composable
fun EditVocTextField(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    textFieldColors: TextFieldColors,
    title: String,
    placeholder: String,
    textState: MutableState<String>,
    onKeyboardDone: () -> Unit = {}
) {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {

        //Title:
        Text(
            text = title,
            color = textHeaderColor,
            textAlign = TextAlign.Start,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        //Text Field:
        OutlinedTextField(
            modifier = modifier
                .padding(top = 8.dp, bottom = 20.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
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
                    onKeyboardDone()
                }
            ),
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        )
    }
}


