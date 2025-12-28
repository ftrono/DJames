package com.ftrono.DJames.ui.navigation

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
import com.ftrono.DJames.application.curNavId
import com.ftrono.DJames.application.innerNavOpen
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.screens.AccountsScreen
import com.ftrono.DJames.application.screens.GuideScreen
import com.ftrono.DJames.application.screens.HomeScreen
import com.ftrono.DJames.application.screens.SettingsScreen
import com.ftrono.DJames.application.screens.LibraryScreen
import com.ftrono.DJames.application.screens.MessagesScreen
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
) {
    val sharedViewModel: SharedViewModel = viewModel()
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
                    towards = if (curNavId > 1) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            }) {
            curNavId = 1
            innerNavOpen.postValue(false)
            extraOpen.postValue(false)
            HomeScreen(navController, chatManager, sharedViewModel)
        }

        //2 -> LIBRARY:
        composable(
            NavigationItem.Library.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavId >= 0) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            }) {
            curNavId = 0
            innerNavOpen.postValue(false)
            extraOpen.postValue(false)
            LibraryScreen(navController)
        }

        //3 -> MESSAGES:
        composable(
            NavigationItem.Messages.route,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = if (curNavId > 2) {
                        AnimatedContentTransitionScope.SlideDirection.End
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }
                )
            }) {
            curNavId = 2
            innerNavOpen.postValue(false)
            extraOpen.postValue(false)
            MessagesScreen(navController, chatManager, sharedViewModel)
        }

        //EXTRA:
        //0 -> ACCOUNT:
        composable(
            NavigationItem.Accounts.route,
            enterTransition = {
                scaleIn() + expandVertically(expandFrom = Alignment.Bottom)
            },
            exitTransition = {
                scaleOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            }
        ) {
            curNavId = 0
            innerNavOpen.postValue(false)
            extraOpen.postValue(true)
            AccountsScreen(navController)
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
            curNavId = 0
            innerNavOpen.postValue(false)
            extraOpen.postValue(true)
            SettingsScreen(navController)
        }

        //0 -> GUIDE:
        composable(
            NavigationItem.Guide.route,
            enterTransition = {
                scaleIn() + expandVertically(expandFrom = Alignment.Bottom)
            },
            exitTransition = {
                scaleOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            }
        ) {
            curNavId = 0
            innerNavOpen.postValue(false)
            extraOpen.postValue(true)
            GuideScreen(navController)
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
