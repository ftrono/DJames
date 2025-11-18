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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.dialogs.MultiPermissionsHandler
import com.ftrono.DJames.application.services.OverlayService
import com.ftrono.DJames.be.agents.chat.ChatManager
import com.ftrono.DJames.ui.components.isKeyboardOpen
import com.ftrono.DJames.ui.navigation.BottomNavigationBar
import com.ftrono.DJames.ui.navigation.Navigation
import com.ftrono.DJames.ui.navigation.SideNavigationRail
import com.ftrono.DJames.ui.theme.DJamesTheme
import com.ftrono.DJames.ui.theme.windowBackground
import java.io.File


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
            spotUserName.postValue(prefs.spotUserName)
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
            //Init recordings directory:
            recDir = File(cacheDir, "recordings")
            recDir!!.mkdirs()
            // messageUtils.updateExistingMessages()   //TODO: use only when needed!
            // libUtils.updateExistingLibrary()   //TODO: use only when needed!
            allMessageIds.postValue(messageUtils.refreshMessages())
            //delete older logs:
            messageUtils.deleteOldMessages()
            //delete older cached Library files:
            libUtils.cleanLibraryCache(context)
            //delete older recFiles in cache:
            if (!overlayActive.value!!) {
                utils.cleanRecordingsCache(context)
            }
        }

        //NLP USER ID:
        if (prefs.nlpUserId == "") {
            prefs.nlpUserId = utils.generateRandomString(12)
        }
        spotUserImageState.postValue(prefs.spotUserImage)
        userNicknameUI.postValue(prefs.userNickname)

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
        val mContext = LocalContext.current

        val navController = rememberNavController()
        val configuration = LocalConfiguration.current
        val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

        //PERMISSIONS HANDLER:
        val permsRequestedState by permsRequested.observeAsState()
        if (!permsRequestedState!!) {
            MultiPermissionsHandler(
                context = mContext,
            )
        }

        // ChatManager:
        val chatManager = ChatManager(mContext)
        chatManager.init()


        //MAIN SCREEN:
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            //SIDE NAV RAIL:
            if (isLandscape) {
                SideNavigationRail(navigationItems, navController)
            }
            //SCAFFOLD:
            Scaffold(
                modifier = Modifier
                    .weight(1F),
                containerColor = colorResource(id = R.color.windowBackground),
                bottomBar = {
                    if (!isLandscape && !isKeyboardOpen()) {
                        BottomNavigationBar(navigationItems, navController)
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .padding(it)
                ) {
                    //SET CURRENT SCREEN FROM NAVIGATION HOST:
                    Navigation(navController, chatManager)
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
                allMessageIds.postValue(messageUtils.refreshMessages())   //Refresh list
            }
        }
    }
}