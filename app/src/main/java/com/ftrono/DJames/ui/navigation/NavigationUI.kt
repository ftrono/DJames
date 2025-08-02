package com.ftrono.DJames.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavController
import com.ftrono.DJames.ui.theme.NavigationItem
import com.ftrono.DJames.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.innerNavOpen
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.navigationItems
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.settingsOpen
import com.ftrono.DJames.application.spotifyLoggedIn


@Preview
@Composable
fun CenteredTopBarPreview() {
    val navController = rememberNavController()
    CenteredTopBar(
        navController = navController,
        preview = true,
        actions = {
            //SETTINGS BUTTON:
            Icon(
                modifier = Modifier
                    .padding(end = 12.dp),
                painter = painterResource(id = R.drawable.item_settings),
                contentDescription = "",
                tint = colorResource(id = R.color.light_grey)
            )

            //"MORE OPTIONS" BUTTON:
            Icon(
                modifier = Modifier
                    .padding(end = 14.dp),
                imageVector = Icons.Default.MoreVert,
                contentDescription = "",
                tint = colorResource(id = R.color.light_grey)
            )
        }
    )
}


//TOP APP BAR:
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenteredTopBar(
    navController: NavController,
    preview: Boolean = false,
    actions: @Composable () -> Unit = {}
) {
    val mContext = LocalContext.current
    val settingsOpenState by settingsOpen.observeAsState()
    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()

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
                if (preview || spotifyLoggedInState!!) {
                    Text(
                        modifier = Modifier.offset(y = -(2.dp)),
                        text = if (preview) "for user_name" else "for ${prefs.spotUserName}",
                        fontSize = 16.sp,
                        color = colorResource(id = R.color.light_grey)
                    )
                }
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
            actions()
        }
    )
}


@Preview
@Composable
fun BottomBarPreview() {
    val navController = rememberNavController()
    BottomNavigationBar(
        items = navigationItems,
        navController = navController,
    )
}


@Composable
fun BottomNavigationBar(
    items: List<NavigationItem>,
    navController: NavController
) {
    // States:
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val settingsOpenState by settingsOpen.observeAsState()
    val innerNavOpenState by innerNavOpen.observeAsState()

    val navBarColors = NavigationBarItemColors(
        selectedIndicatorColor = colorResource(id = R.color.transparent_green),
        selectedIconColor = colorResource(id = R.color.light_grey),
        selectedTextColor = colorResource(id = R.color.colorAccentLight),
        unselectedIconColor = colorResource(id = R.color.light_grey),
        unselectedTextColor = colorResource(id = R.color.light_grey),
        disabledIconColor = colorResource(id = R.color.mid_grey),
        disabledTextColor = colorResource(id = R.color.mid_grey)
    )

    //NAV BAR:
    NavigationBar(
        containerColor = colorResource(id = R.color.windowBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            //NAV ITEMS:
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title
                        )
                    },
                    colors = navBarColors,
                    alwaysShowLabel = true,
                    selected = currentRoute == item.route,
                    onClick = {
                        //Navigate:
                        val curNavRoute = item.route
                        if (curNavRoute == lastNavRoute && (settingsOpenState!! || innerNavOpenState!!)) {
                            navController.popBackStack()
                        } else {
                            navigateTo(navController, curNavRoute)
                        }
                        lastNavRoute = curNavRoute
                    }
                )
            }
        }
    }
}


@Composable
fun SideNavigationRail(
    items: List<NavigationItem>,
    navController: NavController,
) {
    // States:
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val settingsOpenState by settingsOpen.observeAsState()
    val innerNavOpenState by innerNavOpen.observeAsState()

    val navRailColors = NavigationRailItemColors(
        selectedIndicatorColor = colorResource(id = R.color.transparent_green),
        selectedIconColor = colorResource(id = R.color.light_grey),
        selectedTextColor = colorResource(id = R.color.colorAccentLight),
        unselectedIconColor = colorResource(id = R.color.light_grey),
        unselectedTextColor = colorResource(id = R.color.light_grey),
        disabledIconColor = colorResource(id = R.color.mid_grey),
        disabledTextColor = colorResource(id = R.color.mid_grey)
    )

    //NAV RAIL:
    NavigationRail(
        containerColor = colorResource(id = R.color.windowBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            //NAV ITEMS:
            items.forEach { item ->
                NavigationRailItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title
                        )
                    },
                    colors = navRailColors,
                    alwaysShowLabel = true,
                    selected = currentRoute == item.route,
                    onClick = {
                        //Navigate:
                        val curNavRoute = item.route
                        if (curNavRoute == lastNavRoute && (settingsOpenState!! || innerNavOpenState!!)) {
                            navController.popBackStack()
                        } else {
                            navigateTo(navController, curNavRoute)
                        }
                        lastNavRoute = curNavRoute
                    }
                )
            }
        }
    }
}
