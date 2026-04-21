package dk.lashout.podroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.lashout.podroid.service.PlaybackController
import dk.lashout.podroid.ui.components.MiniPlayer
import dk.lashout.podroid.ui.navigation.PodroidNavGraph
import dk.lashout.podroid.ui.navigation.Screen
import dk.lashout.podroid.ui.navigation.bottomNavScreens
import dk.lashout.podroid.ui.theme.PodroidTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var mediaController: PlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaController.connect()

        setContent {
            PodroidTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val playerState by mediaController.playerState.collectAsState()

                val showBottomBar = currentRoute != Screen.Player.route

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            androidx.compose.foundation.layout.Column {
                                if (playerState.currentEpisode != null) {
                                    MiniPlayer(
                                        state = playerState,
                                        onPlayPause = {
                                            if (playerState.isPlaying) mediaController.pause()
                                            else mediaController.play()
                                        },
                                        onExpand = { navController.navigate(Screen.Player.route) }
                                    )
                                }
                                NavigationBar {
                                    bottomNavScreens.forEach { screen ->
                                        val icon = when (screen) {
                                            Screen.Dashboard -> Icons.Default.Home
                                            Screen.Explore -> Icons.Default.Search
                                            Screen.Subscriptions -> Icons.Default.Headset
                                            Screen.Playlist -> Icons.Default.QueueMusic
                                            Screen.History -> Icons.Default.History
                                            Screen.Settings -> Icons.Default.Settings
                                            else -> Icons.Default.Home
                                        }
                                        val label = when (screen) {
                                            Screen.Dashboard -> "Home"
                                            Screen.Explore -> "Explore"
                                            Screen.Subscriptions -> "Podcast"
                                            Screen.Playlist -> "Playlist"
                                            Screen.History -> "History"
                                            Screen.Settings -> "Settings"
                                            else -> ""
                                        }
                                        NavigationBarItem(
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label) },
                                            selected = currentRoute == screen.route,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    PodroidNavGraph(
                        navController = navController,
                        mediaController = mediaController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        mediaController.disconnect()
        super.onDestroy()
    }
}
