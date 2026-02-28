package com.ftrono.DJames.application.screens

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.curLibrarySize
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.libCats
import com.ftrono.DJames.application.dialogs.EditLibContact
import com.ftrono.DJames.application.dialogs.EditLibPlace
import com.ftrono.DJames.application.dialogs.EditLibSpotify
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.components.OptionsItem
import com.ftrono.DJames.ui.components.OptionsMenu
import com.ftrono.DJames.ui.components.LetterStarter
import com.ftrono.DJames.ui.components.LibItemCard
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.dialogs.AddLinkDialog
import com.ftrono.DJames.ui.navigation.FiltersRow
import com.ftrono.DJames.ui.navigation.SplitterSign
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.TopBarMenu
import com.ftrono.DJames.ui.navigation.TopSplitterBar
import com.ftrono.DJames.ui.selectors.libColorSelector
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
    val currentCatState = rememberSaveable { mutableStateOf(libCats[0]) }
    val currentSubCatState = rememberSaveable { mutableStateOf("") }

    val sharedLinkState by sharedLink.observeAsState()
    val addLinkOn = rememberSaveable { mutableStateOf(false) }
    val useParentState = rememberSaveable { mutableStateOf(false) }
    val extractedItemState = rememberSaveable { mutableStateOf("") }

    val snapshot = rememberSaveable { mutableStateOf(0L) }
    val curLibrarySizeState by curLibrarySize.observeAsState()

    val deleteLibOn = rememberSaveable { mutableStateOf(false) }
    if (deleteLibOn.value) {
        DialogDeleteLibrary(mContext, deleteLibOn, snapshot, idState, nameState, currentCatState, currentSubCatState)
    }

    val editLibOn = rememberSaveable { mutableStateOf(editPreview != "") }
    var editCat = if (editPreview != "") editPreview else if (editLibOn.value) currentCatState.value else ""

    if (editLibOn.value) {
        if (editCat == "spotify") {
            EditLibSpotify(
                context = mContext,
                snapshot = snapshot,
                extractedItemState = extractedItemState,
                idState = idState,
                onDismiss = {
                    //cancelable -> true
                    addLinkOn.value = false
                    editLibOn.value = false
                    idState.value = -1
                    sharedLink.postValue("")
                    extractedItemState.value = ""
                },
                preview = preview
            )
        } else if (editCat == "contact") {
            EditLibContact(
                context = mContext,
                snapshot = snapshot,
                idState = idState,
                onDismiss = {
                    //cancelable -> true
                    editLibOn.value = false
                    idState.value = -1
                },
                preview = preview
            )
        } else if (editCat == "place") {
            EditLibPlace(
                context = mContext,
                snapshot = snapshot,
                idState = idState,
                onDismiss = {
                    //cancelable -> true
                    editLibOn.value = false
                    idState.value = -1
                },
                preview = preview
            )
        }
    }

    if (sharedLinkState != "" && !addLinkOn.value) {
        addLinkOn.value = true
    }

    if (addLinkOn.value) {
        AddLinkDialog(
            dialogHeader = "New",
            textBoxHeader = "Save a Spotify link",
            cat = "spotify",   //TODO
            useParentState = useParentState,
            onDismiss = {
                //cancelable -> true
                idState.value = -1
                useParentState.value = false
                sharedLink.postValue("")
                addLinkOn.value = false
            },
            onSave = {
                //TODO: Spotify only!
                Toast.makeText(mContext, "Extracting link info...", Toast.LENGTH_LONG).show()
                spotifyUtils.checkLinkAndExtract(
                    context = mContext,
                    idState = idState,
                    currentCatState = currentCatState,
                    currentSubCatState = currentSubCatState,
                    useParent = useParentState.value,
                    addLinkOnState = addLinkOn,
                    extractedItemState = extractedItemState,
                    editLibOn = editLibOn,
                )
                useParentState.value = false
                snapshot.value = utils.getCurrentTimestamp()   //Refresh list
            }
        )
    }

    var mDisplayMainMenu = rememberSaveable {
        mutableStateOf(false)
    }

    //SCREEN:
    StreetUIScaffold(
        lineDistance = 20.dp,
        topBar = {
            if (!isLandscape) {
                // VERTICAL -> TOP APP BAR:
                StreetUITopBar(
                    pretitle = "Saved items",
                    title = if (currentCatState.value == "spotify") "Spotify links" else "${utils.capitalizeWords(currentCatState.value)}s",
                    showBack = true,
                    onBack = { navController.popBackStack() },
                    optionButtons = {
                        // CAT MENU:
                        TopBarMenu(
                            contentText = if (curLibrarySizeState!! > 999) "999+" else "$curLibrarySizeState",
                            backgroundColor = libColorSelector(cat = currentCatState.value),
                            onClick = { mDisplayMainMenu.value = !mDisplayMainMenu.value },
                        ) {
                            CatOptions(
                                context = mContext,
                                navController = navController,
                                currentCatState = currentCatState,
                                currentSubCatState = currentSubCatState,
                                snapshot = snapshot,
                                mDisplayMenu = mDisplayMainMenu,
                                deleteLibOn = deleteLibOn,
                            )
                        }
                    }
                )
            } else {
                // HORIZONTAL -> TOP SPLITTER BAR:
                TopSplitterBar(
                    currentCatState = currentCatState,
                    currentSubCatState = currentSubCatState,
                    showBack = true,
                    onBack = { navController.popBackStack() },
                    onNavClick = {
                        snapshot.value = utils.getCurrentTimestamp()   //Refresh list
                    },
                    optionButtons = {
                        // CAT MENU:
                        TopBarMenu(
                            contentText = if (curLibrarySizeState!! > 999) "999+" else "$curLibrarySizeState",
                            backgroundColor = libColorSelector(cat = currentCatState.value),
                            onClick = { mDisplayMainMenu.value = !mDisplayMainMenu.value },
                        ) {
                            CatOptions(
                                context = mContext,
                                navController = navController,
                                currentCatState = currentCatState,
                                currentSubCatState = currentSubCatState,
                                snapshot = snapshot,
                                mDisplayMenu = mDisplayMainMenu,
                                deleteLibOn = deleteLibOn,
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
                    if (currentCatState.value == "contact" || currentCatState.value == "place") {
                        editLibOn.value = true
                    } else {
                        addLinkOn.value = true
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

            if (!isLandscape) {
                // VERTICAL -> TOP SPLITTER:
                Row (
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .background(
                            colorResource(R.color.transparent_full)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    SplitterSign(
                        currentCatState = currentCatState,
                        currentSubCatState = currentSubCatState,
                        onNavClick = {
                            snapshot.value = utils.getCurrentTimestamp()   //Refresh list
                        },
                    )
                }
            }

            //CONTENT:
            LibSectionContent(
                context = mContext,
                snapshot = snapshot,
                currentCatState = currentCatState,
                currentSubCatState = currentSubCatState,
                idState = idState,
                nameState = nameState,
                editLibOn = editLibOn,
                deleteLibOn = deleteLibOn,
                isLandscape = isLandscape,
                preview = preview,
            )
        }
    }
}


//DROPDOWN MENU:
@Composable
fun ItemOptions(
    context: Context,
    mDisplayMenu: MutableState<Boolean>,
    deleteLibOn: MutableState<Boolean>,
    editLibOn: MutableState<Boolean>,
    idState: MutableState<Long>,
    nameState: MutableState<String>,
    item: LibraryItem,
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
                    idState.value = item.id
                    mDisplayMenu.value = false
                    editLibOn.value = true
                }
            )
            //2) Item: OPEN LINK
            if (item.id != -2L) {
                OptionsItem(
                    title = if (item.source == "contact") "Call" else "Open link   ",
                    iconVector = if (item.source == "contact") Icons.Default.Call else Icons.AutoMirrored.Default.ArrowForward,
                    onClick = {
                        mDisplayMenu.value = false
                        if (item.source == "contact") {
                            val contactPhone = "${item.phoneSet!!.prefix}${item.phoneSet!!.phone}"
                            utils.makeCall(context, contactPhone = contactPhone, fromService = false)
                        } else {
                            utils.openLink(context, url = item.url, fromService = false)
                        }
                    }
                )
            }
            //3) Item: DELETE LIB ITEM
            if (item.id != -2L) {   //Default
                OptionsItem(
                    title = "Delete",
                    iconVector = Icons.Default.Delete,
                    onClick = {
                        idState.value = item.id
                        nameState.value = item.name
                        mDisplayMenu.value = false
                        deleteLibOn.value = true
                    }
                )
            }
        }
    )
}


@Composable
fun LibSectionContent(
    context: Context,
    snapshot: MutableState<Long>,
    currentCatState: MutableState<String>,
    currentSubCatState: MutableState<String>,
    idState: MutableState<Long>,
    nameState: MutableState<String>,
    editLibOn: MutableState<Boolean>,
    deleteLibOn: MutableState<Boolean>,
    isLandscape: Boolean,
    preview: Boolean = false,
) {

    var libraryItems = libUtils.refreshLibrary(currentCatState.value, currentSubCatState.value, preview)

    // When snapshot changes, reload data
    LaunchedEffect(snapshot.value) {
        libraryItems = libUtils.refreshLibrary(currentCatState.value, currentSubCatState.value, preview)
    }

    //CONTENT:
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // FILTER ROW:
        FiltersRow(
            snapshot = snapshot,
            currentCatState = currentCatState,
            currentSubCatState = currentSubCatState,
            preview = preview,
        )

        if (libraryItems.isEmpty()) {
            //LIBRARY EMPTY:
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "No saved ${
                    libUtils.getLibName(
                        currentCatState.value,
                        currentSubCatState.value,
                        plural = true,
                        lowercase = true
                    )
                }.\nTap on Add!",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                color = colorResource(id = R.color.mid_grey),
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
                libraryItems.forEach { item ->

                    if (item.type == "header") {
                        //HEADER:
                        item(
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            LibLetter(
                                letter = item.name
                            )
                        }

                    } else {
                        //ITEM:
                        item {
                            LibItem(
                                context = context,
                                modifier = Modifier,
                                idState = idState,
                                nameState = nameState,
                                item = item,
                                editLibOn = editLibOn,
                                deleteLibOn = deleteLibOn,
                                preview = preview,
                            )
                            if (item == libraryItems.last()) Spacer(modifier = Modifier.padding(80.dp))
                        }
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
    context: Context,
    modifier: Modifier,
    idState: MutableState<Long>,
    nameState: MutableState<String>,
    item: LibraryItem,
    editLibOn: MutableState<Boolean>,
    deleteLibOn: MutableState<Boolean>,
    preview: Boolean = false,
) {
    val mDisplayMenu = rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        //Card:
        LibItemCard(
            modifier = modifier
                .height(70.dp),
            cardColors = CardDefaults.cardColors(
                containerColor = if (mDisplayMenu.value) {
                    colorResource(id = R.color.dark_grey)
                } else {
                    colorResource(id = R.color.dark_grey_background)
                }
            ),
            source = item.source,
            type = item.type,
            title = utils.trimString(item.name, 20),
            subtitle = utils.trimString(libUtils.getDetail(item), 16),
            imageUrl = if (preview) "" else item.imageUrl,
            isCollection = item.id == -2L,
            onClick = {
                mDisplayMenu.value = !mDisplayMenu.value
            }
        )
        //Options menu:
        ItemOptions(
            context = context,
            mDisplayMenu = mDisplayMenu,
            deleteLibOn = deleteLibOn,
            editLibOn = editLibOn,
            idState = idState,
            nameState = nameState,
            item = item,
        )
    }
}


//DROPDOWN MENU:
@Composable
fun CatOptions(
    context: Context,
    navController: NavController,
    currentCatState: MutableState<String>,
    currentSubCatState: MutableState<String>,
    snapshot: MutableState<Long>,
    mDisplayMenu: MutableState<Boolean>,
    deleteLibOn: MutableState<Boolean>,
) {
    var jsonImport by remember { mutableStateOf<String?>(null) }
    var jsonExport by remember { mutableStateOf<String?>(null) }

    // IMPORTER:
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Assign stream & read JSON content from the selected file
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                jsonImport = inputStream?.bufferedReader().use { reader -> reader?.readText() }
                //Store JSON into Library:
                libUtils.importLibrary(context, jsonImport!!)
                currentSubCatState.value = ""
                snapshot.value = utils.getCurrentTimestamp()   //Refresh list
            } catch (e: Exception) {
                Log.w("LibraryScreen", "ERROR: Cannot read imported document! ", e)
                Toast.makeText(context, "ERROR: Invalid file!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // EXPORTER:
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
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
                    snapshot.value = utils.getCurrentTimestamp()   //Refresh list
                    Toast.makeText(context, "Refreshed!", Toast.LENGTH_SHORT).show()
                }
            )
            //2) Item: IMPORT CATEGORY ITEMS
            OptionsItem(
                title = "Import from file",
                iconVector = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = {
                    mDisplayMenu.value = false
                    filePickerLauncher.launch("application/json")   // Json MIME type
                }
            )
            //3) Item: EXPORT CATEGORY ITEMS
            OptionsItem(
                title = "Export to file",
                iconPainter = painterResource(R.drawable.icon_download),
                onClick = {
                    mDisplayMenu.value = false
                    jsonExport = libUtils.serializeLibrary(currentCatState.value, currentSubCatState.value)
                    saveLauncher.launch(libUtils.getExportFileName(currentCatState.value, currentSubCatState.value))   // Target filename
                    val detailStr = libUtils.getLibName(currentCatState.value, currentSubCatState.value, plural = true)
                    Toast.makeText(context, "$detailStr library ready for export!", Toast.LENGTH_SHORT).show()
                }
            )
            //4) Item: SHARE LIB CATEGORY
            OptionsItem(
                title = "Share",
                iconVector = Icons.Default.Share,
                onClick = {
                    //Prepare and send cached file:
                    jsonExport = libUtils.serializeLibrary(currentCatState.value, currentSubCatState.value)
                    val filename = libUtils.buildFileToSend(context, currentCatState.value, currentSubCatState.value, jsonExport!!)
                    utils.sendCachedFile(context, filename)
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
    snapshot: MutableState<Long>,
    idState: MutableState<Long>,
    nameState: MutableState<String>,
    currentCatState: MutableState<String>,
    currentSubCatState: MutableState<String>,
) {
    val id: Long = idState.value
    // DELETE DIALOG:
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = if (id == -1L) "Delete items" else "Delete item",
        content = {
            // if id == -1L -> no specific ID!
            Text(
                text = if (id == -1L) {
                    "Do you want to delete all saved ${libUtils.getLibName(currentCatState.value, currentSubCatState.value, plural=true, lowercase=true)}?"
                } else {
                    "Do you want to delete this saved item?\n\n${nameState.value}"
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
                libUtils.deleteLibItem(mContext, id)
            } else {
                //Delete all:
                libUtils.deleteLibrary(mContext, currentCatState.value, currentSubCatState.value)
            }
            currentSubCatState.value = ""
            snapshot.value = utils.getCurrentTimestamp()   //Refresh list
            dialogOnState.value = false
            idState.value = -1
            nameState.value = ""
        }
    )
}
