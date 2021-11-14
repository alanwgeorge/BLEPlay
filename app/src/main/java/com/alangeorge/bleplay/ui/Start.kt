package com.alangeorge.bleplay.ui

import androidx.annotation.StringRes
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.alangeorge.bleplay.R
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme

sealed class StartScreens(
    @StringRes val title: Int,
    val icon: ImageVector,
    val route: String
) {
    object ScreenOne : StartScreens(R.string.home_one, Icons.Outlined.Home, "start/one")
    object ScreenTwo : StartScreens(R.string.home_two, Icons.Outlined.AccountBox, "start/two")
    object ScreenThree : StartScreens(R.string.home_three, Icons.Outlined.Call, DEVICE_ROUTE_BASE)
    object ScreenFour : StartScreens(R.string.home_four, Icons.Outlined.Lock, "start/four")

    companion object {
        fun asList() = listOf(ScreenOne, ScreenTwo, ScreenThree, ScreenFour)
    }
}


@Composable
fun StartBottomBar(
    navigateToRoute: (String) -> Unit,
    items: List<StartScreens>,
    currentRoute: String
) {
    BottomNavigation {
        items.forEach { screen ->
            BottomNavigationItem(
                selected = currentRoute == screen.route,
                label = { Text(stringResource(id = screen.title)) },
                onClick = { navigateToRoute(screen.route) },
                alwaysShowLabel = true,
                icon = { Icon(
                    imageVector = screen.icon,
                    contentDescription = stringResource(id = screen.title)
                ) }
            )
        }
    }
}

@Preview
@Composable
fun StartBottomBarPreview() {
    BLEPlayTheme {
        StartBottomBar(
            navigateToRoute = {},
            items = StartScreens.asList(),
            currentRoute = StartScreens.ScreenTwo.route
        )
    }
}