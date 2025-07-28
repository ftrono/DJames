package com.ftrono.DJames.application

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.coroutineScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.dialogs.MultiPermissionsHandler
import com.ftrono.DJames.application.services.OverlayService
import com.ftrono.DJames.ui.components.MainNavigator
import com.ftrono.DJames.ui.components.NavigatorCat
import com.ftrono.DJames.ui.dialogs.DialogLoading
import com.ftrono.DJames.ui.navigation.Navigation
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.theme.DJamesTheme
import com.ftrono.DJames.ui.theme.windowBackground


class MainActivity : ComponentActivity() {

    private val TAG: String = MainActivity::class.java.getSimpleName()

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
        actFilter.addAction(ACTION_MESSAGES_REFRESH)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(mainActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "MainActReceiver started.")

        //CLEANING:
        if (!main_initialized) {
            allMessages.postValue(messageUtils.refreshMessages())
            //delete older logs:
            messageUtils.deleteOldMessages()
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
        val configuration = LocalConfiguration.current
        val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

        val settingsOpenState by settingsOpen.observeAsState()
        val innerNavOpenState by innerNavOpen.observeAsState()
        val userTypingChatState by userTypingChat.observeAsState()

        val navController = rememberNavController()
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


        //MAIN SCREEN: NAVIGATION:
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .windowInsetsPadding(WindowInsets.displayCutout),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                //HORIZONTAL LAYOUT:
                //RIGHT NAVIGATION RAIL:
                MainNavigator(
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    navItems.forEach { navItem ->
                        NavigatorCat(
                            selected = navItem.route == currentRoute,
                            label = navItem.title,
                            iconPainter = painterResource(navItem.icon),
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
                        //DIVIDERS:
                        if (navItem != navItems.last()) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(top = 4.dp, bottom = 4.dp)
                                    .width(50.dp)
                                    .wrapContentHeight(),
                                thickness = 2.dp,
                                color = colorResource(id = R.color.faded_grey)
                            )
                        }
                    }
                }

                //CURRENT SCREEN:
                Box(
                    modifier = Modifier
                        .weight(1F)
                ) {
                    //SET CURRENT SCREEN FROM NAVIGATION HOST:
                    Navigation(navController)
                }
            }
        } else {
            //VERTICAL LAYOUT:
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .windowInsetsPadding(WindowInsets.displayCutout),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                //CURRENT SCREEN:
                Box(
                    modifier = Modifier
                        .weight(1F)
                ) {
                    //SET CURRENT SCREEN FROM NAVIGATION HOST:
                    Navigation(navController)
                }

                //BOTTOM NAVIGATION BAR:
                if (!userTypingChatState!!) {
                    MainNavigator(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        navItems.forEach { navItem ->
                            NavigatorCat(
                                selected = navItem.route == currentRoute,
                                label = navItem.title,
                                iconPainter = painterResource(navItem.icon),
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
                            //DIVIDERS:
                            if (navItem != navItems.last()) {
                                VerticalDivider(
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 4.dp)
                                        .height(40.dp)
                                        .wrapContentWidth(),
                                    thickness = 2.dp,
                                    color = colorResource(id = R.color.faded_grey)
                                )
                            }
                        }
                    }
                }
            }
        }
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

            //Refresh Messages list:
            if (intent.action == ACTION_MESSAGES_REFRESH) {
                Log.d(TAG, "HISTORY: ACTION_MESSAGES_REFRESH.")
                allMessages.postValue(messageUtils.refreshMessages())   //Refresh list
            }
        }
    }
}