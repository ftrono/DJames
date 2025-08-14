package com.ftrono.DJames.application.screens

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.addLinkOn
import com.ftrono.DJames.application.curLibrarySize
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.libHeads
import com.ftrono.DJames.application.libSectionIdentifier
import com.ftrono.DJames.be.database.ItemInfoView
import com.ftrono.DJames.application.dialogs.EditLibArtist
import com.ftrono.DJames.application.dialogs.EditLibContact
import com.ftrono.DJames.application.dialogs.EditLibPlaylist
import com.ftrono.DJames.application.dialogs.EditLibPodcast
import com.ftrono.DJames.application.dialogs.EditLibRoute
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.components.OptionsItem
import com.ftrono.DJames.ui.components.OptionsMenu
import com.ftrono.DJames.ui.components.LetterStarter
import com.ftrono.DJames.ui.components.LibItemCard
import com.ftrono.DJames.ui.components.SplitterSign
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.dialogs.AddLinkDialog
import com.ftrono.DJames.ui.dialogs.DialogLoading
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.TopBarMenu
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libIconSelector
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.Boolean
import kotlin.String


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun LibScreenPreview() {
    val navController = rememberNavController()
    LibraryScreen(navController, editPreview="", preview=true)
}


@Composable
fun LibraryScreen(
    navController: NavController,
    editPreview: String = "",
    preview: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
    val mContext = LocalContext.current
    //Statuses:
    val idState = rememberSaveable { mutableStateOf<Long>(if (editPreview != "") 0L else -1L) }
    val nameState = rememberSaveable { mutableStateOf("") }
    val currentCatState = rememberSaveable { mutableStateOf(libHeads[0]) }
    val sharedLinkState by sharedLink.observeAsState()
    val addLinkState = rememberSaveable { mutableStateOf(sharedLinkState!!) }
    val libraryItems = rememberSaveable {
        mutableStateOf(libUtils.refreshLibrary(currentCatState.value, preview))
    }
    val curLibrarySizeState by curLibrarySize.observeAsState()
    val deleteLibOn = rememberSaveable { mutableStateOf(false) }
    if (deleteLibOn.value) {
        DialogDeleteLibrary(mContext, deleteLibOn, libraryItems, idState, nameState, filter=currentCatState.value)
    }

    val editLibOn = rememberSaveable { mutableStateOf(editPreview != "") }
    var editCat = if (editPreview != "") editPreview else if (editLibOn.value) currentCatState.value else ""

    val loadingDialogOn = rememberSaveable { mutableStateOf(false) }
    if (loadingDialogOn.value) {
        DialogLoading(
            text = "Getting link information..."
        )
    }

    if (editLibOn.value) {
        when (editCat) {
            "artist" -> EditLibArtist(
                context = mContext,
                libraryItems = libraryItems,
                idState = idState,
                filter = editCat,
                initLinkState = addLinkState,
                loadingDialogOn = loadingDialogOn,
                onDismiss = {
                    //cancelable -> true
                    editLibOn.value = false
                    idState.value = -1
                    addLinkState.value = ""
                    sharedLink.postValue("")
                },
                preview = preview
            )
            "playlist" -> EditLibPlaylist(
                context = mContext,
                libraryItems = libraryItems,
                idState = idState,
                filter = editCat,
                initLinkState = addLinkState,
                loadingDialogOn = loadingDialogOn,
                onDismiss = {
                    //cancelable -> true
                    editLibOn.value = false
                    idState.value = -1
                    addLinkState.value = ""
                    sharedLink.postValue("")
                },
                preview = preview
            )
            "podcast" -> EditLibPodcast(
                context = mContext,
                libraryItems = libraryItems,
                idState = idState,
                filter = editCat,
                initLinkState = addLinkState,
                loadingDialogOn = loadingDialogOn,
                onDismiss = {
                    //cancelable -> true
                    editLibOn.value = false
                    idState.value = -1
                    addLinkState.value = ""
                    sharedLink.postValue("")
                },
                preview = preview
            )
            "route" -> EditLibRoute(
                context = mContext,
                libraryItems = libraryItems,
                idState = idState,
                filter = editCat,
                onDismiss = {
                    //cancelable -> true
                    editLibOn.value = false
                    idState.value = -1
                    addLinkState.value = ""
                    sharedLink.postValue("")
                },
                preview = preview
            )
            "contact" -> EditLibContact(
                context = mContext,
                libraryItems = libraryItems,
                idState = idState,
                filter = editCat,
                onDismiss = {
                    //cancelable -> true
                    editLibOn.value = false
                    idState.value = -1
                    addLinkState.value = ""
                    sharedLink.postValue("")
                },
                preview = preview
            )
        }
    }

    val addLinkOnState by addLinkOn.observeAsState()
    if (addLinkOnState!!) {
        AddLinkDialog(
            textState = addLinkState,
            dialogHeader = "New",
            textBoxHeader = "Spotify: Artist, Playlist or Podcast URL",
            headerIcon = Icons.Default.Add,
            onDismiss = {
                //cancelable -> true
                addLinkOn.postValue(false)
                idState.value = -1
                addLinkState.value = ""
                sharedLink.postValue("")
            },
            onSave = {
                addLinkState.value = spotifyUtils.extractUrl(addLinkState.value)
                spotifyUtils.checkAndEditLib(
                    context = mContext,
                    idState = idState,
                    addLinkState = addLinkState,
                    currentCatState = currentCatState,
                    editLibOn = editLibOn,
                    loadingDialogOn = loadingDialogOn
                )
            }
        )
    }

    var mDisplayMainMenu = rememberSaveable {
        mutableStateOf(false)
    }

    if (sharedLinkState != "") {
        spotifyUtils.checkAndEditLib(
            context = mContext,
            idState = idState,
            addLinkState = addLinkState,
            currentCatState = currentCatState,
            editLibOn = editLibOn,
            loadingDialogOn = loadingDialogOn
        )
    }

    //SCREEN:
    StreetUIScaffold(
        lineDistance = 20.dp,
        topBar = {
            if (!isLandscape) {
                StreetUITopBar(
                    pretitle = "Saved items",
                    title = if (currentCatState.value == "artist" || currentCatState.value == "route") {
                        "${utils.capitalizeWords(currentCatState.value)}s   "
                    } else if (currentCatState.value == "podcast") {
                        "${utils.capitalizeWords(currentCatState.value)}s "
                    } else {
                        "${utils.capitalizeWords(currentCatState.value)}s"
                    },
                    showBack = true,
                    onBack = { navController.popBackStack() },
                    optionButtons = {
                        TopBarMenu(
                            contentText = "$curLibrarySizeState",
                            backgroundColor = libColorSelector(cat = currentCatState.value),
                            onClick = { mDisplayMainMenu.value = !mDisplayMainMenu.value },
                        ) {
                            CatOptions(
                                mContext = mContext,
                                libraryItems = libraryItems,
                                mDisplayMenu = mDisplayMainMenu,
                                deleteLibOn = deleteLibOn,
                                head = currentCatState.value
                            )
                        }
                    }
                )
            }
        },
        fab = {
            //FAB -> ADD NEW ITEM:
            ExtendedFloatingActionButton(
                containerColor = colorResource(id = R.color.colorAccent),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add library item",
                        tint = colorResource(id = R.color.light_grey)
                    )
                },
                text = {
                    Text(
                        text = "Add",
                        color = colorResource(id = R.color.light_grey),
                        fontSize = 16.sp
                    )
                },
                onClick = {
                    idState.value = -1
                    if (currentCatState.value == "contact" || currentCatState.value == "route") {
                        editLibOn.value = true
                    } else {
                        addLinkOn.postValue(true)
                    }
                }
            )
        }
    ) {
        //CONTENT:
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            //CAT SELECTORS:
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isLandscape) colorResource(R.color.windowBackground) else colorResource(
                            R.color.transparent_full
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                //BACK BUTTON:
                if (isLandscape) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 18.dp)
                            .size(30.dp)
                            .clickable {
                                navController.popBackStack()
                            },
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colorResource(id = R.color.light_grey)
                    )
                }

                //SPLITTER SIGN (bigger weight with margins):
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .weight(1F),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    SplitterSign(
                        libraryItems = libraryItems,
                        currentCatState = currentCatState,
                        preview = preview
                    )
                }

                //CAT OPTIONS:
                if (isLandscape) {
                    TopBarMenu(
                        contentText = "$curLibrarySizeState",
                        backgroundColor = libColorSelector(cat = currentCatState.value),
                        onClick = { mDisplayMainMenu.value = !mDisplayMainMenu.value },
                    ) {
                        CatOptions(
                            mContext = mContext,
                            libraryItems = libraryItems,
                            mDisplayMenu = mDisplayMainMenu,
                            deleteLibOn = deleteLibOn,
                            head = currentCatState.value
                        )
                    }
                }
            }

            //CONTENT:
            LibSectionContent(
                libraryItems = libraryItems,
                currentCatState = currentCatState,
                idState = idState,
                nameState = nameState,
                editLibOn = editLibOn,
                deleteLibOn = deleteLibOn,
                isLandscape = isLandscape
            )
        }
    }
}


