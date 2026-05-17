package com.ftrono.DJames.application.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.application.copyrightYear
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.queryLangCodes
import com.ftrono.DJames.application.queryLangFull
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.services.OverlayService
import com.ftrono.DJames.application.ttsVoiceIdMap
import com.ftrono.DJames.application.isVolumeUpPreferenceSet
import com.ftrono.DJames.ui.components.CustomRangeSlider
import com.ftrono.DJames.ui.components.CustomSlider
import com.ftrono.DJames.ui.components.DropdownSpinner
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.components.SettingsSection
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.selectors.getSwitchColors
import kotlin.math.roundToInt


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

    // STATUSES:
    val checkedNoise = remember { mutableStateOf(if (preview) true else prefs.enableNoiseSuppression) }
    val checkedSecondNoise = remember { mutableStateOf(if (preview) true else prefs.enableSecondNoiseSuppression) }

    var recFreqRange = 300f..3300f
    var sliderRecFreqPos = remember { mutableStateOf(if (preview) 900f..2500f else prefs.recMinFreq.toFloat()..prefs.recMaxFreq.toFloat()) }
    var sliderSecNoiseDeltaPos = remember { mutableStateOf(if (preview) 500f else prefs.secondNoiseDelta.toFloat()) }
    var sliderRecTimeoutPos = remember { mutableStateOf(if (preview) 10f else prefs.recTimeout.toFloat()) }
    var sliderMessTimeoutPos = remember { mutableStateOf(if (preview) 60f else prefs.messageTimeout.toFloat()) }
    var sliderClockTimeoutPos = remember { mutableStateOf(if (preview) 10f else prefs.clockTimeout.toFloat()) }

    val checkedRecToDownloads = remember { mutableStateOf(if (preview) false else prefs.recToDownloads) }
    val checkedStartup = remember { mutableStateOf(if (preview) true else prefs.autoStartup) }
    val checkedSilenceQueries = remember { mutableStateOf(if (preview) true else prefs.silenceEnabledQueries) }
    val checkedSilenceMess = remember { mutableStateOf(if (preview) true else prefs.silenceEnabledMess) }
    val checkedAutoClock = remember { mutableStateOf(if (preview) true else prefs.autoClock) }
    val checkedClockRedirect = remember { mutableStateOf(if (preview) true else prefs.clockRedirectEnabled) }
    val checkedVolumeEnabled = remember { mutableStateOf(if (preview) true else prefs.volumeUpEnabled) }

    val checkedIntro = remember { mutableStateOf(if (preview) true else prefs.enableIntro) }
    val queryLangState = rememberSaveable { mutableStateOf(if (preview) "English" else queryLangFull[queryLangCodes.indexOf(prefs.queryLanguage)]) }
    val voiceAccentState = rememberSaveable { mutableStateOf(if (preview) "British" else prefs.voiceAccent) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
                title = "Preferences",
                subtitle = "App settings",
                showBack = true,
                onBack = {
                    navController.popBackStack()
                    // Toast.makeText(mContext, "Preferences saved!", Toast.LENGTH_SHORT).show()
                },
                optionButtons = { }
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

            //SECTION: VOICE & SYSTEM SETTINGS:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, bottom=4.dp),
                title = "Voice & system",
                signColor = colorResource(id = R.color.greenSign),
                iconPainter = painterResource(id = R.drawable.icon_speak)
            ) {
                // Voice language:
                Text(
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    text = "Default language",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                DropdownSpinner(
                    context = mContext,
                    parentOptions = queryLangFull,
                    init = queryLangState.value,
                    state = queryLangState,
                    focusColorLight = colorResource(id = R.color.greenSignLight),
                    focusColorDark = colorResource(id = R.color.greenSign),
                    optionsBackground = colorResource(id = R.color.dark_grey),
                    prefName = "queryLanguage",
                    width = 200
                )

                // Voice accent:
                Text(
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    text = "Voice accent",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                DropdownSpinner(
                    context = mContext,
                    parentOptions = ttsVoiceIdMap.keys.toList(),
                    init = voiceAccentState.value,
                    state = voiceAccentState,
                    focusColorLight = colorResource(id = R.color.greenSignLight),
                    focusColorDark = colorResource(id = R.color.greenSign),
                    optionsBackground = colorResource(id = R.color.dark_grey),
                    prefName = "voiceAccent",
                    width = 200
                )

                //Android TTS settings:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Android TTS settings",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    RoundedSign(
                        signSize = 40.dp,
                        contentSize = 20,
                        backgroundColor = colorResource(id = R.color.greenSign),
                        borderColor = colorResource(id = R.color.greenSign),
                        contentColor = colorResource(id = R.color.light_grey),
                        iconVector = Icons.AutoMirrored.Default.ArrowForward,
                        clickable = true,
                        onClick = {
                            //Open system voice settings:
                            val intent1 = Intent("com.android.settings.TTS_SETTINGS")
                            mContext.startActivity(intent1)
                        }
                    )
                }

                //Go to App Permissions:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manage app permissions",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    RoundedSign(
                        signSize = 40.dp,
                        contentSize = 20,
                        backgroundColor = colorResource(id = R.color.greenSign),
                        borderColor = colorResource(id = R.color.greenSign),
                        contentColor = colorResource(id = R.color.light_grey),
                        iconVector = Icons.AutoMirrored.Default.ArrowForward,
                        clickable = true,
                        onClick = {
                            //Open app preferences:
                            val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", mContext.packageName, null)
                            intent1.setData(uri)
                            mContext.startActivity(intent1)
                        }
                    )
                }

                //Auto Startup:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top=8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Start Overlay when app is opened",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
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


            //SECTION: QUERY RECORDING:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, top=16.dp, bottom=4.dp),
                title = "Query recording",
                signColor = colorResource(id = R.color.yellowSign),
                iconPainter = painterResource(id = R.drawable.icon_mic)
            ) {
                //Enable spoken intro:
                Row(
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Make DJames say hello\nbefore starting a voice query",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = checkedIntro.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.yellowSign)
                        ),
                        onCheckedChange = {
                            checkedIntro.value = it
                            prefs.enableIntro = it
                        }
                    )
                }

                //Voice queries: Req timeout:
                Text(
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    //.padding(top=8.dp, bottom = 4.dp),
                    text = "Timeout voice query recording after",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                CustomSlider(
                    modifier =  Modifier
                        .padding(top=4.dp),
                    position = sliderRecTimeoutPos,
                    range = 5f..20f,
                    steps = 15,   // (max - min) / (steps + 1),
                    unit = "seconds",
                    trackColor = colorResource(R.color.yellowSign),
                    thumbColor = colorResource(R.color.yellowSignLight),
                    tickColor = colorResource(R.color.faded_grey),
                    onDone = {
                        // Update prefs:
                        prefs.recTimeout = sliderRecTimeoutPos.value.roundToInt()
                    }
                )

                //Voice queries: Silence detection:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top=4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Stop recording voice query\nwhen silence is detected",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = checkedSilenceQueries.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.yellowSign)
                        ),
                        onCheckedChange = {
                            checkedSilenceQueries.value = it
                            prefs.silenceEnabledQueries = it
                        }
                    )
                }
            }


            //SECTION: VOICE MESSAGES:
            SettingsSection(
                modifier = Modifier
                    .padding(end=8.dp, top=16.dp, bottom=4.dp),
                title = "WhatsApp voice message",
                signColor = colorResource(id = R.color.blueSign),
                iconPainter = painterResource(id = R.drawable.logo_whatsapp)
            ) {
                //Mess timeout:
                Text(
                    modifier = Modifier
                        .padding(top=8.dp, bottom=4.dp),
                    text = "Timeout message recording after",
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                CustomSlider(
                    modifier =  Modifier
                        .padding(top=4.dp),
                    position = sliderMessTimeoutPos,
                    range = 30f..120f,
                    steps = 15,   // (max - min) / (steps + 1),
                    unit = "seconds",
                    trackColor = colorResource(R.color.blueSign),
                    thumbColor = colorResource(R.color.blueSignLight),
                    tickColor = colorResource(R.color.faded_grey),
                    onDone = {
                        // Update prefs:
                        prefs.messageTimeout = sliderMessTimeoutPos.value.roundToInt()
                    }
                )

                //Voice messages: Silence detection:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top=4.dp, bottom=8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Stop recording WhatsApp voice\nmessage when silence is detected",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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


            //SECTION: NOISE FILTERING:
            SettingsSection(
                modifier = Modifier
                    .padding(top=8.dp, end=8.dp, bottom=4.dp),
                title = "Noise filtering",
                signColor = colorResource(id = R.color.brownSign),
                iconPainter = painterResource(id = R.drawable.icon_headphones)
            ) {
                //Experimental: Enable Noise Suppression:
                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Enable noise suppression",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = checkedNoise.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.brownSign)
                        ),
                        onCheckedChange = {
                            checkedNoise.value = it
                            prefs.enableNoiseSuppression = it
                        }
                    )
                }


                if (checkedNoise.value) {
                    // Experimental: Rec frequencies range:
                    Text(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp),
                        text = "Noise: audio cutout frequencies",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    CustomRangeSlider(
                        modifier = Modifier
                            .padding(top = 4.dp),
                        rangePosition = sliderRecFreqPos,
                        range = recFreqRange,
                        steps = 15,   // (max - min) / (steps + 1),
                        unit = "Hz",
                        trackColor = colorResource(R.color.brownSign),
                        thumbColor = colorResource(R.color.brownSignLight),
                        tickColor = colorResource(R.color.faded_grey),
                        onDone = {
                            // Update prefs:
                            prefs.recMinFreq = sliderRecFreqPos.value.start.roundToInt()
                            prefs.recMaxFreq = sliderRecFreqPos.value.endInclusive.roundToInt()
                        }
                    )


                    //Experimental: Enable Second Noise Suppression:
                    Row(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "Noise: apply suppression twice",
                            color = colorResource(id = R.color.light_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = checkedSecondNoise.value,
                            colors = getSwitchColors(
                                color = colorResource(id = R.color.brownSign)
                            ),
                            onCheckedChange = {
                                checkedSecondNoise.value = it
                                prefs.enableSecondNoiseSuppression = it
                            }
                        )
                    }


                    if (checkedSecondNoise.value) {
                        //Noise: additional cutout:
                        Text(
                            modifier = Modifier
                                .padding(bottom = 4.dp),
                            //.padding(top=8.dp, bottom = 4.dp),
                            text = "Noise: increase 2nd pass cutout of",
                            color = colorResource(id = R.color.light_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CustomSlider(
                            modifier = Modifier
                                .padding(top = 4.dp),
                            position = sliderSecNoiseDeltaPos,
                            range = 0f..600f,
                            steps = 5,   // (max - min) / (steps + 1),
                            unit = "Hz",
                            trackColor = colorResource(R.color.brownSign),
                            thumbColor = colorResource(R.color.brownSignLight),
                            tickColor = colorResource(R.color.faded_grey),
                            onDone = {
                                // Update prefs:
                                prefs.secondNoiseDelta = sliderSecNoiseDeltaPos.value.roundToInt()
                            }
                        )
                    }
                }
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
                        modifier = Modifier.weight(1f),
                        text = "Show Clock screen when Overlay\nis started",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                            .offset(y = -(8.dp))
                            .weight(1F),
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
                    CustomSlider(
                        modifier =  Modifier
                            .padding(top=4.dp),
                        position = sliderClockTimeoutPos,
                        range = 5f..20f,
                        steps = 15,   // (max - min) / (steps + 1),
                        unit = "seconds",
                        prefix = "after",
                        trackColor = colorResource(R.color.midfaded_grey),
                        thumbColor = colorResource(R.color.mid_grey),
                        tickColor = colorResource(R.color.faded_grey),
                        onDone = {
                            // Update prefs:
                            prefs.clockTimeout = sliderClockTimeoutPos.value.roundToInt()
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
                iconPainter = painterResource(id = R.drawable.icon_warning)
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
                            .offset(y = -(8.dp))
                            .weight(1F),
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
                    Switch(
                        checked = checkedVolumeEnabled.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.redSign)
                        ),
                        onCheckedChange = {
                            checkedVolumeEnabled.value = it
                            prefs.volumeUpEnabled = it
                            isVolumeUpPreferenceSet.postValue(it)
                            restartOverlay(mContext)
                        }
                    )
                }

                //Save recs to Downloads folder:
                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Save recordings to Downloads",
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = checkedRecToDownloads.value,
                        colors = getSwitchColors(
                            color = colorResource(id = R.color.redSign)
                        ),
                        onCheckedChange = {
                            checkedRecToDownloads.value = it
                            prefs.recToDownloads = it
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


//Validate integer value before saving:
fun validateIntValue(newVal: String, origVal: String, min_val: Int, max_val: Int) : String {
    val newInt = newVal.toInt()
    return if (newInt in min_val..max_val) {
        newVal
    } else {
        origVal
    }
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