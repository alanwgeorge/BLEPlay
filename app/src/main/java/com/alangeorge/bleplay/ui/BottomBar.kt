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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.alangeorge.bleplay.R
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme

sealed class BottomBarScreens(
    @StringRes val title: Int,
    val icon: ImageVector,
    val route: String
) {
    object ScreenOne : BottomBarScreens(R.string.bottombar_ble_scan, Icons.Outlined.Call, DEVICE_ROUTE_SCAN)
    object ScreenTwo : BottomBarScreens(R.string.bottombar_two, Icons.Outlined.AccountBox, "start/two")
    object ScreenThree : BottomBarScreens(R.string.bottombar_three, Icons.Outlined.Home, "start/three")
    object ScreenFour : BottomBarScreens(R.string.bottombar_four, Icons.Outlined.Lock, "start/four")

    companion object {
        fun asList() = listOf(ScreenOne, ScreenTwo, ScreenThree, ScreenFour)
    }

    override fun toString(): String {
        return "StartScreens(title=$title, icon=$icon, route='$route')"
    }

}

@Composable
fun BottomBar(
    navigateToRoute: (BottomBarScreens) -> Unit,
    items: List<BottomBarScreens>,
    currentRoute: String
) {
    BottomNavigation {
        items.forEach { screen ->
            BottomNavigationItem(
                selected = currentRoute == screen.route,
                label = { Text(stringResource(id = screen.title)) },
                onClick = { navigateToRoute(screen) },
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
fun BottomBarPreview() {
    BLEPlayTheme {
        BottomBar(
            navigateToRoute = {},
            items = BottomBarScreens.asList(),
            currentRoute = BottomBarScreens.ScreenTwo.route
        )
    }
}