package com.ftrono.DJames.application.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.application.autoStopQueriesState
import com.ftrono.DJames.application.copyrightYear
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.services.OverlayService
import com.ftrono.DJames.ui.components.DropdownSpinner
import com.ftrono.DJames.ui.components.SettingsSection
import com.ftrono.DJames.ui.components.SettingsUserSection
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.selectors.getSwitchColors
import com.ftrono.DJames.ui.selectors.getTextFieldColors

@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun SettingsScreenPreview() {
    val navController = rememberNavController()
    SettingsScreen(navController, preview=true)
}

@Composable
fun SettingsScreen(navController: NavController, preview: Boolean = false) {
    val mContext = LocalContext.current
    //TODO: STATUSES:
    val checkedV3 = remember { mutableStateOf(if (preview) true else prefs.enableV3) }
    val checkedStartup = remember { mutableStateOf(if (preview) true else prefs.autoStartup) }
    val checkedSilenceQueries by autoStopQueriesState.observeAsState()
    val checkedSilenceMess = remember { mutableStateOf(if (preview) true else prefs.silenceEnabledMess) }
    val checkedAutoClock = remember { mutableStateOf(if (preview) true else prefs.autoClock) }
    val checkedClockRedirect = remember { mutableStateOf(if (preview) true else prefs.clockRedirectEnabled) }
    val checkedVolumeEnabled = remember { mutableStateOf(if (preview) true else prefs.volumeUpEnabled) }
    var recTimeout by rememberSaveable { mutableStateOf(if (preview) "5" else prefs.recTimeout) }
    var messTimeout by rememberSaveable { mutableStateOf(if (preview) "5" else prefs.messageTimeout) }
    var clockTimeout by rememberSaveable { mutableStateOf(if (preview) "5" else prefs.clockTimeout) }
    //val textQueryLangState = rememberSaveable { mutableStateOf(if (preview) "English" else queryLangFull[queryLangCodes.indexOf(prefs.queryLanguage)]) }
    val textMessLangState = rememberSaveable { mutableStateOf(if (preview) "English" else messLangFull[messLangCodes.indexOf(prefs.messageLanguage)]) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // SCREEN:
    StreetUIScaffold(
        modifier = Modifier
            .clickable {
                focusManager.clearFocus()   //TODO
            },
        lineDistance = 20.dp,
        topBar = {
            StreetUITopBar(
                pretitle = "",
                title = "Preferences",
                subtitle = "Profile & settings",
                showBack = true,
                onBack = { navController.popBackStack() },
                optionButtons = {
                    //SAVE BUTTON:
                    Icon(
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .size(35.dp)
                            .clickable {
                                saveSettings(
                                    mContext,
                                    newRecTimeout = recTimeout,
                                    newMessTimeout = messTimeout,
                                    newClockTimeout = clockTimeout
                                )
                                navController.popBackStack()
                            },
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = colorResource(id = R.color.light_grey)
                    )
                }
            )
        }
    ) {
        //SETTINGS LIST:
        Column(
            modifier = Modifier
                .padding(top = 10.dp, start = 32.dp, end = 24.dp, bottom = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {

            //SECTION: USER NICKNAME:
            SettingsUserSection(
                modifier = Modifier
                    .padding(bottom = 4.dp),
                textHeaderColor = colorResource(id = R.color.greenSignLight),
                textFieldColors = getTextFieldColors(
                    colorLight = colorResource(id = R.color.greenSignLight),
                    colorDark = colorResource(id = R.color.greenSign)
                ),
                preview = preview
            )

            //SECTION: OVERLAY BUTTON:
            SettingsSection(
                modifier = Modifier
                    .padding(top=8.dp, end=8.dp, bottom=4.dp),
                title = "Overlay button",
                signColor = colorResource(id = R.color.greenSign),
                iconPainter = painterResource(id = R.drawable.sign_touch)
            ) {

                //Auto Startup:
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Start Overlay when app is opened",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = checkedStartup.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.greenSign)
                        ),
                        onCheckedChange = {
                            //UPDATE:
                            checkedStartup.value = it
                            prefs.autoStartup = it
                        }
                    )
                }
            }


            //SECTION: VOICE QUERIES:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, top=16.dp, bottom=4.dp),
                title = "Voice queries",
                signColor = colorResource(id = R.color.yellowSign),
                iconPainter = painterResource(id = R.drawable.icon_speak)
            ) {

                //Voice queries: Enable v3:
                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice queries: enable v3 (alpha)",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = checkedV3.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.yellowSign)
                        ),
                        onCheckedChange = {
                            prefs.enableV3 = it
                        }
                    )
                }

                //Req timeout:
                Text(
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                        //.padding(top=8.dp, bottom = 4.dp),
                    text = "Voice queries: timeout recording after",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
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
                    value = recTimeout,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            saveSettings(mContext, newRecTimeout=recTimeout, newMessTimeout=messTimeout, newClockTimeout=clockTimeout)
                        }
                    ),
                    suffix = {
                        Text(
                            text = "seconds",
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic
                        )
                    },
                    supportingText = {
                        Text(
                            text = "(keep between 5 and 15)"
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Write here...",
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic
                        )
                    },
                    onValueChange = { newText ->
                        recTimeout = newText.trimStart { it == '0' }
                        //TODO
                    }
                )

                //Voice queries: Silence detection:
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice queries: Stop recording\nwhen silence is detected",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = checkedSilenceQueries!!,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.yellowSign)
                        ),
                        onCheckedChange = {
                            autoStopQueriesState.postValue(it)
                            prefs.silenceEnabledQueries = it
                        }
                    )
                }
            }


            //SECTION: MESSAGING:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, top=16.dp, bottom=4.dp),
                title = "Messaging",
                signColor = colorResource(id = R.color.blueSign),
                iconPainter = painterResource(id = R.drawable.sign_message)
            ) {

                //Mess language:
                Text(
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    text = "Messages: default language",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                DropdownSpinner(
                    context = mContext,
                    parentOptions = messLangFull,
                    init = textMessLangState.value,
                    state = textMessLangState,
                    focusColorLight = colorResource(id = R.color.blueSignLight),
                    focusColorDark = colorResource(id = R.color.blueSign),
                    prefName = "messageLanguage",
                    width = 200
                )

                //Mess timeout:
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp),
                    text = "Messages: timeout recording after",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 20.dp)
                        .width(250.dp)
                        .wrapContentHeight()
                        .focusRequester(focusRequester),
                    colors = getTextFieldColors(
                        colorLight = colorResource(id = R.color.blueSignLight),
                        colorDark = colorResource(id = R.color.blueSign)
                    ),
                    value = messTimeout,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            saveSettings(
                                mContext,
                                newRecTimeout = recTimeout,
                                newMessTimeout = messTimeout,
                                newClockTimeout = clockTimeout
                            )
                        }
                    ),
                    suffix = {
                        Text(
                            text = "seconds",
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic
                        )
                    },
                    supportingText = {
                        Text(
                            text = "(keep between 5 and 20)"
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Write here...",
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic
                        )
                    },
                    onValueChange = { newText ->
                        messTimeout = newText.trimStart { it == '0' }
                        //TODO
                    }
                )

                //Messages: Silence detection:
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Messages: Stop recording\nwhen silence is detected",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = checkedSilenceMess.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.blueSign)
                        ),
                        onCheckedChange = {
                            checkedSilenceMess.value = it
                            prefs.silenceEnabledMess = it
                        }
                    )
                }
            }


            //SECTION: ROUTES:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, top=16.dp, bottom=4.dp),
                title = "Routes",
                signColor = colorResource(id = R.color.brownSign),
                iconPainter = painterResource(id = R.drawable.sign_place)
            ) {

                //Routes language:
                Text(
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    text = "Routes: default language",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                DropdownSpinner(
                    context = mContext,
                    parentOptions = messLangFull,
                    init = textMessLangState.value,
                    state = textMessLangState,
                    focusColorLight = colorResource(id = R.color.brownSignLight),
                    focusColorDark = colorResource(id = R.color.brownSign),
                    prefName = "routeLanguage",
                    width = 200
                )
            }


            //SECTION: CLOCK SCREEN:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, top=16.dp, bottom=4.dp),
                title = "Clock screen",
                signColor = colorResource(id = R.color.dark_grey),
                iconPainter = painterResource(id = R.drawable.icon_clock)
            ) {

                //Auto Clock:
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show Clock screen when Overlay\nis started",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = checkedAutoClock.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.faded_grey)
                        ),
                        onCheckedChange = {
                            checkedAutoClock.value = it
                            prefs.autoClock = it
                        }
                    )
                }

                //Clock redirect:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .offset(y = -(8.dp)),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Go back automatically to Clock screen",
                            color = colorResource(id = R.color.light_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(Only when Spotify is launched\nfor the first time)",
                            color = colorResource(id = R.color.mid_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = checkedClockRedirect.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.faded_grey)
                        ),
                        onCheckedChange = {
                            checkedClockRedirect.value = it
                            prefs.clockRedirectEnabled = it
                        }
                    )
                }

                if (checkedClockRedirect.value) {
                    //After (timeout):
                    OutlinedTextField(
                        modifier = Modifier
                            .width(250.dp)
                            .wrapContentHeight()
                            .focusRequester(focusRequester),
                        colors = getTextFieldColors(
                            colorLight = colorResource(id = R.color.mid_grey),
                            colorDark = colorResource(id = R.color.faded_grey)
                        ),
                        value = clockTimeout,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                saveSettings(mContext, newRecTimeout=recTimeout, newMessTimeout=messTimeout, newClockTimeout=clockTimeout)
                            }
                        ),
                        prefix = {
                            Text(
                                text = "after     ",
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic
                            )
                        },
                        suffix = {
                            Text(
                                text = "seconds",
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic
                            )
                        },
                        supportingText = {
                            Text(
                                text = "(keep between 5 and 30)"
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Write here...",
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic
                            )
                        },
                        onValueChange = { newText ->
                            clockTimeout = newText.trimStart { it == '0' }
                            //TODO
                        }
                    )
                }
            }


            //SECTION: ADVANCED:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, top=16.dp, bottom=4.dp),
                title = "Advanced",
                signColor = colorResource(id = R.color.redSign),
                iconPainter = painterResource(id = R.drawable.sign_warning)
            ) {

                //VolumeUp enabled:
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .offset(y = -(8.dp)),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "VOLUME-UP key starts recording",
                            color = colorResource(id = R.color.light_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Keep this enabled if you use\nBluetooth remotes!",
                            color = colorResource(id = R.color.mid_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = checkedVolumeEnabled.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.redSign)
                        ),
                        onCheckedChange = {
                            checkedVolumeEnabled.value = it
                            prefs.volumeUpEnabled = it
                            restartOverlay(mContext)
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


//Validate timeout before saving:
fun validateTimeout(newVal: String, origVal: String, min_val: Int, max_val: Int) : String {
    val newInt = newVal.toInt()
    return if (newInt in min_val..max_val) {
        newVal
    } else {
        origVal
    }
}


//Save Settings:
fun saveSettings(mContext: Context, newRecTimeout: String, newMessTimeout: String, newClockTimeout: String) {
    //RecTimeout:
    if (newRecTimeout.isNotEmpty()) {
        //validate & overwrite:
        prefs.recTimeout = validateTimeout(newVal = newRecTimeout, origVal = prefs.recTimeout, min_val = 5, max_val = 15)
    }
    //MessageTimeout:
    if (newMessTimeout.isNotEmpty()) {
        //validate & overwrite:
        prefs.messageTimeout = validateTimeout(newVal = newMessTimeout, origVal = prefs.messageTimeout, min_val = 5, max_val = 20)
    }
    //ClockTimeout:
    if (newClockTimeout.isNotEmpty()) {
        //validate & overwrite:
        prefs.clockTimeout = validateTimeout(newVal = newClockTimeout, origVal = prefs.clockTimeout, min_val = 5, max_val = 30)
    }
    Toast.makeText(mContext, "Preferences saved!", Toast.LENGTH_SHORT).show()
}


//Restart Overlay:
fun restartOverlay(mContext: Context) {
    //Restart overlay service:
    if (utils.isMyServiceRunning(OverlayService::class.java, mContext)) {
        mContext.stopService(Intent(mContext, OverlayService::class.java))
        if (!utils.isMyServiceRunning(OverlayService::class.java, mContext)) {
            var intentOS = Intent(mContext, OverlayService::class.java)
            mContext.startService(intentOS)
        }
    }
}