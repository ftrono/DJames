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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.dialogs.GeneralDialog
import com.ftrono.DJames.screen.updateHistory
import com.ftrono.DJames.services.OverlayService
import com.ftrono.DJames.ui.Navigation
import com.ftrono.DJames.ui.OptionsItem
import com.ftrono.DJames.ui.OptionsMenu
import com.ftrono.DJames.ui.navigateTo
import com.ftrono.DJames.ui.theme.DJamesTheme
import com.ftrono.DJames.ui.theme.NavigationItem
import com.ftrono.DJames.ui.theme.windowBackground
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.lang.Thread.sleep


class MainActivity : ComponentActivity() {

    private val TAG: String = MainActivity::class.java.getSimpleName()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acts_active.add(TAG)

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
            //Init log directory:
            logDir = File(cacheDir, "log_requests")
            if (!logDir!!.exists()) {
                logDir!!.mkdir()
            }
            historyKeys.postValue(updateHistory(this@MainActivity))
            //Init library directories:
            libraryDir = File(cacheDir, "library")
            if (!libraryDir!!.exists()) {
                libraryDir!!.mkdir()
            }
            //delete older logs:
            utils.deleteOldLogs()
            //delete older cached Library files:
            libUtils.cleanLibraryCache(this@MainActivity)
            //delete older recFiles in cache:
            if (!overlayActive.value!!) {
                utils.cleanOlderRecs(this@MainActivity)
            }

