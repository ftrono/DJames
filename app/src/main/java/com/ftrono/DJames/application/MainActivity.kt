package com.ftrono.DJames.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.services.FloatingViewService
import com.ftrono.DJames.ui.Navigation
import com.ftrono.DJames.ui.navigateTo
import com.ftrono.DJames.ui.theme.DJamesTheme
import com.ftrono.DJames.ui.theme.NavigationItem
import com.ftrono.DJames.ui.theme.windowBackground
import com.ftrono.DJames.utilities.Utilities
import java.io.File


class MainActivity : ComponentActivity() {

    private val TAG: String = MainActivity::class.java.getSimpleName()
    private var utils = Utilities()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acts_active.add(TAG)

        //TODO: TEMP:
        if (prefs.overlayPosition !in overlayPosOptions) {
            prefs.overlayPosition = "Right"
        } else if (prefs.queryLanguage !in queryLangCodes) {
            prefs.queryLanguage = "en"
        } else if (prefs.messageLanguage !in messLangCodes) {
            prefs.messageLanguage = "en"
        }

        enableEdgeToEdge(
            //For safe padding:
            statusBarStyle = SystemBarStyle.auto(windowBackground.toArgb(), windowBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(windowBackground.toArgb(), windowBackground.toArgb())
        )
        setContent {
            DJamesTheme {
                //Background:
                Surface (
                    modifier = Modifier.fillMaxSize(),
                    color = windowBackground
                ) {
                    MainScreen()
                }
            }
        }

        //Screen density:
        density = resources.displayMetrics.density

        //Check Login status:
        if (prefs.spotifyToken == "") {
            spotifyLoggedIn.postValue(false)
        } else {
            spotifyLoggedIn.postValue(true)
            mainSubtitle.postValue("for ${prefs.spotUserName}")
        }

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_MAIN_LOGGED_IN)
        actFilter.addAction(ACTION_FINISH_MAIN)
        actFilter.addAction(ACTION_LOG_REFRESH)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(mainActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "MainActReceiver started.")

        //CLEANING:
        if (!main_initialized) {
            //Init log directory:
            logDir = File(cacheDir, "log_requests")
            if (!logDir!!.exists()) {
                logDir!!.mkdir()
            }
            //Init vocabulary directory:
            vocDir = File(cacheDir, "vocabulary")
            if (!vocDir!!.exists()) {
                vocDir!!.mkdir()
            }
            //delete older logs:
            utils.deleteOldLogs()
            //delete older recFiles in cache:
            if (!overlayActive.value!!) {
                try {
                    File(cacheDir, "$recFileName.mp3").delete()
                    Log.d(TAG, "Recording mp3 deleted.")
                } catch (e: Exception) {
                    Log.w(TAG, "Recording mp3 not deleted.")
                }
                try {
                    File(cacheDir, "$recFileName.flac").delete()
                    Log.d(TAG, "Recording flac deleted.")
                } catch (e: Exception) {
                    Log.w(TAG, "Recording flac not deleted.")
                }
            }
        }

        //NLP USER ID:
        if (prefs.nlpUserId == "") {
            prefs.nlpUserId = utils.generateRandomString(12)
        }

        //AUTO START-UP:
        if (prefs.autoStartup && !main_initialized && !utils.isMyServiceRunning(FloatingViewService::class.java, this@MainActivity)) {
            try {
                var intentOS = Intent(this@MainActivity, FloatingViewService::class.java)
                intentOS.putExtra("faded", false)
                this@MainActivity.startService(intentOS)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot auto-start Overlay Service. EXCEPTION: ", e)
            }

        }

        //Done:
        main_initialized = true

    }

