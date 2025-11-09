package com.ftrono.DJames.application.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.allMessageIds
import com.ftrono.DJames.application.chatInputPlaceholder
import com.ftrono.DJames.application.dialogs.DialogRequestOverlay
import com.ftrono.DJames.application.dialogs.SinglePermissionHandler
import com.ftrono.DJames.application.maxHistoryDays
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.chat.ActionsExecutor
import com.ftrono.DJames.be.chat.ChatManager
import com.ftrono.DJames.be.database.Message
import com.ftrono.DJames.be.models.ActionType
import com.ftrono.DJames.ui.components.ChatInputField
import com.ftrono.DJames.ui.components.ConvStarterBubble
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.components.OptionsItem
import com.ftrono.DJames.ui.components.OptionsMenu
import com.ftrono.DJames.ui.components.MessageBubble
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.overlay.TypingIndicator
import com.ftrono.DJames.ui.navigation.SharedViewModel
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.TopBarMenu
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libIconSelector
import com.ftrono.DJames.ui.selectors.messagesColorSelectorLight
import com.ftrono.DJames.ui.selectors.messagesIconSelector


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun MessagesScreenPreview() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val chatManager = ChatManager(context)
    chatManager.init()
    val sharedViewModel: SharedViewModel = viewModel()
    MessagesScreen(navController, chatManager, sharedViewModel, preview=true)
}

