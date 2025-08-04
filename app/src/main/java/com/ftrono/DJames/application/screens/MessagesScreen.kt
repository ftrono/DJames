package com.ftrono.DJames.application.screens

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.allMessages
import com.ftrono.DJames.application.curMessagesSize
import com.ftrono.DJames.application.datetimeShortFormat
import com.ftrono.DJames.application.dialogs.DialogRequestOverlay
import com.ftrono.DJames.application.dialogs.SinglePermissionHandler
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.messagesListTriggerGap
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.Message
import com.ftrono.DJames.ui.components.ChatInputField
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.components.OptionsItem
import com.ftrono.DJames.ui.components.OptionsMenu
import com.ftrono.DJames.ui.components.MessageBubble
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.TopBarMenu
import com.ftrono.DJames.ui.selectors.messagesColorSelectorLight
import com.ftrono.DJames.ui.selectors.messagesIconSelector
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun MessagesScreenPreview() {
    val navController = rememberNavController()
    MessagesScreen(navController, preview=true)
}

@Composable
fun MessagesScreen(
    navController: NavController,
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

    var hasMore = remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    var offset = 0L
    val allMessagesState by allMessages.observeAsState()
    allMessages.postValue(messageUtils.refreshMessages(offset, preview))
    val curMessagesSizeState by curMessagesSize.observeAsState()

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
                subtitle = if (selectedMessageIds.isNotEmpty()) "Selected" else "Last 30 days",
                showBack = true,
                onBack = { navController.popBackStack() },
                optionButtons = {
                    TopBarMenu(
                        contentText = "${if (selectedMessageIds.isNotEmpty()) selectedMessageIds.size else curMessagesSizeState}",
                        backgroundColor = if (selectedMessageIds.isNotEmpty()) colorResource(R.color.faded_grey) else colorResource(R.color.greenSign),
                        onClick = { mDisplayMainMenu.value = !mDisplayMainMenu.value },
                    ) {
                        MessagesOptions(
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
        if (allMessagesState!!.isEmpty()) {
            //MESSAGES EMPTY:
            Text(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight()
                    .wrapContentWidth()
                    .clickable(
                        // This makes the rest of the screen clear focus on tap
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                    },
                text = "No messages",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                color = colorResource(id = R.color.mid_grey),
            )
        } else {
            //MESSAGES LIST:
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .clickable(
                        // This makes the rest of the screen clear focus on tap
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                    },
                state = listState,
                reverseLayout = true
            ) {
                itemsIndexed(
                    allMessagesState!!
                ) { index, item ->
                    // STARTER / MESSAGE ITEM:
                    val message = Json.decodeFromString<Message>(item)
                    if (message.type == "starter") {
                        ConvStarter(
                            message = message,
                            selectedMessageIds = selectedMessageIds
                        )
                    } else  {
                        MessageItem(
                            message = message,
                            selectedMessageIds = selectedMessageIds
                        )
                    }
                }

                // Loader at the bottom
                if (hasMore.value) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }

            LaunchedEffect(listState) {
                snapshotFlow { listState.firstVisibleItemIndex }
                    .collect {
                        // If we're near the end and there are more messages:
                        if (listState.firstVisibleItemIndex >= (allMessages.value!!.size - messagesListTriggerGap) && hasMore.value) {
                            val newMessages = messageUtils.refreshMessages(offset, preview)
                            if (newMessages.isNotEmpty()) {
                                allMessages.postValue(allMessages.value!! + newMessages)
                                Log.d("MessagesScreen", "Messages list enlarged!")
                                offset += newMessages.size
                            } else {
                                hasMore.value = false
                            }
                        }
                    }
            }
        }

        // CHAT INPUT FIELD:
        ChatInputField(
            context = mContext,
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
            placeholder = "Ask me anything...",
            enableLeftButton = true,
            onSend = { }   //TODO
        )
    }
}


@Composable
fun ConvStarter(
    message: Message,
    selectedMessageIds: SnapshotStateList<Long>,
) {
    //INFO:
    val mContext = LocalContext.current
    var mDisplayMenu = rememberSaveable {
        mutableStateOf(false)
    }
    val deleteLogOn = rememberSaveable { mutableStateOf(false) }
    if (deleteLogOn.value) {
        DialogDeleteMessages(mContext, deleteLogOn, selectedMessageIds, starterId=message.timestamp)
    }

    // CONV STARTER:
    Row (
        modifier = Modifier
            .padding(
                start = 32.dp,
                end = 24.dp,
            )
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box() {
            Card(
                modifier = Modifier
                    .padding(
                        top = 2.dp, bottom = 2.dp,
                    )
                    .clickable {
                        if (selectedMessageIds.isNotEmpty()) {
                            // Add entire conversation to selection:
                            val idsToAdd = messageUtils.getMessageIDsByStarterId(message.starterId)
                            for (id in idsToAdd) {
                                if (!selectedMessageIds.contains(id)) {
                                    selectedMessageIds.add(id)
                                }
                            }
                        } else {
                            // Show options:
                            mDisplayMenu.value = !mDisplayMenu.value
                        }
                    },
                border = BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.light_grey)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(start=4.dp, end=4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SIGN:
                    Icon(
                        modifier = Modifier
                            .size(18.dp),
                        painter = painterResource(R.drawable.arrow_down),
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
                            .size(18.dp),
                        imageVector = Icons.Default.MoreVert,
                        tint = colorResource(R.color.black),
                        contentDescription = "More"
                    )
                }
            }
            //"MORE OPTIONS" BUTTON:
            ConvItemOptions(
                mContext = mContext,
                mDisplayMenu = mDisplayMenu,
                deleteLogOn = deleteLogOn,
                starterId = message.timestamp
            )
        }
    }

}