//DROPDOWN MENU:
@Composable
fun ChipOptions(
    mDisplayMenu: MutableState<Boolean>,
    deleteLibOn: MutableState<Boolean>,
    editLibOn: MutableState<Boolean>,
    idState: MutableState<Long>,
    nameState: MutableState<String>,
    id: Long,
    name: String
) {
    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: EDIT LIB ITEM
            OptionsItem(
                title = "Edit",
                iconVector = Icons.Default.Edit,
                onClick = {
                    idState.value = id
                    mDisplayMenu.value = false
                    editLibOn.value = true
                }
            )
            //2) Item: DELETE LIB ITEM
            OptionsItem(
                title = "Delete",
                iconVector = Icons.Default.Delete,
                onClick = {
                    idState.value = id
                    nameState.value = name
                    mDisplayMenu.value = false
                    deleteLibOn.value = true
                }
            )
        }
    )
}


@Composable
fun LibSectionContent(
    libraryItems: MutableState<List<String>>,
    currentCatState: MutableState<String>,
    idState: MutableState<Long>,
    nameState: MutableState<String>,
    editLibOn: MutableState<Boolean>,
    deleteLibOn: MutableState<Boolean>,
    isLandscape: Boolean
) {

    //CONTENT:
    if (libraryItems.value.isEmpty()) {
        //LIBRARY EMPTY:
        Text(
            text = "${currentCatState.value.replaceFirstChar { it.uppercase() }}s library\nis empty",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = colorResource(id = R.color.mid_grey),
            modifier = Modifier
                .fillMaxSize()
        )
    } else {
        //LIBRARY LIST:
        LazyVerticalGrid(
            modifier = Modifier
                .padding(start = 32.dp, end = 24.dp, bottom = 12.dp)
                .fillMaxSize(),
            columns = GridCells.Fixed(if (isLandscape) 3 else 2),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            //ITEMS:
            libraryItems.value.forEach { item ->

                if (item.contains(libSectionIdentifier)) {
                    //HEADER:
                    item(
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        LibLetter(
                            letter = item.replace(libSectionIdentifier, "")
                        )
                    }

                } else {
                    //ITEM:
                    item {
                        LibItem(
                            modifier = Modifier
                                .height(60.dp),
                            currentCatState = currentCatState,
                            idState = idState,
                            nameState = nameState,
                            itemJson = item,
                            editLibOn = editLibOn,
                            deleteLibOn = deleteLibOn
                        )
                        if (item == libraryItems.value.last()) Spacer(modifier = Modifier.padding(60.dp))
                    }
                }

            }
        }
    }
}


