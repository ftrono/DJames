package com.ftrono.DJames.application

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.dialogs.MultiPermissionsHandler
import com.ftrono.DJames.application.services.OverlayService
import com.ftrono.DJames.be.spotify.SpotifyLoginUtils
import com.ftrono.DJames.ui.components.OptionsItem
import com.ftrono.DJames.ui.components.OptionsMenu
import com.ftrono.DJames.ui.dialogs.DialogLoading
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.navigation.Navigation
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.theme.DJamesTheme
import com.ftrono.DJames.ui.theme.NavigationItem
import com.ftrono.DJames.ui.theme.windowBackground


class MainActivity : ComponentActivity() {

    private val TAG: String = MainActivity::class.java.getSimpleName()
    private var spotifyLoginUtils = SpotifyLoginUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acts_active.add(TAG)
        val context = this@MainActivity

        installSplashScreen()
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
            userGender.postValue(prefs.userGender)
        }

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_FINISH_MAIN)
        actFilter.addAction(ACTION_LOG_REFRESH)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(mainActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "MainActReceiver started.")

        //CLEANING:
        if (!main_initialized) {
            historyItems.postValue(logUtils.refreshHistory())
            //delete older logs:
            logUtils.deleteOldLogs()
            //delete older cached Library files:
            libUtils.cleanLibraryCache(context)
            //delete older recFiles in cache:
            if (!overlayActive.value!!) {
                utils.cleanOlderRecs(context)
            }
        }

        //NLP USER ID:
        if (prefs.nlpUserId == "") {
            prefs.nlpUserId = utils.generateRandomString(12)
        }
        spotUserImageState.postValue(prefs.spotUserImage)
        userNicknameState.postValue(prefs.userNickname)

        //Prefs:
        autoStopQueriesState.postValue(prefs.silenceEnabledQueries)

        //AUTO START-UP:
        if (
            Settings.canDrawOverlays(context)
            && utils.checkPermission(context, Manifest.permission.RECORD_AUDIO)
            && prefs.autoStartup && !main_initialized && prefs.spotifyToken != ""
            && !utils.isMyServiceRunning(OverlayService::class.java, context)
            ) {
            try {
                var intentOS = Intent(context, OverlayService::class.java)
                context.startService(intentOS)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot auto-start Overlay Service. EXCEPTION: ", e)
            }

        }

        //Done:
        overlayPos.postValue(prefs.overlayPosition)
        handleShareIntent(intent)
        main_initialized = true

    }


    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        unregisterReceiver(mainActReceiver)
        acts_active.remove(TAG)
    }


    @Preview
    @Preview(heightDp = 360, widthDp = 800)
    @Composable
    fun MainScreenPreview() {
        MainScreen(preview = true)
    }


    @Composable
    fun MainScreen(
        preview: Boolean = false
    ) {
        val navController = rememberNavController()
        val navItems = listOf(
            NavigationItem.Home,
            NavigationItem.Guide,
            NavigationItem.Library,
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

        val mContext = LocalContext.current
        val dialogLoggingInOn by showLoggingIn.observeAsState()
        if (dialogLoggingInOn!!) {
            DialogLoading(
                text = "Logging in to Spotify..."
            )
            spotifyLoginUtils.getSpotifyUserData(
                mContext,
                navController,
                lifecycle.coroutineScope
            )
        }

        //PERMISSIONS HANDLER:
        val permsRequestedState by permsRequested.observeAsState()
        if (!permsRequestedState!!) {
            MultiPermissionsHandler(
                context = mContext,
            )
        }

        val myNavigationSuiteItemColors = NavigationSuiteDefaults.itemColors(
            navigationBarItemColors = NavigationBarItemDefaults.colors(
                indicatorColor = colorResource(id = R.color.transparent_green),
                selectedIconColor = colorResource(id = R.color.light_grey),
                selectedTextColor = colorResource(id = R.color.colorAccentLight),
                unselectedIconColor = colorResource(id = R.color.light_grey),
                unselectedTextColor = colorResource(id = R.color.light_grey)
            ),
            navigationRailItemColors = NavigationRailItemDefaults.colors(
                indicatorColor = colorResource(id = R.color.transparent_green),
                selectedIconColor = colorResource(id = R.color.light_grey),
                selectedTextColor = colorResource(id = R.color.colorAccentLight),
                unselectedIconColor = colorResource(id = R.color.light_grey),
                unselectedTextColor = colorResource(id = R.color.light_grey)
            )
        )

        //MAIN SCREEN: NAVIGATION:
        NavigationSuiteScaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.displayCutout),
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
                                painter = painterResource(id = navItem.icon),
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
                topBar = {
                    TopBar(
                        navController,
                        spotifyLoggedInState!!,
                        settingsOpenState!!
                    )
                },
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
    fun TopBar(
        navController: NavController,
        spotifyLoggedInState: Boolean,
        settingsOpenState: Boolean
    ) {
        val mContext = LocalContext.current

        // STATES:
        var mDisplayMenu = rememberSaveable {
            mutableStateOf(false)
        }

        val logoutDialogOn = rememberSaveable { mutableStateOf(false) }
        if (logoutDialogOn.value) {
            DialogLogout(
                mContext,
                logoutDialogOn,
                navController,
                settingsOpenState
            )
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.offset(y = (2.dp)),
                        text = stringResource(id = R.string.app_title),
                        fontSize = 22.sp,
                        color = colorResource(id = R.color.light_grey),
                        fontWeight = FontWeight.Bold
                    )
                    if (spotifyLoggedInState)
                        Text(
                            modifier = Modifier.offset(y = -(2.dp)),
                            text = "for ${prefs.spotUserName}",
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.light_grey)
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
                Icon(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable {
                            //Navigate:
                            val curNavRoute = NavigationItem.Settings.route
                            if (curNavRoute == lastNavRoute && (settingsOpenState)) {
                                navController.popBackStack()
                            } else {
                                navigateTo(navController, curNavRoute)
                            }
                            lastNavRoute = curNavRoute
                        },
                    painter = painterResource(id = R.drawable.item_settings),
                    contentDescription = "",
                    tint = if (settingsOpenState) {
                        colorResource(id = R.color.colorAccentLight)
                    } else {
                        colorResource(id = R.color.light_grey)
                    }
                )

                //"MORE OPTIONS" BUTTON:
                Icon(
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .clickable { mDisplayMenu.value = !mDisplayMenu.value },
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "",
                    tint = colorResource(id = R.color.light_grey)
                )

                //DROPDOWN MENU:
                OptionsMenu(
                    expandedState = mDisplayMenu,
                    backgroundColor = colorResource(id = R.color.dark_grey_background),
                    options = {
                        //1) Item: LOGIN/LOGOUT
                        OptionsItem(
                            title = if (!spotifyLoggedInState) "Login to Spotify" else "Logout from Spotify",
                            iconPainter = painterResource(id = R.drawable.item_user),
                            onClick = {
                                if (!spotifyLoggedInState) {
                                    //Login user -> Open WebView:
                                    val intent1 =
                                        Intent(this@MainActivity, AuthActivity::class.java)
                                    startActivity(intent1)
                                } else {
                                    //LOG OUT:
                                    logoutDialogOn.value = true
                                }
                                mDisplayMenu.value = false
                            }
                        )
                        //2) Item: VOICE SETTINGS
                        OptionsItem(
                            title = "Voice settings",
                            iconPainter = painterResource(id = R.drawable.icon_speak),
                            onClick = {
                                //Set app preferences:
                                val intent1 = Intent("com.android.settings.TTS_SETTINGS")
                                startActivity(intent1)
                                mDisplayMenu.value = false
                            }
                        )
                        //3) Item: PERMISSIONS
                        OptionsItem(
                            title = "Permissions",
                            iconPainter = painterResource(id = R.drawable.item_permissions),
                            onClick = {
                                //Set app preferences:
                                val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent1.setData(uri)
                                startActivity(intent1)
                                mDisplayMenu.value = false
                            }
                        )
                    }
                )
            }
        )
    }


    @Composable
    fun DialogLogout(
        mContext: Context,
        dialogOnState: MutableState<Boolean>,
        navController: NavController,
        settingsOpenState: Boolean
    ) {
        GeneralDialog(
            dialogOnState = dialogOnState,
            backgroundColor = colorResource(id = R.color.colorPrimaryDark),
            title = "Logout",
            content = {
                Text(
                    text = "You will need to login again to Spotify to use DJames.\n\nDo you want to log out?",   // and you'll lose your saved library & history
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 14.sp
                )
            },
            dismissText = "No",
            confirmText = "Yes",
            onConfirm = {
                spotifyLoginUtils.logout(
                    mContext,
                    navController,
                    settingsOpenState
                )
                dialogOnState.value = false
            }
        )
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { handleShareIntent(it) }
    }

    private fun handleShareIntent(
        intent: Intent
    ) {
        if (intent.action == Intent.ACTION_SEND) {
            val type = intent.type

            when {
                type == "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    sharedText?.let {
                        Log.d("SharedData", "Received URL/Text: $it")
                        sharedLink.postValue(spotifyUtils.extractUrl(it))
                    }
                }
            }
        }
    }


    //PERSONAL RECEIVER:
    private var mainActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Finish activity:
            if (intent!!.action == ACTION_FINISH_MAIN) {
                Log.d(TAG, "MAIN: ACTION_FINISH_MAIN.")
                finishAndRemoveTask()
            }

            //Refresh History list:
            if (intent.action == ACTION_LOG_REFRESH) {
                Log.d(TAG, "HISTORY: ACTION_LOG_REFRESH.")
                historyItems.postValue(logUtils.refreshHistory())   //Refresh list
            }
        }
    }
}