@Composable
fun MessageItem(
    message: Message,
    selectedMessageIds: SnapshotStateList<Long>
) {
    //INFO:
    val mContext = LocalContext.current
    val extraDetails = if (message.type == "ai") buildExtraDetails(message) else ""

    //MESSAGE ROW:
    Row (
        modifier = Modifier
            .padding(
                start = 32.dp,
                end = 24.dp,
            )
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (message.type == "ai") Arrangement.Start else Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp),
        ) {

            // MESSAGE BUBBLE:
            MessageBubble(
                mContext = mContext,
                selectedMessageIds = selectedMessageIds,
                fromUser = message.type == "user",
                messageId = message.id,
                requestIntent = message.requestIntent
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp),
                    horizontalAlignment = if (message.type == "ai") Alignment.Start else Alignment.End,
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
                        textAlign = if (message.type == "ai") TextAlign.Start else TextAlign.End,
                        text = message.text
                    )
                }
            }
            if (extraDetails != "") {
                MessageBubble(
                    mContext = mContext,
                    selectedMessageIds = selectedMessageIds,
                    fromUser = message.type == "user",
                    messageId = message.id,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(4.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            //CAT ICON:
                            Icon(
                                modifier = Modifier
                                    .padding(
                                        start = 8.dp,
                                        end = 2.dp,
                                        top = 4.dp,
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
                                    .padding(start = 2.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
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
                                .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 8.dp)
                                .wrapContentWidth()
                                .wrapContentHeight(),
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
        }
    }
}


//DROPDOWN MENU:
@Composable
fun MessagesOptions(
    mDisplayMenu: MutableState<Boolean>,
    deleteOn: MutableState<Boolean>,
    selectedMessageIds: SnapshotStateList<Long>
) {
    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: UNSELECT ALL
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
            //2) Item: DELETE ALL MESSAGES
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
            allMessages.postValue(messageUtils.refreshMessages())   //Refresh list
            dialogOnState.value = false
        }
    )
}


//BUILD LOG VIEW INFO:
fun buildExtraDetails(message: Message): String {
    val trimLength = 40
    val intentName = message.requestIntent
    var detailText = ""

    if (intentName.contains("Call") || intentName.contains("Message")) {
        //Calls & Messages:
        val itemInfo = message.attachments.usable
        detailText = if (itemInfo.name == "") "" else "Contact:  ${itemInfo.name}"

    } else if (intentName.contains("Drive")) {
        //Drive:
        val itemInfo = message.attachments.usable
        if (itemInfo.detail == "") {
            detailText = if (itemInfo.name == "") "" else "Route:  ${itemInfo.name}"
        } else {
            detailText = if (itemInfo.name == "") "" else "Route:  ${itemInfo.name}\nDetail:  ${itemInfo.detail}"
        }

    } else if (intentName.contains("Play")) {
        //Play requests:
        val playable = message.attachments.spotifyPlay

        if (playable.type == "podcast" || playable.type == "episode") {
            //Podcast:
            var podcastName = utils.capitalizeWords(playable.contextName)
            detailText = if (podcastName == "") "" else "Podcast:  $podcastName"
            var episodeName = utils.capitalizeWords(playable.name)
            var episodeDate = utils.capitalizeWords(playable.releaseDate)
            detailText += if (episodeName == "") "" else "\nEpisode:  ($episodeDate) \"$episodeName\""

        } else if (playable.type == "playlist" || playable.type == "collection") {
            //Playlist / artist playlist / collection:
            detailText = if (playable.name == "") "" else "Playlist:  ${utils.capitalizeWords(playable.name)}"

        } else if (playable.type == "artist") {
            //Artist:
            detailText = if (playable.name == "") "" else "Artist:  ${utils.capitalizeWords(playable.name)}"

        } else if (playable.type == "album") {
            //Album:
            var matchName = utils.trimString(playable.name, trimLength)
            if (matchName != "") {
                var artistName = utils.trimString(playable.artistsNames.joinToString(", "), trimLength)
                if (playable.albumType != "album") {
                    detailText = "Album:  $matchName  (${utils.capitalizeWords(playable.albumType)})\nArtist:  $artistName"
                } else {
                    detailText = "Album:  $matchName\nArtist:  $artistName"
                }
            }

        } else {
            //Track:
            var matchName = utils.trimString(playable.name, trimLength)
            if (matchName != "") {
                var artistName = utils.trimString(playable.artistsNames.joinToString(", "), trimLength)

                //Context:
                var contextType = playable.contextType
                var contextName = ""
                if (contextType == "Playlist" && !message.attachments.contextError && !message.attachments.playedExternally) {
                    //Use Playlist:
                    contextName = playable.contextName
                } else {
                    //Default to Album type:
                    contextType = utils.capitalizeWords(playable.albumType)
                    contextName = playable.albumName
                }
                var contextFull = "$contextName  ($contextType)"
                if (message.attachments.playedExternally) {
                    contextFull = "$contextFull [EXT]"
                }
                detailText = "Track:  $matchName\nArtist:  $artistName\nContext:  $contextFull"
            }
        }
    }
    //Add confidence:
    if (message.attachments.matchScore > 0 && detailText.trim() != "") {
        detailText = detailText + "\nMatch:  ${message.attachments.matchScore}%"
    }
    return detailText
}
