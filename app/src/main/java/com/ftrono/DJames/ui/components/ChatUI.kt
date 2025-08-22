package com.ftrono.DJames.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.ui.selectors.messagesColorSelector
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.datetimeShortFormat
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.chat.ChatManager
import com.ftrono.DJames.be.database.Message
import com.ftrono.DJames.ui.navigation.SharedViewModel
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.selectors.actionsIconSelector
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.theme.NavigationItem


// CHAT UI
@Composable
fun ConvStarterBubble(
    modifier: Modifier = Modifier,
    mDisplayMenu: MutableState<Boolean>,
    selectedMessageIds: SnapshotStateList<Long>,
    message: Message,
    options: @Composable () -> Unit = {}
) {
    // CONV STARTER:
    Row (
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box() {
            Card(
                modifier = Modifier
                    .padding(
                        top = 2.dp, bottom = 2.dp,
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                // Add / remove entire conversation to selection:
                                val idsToAdd = messageUtils.getMessageIDsByStarterId(message.starterId)
                                for (id in idsToAdd) {
                                    if (!selectedMessageIds.contains(id)) {
                                        selectedMessageIds.add(id)
//                                    } else {
//                                        selectedMessageIds.remove(id)
                                    }
                                }
                            },
                            onTap = {
                                if (selectedMessageIds.isNotEmpty()) {
                                    // Add / remove entire conversation to selection:
                                    val idsToAdd = messageUtils.getMessageIDsByStarterId(message.starterId)
                                    for (id in idsToAdd) {
                                        if (!selectedMessageIds.contains(id)) {
                                            selectedMessageIds.add(id)
//                                        } else {
//                                            selectedMessageIds.remove(id)
                                        }
                                    }
                                } else {
                                    // Show options:
                                    mDisplayMenu.value = !mDisplayMenu.value
                                }
                            }
                        )
                    },
                border = BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.light_grey)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(start=4.dp, end=4.dp, top=1.dp, bottom=1.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SIGN:
//                    Icon(
//                        modifier = Modifier
//                            .size(18.dp),
//                        painter = painterResource(R.drawable.arrow_down),
//                        tint = colorResource(R.color.black),
//                        contentDescription = "More"
//                    )
                    // CHANNEL:
                    Icon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(12.dp),
                        painter = if (message.fromVoice) painterResource(R.drawable.icon_speak) else painterResource(R.drawable.sign_message),
                        tint = colorResource(R.color.black),
                        contentDescription = "More"
                    )
                    // STARTER DATETIME:
                    Text(
                        modifier = Modifier
                            .padding(start=4.dp, end=4.dp),
                        color = colorResource(id = R.color.black),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        text = messageUtils.convertTimestamp(message.timestamp, datetimeShortFormat)
                    )
                    // "MORE" ICON:
                    Icon(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp),
                        imageVector = Icons.Default.MoreVert,
                        tint = colorResource(R.color.black),
                        contentDescription = "More"
                    )
                }
            }
            options()
        }
    }
}


@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    selectedMessageIds: SnapshotStateList<Long>,
    messageId: Long,
    fromUser: Boolean,
    requestIntent: String = "",
    showButton: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    //MESSAGE ROW:
    Row (
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (!fromUser) Arrangement.Start else Arrangement.End
    ) {
        // MESSAGE BUBBLE:
        Card(
            modifier = modifier
                .widthIn(max = 300.dp)   // sets max width
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (!selectedMessageIds.contains(messageId)) {
                                // Select:
                                selectedMessageIds.add(messageId)
                            } else {
                                // Unselect:
                                selectedMessageIds.remove(messageId)
                            }
                        },
                        onTap = {
                            if (selectedMessageIds.isNotEmpty()) {
                                if (!selectedMessageIds.contains(messageId)) {
                                    // Select:
                                    selectedMessageIds.add(messageId)
                                } else {
                                    // Unselect:
                                    selectedMessageIds.remove(messageId)
                                }
                            } else {
                                onClick()
                            }
                        }
                    )
                },
            border = BorderStroke(1.dp, colorResource(id = R.color.dark_grey)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedMessageIds.contains(messageId)) {
                    colorResource(R.color.colorAccentLight)
                } else if (fromUser) {
                    messagesColorSelector(requestIntent)
                } else {
                    colorResource(id = R.color.dark_grey_background)
                }
            )
        ) {
            content()
        }

        // ACTION:
        if (showButton) {
            RoundedSign(
                modifier = Modifier
                    .offset(x = -(20).dp),
                signSize = 40.dp,
                contentSize = 20,
                backgroundColor = if (selectedMessageIds.contains(messageId)) {
                    colorResource(R.color.colorPrimaryDark)
                } else {
                    messagesColorSelector(cat = requestIntent)
                },
                borderColor = if (selectedMessageIds.contains(messageId)) {
                    colorResource(R.color.colorPrimaryDark)
                } else {
                    messagesColorSelector(cat = requestIntent)
                },
                contentColor = colorResource(R.color.light_grey),
                iconVector = actionsIconSelector(requestIntent),
                circle = true,
                clickable = true,
                onClick = {
                    if (selectedMessageIds.isNotEmpty()) {
                        if (!selectedMessageIds.contains(messageId)) {
                            // Select:
                            selectedMessageIds.add(messageId)
                        } else {
                            // Unselect:
                            selectedMessageIds.remove(messageId)
                        }
                    } else {
                        onClick()
                    }
                }
            )
        }
    }
}


