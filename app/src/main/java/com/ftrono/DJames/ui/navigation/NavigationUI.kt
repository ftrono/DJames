package com.ftrono.DJames.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.innerNavOpen
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.navigationItems
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.libHeads
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.components.SplitterSign


@Composable
fun TopBarMenu(
    backgroundColor: Color? = null,
    imageRes: Painter? = null,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    contentText: String = "",
    imageUrl: String = "",
    moreOnly: Boolean = false,
    onClick: () -> Unit = {},
    optionsMenu: @Composable () -> Unit = {}
) {
    Box() {
        Row(
            modifier = Modifier
                .padding(end = 12.dp)
                .clickable {
                    onClick()
                },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (moreOnly) {
                // MORE ICON:
                Icon(
                    modifier = Modifier
                        .size(28.dp),
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colorResource(R.color.light_grey)
                )
            } else {
                // ROUNDED SIGN ICON:
                RoundedSign(
                    modifier = Modifier,
                    signSize = 48.dp,
                    contentSize = 24,
                    backgroundColor = backgroundColor ?: colorResource(R.color.dark_grey),
                    borderColor = colorResource(id = R.color.mid_grey),
                    contentColor = colorResource(id = R.color.light_grey),
                    borderWidth = 2.5.dp,
                    contentText = contentText,
                    imageUrl = imageUrl,
                    imageRes = imageRes,
                    iconPainter = iconPainter,
                    iconVector = iconVector ?: Icons.Outlined.Person,   //Residual
                )
                // MORE ICON:
                Icon(
                    modifier = Modifier
                        .size(28.dp),
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colorResource(R.color.light_grey)
                )
            }
        }
        // OPTIONS MENU:
        optionsMenu()
    }
}


@Preview
@Composable
fun TopBarPreview() {
    StreetUITopBar(
        pretitle = "",
        title = "DJames",
        subtitle = "for user_name",
        showBack = true,
        optionButtons = {
            //TODO
            TopBarMenu(
                backgroundColor = colorResource(R.color.blueSign),
                contentText = "20"
            )
        }
    )
}


//TOP APP BAR:
@Composable
fun StreetUITopBar(
    pretitle: String = "",
    title: String,
    subtitle: String = "",
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    optionButtons: @Composable() (RowScope.() -> Unit) = {}
) {
    //HEADER:
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .background(colorResource(id = R.color.windowBackground)),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //BACK:
        if (showBack) {
            Icon(
                modifier = Modifier
                    .padding(start = 18.dp)
                    .size(30.dp)
                    .clickable {
                        onBack()
                    },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorResource(id = R.color.light_grey)
            )
        }

        //HEADERS TEXT:
        Column(
            modifier = Modifier
                .padding(start = 18.dp, end = 30.dp)
                .weight(1F)
        ) {
            if (pretitle != "") {
                Text(
                    text = pretitle,
                    fontSize = 14.sp,
                    // fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                )
            }
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey),
            )
            if (subtitle != "") {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    // fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                )
            }
        }

        //OPTIONS BUTTONS:
        optionButtons()
    }
}


@Preview(widthDp = 500)
@Composable
fun TopSplitterBarPreview() {
    val currentCatState = rememberSaveable { mutableStateOf(libHeads[0]) }
    TopSplitterBar(
        currentCatState = currentCatState,
        showBack = true,
        optionButtons = {
            //TODO
            TopBarMenu(
                backgroundColor = colorResource(R.color.blueSign),
                contentText = "20"
            )
        }
    )
}



//TOP SPLITTER BAR:
@Composable
fun TopSplitterBar(
    currentCatState: MutableState<String>,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onNavClick: () -> Unit = {},
    optionButtons: @Composable() (RowScope.() -> Unit) = {}
) {
    //HEADER:
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .background(colorResource(id = R.color.windowBackground)),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //BACK:
        if (showBack) {
            Icon(
                modifier = Modifier
                    .padding(start = 18.dp)
                    .size(30.dp)
                    .clickable {
                        onBack()
                    },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorResource(id = R.color.light_grey)
            )
        }

        //SPLITTER SIGN (bigger weight with margins):
        Row(
            modifier = Modifier
                .weight(1F),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            SplitterSign(
                currentCatState = currentCatState,
                onNavClick = onNavClick,
            )
        }

        //OPTIONS BUTTONS:
        optionButtons()
    }
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
    val extraOpenState by extraOpen.observeAsState()
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
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
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
                        if (curNavRoute == lastNavRoute && (extraOpenState!! || innerNavOpenState!!)) {
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
    val extraOpenState by extraOpen.observeAsState()
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
                        if (curNavRoute == lastNavRoute && (extraOpenState!! || innerNavOpenState!!)) {
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
