package com.ftrono.DJames.ui.dialogs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.R
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.showLoggingIn
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.ui.components.EditLibTitle
import com.ftrono.DJames.ui.selectors.getSwitchColors
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libColorSelectorLight
import com.ftrono.DJames.ui.selectors.libIconSelector


// DIALOG WINDOWS:

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
fun DialogLoadingPreview() {
    val dialogOnState by showLoggingIn.observeAsState()
    DialogLoading(
        text = "Logging in to Spotify..."
    )
}


@Composable
fun DialogLoading(
    text: String
) {
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false
        ),
        onDismissRequest = { }
    ) {
        //CONTAINER:
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.dark_grey_background)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                //MESSAGE:
                Row (
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .width(40.dp),
                        color = colorResource(id = R.color.light_grey),
                        trackColor = colorResource(id = R.color.dark_grey),
                        strokeWidth = 8.dp
                    )
                    Text(
                        text = text,
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun DialogEditLibPreview() {
    val filter = "artist"
    Dialog (
        onDismissRequest = {}
    ) {
        EditLibDialog(
            title = filter,
            cat = "spotify",
            subcat = "artist",
        )
    }
}


//Main Container: EditLibDialog:
@Composable
fun EditLibDialog(
    modifier: Modifier = Modifier,
    title: String,
    cat: String,
    subcat: String = "",
    onDismiss: () -> Unit = {},
    onSave: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onGo: () -> Unit = {},
    smallHeader: Boolean = true,
    showRefresh: Boolean = true,
    showGo: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    //EDIT DIALOG CONTAINER:
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        //border = BorderStroke(2.dp, colorResource(id = R.color.faded_grey)),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {

        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {

            //TITLE:
            EditLibHeader(
                title = title,
                cat = cat,
                subcat = subcat,
                onCancel = { onDismiss() },
                onSave = { onSave() },
                onRefresh = { onRefresh() },
                onGo = { onGo() },
                small = smallHeader,
                showRefresh = showRefresh,
                showGo = showGo
            )

            //CONTENT:
            Column(
                modifier = Modifier
                    .padding(8.dp)
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


//EditLib Header:
@Composable
fun EditLibHeader(
    title: String,
    cat: String,
    subcat: String = "",
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit,
    onGo: () -> Unit,
    small: Boolean = false,
    showRefresh: Boolean = false,
    showGo: Boolean = false
) {
    //HEADER:
    Box(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        //HEADER CONTENT:
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //ICONS:
            Icon(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(if (small) 20.dp else 36.dp),
                painter = libIconSelector(cat=cat),
                contentDescription = title,
                tint = if (cat == "spotify") libColorSelector(cat=cat) else libColorSelectorLight(cat=cat),
            )
            if (subcat != "") {
                Icon(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(if (small) 20.dp else 36.dp),
                    painter = libIconSelector(cat=subcat),
                    contentDescription = title,
                    tint = libColorSelectorLight(cat=subcat),
                )
            }
            //TITLE:
            Text(
                text = "${title.replaceFirstChar { it.uppercase() }}",
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f),
                color = colorResource(id = R.color.light_grey),
                textAlign = TextAlign.Start,
                fontSize = if (small) 16.sp else 24.sp,
                fontWeight = FontWeight.Bold
            )
            if (showGo) {
                //GO BUTTON:
                Icon(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(35.dp)
                        .clickable {
                            onGo()
                        },
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go",
                    tint = libColorSelectorLight(cat = if (subcat != "") subcat else cat),
                )
            }
            if (showRefresh) {
                //REFRESH BUTTON:
                Icon(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(35.dp)
                        .clickable {
                            onRefresh()
                        },
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = libColorSelectorLight(cat = if (subcat != "") subcat else cat),
                )
            }
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
                tint = libColorSelectorLight(cat = if (subcat != "") subcat else cat),
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
                tint = libColorSelectorLight(cat = if (subcat != "") subcat else cat),
            )
        }
    }
}



@Preview
@Composable
fun AddLinkDialogPreview() {
    val useParentState = rememberSaveable { mutableStateOf(false) }
    Dialog (
        onDismissRequest = {}
    ) {
        AddLinkDialog(
            dialogHeader = "New",
            textBoxHeader = "Save a Spotify link",
            useParentState = useParentState,
        )
    }
}


@Composable
fun AddLinkDialog(
    modifier: Modifier = Modifier,
    cat: String = "spotify",   //TODO
    dialogHeader: String,
    textBoxHeader: String,
    useParentState: MutableState<Boolean>,
    onDismiss: () -> Unit = {},
    onSave: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val textState = rememberSaveable { mutableStateOf(sharedLink.value!!) }
    val checkboxDescription = rememberSaveable { mutableStateOf("") }

    LaunchedEffect(textState.value) {
        if (textState.value.contains("/track/")) {
            checkboxDescription.value = "Save Artist instead"
        } else if (textState.value.contains("/episode/")) {
            checkboxDescription.value = "Save Podcast instead"
        } else {
            checkboxDescription.value = ""
            useParentState.value = false
        }
    }

    //MAIN:
    Dialog(
        onDismissRequest = {
            onDismiss
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        EditLibDialog(
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            title = dialogHeader,
            cat = cat,
            onDismiss = onDismiss,
            onSave = {
                sharedLink.postValue(spotifyUtils.extractUrl(textState.value))
                onSave()
             },
            smallHeader = false,
            showRefresh = false,
        ) {
            EditLibTitle(
                title = textBoxHeader,
                textHeaderColor = libColorSelector(cat = cat),
            )

            OutlinedTextField(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = getTextFieldColors(
                    colorLight = libColorSelectorLight("spotify"),
                    colorDark = libColorSelector("spotify")
                ),
                value = textState.value,
                textStyle = TextStyle(
                    fontSize = 16.sp
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        sharedLink.postValue(spotifyUtils.extractUrl(textState.value))
                        onSave()
                    }
                ),
                placeholder = {
                    Text(
                        text = "Paste link here...",
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic
                    )
                },
                onValueChange = { newText ->
                    textState.value = newText
                }
            )

            //Use parent:
            if (checkboxDescription.value != "") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = checkboxDescription.value,
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = useParentState.value,
                        colors = getSwitchColors(
                            color = libColorSelector(cat = cat),
                        ),
                        onCheckedChange = {
                            //UPDATE:
                            useParentState.value = it
                        }
                    )
                }
            }
        }
    }
}