@Composable
fun MessagesScreen(
    navController: NavController,
    chatManager: ChatManager,
    sharedViewModel: SharedViewModel,
    preview: Boolean = false
) {
    val mContext = LocalContext.current

    //Overlay permission management:
    val requestOverlayOn = rememberSaveable { mutableStateOf(false) }
    if (requestOverlayOn.value) {
        DialogRequestOverlay(
            mContext = mContext,
            dialogOnState = requestOverlayOn
        )
    }
    // Mic permissions management:
    val requestPermissions = rememberSaveable { mutableStateOf(false) }
    if (requestPermissions.value) {
        SinglePermissionHandler(
            context = mContext,
            dialogOnState = requestPermissions,
            permission = Manifest.permission.RECORD_AUDIO
        )
    }

    // States:
    val focusManager = LocalFocusManager.current
    val selectedMessageIds = remember { mutableStateListOf<Long>() }

    val queryState by queryStatus.observeAsState()
    val allMessageIdsState by allMessageIds.observeAsState()
    allMessageIds.postValue(messageUtils.refreshMessages(preview))

    val mDisplayMainMenu = rememberSaveable {
        mutableStateOf(false)
    }

    val deleteAllOn = rememberSaveable { mutableStateOf(false) }
    if (deleteAllOn.value) {
        DialogDeleteMessages(mContext, deleteAllOn, selectedMessageIds)
    }

    // SCREEN:
    StreetUIScaffold(
        modifier = Modifier
            .clickable(
                // This makes the rest of the screen clear focus on tap
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        lineDistance = 20.dp,
        topBar = {
            StreetUITopBar(
                pretitle = "",
                title = "Messages",
                subtitle = if (selectedMessageIds.isNotEmpty()) "Selected" else "Last $maxHistoryDays days",
                showBack = true,
                onBack = { navController.popBackStack() },
                optionButtons = {
                    if (selectedMessageIds.isNotEmpty()) {
                        //CLEAR SELECTION BUTTON:
                        Icon(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(32.dp)
                                .clickable {
                                    selectedMessageIds.clear()
                                },
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear selection",
                            tint = colorResource(R.color.light_grey)
                        )
                    }
                    TopBarMenu(
                        contentText = "${if (selectedMessageIds.isNotEmpty()) {
                            selectedMessageIds.size
                        } else if (allMessageIdsState!!.size > 999) {
                            "999+"
                        } else  {
                            allMessageIdsState!!.size
                        }}",
                        backgroundColor = if (selectedMessageIds.isNotEmpty()) colorResource(R.color.faded_grey) else colorResource(R.color.colorPrimary),
                        onClick = { mDisplayMainMenu.value = !mDisplayMainMenu.value },
                    ) {
                        MessagesOptions(
                            context = mContext,
                            navController = navController,
                            mDisplayMenu = mDisplayMainMenu,
                            deleteOn = deleteAllOn,
                            selectedMessageIds = selectedMessageIds
                        )
                    }
                }
            )
        }
    ) {

        //CONTENT:
        if (allMessageIdsState!!.isEmpty()) {
            //MESSAGES EMPTY:
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "No messages",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = colorResource(id = R.color.mid_grey),
                )
            }
        } else {
            //MESSAGES LIST:
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                state = rememberLazyListState(),
                reverseLayout = true
            ) {
                itemsIndexed(
                    allMessageIdsState!!
                ) { index, id ->
                    // MESSAGES CONTENT:
                    val message = messageUtils.getMessageById(id, preview)
                    Column(
                        modifier = Modifier
                            .padding(
                                start = 32.dp,
                                end = 24.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // STARTER:
                        if (message.isStart) {
                            ConvStarter(
                                context = mContext,
                                message = message,
                                selectedMessageIds = selectedMessageIds
                            )
                        }
                        // MESSAGE:
                        MessageItem(
                            context = mContext,
                            message = message,
                            selectedMessageIds = selectedMessageIds
                        )
                        // EXTRA DETAILS:
                        if (!message.fromUser && message.actionType != null) {
                            val extraDetails = messageUtils.buildExtraDetails(message)
                            if (extraDetails != "") {
                                MessageDetail(
                                    context = mContext,
                                    message = message,
                                    selectedMessageIds = selectedMessageIds,
                                    extraDetails = extraDetails,
                                    showButton = (
                                        message.actionType != ActionType.SMS &&
                                        message.actionType != ActionType.WA_VOICE &&
                                        message.actionType != ActionType.WA_VOICE
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // TYPING INDICATOR:
        if (queryState == "processing") {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TypingIndicator()
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    text = "Typing...",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = colorResource(id = R.color.mid_grey),
                )
            }
        }
        // CHAT INPUT FIELD:
        ChatInputField(
            context = mContext,
            sharedViewModel = sharedViewModel,
            requestPermissions = requestPermissions,
            requestOverlayOn = requestOverlayOn,
            modifier = Modifier
                .padding(
                    start = 32.dp,
                    end = 24.dp,
                    top = 6.dp,
                    bottom = 2.dp
                )
                .imePadding()
                .fillMaxWidth(),
            placeholder = chatInputPlaceholder,
            enableLeftButton = true,
            onSend = {
                val curText = sharedViewModel.text.trim()
                if (curText != "") {
                    chatManager.processQuery(curText)
                    sharedViewModel.text = ""
                }
            }
        )
    }
}


@Composable
fun ConvStarter(
    context: Context,
    message: Message,
    selectedMessageIds: SnapshotStateList<Long>,
) {
    //INFO:
    var mDisplayMenu = rememberSaveable {
        mutableStateOf(false)
    }
    val deleteLogOn = rememberSaveable { mutableStateOf(false) }
    if (deleteLogOn.value) {
        DialogDeleteMessages(context, deleteLogOn, selectedMessageIds, starterId=message.starterId)
    }

    // CONV STARTER:
    ConvStarterBubble(
        modifier = Modifier
            .padding(
                start = 32.dp,
                end = 24.dp,
                bottom = 8.dp,
            ),
        mDisplayMenu = mDisplayMenu,
        selectedMessageIds = selectedMessageIds,
        message = message,
        options = {
            //"MORE OPTIONS" BUTTON:
            ConvItemOptions(
                mContext = context,
                mDisplayMenu = mDisplayMenu,
                deleteLogOn = deleteLogOn,
                starterId = message.starterId
            )
        }
    )
}


@Composable
fun MessageItem(
    context: Context,
    message: Message,
    selectedMessageIds: SnapshotStateList<Long>
) {
    MessageBubble(
        modifier = Modifier
            .padding(
                top = 2.dp,
                start = if (message.fromUser) 40.dp else 0.dp,
                end = if (!message.fromUser) 40.dp else 0.dp,
            ),
        selectedMessageIds = selectedMessageIds,
        fromUser = message.fromUser,
        messageId = message.id,
        requestIntent = message.requestIntent,
        onClick = {
            // Open Log file via external app:
            val filename = messageUtils.prepareLogFile(context, message.id)
            messageUtils.openLogViaApp(context, filename)
        }
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp),
            horizontalAlignment = if (!message.fromUser) Alignment.Start else Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            //MESSAGE TEXT:
            Text(
                modifier = Modifier
                    .padding(start=2.dp, end=2.dp),
                color = if (selectedMessageIds.contains(message.id)) {
                    colorResource(R.color.colorPrimaryDark)
                } else {
                    colorResource(id = R.color.light_grey)
                },
                fontSize = 16.sp,
                lineHeight = 16.sp,
                textAlign = if (!message.fromUser) TextAlign.Start else TextAlign.End,
                text = message.text
            )
        }
    }
}


@Composable
fun MessageDetail(
    context: Context,
    message: Message,
    selectedMessageIds: SnapshotStateList<Long>,
    extraDetails: String,
    showButton: Boolean = false,
) {
    MessageBubble(
        modifier = Modifier
            .padding(top=2.dp),
        selectedMessageIds = selectedMessageIds,
        fromUser = message.fromUser,
        messageId = message.id,
        requestIntent = message.requestIntent,
        showButton = showButton,
        onClick = {
            if (showButton) {
                val actionsExecutor = ActionsExecutor(context)
                actionsExecutor.launchAction(
                    action = message.actionType!!,
                    usable = message.attachments.usable,
                    playable = message.attachments.spotifyPlay,
                    reqLanguage = message.langCode,
                    fromOldChat = true,
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(top=4.dp, bottom=4.dp, start=4.dp, end=24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            //HEADER ROW:
            Row(
                modifier = Modifier
                    .padding(top=4.dp, bottom=2.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //SOURCE ICON:
                if (message.attachments.spotifyPlay != null) {
                    Icon(
                        modifier = Modifier
                            .padding(
                                start = 8.dp,
                                end = 2.dp,
                                bottom = 2.dp
                            )
                            .size(16.dp),
                        painter = libIconSelector("spotify"),
                        contentDescription = "spotify",
                        tint = libColorSelector("spotify")
                    )
                }
                //CAT ICON:
                Icon(
                    modifier = Modifier
                        .padding(
                            start = if (message.attachments.spotifyPlay != null) 2.dp else 8.dp,
                            end = 2.dp,
                            bottom = 2.dp
                        )
                        .size(16.dp),
                    painter = messagesIconSelector(cat = message.requestIntent),
                    contentDescription = message.requestIntent,
                    tint = if (selectedMessageIds.contains(message.id)) {
                        colorResource(R.color.colorPrimaryDark)
                    } else {
                        messagesColorSelectorLight(cat = message.requestIntent)
                    }
                )
                //CAT NAME:
                Text(
                    modifier = Modifier
                        .padding(start = 2.dp, end = 8.dp, bottom = 2.dp),
                    color = if (selectedMessageIds.contains(message.id)) {
                        colorResource(R.color.colorPrimaryDark)
                    } else {
                        messagesColorSelectorLight(cat = message.requestIntent)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    text = message.requestIntent
                )
            }
            //FULL DETAILS TEXT:
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
                color = if (selectedMessageIds.contains(message.id)) {
                    colorResource(R.color.colorPrimaryDark)
                } else {
                    colorResource(id = R.color.mid_grey)
                },
                fontSize = 12.sp,
                lineHeight = 14.sp,
                text = extraDetails
            )
        }
    }
}



//DROPDOWN MENU:
@Composable
fun MessagesOptions(
    context: Context,
    navController: NavController,
    mDisplayMenu: MutableState<Boolean>,
    deleteOn: MutableState<Boolean>,
    selectedMessageIds: SnapshotStateList<Long>
) {
    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: REFRESH MESSAGES
            OptionsItem(
                title = "Refresh",
                iconVector = Icons.Default.Refresh,
                onClick = {
                    mDisplayMenu.value = false
                    allMessageIds.postValue(messageUtils.refreshMessages())
                    Toast.makeText(context, "Messages updated!", Toast.LENGTH_SHORT).show()
                }
            )
            //2) Item: UNSELECT ALL
            if (selectedMessageIds.isNotEmpty()) {
                OptionsItem(
                    title = "Clear selection",
                    iconVector = Icons.Default.Clear,
                    onClick = {
                        mDisplayMenu.value = false
                        selectedMessageIds.clear()
                    }
                )
            }
            //3) Item: DELETE ALL MESSAGES
            OptionsItem(
                title =  if (selectedMessageIds.isNotEmpty()) {
                    "Delete selected"
                } else {
                    "Delete all messages"
                },
                iconVector = Icons.Default.Delete,
                onClick = {
                    mDisplayMenu.value = false
                    deleteOn.value = true
                }
            )
        }
    )
}


//DROPDOWN MENU:
@Composable
fun ConvItemOptions(
    mContext: Context,
    mDisplayMenu: MutableState<Boolean>,
    deleteLogOn: MutableState<Boolean>,
    starterId: Long
) {
    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: VIEW CONV
            OptionsItem(
                title = "View conversation",
                iconVector = Icons.Default.Search,
                onClick = {
                    val filename = messageUtils.prepareLogFile(mContext, starterId=starterId)
                    messageUtils.openLogViaApp(mContext, filename)
                    mDisplayMenu.value = false
                }
            )
            //2) Item: SHARE CONV
            OptionsItem(
                title = "Share conversation",
                iconVector = Icons.Default.Share,
                onClick = {
                    //Prepare & send cached file:
                    val filename = messageUtils.prepareLogFile(mContext, starterId=starterId)
                    utils.sendCachedFile(mContext, filename)
                    mDisplayMenu.value = false
                }
            )
            //3) Item: DELETE CONV
            OptionsItem(
                title = "Delete conversation",
                iconVector = Icons.Default.Delete,
                onClick = {
                    mDisplayMenu.value = false
                    deleteLogOn.value = true
                }
            )
        }
    )
}


@Composable
fun DialogDeleteMessages(
    mContext: Context,
    dialogOnState: MutableState<Boolean>,
    selectedMessageIds: SnapshotStateList<Long>,
    starterId: Long = 0,
) {

    //DELETE DIALOG:
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = if (starterId > 0) "Delete conversation" else if (selectedMessageIds.size == 1) "Delete message" else "Delete messages",
        content = {
            Text(
                text = if (starterId > 0) {
                    "Do you want to delete this conversation?"
                } else if (selectedMessageIds.size == 1) {
                    "Do you want to delete this message?"
                } else if (selectedMessageIds.size > 1) {
                    "Do you want to delete the selected messages?"
                } else {
                    "Do you want to delete all message history?"
                },
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "No",
        confirmText = "Yes",
        onConfirm = {
            if (starterId > 0) {
                //Delete conversation:
                messageUtils.deleteConversation(mContext, starterId)
            } else if (selectedMessageIds.isNotEmpty()) {
                //Delete selected messages:
                messageUtils.deleteMessageItems(mContext, selectedMessageIds.toList())
            } else {
                //Delete all messages:
                messageUtils.deleteAllMessages(mContext)
            }
            selectedMessageIds.clear()
            allMessageIds.postValue(messageUtils.refreshMessages())   //Refresh list
            dialogOnState.value = false
        }
    )
}