@Preview
@Composable
fun SplitSendButtonPreview() {
    SplitSendButton(
        enableLeftButton = true,
        onLeftClick = {},
        onRightClick = {},
        testChat = true
    )
}


@Composable
fun SplitSendButton(
    enableLeftButton: Boolean = true,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    testChat: Boolean = false,
) {
    val overlayActiveState by overlayActive.observeAsState()
    val queryStatusState by queryStatus.observeAsState()

    Row(
        modifier = Modifier
            .padding(2.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        // LEFT:
        if (enableLeftButton && !isKeyboardOpen()) {
            Card(
                modifier = Modifier
                    .padding(end = 1.dp)
                    .fillMaxHeight(),
                onClick = onLeftClick,
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (testChat || overlayActiveState!!) colorResource(id = R.color.colorStop) else colorResource(id = R.color.colorAccent)
                ),
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DriveIcon(
                        iconSize = 24.dp,
                        showForbidden = testChat || overlayActiveState!!
                    )
                }
            }
        }

        // RIGHT:
        Card(
            modifier = Modifier
                .padding(start = 1.dp)
                .fillMaxHeight(),
            onClick = onRightClick,
            shape = if (!enableLeftButton || isKeyboardOpen()) RoundedCornerShape(20.dp) else RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (queryStatusState != "ready") colorResource(id = R.color.faded_grey) else colorResource(id = R.color.colorAccent)
            ),
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (queryStatusState == "ready") {
                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        imageVector = Icons.AutoMirrored.Default.Send,
                        tint = colorResource(R.color.light_grey),
                        contentDescription = "Send"
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        painter = painterResource(R.drawable.icon_no_send),
                        tint = colorResource(R.color.light_grey),
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun ChatInputPreview() {
    val mContext = LocalContext.current
    val sharedViewModel: SharedViewModel = viewModel()
    val requestPermissions = rememberSaveable { mutableStateOf(false) }
    val requestOverlayOn = rememberSaveable { mutableStateOf(false) }

    ChatInputField(
        context = mContext,
        sharedViewModel = sharedViewModel,
        requestPermissions = requestPermissions,
        requestOverlayOn = requestOverlayOn,
        placeholder = "Ask me anything...",
        enableLeftButton = true
    )
}


@Composable
fun ChatInputField(
    context: Context,
    modifier: Modifier = Modifier,
    sharedViewModel: SharedViewModel,
    requestOverlayOn: MutableState<Boolean>,
    requestPermissions: MutableState<Boolean>,
    placeholder: String,
    enableLeftButton: Boolean = true,
    onSend: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val queryState by queryStatus.observeAsState()
    val textFieldColors = getTextFieldColors(
        colorLight = colorResource(R.color.colorAccentLight),
        colorDark = colorResource(R.color.colorAccent)
    )

    fun onKeyboardDone() {
        focusManager.clearFocus()
        keyboardController!!.hide()
        if (queryState == "ready") onSend()
    }

    //Text Field:
    OutlinedTextField(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            .focusRequester(focusRequester),
        enabled = queryState == "ready",
        colors = textFieldColors,
        value = sharedViewModel.text,
        interactionSource = interactionSource,
        onValueChange = { newText ->
            sharedViewModel.text = newText
        },
        textStyle = TextStyle(
            fontSize = 16.sp
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend = {
                onKeyboardDone()
            }
        ),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic
            )
        },
        trailingIcon = {
            SplitSendButton(
                enableLeftButton = enableLeftButton,
                onLeftClick = {
                    // Start / Stop DRIVE mode:
                    utils.startStopDriveMode(
                        context = context,
                        requestOverlayOn = requestOverlayOn,
                        requestPermissions = requestPermissions,
                        openClock = false,
                    )
                },
                onRightClick = { onKeyboardDone() },
            )
        }
    )
}