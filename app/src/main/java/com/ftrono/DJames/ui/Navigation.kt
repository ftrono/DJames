package com.ftrono.DJames.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ftrono.DJames.application.curNavId
import com.ftrono.DJames.application.filter
import com.ftrono.DJames.application.innerNavOpen
import com.ftrono.DJames.application.settingsOpen
import com.ftrono.DJames.screen.GuideScreen
import com.ftrono.DJames.screen.HistoryScreen
import com.ftrono.DJames.screen.HomeScreen
import com.ftrono.DJames.screen.MyDJamesScreen
import com.ftrono.DJames.screen.SettingsScreen
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.ui.theme.MyDJamesItem
import com.ftrono.DJames.ui.theme.NavigationItem


//NAV HOST:
@Composable
fun Navigation(navController: NavHostController) {
    val filterState by filter.observeAsState()
    NavHost(
        modifier = Modifier
            .fillMaxSize(),
        navController = navController,
        startDestination = NavigationItem.Home.route
    ) {
        //MAIN:
        //0 -> HOME:
        composable(
            NavigationItem.Home.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavId > 0) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            }) {
            curNavId = 0
            innerNavOpen.postValue(false)
            settingsOpen.postValue(false)
            HomeScreen()
        }

        //1 -> GUIDE:
        composable(
            NavigationItem.Guide.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavId > 1) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            }) {
            curNavId = 1
            innerNavOpen.postValue(false)
            settingsOpen.postValue(false)
            GuideScreen(navController)
        }

        //2 -> MY DJAMES:
        composable(
            NavigationItem.MyDJames.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavId >= 2) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            }) {
            curNavId = 2
            innerNavOpen.postValue(false)
            settingsOpen.postValue(false)
            MyDJamesScreen(navController)
        }

        //3 -> HISTORY:
        composable(
            NavigationItem.History.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavId > 3) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            }) {
            curNavId = 3
            innerNavOpen.postValue(false)
            settingsOpen.postValue(false)
            HistoryScreen()
        }

        //EXTRA:
        //0 -> SETTINGS:
        composable(
            NavigationItem.Settings.route,
            enterTransition = {
                scaleIn() + expandVertically(expandFrom = Alignment.Bottom)
            },
            exitTransition = {
                scaleOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            }
        ) {
            curNavId = 0
            innerNavOpen.postValue(false)
            settingsOpen.postValue(true)
            SettingsScreen(navController)
        }

        //MYDJAMES VOC:
        //1 -> ARTISTS:
        composable(
            MyDJamesItem.Artists.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            }) {
            curNavId = 2
            innerNavOpen.postValue(true)
            settingsOpen.postValue(false)
            filter.postValue("artist")
            VocabularyScreen(
                navController = navController,
                filter = filterState!!,
                myDJamesItem = MyDJamesItem.Artists
            )
        }

        //2 -> PLAYLISTS:
        composable(
            MyDJamesItem.Playlists.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            }) {
            curNavId = 2
            innerNavOpen.postValue(true)
            settingsOpen.postValue(false)
            filter.postValue("playlist")
            VocabularyScreen(
                navController = navController,
                filter = filterState!!,
                myDJamesItem = MyDJamesItem.Playlists
            )
        }

        //3 -> CONTACTS:
        composable(
            MyDJamesItem.Contacts.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            }) {
            curNavId = 2
            innerNavOpen.postValue(true)
            settingsOpen.postValue(false)
            filter.postValue("contact")
            VocabularyScreen(
                navController = navController,
                filter = filterState!!,
                myDJamesItem = MyDJamesItem.Contacts
            )
        }
    }
}


//Helper: navigate to route:
fun navigateTo(navController: NavController, route: String, inner: Boolean = false) {
    navController.navigate(route) {
        // Pop up to the start destination of the graph to avoid building up a large stack of destinations on the back stack as users select items:
        navController.graph.startDestinationRoute?.let { route ->
            if (inner) {
                popUpTo(navController.currentBackStackEntry!!.id) {
                    saveState = true
                }
            } else {
                popUpTo(route) {
                    saveState = true
                }
            }
        }

        // Avoid multiple copies of the same destination when reselecting the same item:
        launchSingleTop = true
        // Restore state when reselecting a previously selected item:
        restoreState = true
    }
}
