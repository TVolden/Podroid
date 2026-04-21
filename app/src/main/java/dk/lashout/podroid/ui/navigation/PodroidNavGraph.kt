package dk.lashout.podroid.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.gson.Gson
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.service.PlaybackController
import dk.lashout.podroid.ui.screens.dashboard.DashboardScreen
import dk.lashout.podroid.ui.screens.explore.ExploreScreen
import dk.lashout.podroid.ui.screens.history.HistoryScreen
import dk.lashout.podroid.ui.screens.player.PlayerScreen
import dk.lashout.podroid.ui.screens.playlist.PlaylistDetailScreen
import dk.lashout.podroid.ui.screens.playlist.PlaylistScreen
import dk.lashout.podroid.ui.screens.podcastdetail.PodcastDetailScreen
import dk.lashout.podroid.ui.screens.settings.SettingsScreen
import dk.lashout.podroid.ui.screens.subscriptions.SubscriptionsScreen

@Composable
fun PodroidNavGraph(
    navController: NavHostController,
    mediaController: PlaybackController,
    modifier: Modifier = Modifier
) {
    val gson = remember { Gson() }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onEpisodeClick = { episode ->
                    mediaController.playEpisodeFromSubscription(episode)
                    navController.navigate(Screen.Player.route)
                },
                onPodcastClick = { podcastId ->
                    navController.navigate(
                        "${Screen.PodcastDetail.route.substringBefore("{")}${Uri.encode(podcastId)}"
                    )
                }
            )
        }

        composable(Screen.Explore.route) {
            ExploreScreen(
                onPodcastClick = { podcast ->
                    val json = Uri.encode(gson.toJson(podcast))
                    navController.navigate("${Screen.PodcastDetail.route.substringBefore("{")}$json")
                }
            )
        }

        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen(
                onPodcastClick = { podcast ->
                    val json = Uri.encode(gson.toJson(podcast))
                    navController.navigate("${Screen.PodcastDetail.route.substringBefore("{")}$json")
                }
            )
        }

        composable(
            route = Screen.PodcastDetail.route,
            arguments = listOf(navArgument("podcastId") { type = NavType.StringType })
        ) { backStackEntry ->
            val podcastJson = Uri.decode(backStackEntry.arguments?.getString("podcastId") ?: "")
            val podcast = runCatching { gson.fromJson(podcastJson, Podcast::class.java) }.getOrNull()
            PodcastDetailScreen(
                podcast = podcast,
                onBack = { navController.popBackStack() },
                onEpisodeClick = { episode ->
                    mediaController.playEpisodeFromSubscription(episode)
                    navController.navigate(Screen.Player.route)
                },
                onCatchUpFromHere = { episode ->
                    mediaController.catchUpFromHere(episode)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(Screen.Playlist.route) {
            PlaylistScreen(
                onPlaylistClick = { playlist ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) {
            PlaylistDetailScreen(
                onBack = { navController.popBackStack() },
                onEpisodeClick = { episode, playlistId, index ->
                    mediaController.playPlaylist(playlistId, episode, index)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onEpisodeClick = { episode ->
                    mediaController.playEpisodeFromSubscription(episode)
                    navController.navigate(Screen.Player.route)
                },
                onPodcastClick = { podcastId ->
                    navController.navigate(
                        "${Screen.PodcastDetail.route.substringBefore("{")}${Uri.encode(podcastId)}"
                    )
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