//    override fun onResume() {
//        super.onResume()
//        //ON RESUME() ONLY:
//        //Check Login status:
//        if (!spotifyLoggedIn.value!!) {
//            setViewLoggedOut()
//        }
//    }


    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        unregisterReceiver(mainActReceiver)
        acts_active.remove(TAG)
    }


    @Preview
    @Preview(heightDp = 360, widthDp = 800)
    @Composable
    fun MainScreen() {
        val navController = rememberNavController()
        val navItems = listOf(
            NavigationItem.Home,
            NavigationItem.Guide,
            NavigationItem.Vocabulary,
            NavigationItem.History
        )

        val spotifyLoggedInState by spotifyLoggedIn.observeAsState()
        val settingsOpenState by settingsOpen.observeAsState()
        val innerNavOpenState by innerNavOpen.observeAsState()
        val configuration = LocalConfiguration.current
        val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
        val customNavSuiteType = if (isLandscape) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val myNavigationSuiteItemColors = NavigationSuiteDefaults.itemColors(
            navigationBarItemColors = NavigationBarItemDefaults.colors(
                indicatorColor = colorResource(id = R.color.transparent_green),
                selectedIconColor = colorResource(id = R.color.colorAccentLight),
                selectedTextColor = colorResource(id = R.color.colorAccentLight),
                unselectedIconColor = colorResource(id = R.color.mid_grey),
                unselectedTextColor = colorResource(id = R.color.mid_grey)
            ),
            navigationRailItemColors = NavigationRailItemDefaults.colors(
                indicatorColor = colorResource(id = R.color.transparent_green),
                selectedIconColor = colorResource(id = R.color.colorAccentLight),
                selectedTextColor = colorResource(id = R.color.colorAccentLight),
                unselectedIconColor = colorResource(id = R.color.mid_grey),
                unselectedTextColor = colorResource(id = R.color.mid_grey)
            )
        )

        //MAIN SCREEN: NAVIGATION:
        NavigationSuiteScaffold(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .shadow(
                    elevation = 4.dp,
                    spotColor = colorResource(id = R.color.mid_grey)
                ),
            layoutType = customNavSuiteType,
            navigationSuiteItems = {
                navItems.forEach { navItem ->
                    item(
                        modifier = if (isLandscape) {
                            Modifier
                                .offset(y = 12.dp)
                                .padding(4.dp)
                        } else {
                            Modifier
                        },
                        icon = {
                            Icon(
                                painterResource(id = navItem.icon),
                                contentDescription = navItem.title
                            )
                        },
                        label = {
                            Text(
                                text = navItem.title
                            )
                        },
                        colors = myNavigationSuiteItemColors,
                        alwaysShowLabel = true,
                        selected = currentRoute == navItem.route,
                        onClick = {
                            //Navigate:
                            val curNavRoute = navItem.route
                            if (curNavRoute == lastNavRoute && (settingsOpenState!! || innerNavOpenState!!)) {
                                navController.popBackStack()
                            } else {
                                navigateTo(navController, curNavRoute)
                            }
                            lastNavRoute = curNavRoute
                        }
                    )
                }
            },
            containerColor = colorResource(id = R.color.black),
            contentColor = colorResource(id = R.color.mid_grey),
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = colorResource(id = R.color.windowBackground),
                navigationBarContentColor = colorResource(id = R.color.mid_grey),
                navigationRailContainerColor = colorResource(id = R.color.windowBackground),
                navigationRailContentColor = colorResource(id = R.color.mid_grey),
            )
        ) {
            //MAIN SCREEN: SCAFFOLD:
            Scaffold(
                topBar = { TopBar(navController, spotifyLoggedInState!!, settingsOpenState!!) },
                // Set background color to avoid the white flashing when you switch between screens:
                containerColor = colorResource(id = R.color.windowBackground)
            ) {
                Box(
                    modifier = Modifier
                        .padding(it)
                ) {
                    //SET CURRENT SCREEN FROM NAVIGATION HOST:
                    Navigation(navController)
                }
            }
        }
    }


    //TOP APP BAR:
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar(navController: NavController, spotifyLoggedInState: Boolean, settingsOpenState: Boolean) {
        val mContext = LocalContext.current

        // STATES:
        var mDisplayMenu by rememberSaveable {
            mutableStateOf(false)
        }
        val mainSubtitleState by mainSubtitle.observeAsState()

        val logoutDialogOn = rememberSaveable { mutableStateOf(false) }
        if (logoutDialogOn.value) {
            DialogLogout(mContext, logoutDialogOn)
        }

        CenterAlignedTopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    spotColor = colorResource(id = R.color.mid_grey)
                ),
            windowInsets = WindowInsets(
                top = 0.dp,
                bottom = 0.dp
            ),
            title = {
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Text(
                        modifier = Modifier.offset(y = (2.dp)),
                        text = stringResource(id = R.string.app_title),
                        fontSize = 22.sp,
                        color = colorResource(id = R.color.light_grey),
                    )
                    if (spotifyLoggedInState)
                        Text(
                            modifier = Modifier.offset(y = -(2.dp)),
                            text = mainSubtitleState!!,
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.mid_grey)
                        )
                }
            },
            colors = TopAppBarColors(
                containerColor = colorResource(id = R.color.windowBackground),
                scrolledContainerColor = colorResource(id = R.color.windowBackground),
                navigationIconContentColor = colorResource(id = R.color.mid_grey),
                titleContentColor = colorResource(id = R.color.light_grey),
                actionIconContentColor = colorResource(id = R.color.mid_grey)
            ),
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            actions = {
                //SETTINGS BUTTON:
                IconButton(
                    onClick = {
                        //Navigate:
                        val curNavRoute = NavigationItem.Settings.route
                        if (curNavRoute == lastNavRoute && (settingsOpenState)) {
                            navController.popBackStack()
                        } else {
                            navigateTo(navController, curNavRoute)
                        }
                        lastNavRoute = curNavRoute
                    }) {
                    Icon(
                        painter = painterResource(id = R.drawable.item_settings),
                        contentDescription = "",
                        tint = if (settingsOpenState) {
                            colorResource(id = R.color.colorAccentLight)
                        } else {
                            colorResource(id = R.color.light_grey)
                        }
                    )
                }

                //"MORE OPTIONS" BUTTON:
                Icon(
                    modifier = Modifier
                        .padding(end=8.dp)
                        .clickable { mDisplayMenu = !mDisplayMenu },
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "",
                    tint = colorResource(id = R.color.light_grey)
                )

                //DROPDOWN MENU:
                DropdownMenu(
                    modifier = Modifier
                        .background(colorResource(id = R.color.dark_grey_background)),
                    shape = RoundedCornerShape(20.dp),
                    expanded = mDisplayMenu,
                    onDismissRequest = { mDisplayMenu = false }
                ) {

                    //1) Item: LOGIN/LOGOUT
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (!spotifyLoggedInState) "Login to Spotify" else "Logout from Spotify",
                                color = colorResource(id = R.color.light_grey),
                                fontSize = 16.sp
                            )},
                        leadingIcon = {
                            Icon(
                                painterResource(id = R.drawable.item_user),
                                "",
                                tint = colorResource(id = R.color.mid_grey)
                            )
                        },
                        onClick = {
                            if (!spotifyLoggedInState) {
                                //Login user -> Open WebView:
                                val intent1 = Intent(this@MainActivity, WebAuth::class.java)
                                startActivity(intent1)
                            } else {
                                //LOG OUT:
                                logoutDialogOn.value = true
                            }
                            mDisplayMenu = false
                        })

                    //2) Item: VOICE SETTINGS
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Voice settings",
                                color = colorResource(id = R.color.light_grey),
                                fontSize = 16.sp
                            )},
                        leadingIcon = {
                            Icon(
                                painterResource(id = R.drawable.speak_icon_gray),
                                "",
                                tint = colorResource(id = R.color.mid_grey)
                            )
                        },
                        onClick = {
                            //Set app preferences:
                            val intent1 = Intent("com.android.settings.TTS_SETTINGS")
                            startActivity(intent1)
                            mDisplayMenu = false
                        })

                    //3) Item: PERMISSIONS
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Permissions",
                                color = colorResource(id = R.color.light_grey),
                                fontSize = 16.sp
                            )},
                        leadingIcon = {
                            Icon(
                                painterResource(id = R.drawable.item_permissions),
                                "",
                                tint = colorResource(id = R.color.mid_grey)
                            )
                        },
                        onClick = {
                            //Set app preferences:
                            val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent1.setData(uri)
                            startActivity(intent1)
                            mDisplayMenu = false
                        })

                }
            }
        )
    }


    @Composable
    fun DialogLogout(mContext: Context, dialogOnState: MutableState<Boolean>) {
        //LOGOUT DIALOG:
        if (dialogOnState.value) {
            AlertDialog(
                onDismissRequest = {
                    //cancelable -> true
                    dialogOnState.value = false
                },
                containerColor = colorResource(id = R.color.dark_grey),
                title = {
                    Text(
                        text = "Logout",
                        color = colorResource(id = R.color.light_grey)
                    ) },
                text = {
                    Text(
                        text = "You will need to login again to Spotify to use DJames.\n\nDo you want to log out?",   // and you'll lose your saved vocabulary & history
                        color = colorResource(id = R.color.mid_grey)
                    ) },
                dismissButton = {
                    Text(
                        modifier = Modifier
                            .padding(end = 20.dp)
                            .clickable {
                                dialogOnState.value = false
                            },
                        text = "No",
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.colorAccentLight)
                    )
                },
                confirmButton = {
                    Text(
                        modifier = Modifier
                            .clickable {
                                logout(mContext)
                                dialogOnState.value = false
                            },
                        text = "Yes",
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.colorAccentLight)
                    )
                }
            )
        }
    }


    //LOGOUT:
    fun logout(context: Context) {
        //Delete tokens & user details:
        var utils = Utilities()
        spotifyLoggedIn.postValue(false)
        prefs.spotifyToken = ""
        prefs.refreshToken = ""
        prefs.spotUserId = ""
        prefs.spotUserName = ""
        prefs.spotUserEMail = ""
        prefs.spotUserImage = ""
        prefs.spotCountry = ""
        prefs.nlpUserId = utils.generateRandomString(12)
        //utils.deleteUserCache()
        Toast.makeText(context, "Djames is now LOGGED OUT from your Spotify.", Toast.LENGTH_LONG).show()
    }


    //PERSONAL RECEIVER:
    private var mainActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Success: logged in:
            if (intent!!.action == ACTION_MAIN_LOGGED_IN) {
                Log.d(TAG, "MAIN: ACTION_MAIN_LOGGED_IN.")
                spotifyLoggedIn.postValue(true)
                Toast.makeText(context, "SUCCESS: DJames is now LOGGED IN to your Spotify!", Toast.LENGTH_LONG).show()
            }

            //Finish activity:
            if (intent.action == ACTION_FINISH_MAIN) {
                Log.d(TAG, "MAIN: ACTION_FINISH_MAIN.")
                finishAndRemoveTask()
            }

            //Refresh RecycleView:
            if (intent.action == ACTION_LOG_REFRESH) {
                Log.d(TAG, "HISTORY: ACTION_LOG_REFRESH.")
                historySize.postValue(historySize.value!! + 1)   //Refresh list
            }

        }
    }

}