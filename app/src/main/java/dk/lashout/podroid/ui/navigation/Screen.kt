package dk.lashout.podroid.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Explore : Screen("explore")
    object Subscriptions : Screen("subscriptions")
    object Playlist : Screen("playlist")
    object History : Screen("history")
    object Settings : Screen("settings")
    object Player : Screen("player")
    object PodcastDetail : Screen("podcast_detail/{podcastId}") {
        fun createRoute(podcastId: String) = "podcast_detail/$podcastId"
    }
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Explore,
    Screen.Subscriptions,
    Screen.Playlist,
    Screen.History,
    Screen.Settings
)
