package com.ftrono.DJames.application.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.AuthActivity
import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.application.copyrightYear
import com.ftrono.DJames.application.datetimeExportFormat
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.genders
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.showLoggingIn
import com.ftrono.DJames.application.spotUserImageState
import com.ftrono.DJames.application.spotUserName
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.spotifyLoginUtils
import com.ftrono.DJames.application.userGender
import com.ftrono.DJames.application.userNicknameUI
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.models.ZipBackup
import com.ftrono.DJames.ui.components.DropdownSpinner
import com.ftrono.DJames.ui.components.EditLibDynamicNameSection
import com.ftrono.DJames.ui.components.ExtServiceAccountItem
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.components.SettingsSection
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.dialogs.DialogLoading
import com.ftrono.DJames.ui.navigation.DialogLogout
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun AccountsScreenPreview() {
    val navController = rememberNavController()
    AccountsScreen(navController, preview=true)
}

@Composable
fun AccountsScreen(navController: NavController, preview: Boolean = false) {
    val mContext = LocalContext.current
    val extraOpenState by extraOpen.observeAsState()

    // Profile:
    val textHeaderColor = colorResource(id = R.color.light_grey)
    val userImage by spotUserImageState.observeAsState()
    val userNicknameUIState by userNicknameUI.observeAsState()
    val genderState = rememberSaveable { mutableStateOf(if (preview) "Sir" else prefs.userGender) }
    val textNickName = rememberSaveable { mutableStateOf(if (preview) genderState.value else userNicknameUI.value!!) }

    // Ext services:
    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()
    val spotUserNameState by spotUserName.observeAsState()

    // LOGIN / LOGOUT:
    val lifecycleOwner = LocalLifecycleOwner.current
    val logoutDialogOn = rememberSaveable { mutableStateOf(false) }
    if (logoutDialogOn.value) {
        DialogLogout(
            mContext,
            logoutDialogOn,
            navController,
            extraOpenState!!
        )
    }

    val dialogLoggingInOn by showLoggingIn.observeAsState()
    if (dialogLoggingInOn!!) {
        DialogLoading(
            text = "Logging in to Spotify..."
        )
        spotifyLoginUtils.getSpotifyUserData(
            context = mContext,
            navController = navController,
            scope = lifecycleOwner.lifecycle.coroutineScope
        )
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current


    // BACKUP & RESTORE:
    /*
        library_backup.zip
        ├── prefs.json
        └── library.json
    */


    var jsonPrefs by remember { mutableStateOf<String?>(null) }
    var jsonLibrary by remember { mutableStateOf<String?>(null) }

    // Helpers:
    fun readJsonsFromZip(inputStream: InputStream): ZipBackup {
        var jsonPrefs: String? = null
        var jsonLibrary: String? = null

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val buffer = ByteArrayOutputStream()
                val data = ByteArray(1024)
                var count: Int

                while (zip.read(data).also { count = it } != -1) {
                    buffer.write(data, 0, count)
                }

                val content = buffer.toString(Charsets.UTF_8.name())
                when (entry.name) {
                    "prefs.json" -> {
                        jsonPrefs = content
                    }
                    "library.json" -> {
                        jsonLibrary = content
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return ZipBackup(
            jsonPrefs=jsonPrefs?: "",
            jsonLibrary=jsonLibrary?: "",
        )
    }

    fun writeJsonsToZip(
        outputStream: OutputStream,
        jsonPrefs: String,
        jsonLibrary: String
    ) {
        ZipOutputStream(outputStream).use { zip ->

            zip.putNextEntry(ZipEntry("prefs.json"))
            zip.write(jsonPrefs.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("library.json"))
            zip.write(jsonLibrary.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                mContext.contentResolver.openInputStream(it)?.use { inputStream ->
                    readJsonsFromZip(inputStream).also { result ->
                        jsonPrefs = result.jsonPrefs
                        jsonLibrary = result.jsonLibrary
                    }
                }

                // Update prefs using imported JSON:
                jsonPrefs?.let {
                    utils.importSharedPreferences(mContext, it)
                }

                // Update library using imported JSON:
                jsonLibrary?.let {
                    libUtils.importLibrary(mContext, it)
                }

                Toast.makeText(mContext, "Data restored successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w("AccountsScreen", "ERROR: Cannot read ZIP!", e)
                Toast.makeText(mContext, "ERROR: Invalid ZIP backup!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            try {
                mContext.contentResolver.openOutputStream(it)?.use { outputStream ->
                    writeJsonsToZip(
                        outputStream = outputStream,
                        jsonPrefs = jsonPrefs ?: "",
                        jsonLibrary = jsonLibrary ?: ""
                    )
                }
                Toast.makeText(mContext, "Data exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w("AccountsScreen", "ERROR: Cannot write ZIP!", e)
                Toast.makeText(mContext, "ERROR: Cannot save ZIP!", Toast.LENGTH_SHORT).show()
            }
        }
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
                title = "Accounts",
                subtitle = "Your profile & services",
                showBack = true,
                onBack = {
                    navController.popBackStack()
                    // Toast.makeText(mContext, "Account preferences saved!", Toast.LENGTH_SHORT).show()
                },
                optionButtons = { }
            )
        }
    ) {
        //ACCOUNTS SETTINGS LIST:
        Column(
            modifier = Modifier
                .padding(top = 10.dp, start = 32.dp, end = 24.dp, bottom = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {

            //SECTION: PROFILE:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, bottom=4.dp),
                title = "Your profile",
                signColor = colorResource(id = R.color.yellowSign),
                iconPainter = painterResource(id = R.drawable.icon_user)
            ) {
                LaunchedEffect(userNicknameUIState!!) {
                    if (!preview) {
                        textNickName.value = userNicknameUI.value!!
                    }
                }

                //Title:
                Text(
                    modifier = Modifier
                        .padding(top=4.dp, bottom = 8.dp),
                    text = "Write a nickname for you",
                    color = textHeaderColor,
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                //USER NICKNAME:
                EditLibDynamicNameSection(
                    modifier = Modifier
                        .fillMaxWidth(),
                    filter = "user",
                    textState = textNickName,
                    imageUrl = userImage!!,
                    preview = preview,
                    onClick = {
                        // Store new nickname to prefs!:
                        if (textNickName.value != "") {
                            textNickName.value = utils.cleanString(textNickName.value)
                            userNicknameUI.value = textNickName.value
                            prefs.userNickname = textNickName.value
                        } else {
                            textNickName.value = userNicknameUI.value!!
                        }
                    }
                )

                //USER GENDER:
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    text = "Refer to yourself as",
                    color = textHeaderColor,
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                DropdownSpinner(
                    context = mContext,
                    parentOptions = genders,
                    init = genderState.value,
                    state = genderState,
                    focusColorLight = colorResource(id = R.color.greenSignLight),
                    focusColorDark = colorResource(id = R.color.greenSign),
                    optionsBackground = colorResource(id = R.color.dark_grey),
                    prefName = "userGender",
                    width = 200
                )
            }


            //SECTION: EXT SERVICES:
            SettingsSection(
                modifier = Modifier
                    .padding(top=8.dp, end=8.dp, bottom=4.dp),
                title = "Your services",
                subtitle = "Connect your music apps",
                signColor = colorResource(id = R.color.greenSign),
                iconPainter = painterResource(id = R.drawable.icon_headphones)
            ) {

                // 1) Spotify:
                ExtServiceAccountItem(
                    modifier = Modifier,
                    name = "Spotify",
                    backgroundColor = colorResource(R.color.colorPrimary),
                    iconPainter = painterResource(id = R.drawable.logo_spotify),
                    loggedInState = spotifyLoggedInState!!,
                    userNameState = spotUserNameState!!,
                    onClick = {
                        if (!spotifyLoggedInState!!) {
                            //Login user -> Open WebView:
                            val intent1 = Intent(mContext, AuthActivity::class.java)
                            mContext.startActivity(intent1)
                        } else {
                            //LOG OUT:
                            logoutDialogOn.value = true
                        }
                    }
                )
            }

            //SECTION: BACKUP & RESTORE DATA:
            SettingsSection(
                modifier = Modifier
                    .padding(top=8.dp, end=8.dp, bottom=4.dp),
                title = "Backup & restore",
                subtitle = "Saved items & app preferences",
                signColor = colorResource(id = R.color.redSignDark),
                iconVector = Icons.Default.Build,
            ) {

                // Export data:
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup & export data",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    RoundedSign(
                        signSize = 40.dp,
                        contentSize = 20,
                        backgroundColor = colorResource(id = R.color.redSignDark),
                        borderColor = colorResource(id = R.color.redSignDark),
                        contentColor = colorResource(id = R.color.light_grey),
                        iconPainter = painterResource(R.drawable.icon_download),
                        clickable = true,
                        onClick = {
                            //EXPORT:
                            jsonLibrary = libUtils.serializeLibrary()
                            jsonPrefs = utils.exportSharedPreferences(mContext)
                            saveLauncher.launch("djames_backup_${utils.convertTimestamp(utils.getCurrentTimestamp(), datetimeExportFormat)}.zip")   // Target filename
                            Toast.makeText(mContext, "Backup ready for export!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Import data:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top=8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Restore data from backup",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    RoundedSign(
                        signSize = 40.dp,
                        contentSize = 20,
                        backgroundColor = colorResource(id = R.color.redSignDark),
                        borderColor = colorResource(id = R.color.redSignDark),
                        contentColor = colorResource(id = R.color.light_grey),
                        iconVector = Icons.AutoMirrored.Default.ExitToApp,
                        clickable = true,
                        onClick = {
                            //IMPORT:
                            filePickerLauncher.launch("application/zip")   // Json MIME type
                        }
                    )
                }
            }


            //FINAL INFO:
            //App version:
            Text(
                modifier = Modifier
                    .padding(top = 30.dp)
                    .fillMaxWidth(),
                text = "Version $appVersion",
                color = colorResource(id = R.color.midfaded_grey),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )

            //App Copyright:
            Text(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                text = "Copyright © Francesco Trono ($copyrightYear)",
                color = colorResource(id = R.color.midfaded_grey),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )

        }
    }
}