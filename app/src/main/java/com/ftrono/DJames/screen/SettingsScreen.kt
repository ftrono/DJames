package com.ftrono.DJames.screen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.application.copyrightYear
import com.ftrono.DJames.application.messLangCaps
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.messLangNames
import com.ftrono.DJames.application.overlayPosOptions
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.queryLangCaps
import com.ftrono.DJames.application.queryLangCodes
import com.ftrono.DJames.services.FloatingViewService
import com.ftrono.DJames.ui.theme.MyDJamesItem
import com.ftrono.DJames.utilities.Utilities

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
    var checkedStartup by remember { mutableStateOf(if (preview) true else prefs.autoStartup) }
    var checkedSilence by remember { mutableStateOf(if (preview) true else prefs.silenceEnabled) }
    var checkedAutoClock by remember { mutableStateOf(if (preview) true else prefs.autoClock) }
    var checkedClockRedirect by remember { mutableStateOf(if (preview) true else prefs.clockRedirectEnabled) }
    var checkedVolumeEnabled by remember { mutableStateOf(if (preview) true else prefs.volumeUpEnabled) }
    var recTimeout by rememberSaveable { mutableStateOf(if (preview) "5" else prefs.recTimeout) }
    var messTimeout by rememberSaveable { mutableStateOf(if (preview) "5" else prefs.messageTimeout) }
    var clockTimeout by rememberSaveable { mutableStateOf(if (preview) "5" else prefs.clockTimeout) }
    var textOverlayPosState = rememberSaveable { mutableStateOf(if (preview) "Right" else prefs.overlayPosition) }
    var textQueryLangState = rememberSaveable { mutableStateOf(if (preview) "English" else queryLangCaps[queryLangCodes.indexOf(prefs.queryLanguage)]) }
    var textMessLangState = rememberSaveable { mutableStateOf(if (preview) "English" else messLangCaps[messLangCodes.indexOf(prefs.messageLanguage)]) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    //Switch colors:
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = colorResource(id = R.color.light_grey),
        checkedTrackColor = colorResource(id = R.color.colorAccent),
        uncheckedThumbColor = colorResource(id = R.color.mid_grey),
        uncheckedTrackColor = colorResource(id = R.color.faded_grey),
    )

    //TextField colors:
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colorResource(id = R.color.colorAccentLight),
        unfocusedBorderColor = colorResource(id = R.color.mid_grey),
        focusedTextColor = colorResource(id = R.color.light_grey),
        unfocusedTextColor = colorResource(id = R.color.light_grey),
        focusedPlaceholderColor = colorResource(id = R.color.mid_grey),
        unfocusedPlaceholderColor = colorResource(id = R.color.mid_grey),
        focusedPrefixColor = colorResource(id = R.color.mid_grey),
        unfocusedPrefixColor = colorResource(id = R.color.mid_grey),
        focusedSuffixColor = colorResource(id = R.color.mid_grey),
        unfocusedSuffixColor = colorResource(id = R.color.mid_grey),
        focusedSupportingTextColor = colorResource(id = R.color.colorAccentLight),
        unfocusedSupportingTextColor = colorResource(id = R.color.mid_grey),
        cursorColor = colorResource(id = R.color.colorAccentLight),
        selectionColors = TextSelectionColors(
            handleColor = colorResource(id = R.color.colorAccent),
            backgroundColor = colorResource(id = R.color.transparent_green)
        )
    )


    Box(modifier = Modifier
        .fillMaxSize()
        .background(colorResource(id = R.color.windowBackground))
        .clickable {
            focusManager.clearFocus()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            //HEADER:
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(colorResource(id = R.color.windowBackground)),
                contentAlignment = Alignment.Center
            ) {
                //HEADER CONTENT:
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            //GRADIENT:
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to colorResource(id = R.color.transparent_full),
                                    0.3f to colorResource(id = R.color.transparent_full),
                                    1f to colorResource(id = R.color.windowBackground)
                                )
                            )
                        ),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //BACK:
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .offset(x = (6.dp))
                                .size(32.dp),
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorResource(id = R.color.colorAccentLight)
                        )
                    }
                    //TEXT HEADER:
                    Text(
                        text = "Preferences",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.light_grey),
                        modifier = Modifier
                            .padding(
                                start = 6.dp
                            )
                            .wrapContentWidth()
                    )
                    //OPTIONS BUTTONS:
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        //SAVE BUTTON:
                        IconButton(
                            modifier = Modifier
                                .padding(end=12.dp),
                            onClick = {
                                saveSettings(mContext, newRecTimeout=recTimeout, newMessTimeout=messTimeout, newClockTimeout=clockTimeout)
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(35.dp),
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = colorResource(id = R.color.colorAccentLight)
                            )
                        }
                    }
                }
            }

            //SETTINGS LIST:
            Column(
                modifier = Modifier
                    .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                //SECTION: OVERLAY BUTTON:
                Text(
                    text = "🔘  Overlay button",
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp),
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                //Auto Startup:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 36.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .offset(y = -(8.dp)),
                        text = "Start Overlay when app is opened",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        modifier = Modifier
                            .offset(y = -(8.dp)),
                        checked = checkedStartup,
                        colors = switchColors,
                        onCheckedChange = {
                            //UPDATE:
                            checkedStartup = it
                            prefs.autoStartup = it
                        }
                    )
                }

                //Overlay position:
                Text(
                    modifier = Modifier
                        .padding(bottom = 4.dp, start=36.dp),
                    text = "Overlay button position",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                )
                DropdownSpinner(
                    mContext,
                    parentOptions=overlayPosOptions,
                    init=textOverlayPosState.value,
                    state=textOverlayPosState,
                    prefName="overlayPosition",
                    width=200,
                    start=36
                )


                //SECTION: VOICE QUERIES:
                Text(
                    text = "🗣️  Voice queries",
                    modifier = Modifier.padding(top = 30.dp),
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                //Req language:
                Text(
                    modifier = Modifier
                        .padding(top=20.dp, bottom = 4.dp, start=36.dp),
                    text = "Voice queries: default language",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                )
                DropdownSpinner(
                    mContext,
                    parentOptions=queryLangCaps,
                    init=textQueryLangState.value,
                    state=textQueryLangState,
                    prefName="queryLanguage",
                    width=200,
                    start=36
                )

                //Req timeout:
                Text(
                    modifier = Modifier
                        .padding(top=4.dp, bottom = 4.dp, start=36.dp),
                    text = "Voice queries: timeout recording after",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 20.dp, start = 36.dp)
                        .width(250.dp)
                        .wrapContentHeight()
                        .focusRequester(focusRequester),
                    colors = textFieldColors,
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

                //Silence detection:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .offset(y = -(8.dp)),
                        text = "Stop recording when silence\nis detected",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        modifier = Modifier
                            .offset(y = -(8.dp)),
                        checked = checkedSilence,
                        colors = switchColors,
                        onCheckedChange = {
                            checkedSilence = it
                            prefs.silenceEnabled = it
                        }
                    )
                }


                //SECTION: MESSAGING:
                Text(
                    text = "💬  Messaging",
                    modifier = Modifier.padding(top = 30.dp),
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                //Mess language:
                Text(
                    modifier = Modifier
                        .padding(top=20.dp, bottom = 4.dp, start=36.dp),
                    text = "Messages: default language",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                )
                DropdownSpinner(
                    mContext,
                    parentOptions=messLangCaps,
                    init=textMessLangState.value,
                    state=textMessLangState,
                    prefName="messageLanguage",
                    width=200,
                    start=36
                )

                //Mess timeout:
                Text(
                    modifier = Modifier
                        .padding(top=8.dp, bottom = 4.dp, start=36.dp),
                    text = "Messages: timeout recording after",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 20.dp, start = 36.dp)
                        .width(250.dp)
                        .wrapContentHeight()
                        .focusRequester(focusRequester),
                    colors = textFieldColors,
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


                //SECTION: CLOCK SCREEN:
                Text(
                    text = "🕑  Clock screen",
                    modifier = Modifier
                        .padding(top = 30.dp, bottom = 12.dp),
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                //Auto Clock:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 36.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .offset(y = -(8.dp)),
                        text = "Show Clock screen when Overlay\nis started",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        modifier = Modifier
                            .offset(y = -(8.dp)),
                        checked = checkedAutoClock,
                        colors = switchColors,
                        onCheckedChange = {
                            checkedAutoClock = it
                            prefs.autoClock = it
                        }
                    )
                }

                //Clock redirect:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 36.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .offset(y = -(16.dp)),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Go back automatically to Clock screen",
                            color = colorResource(id = R.color.light_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 14.sp,
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
                        modifier = Modifier
                            .offset(y = -(8.dp)),
                        checked = checkedClockRedirect,
                        colors = switchColors,
                        onCheckedChange = {
                            checkedClockRedirect = it
                            prefs.clockRedirectEnabled = it
                        }
                    )
                }

                if (checkedClockRedirect) {
                    //After (timeout):
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(bottom = 20.dp, start = 36.dp)
                            .width(250.dp)
                            .wrapContentHeight()
                            .focusRequester(focusRequester),
                        colors = textFieldColors,
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


                //SECTION: ADVANCED:
                Text(
                    text = "⚠️  Advanced",
                    modifier = Modifier
                        .padding(top = 30.dp, bottom = 12.dp),
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                //VolumeUp enabled:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 36.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .offset(y = -(16.dp)),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "VOLUME-UP key starts recording",
                            color = colorResource(id = R.color.light_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 14.sp,
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
                        modifier = Modifier
                            .offset(y = -(8.dp)),
                        checked = checkedVolumeEnabled,
                        colors = switchColors,
                        onCheckedChange = {
                            checkedVolumeEnabled = it
                            prefs.volumeUpEnabled = it
                            restartOverlay(mContext)
                        }
                    )
                }

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
    val utils = Utilities()
    //Restart overlay service:
    if (utils.isMyServiceRunning(FloatingViewService::class.java, mContext)) {
        mContext.stopService(Intent(mContext, FloatingViewService::class.java))
        if (!utils.isMyServiceRunning(FloatingViewService::class.java, mContext)) {
            var intentOS = Intent(mContext, FloatingViewService::class.java)
            intentOS.putExtra("faded", false)
            mContext.startService(intentOS)
        }
    }
}