package mega.privacy.android.feature.sync.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.navigation
import com.google.accompanist.navigation.animation.composable
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute

const val syncRoute = "sync"

private const val syncEmptyRoute = "sync/empty"
private const val syncNewFolderRoute = "sync/new-folder"

@OptIn(ExperimentalAnimationApi::class)
internal fun NavGraphBuilder.syncNavGraph(navController: NavController) {
    navigation(startDestination = syncEmptyRoute, route = syncRoute) {
        composable(route = syncEmptyRoute) {
            SyncEmptyScreen {
                navController.navigate(syncNewFolderRoute)
            }
        }
        composable(route = syncNewFolderRoute) {
            SyncNewFolderScreenRoute(hiltViewModel())
        }
    }
}