@Composable
fun LibLetter(
    letter: String
) {
    //TEXT LABEL:
    Column (
        modifier = Modifier
            .padding(top=4.dp)
    ) {
        //Item id:
        LetterStarter(
            text = letter.uppercase(),
            fontSize = 16.sp,
            backgroundColor = colorResource(id = R.color.white),
            borderColor = colorResource(id = R.color.dark_grey),
            fontColor = colorResource(id = R.color.black)
        )
    }
}


@Composable
fun LibItem(
    modifier: Modifier,
    currentCatState: MutableState<String>,
    idState: MutableState<Long>,
    nameState: MutableState<String>,
    itemJson: String,
    editLibOn: MutableState<Boolean>,
    deleteLibOn: MutableState<Boolean>
) {
    val mDisplayMenu = rememberSaveable { mutableStateOf(false) }
    val itemInfo = Json.decodeFromString<ItemInfoView>(itemJson)
    //Init aliases:
    val id: Long = itemInfo.id
    val itemName = itemInfo.name
    val itemImage = itemInfo.imageUrl
    val itemAliases = itemInfo.aliases.toMutableList()
    itemAliases.removeAt(0)

    //LIB CHIPS:
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        //Card:
        LibItemCard(
            modifier = modifier,
            cardColors = CardDefaults.cardColors(
                containerColor = if (mDisplayMenu.value) {
                    colorResource(id = R.color.dark_grey)
                } else {
                    colorResource(id = R.color.dark_grey_background)
                }
            ),
            cardBorderColor = colorResource(id = R.color.faded_grey),
            signBackgroundColor = libColorSelector(cat = currentCatState.value),
            signBorderColor = colorResource(id = R.color.midfaded_grey),
            signIconColor = colorResource(id = R.color.light_grey),
            signIconPainter = libIconSelector(cat = currentCatState.value),
            circle = currentCatState.value != "playlist" && currentCatState.value != "podcast" && currentCatState.value != "route",
            title = utils.trimString(itemName, 24),
            subtitle = if (currentCatState.value != "route" && itemAliases.isNotEmpty()) {
                    utils.trimString("\"" + itemAliases.joinToString("\", \"") + "\"", 16)
                } else if (itemInfo.detail != "") {
                    utils.trimString(itemInfo.detail, 16)
                } else "",
            imageUrl = itemImage,
            onClick = {
                mDisplayMenu.value = !mDisplayMenu.value
            }
        )
        //Options menu:
        ChipOptions(mDisplayMenu, deleteLibOn, editLibOn, idState, nameState, id=id, name=itemName)
    }
}


