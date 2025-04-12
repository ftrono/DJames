package com.ftrono.DJames.dialogs

import android.content.res.Configuration
import android.media.Image
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.R
import com.ftrono.DJames.application.showLoggingIn
import com.ftrono.DJames.screen.saveSettings
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.ui.vocIconSelector


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
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.dark_grey_background)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .wrapContentWidth()
                    .wrapContentHeight(),
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
                            .padding(end=16.dp)
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
fun DialogEditVocPreview() {
    val filter = "artist"
    Dialog (
        onDismissRequest = {}
    ) {
        EditVocDialog(
            title = filter,
            headerColor = vocColorSelectorLight(cat = filter),
            headerPainter = vocIconSelector(cat = filter)
        )
    }
}


//Main Container: EditVocDialog:
@Composable
fun EditVocDialog(
    modifier: Modifier = Modifier,
    title: String,
    headerColor: Color,
    headerIcon: ImageVector? = null,
    headerPainter: Painter? = null,
    onDismiss: () -> Unit = {},
    onSave: () -> Unit = {},
    onRefresh: () -> Unit = {},
    smallHeader: Boolean = true,
    showRefresh: Boolean = true,
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
                title = title,
                color = headerColor,
                painter = headerPainter,
                icon = headerIcon,
                onCancel = { onDismiss() },
                onSave = { onSave() },
                onRefresh = { onRefresh() },
                small = smallHeader,
                showRefresh = showRefresh
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


//EditVoc Header:
@Composable
fun EditVocHeader(
    title: String,
    color: Color,
    icon: ImageVector? = null,
    painter: Painter? = null,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit,
    small: Boolean = false,
    showRefresh: Boolean = false
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
            if (icon != null) {
                //ImageVector:
                Icon(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(if (small) 20.dp else 36.dp),
                    imageVector = icon,
                    contentDescription = title,
                    tint = color
                )
            } else {
                //Painter:
                Icon(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(if (small) 20.dp else 36.dp),
                    painter = painter!!,
                    contentDescription = title,
                    tint = color
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
                    tint = color
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
                tint = color
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
                tint = color
            )
        }
    }
}



@Preview
@Composable
fun AddLinkDialogPreview() {
    val textState = rememberSaveable { mutableStateOf("") }
    Dialog (
        onDismissRequest = {}
    ) {
        AddLinkDialog(
            textState = textState,
            textBoxHeader = "Paste link here",
            header = "New",
            headerColor = colorResource(R.color.mid_grey),
            headerIcon = Icons.Default.Add,
        )
    }
}


@Composable
fun AddLinkDialog(
    modifier: Modifier = Modifier,
    textState: MutableState<String>,
    textBoxHeader: String,
    header: String,
    headerColor: Color,
    headerIcon: ImageVector? = null,
    headerPainter: Painter? = null,
    onDismiss: () -> Unit = {},
    onSave: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    //MAIN:
    EditVocDialog(
        modifier = Modifier
            .focusRequester(focusRequester),
        title = header,
        headerColor = headerColor,
        headerIcon = headerIcon,
        onDismiss = onDismiss,
        onSave = onSave,
        smallHeader = false,
        showRefresh = false,
    ) {
        EditVocTitle(
            title = textBoxHeader,
            textHeaderColor = headerColor,
        )

        OutlinedTextField(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 20.dp)
                .width(250.dp)
                .wrapContentHeight()
                .focusRequester(focusRequester),
            colors = getTextFieldColors(
                colorLight = colorResource(id = R.color.yellowSignLight),
                colorDark = colorResource(id = R.color.yellowSign)
            ),
            value = textState.value,
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSave()
                }
            ),
            placeholder = {
                Text(
                    text = "Paste Spotify link here...",
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic
                )
            },
            onValueChange = { newText ->
                textState.value = newText
            }
        )
    }
}