            //TODO: TEMP:
//            libUtils.migrateArtists(this@MainActivity)
//            libUtils.migratePlaylists(this@MainActivity)
//            libUtils.migrateContacts(this@MainActivity)
        }

        //NLP USER ID:
        if (prefs.nlpUserId == "") {
            prefs.nlpUserId = utils.generateRandomString(12)
        }

        //AUTO START-UP:
        if (prefs.autoStartup && !main_initialized && prefs.spotifyToken != "" && !utils.isMyServiceRunning(OverlayService::class.java, this@MainActivity)) {
            try {
                var intentOS = Intent(this@MainActivity, OverlayService::class.java)
                this@MainActivity.startService(intentOS)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot auto-start Overlay Service. EXCEPTION: ", e)
            }

        }

        //Done:
        overlayPos.postValue(prefs.overlayPosition)
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

        val mContext = LocalContext.current
        val dialogLoggingInOn by showLoggingIn.observeAsState()
        if (dialogLoggingInOn!!) {
            DialogLoggingIn(mContext = mContext)
            getSpotifyUserData(mContext)
        }

        val myNavigationSuiteItemColors = NavigationSuiteDefaults.itemColors(
            navigationBarItemColors = NavigationBarItemDefaults.colors(
                indicatorColor = colorResource(id = R.color.transparent_green),
                selectedIconColor = colorResource(id = R.color.light_grey),
                selectedTextColor = colorResource(id = R.color.colorAccentLight),
                unselectedIconColor = colorResource(id = R.color.mid_grey),
                unselectedTextColor = colorResource(id = R.color.mid_grey)
            ),
            navigationRailItemColors = NavigationRailItemDefaults.colors(
                indicatorColor = colorResource(id = R.color.transparent_green),
                selectedIconColor = colorResource(id = R.color.light_grey),
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
                                painter = painterResource(id = navItem.icon),
                                contentDescription = navItem.title,
                                tint = colorResource(id = R.color.light_grey)
                            )
                        },
                        label = {
                            Text(
                                text = navItem.title,
                                color = colorResource(id = R.color.light_grey)
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
        var mDisplayMenu = rememberSaveable {
            mutableStateOf(false)
        }

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
                                    val intent1 = Intent(this@MainActivity, AuthActivity::class.java)
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
    fun DialogLogout(mContext: Context, dialogOnState: MutableState<Boolean>) {
        GeneralDialog(
            dialogOnState = dialogOnState,
            backgroundColor = colorResource(id = R.color.colorPrimaryDark),
            title = "Logout",
            content = {
                Text(
                    text = "You will need to login again to Spotify to use DJames.\n\nDo you want to log out?",   // and you'll lose your saved vocabulary & history
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 14.sp
                )
            },
            dismissText = "No",
            confirmText = "Yes",
            onConfirm = {
                logout(mContext)
                dialogOnState.value = false
            }
        )
    }


    @Composable
    fun DialogLoggingIn(mContext: Context) {
        val dialogOnState by showLoggingIn.observeAsState()
        if (dialogOnState!!) {
            Dialog(
                properties = DialogProperties(
                    dismissOnBackPress = false, dismissOnClickOutside = false
                ),
                onDismissRequest = {
                    showLoggingIn.postValue(false)
                }
            ) {
                //CONTAINER:
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.dark_grey_background)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .wrapContentWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        //MESSAGE:
                        Text(
                            text = "Logging in to Spotify...",
                            color = colorResource(id = R.color.light_grey),
                            textAlign = TextAlign.Start,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }


    fun getSpotifyUserData(mContext: Context) {
        //Get user profile data:
        //BUILD GET REQUEST:
        val url = "https://api.spotify.com/v1/me"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $spotTempToken")
            .build()

        //GET:
        lifecycle.coroutineScope.launch {
            Log.d(TAG, "Performing Spotify.me request...")
            val meResponse = utils.makeRequest(client, request)
            if (meResponse != "") {
                Log.d(TAG, "Spotify.me: answer received!")
                try {
                    //RESPONSE RECEIVED -> USER'S PROFILE DATA:
                    val respJSON = JsonParser.parseString(meResponse).asJsonObject
                    var product = respJSON.get("product").asString
                    //User must be PREMIUM:
                    if (product == "premium" || product == "duo" || product == "family") {
                        //Log.d(TAG, response)
                        //SUCCESS!
                        prefs.spotifyToken = spotTempToken
                        prefs.refreshToken = refrTempToken
                        prefs.spotUserName = respJSON.get("display_name").asString
                        prefs.spotUserId = respJSON.get("id").asString
                        prefs.spotUserEMail = respJSON.get("email").asString
                        prefs.spotCountry = respJSON.get("country").asString
                        if (downloadSpotifyProfileImage) {
                            //Spotify profile image:
                            try {
                                val images = respJSON.getAsJsonArray("images")
                                val firstImage = images.get(0).asJsonObject
                                prefs.spotUserImage = firstImage.get("url").asString
                            } catch (e: Exception) {
                                Log.w(TAG, "Unable to retrieve user image: ", e)
                                prefs.spotUserImage = ""
                            }
                        }
                        //Generate NLP user ID:
                        prefs.nlpUserId = utils.generateRandomString(30, numOnly = true)

                        sleep(1000)
                        Log.d(TAG, "Spotify.me: success! User is enabled.")
                        Toast.makeText(mContext, "SUCCESS: DJames is now LOGGED IN to your Spotify!", Toast.LENGTH_LONG).show()
                        spotifyLoggedIn.postValue(true)
                    } else {
                        Log.w(TAG, "Spotify.me: PROBLEM -> user not enabled! USER TYPE: $product")
                        Toast.makeText(mContext, "ERROR: to use DJames, you need to be a Spotify Premium user! :(", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Profile parsing error: ", e)
                    Toast.makeText(mContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                }
            }
            spotTempToken = ""
            refrTempToken = ""
            showLoggingIn.postValue(false)
        }
    }


    //LOGOUT:
    fun logout(context: Context) {
        //Delete tokens & user details:
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

            //Finish activity:
            if (intent!!.action == ACTION_FINISH_MAIN) {
                Log.d(TAG, "MAIN: ACTION_FINISH_MAIN.")
                finishAndRemoveTask()
            }

            //Refresh History list:
            if (intent.action == ACTION_LOG_REFRESH) {
                Log.d(TAG, "HISTORY: ACTION_LOG_REFRESH.")
                historyKeys.postValue(updateHistory(context!!))   //Refresh list
            }
        }
    }
}