//DROPDOWN MENU:
@Composable
fun CatOptions(
    mContext: Context,
    libraryItems: MutableState<List<String>>,
    mDisplayMenu: MutableState<Boolean>,
    deleteLibOn: MutableState<Boolean>,
    head: String
) {
    var selectedJsonUri by remember { mutableStateOf<Uri?>(null) }
    var jsonImport by remember { mutableStateOf<String?>(null) }
    var jsonExport by remember { mutableStateOf<String?>(null) }

    // IMPORTER:
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Assign stream & read JSON content from the selected file
        selectedJsonUri = uri
        uri?.let {
            try {
                val inputStream = mContext.contentResolver.openInputStream(it)
                jsonImport = inputStream?.bufferedReader().use { reader -> reader?.readText() }
                //Store JSON into Library:
                libUtils.importLibrary(mContext, head, jsonImport!!)
                libraryItems.value = libUtils.refreshLibrary(head)   //Refresh list
            } catch (e: Exception) {
                Log.w("LibraryScreen", "ERROR: Cannot read imported document! ", e)
                Toast.makeText(mContext, "ERROR: Invalid file!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // EXPORTER:
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val outputStream = mContext.contentResolver.openOutputStream(it)
            outputStream?.bufferedWriter().use { writer ->
                writer?.write(jsonExport)
            }
        }
    }

    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: REFRESH LIB CATEGORY
            OptionsItem(
                title = "Refresh",
                iconVector = Icons.Default.Refresh,
                onClick = {
                    mDisplayMenu.value = false
                    libraryItems.value = libUtils.refreshLibrary(head)
                    Toast.makeText(mContext, "${head.replaceFirstChar { it.uppercase() }}s library updated!", Toast.LENGTH_SHORT).show()
                }
            )
            //2) Item: IMPORT CATEGORY ITEMS
            OptionsItem(
                title = "Import from file",
                iconVector = Icons.AutoMirrored.Filled.List,
                onClick = {
                    mDisplayMenu.value = false
                    filePickerLauncher.launch("application/json")   // Json MIME type
                }
            )
            //3) Item: EXPORT CATEGORY ITEMS
            OptionsItem(
                title = "Export to file",
                iconVector = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = {
                    mDisplayMenu.value = false
                    jsonExport = libUtils.serializeLibrary(head)
                    saveLauncher.launch("library_${head}s.json")   // Target filename
                    Toast.makeText(mContext, "${head.replaceFirstChar { it.uppercase() }}s library ready for export!", Toast.LENGTH_SHORT).show()
                }
            )
            //4) Item: SHARE LIB CATEGORY
            OptionsItem(
                title = "Share",
                iconVector = Icons.Default.Share,
                onClick = {
                    //Prepare and send cached file:
                    jsonExport = libUtils.serializeLibrary(head)
                    val filename = libUtils.buildFileToSend(mContext, head, jsonExport!!)
                    utils.sendCachedFile(mContext, filename)
                    mDisplayMenu.value = false
                }
            )
            //5) Item: DELETE LIB CATEGORY
            OptionsItem(
                title = "Delete all",
                iconVector = Icons.Default.Delete,
                onClick = {
                    mDisplayMenu.value = false
                    deleteLibOn.value = true
                }
            )
        }
    )
}


@Composable
fun DialogDeleteLibrary(
    mContext: Context,
    dialogOnState: MutableState<Boolean>,
    libraryItems: MutableState<List<String>>,
    idState: MutableState<Long>,
    nameState: MutableState<String>,
    filter: String
) {
    val id: Long = idState.value
    //DELETE DIALOG:
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = if (id > -1) "Delete $filter" else "Delete ${filter}s",
        content = {
            Text(
                text = if (id > -1) {
                    "Do you want to delete this $filter?\n\n${nameState.value}"
                } else {
                    "Do you want to delete all ${filter}s in library?"
                },
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "No",
        onDismiss = {
            dialogOnState.value = false
            idState.value = -1
            nameState.value = ""
        },
        confirmText = "Yes",
        onConfirm = {
            if (id > -1) {
                libUtils.deleteLibraryItem(mContext, filter, id)
            } else {
                //Delete all:
                libUtils.deleteLibrary(mContext, filter)
            }
            libraryItems.value = libUtils.refreshLibrary(filter)   //Refresh list
            dialogOnState.value = false
            idState.value = -1
            nameState.value = ""
        }
    )
}
