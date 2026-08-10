package com.ftrono.DJames.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ftrono.DJames.application.curNavLevel
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.screens.AccountsScreen
import com.ftrono.DJames.application.screens.ClockScreen
import com.ftrono.DJames.application.screens.HomeScreen
import com.ftrono.DJames.application.screens.SettingsScreen
import com.ftrono.DJames.application.screens.LibraryScreen
import com.ftrono.DJames.application.screens.MessagesScreen
import com.ftrono.DJames.application.clockMode
import com.ftrono.DJames.be.agents.chat.ChatManager
import com.ftrono.DJames.ui.theme.NavigationItem


//NAV HOST:
class SharedViewModel : ViewModel() {
    var text by mutableStateOf("")
}

@Composable
fun Navigation(
    navController: NavHostController,
    chatManager: ChatManager,
    preview: Boolean = false,
) {
    val sharedViewModel: SharedViewModel = viewModel()
    NavHost(
        modifier = Modifier
            .fillMaxSize(),
        navController = navController,
        startDestination = if (clockMode) NavigationItem.Clock.route else NavigationItem.Home.route
    ) {
        //MAIN:
        //HOME:
        composable(
            NavigationItem.Home.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavLevel > 0) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                )
            }) {
            curNavLevel = 0
            clockMode = false
            extraOpen.postValue(false)
            HomeScreen(navController, preview)
        }

        //CLOCK:
        composable(
            NavigationItem.Clock.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavLevel > 0) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                )
            }) {
            curNavLevel = 0
            clockMode = true
            extraOpen.postValue(false)
            ClockScreen(navController, preview)
        }

        //LIBRARY:
        composable(
            NavigationItem.Library.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavLevel > 0) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                )
            }) {
            curNavLevel = 1
            extraOpen.postValue(false)
            LibraryScreen(navController, preview=preview)
        }

        //MESSAGES:
        composable(
            NavigationItem.Messages.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                )
            }) {
            curNavLevel = 1
            extraOpen.postValue(false)
            MessagesScreen(navController, chatManager, sharedViewModel, preview)
        }

        //EXTRA:
        //ACCOUNT:
        composable(
            NavigationItem.Accounts.route,
            enterTransition = {
                scaleIn() + expandVertically(expandFrom = Alignment.Bottom)
            },
            exitTransition = {
                scaleOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            }
        ) {
            curNavLevel = 0
            extraOpen.postValue(true)
            AccountsScreen(navController, preview)
        }

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
            curNavLevel = 0
            extraOpen.postValue(true)
            SettingsScreen(navController, preview)
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
