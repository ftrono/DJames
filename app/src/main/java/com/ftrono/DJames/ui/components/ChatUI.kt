package com.ftrono.DJames.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.focus.onFocusChanged
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
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.userTypingChat
import com.ftrono.DJames.ui.selectors.messagesColorSelector
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.selectors.getTextFieldColors


@Composable
fun MessageBubble(
    mContext: Context,
    selectedMessageIds: SnapshotStateList<Long>,
    messageId: Long,
    fromUser: Boolean,
    requestIntent: String = "",
    content: @Composable () -> Unit
) {
    // MESSAGE BUBBLE:
    Card(
        modifier = Modifier
            .padding(
                top = 2.dp,
                start = if (fromUser) 40.dp else 0.dp,
                end = if (!fromUser) 40.dp else 0.dp,
            )
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
                            // Open Log file via external app:
                            val filename = messageUtils.prepareLogFile(mContext, messageId)
                            messageUtils.openLogViaApp(mContext, filename)
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
    ) { content() }
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
    val userTypingChatState by userTypingChat.observeAsState()
    val overlayActiveState by overlayActive.observeAsState()

    Row(
        modifier = Modifier
            .padding(2.dp)
            .height(48.dp)
            .wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        // Left button:
        if (enableLeftButton && !userTypingChatState!!) {
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
                        .padding(start = 20.dp, end = 20.dp)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        painter = if (testChat || overlayActiveState!!) painterResource(R.drawable.sign_message) else painterResource(R.drawable.icon_speak),
                        tint = colorResource(R.color.light_grey),
                        contentDescription = "Drive"
                    )
                }
            }
        }

        // Right button:
        Card(
            modifier = Modifier
                .padding(start = 1.dp)
                .fillMaxHeight(),
            onClick = onRightClick,
            shape = if (!enableLeftButton || userTypingChatState!!) RoundedCornerShape(20.dp) else RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.colorAccent)
            ),
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    imageVector = Icons.AutoMirrored.Default.Send,
                    tint = colorResource(R.color.light_grey),
                    contentDescription = "Send"
                )
            }
        }
    }
}


@Preview
@Composable
fun ChatInputPreview() {
    val mContext = LocalContext.current
    val requestPermissions = rememberSaveable { mutableStateOf(false) }
    val requestOverlayOn = rememberSaveable { mutableStateOf(false) }
    val chatText = rememberSaveable { mutableStateOf("") }

    ChatInputField(
        context = mContext,
        requestPermissions = requestPermissions,
        requestOverlayOn = requestOverlayOn,
        textState = chatText,
        placeholder = "Ask me anything...",
        enableLeftButton = true
    )
}


@Composable
fun ChatInputField(
    context: Context,
    modifier: Modifier = Modifier,
    requestOverlayOn: MutableState<Boolean>,
    requestPermissions: MutableState<Boolean>,
    textState: MutableState<String>,
    placeholder: String,
    enableLeftButton: Boolean = true,
    onSend: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val textFieldColors = getTextFieldColors(
        colorLight = colorResource(R.color.colorAccentLight),
        colorDark = colorResource(R.color.colorAccent)
    )

    fun onKeyboardDone() {
        focusManager.clearFocus()
        keyboardController!!.hide()
        onSend()
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
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                userTypingChat.postValue(focusState.isFocused)
            },
        colors = textFieldColors,
        value = textState.value,
        interactionSource = interactionSource,
        onValueChange = { newText ->
            textState.value = newText
        },
        textStyle = TextStyle(
            fontSize = 16.sp
        ),
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
                },   //TODO
                onRightClick = { onKeyboardDone() },
            )
        }
    )
}