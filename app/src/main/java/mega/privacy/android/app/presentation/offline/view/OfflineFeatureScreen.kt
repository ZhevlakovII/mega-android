package mega.privacy.android.app.presentation.offline.view

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUiState
import mega.privacy.android.app.presentation.view.NodeGridView
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeGridViewItem
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Scaffold for the Offline Flow Screen
 */
@Composable
fun OfflineFeatureScreen(
    uiState: OfflineUiState,
    backgroundColor: Color = MaterialTheme.colors.background,
    fileTypeIconMapper: FileTypeIconMapper,
    rootFolderOnly: Boolean = true,
    spanCount: Int = 2,
    onOfflineItemClicked: (OfflineNodeUIItem) -> Unit,
    onItemLongClicked: (OfflineNodeUIItem) -> Unit,
    onOptionClicked: (OfflineNodeUIItem) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
        backgroundColor = backgroundColor
    ) { padding ->
        val listState = rememberLazyListState()
        val gridState = rememberLazyGridState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.currentViewType == ViewType.LIST || rootFolderOnly) {
                LazyColumn(
                    state = listState
                ) {
                    items(uiState.offlineNodes) {
                        NodeListViewItem(
                            title = it.offlineNode.name,
                            subtitle = getOfflineNodeDescription(it.offlineNode),
                            icon = if (it.offlineNode.isFolder) {
                                IconPackR.drawable.ic_folder_medium_solid
                            } else {
                                fileTypeIconMapper(getFileExtension(it.offlineNode.name))
                            },
                            thumbnailData = it.offlineNode.thumbnail,
                            onMoreClicked = {
                                onOptionClicked(it)
                            },
                            onItemClicked = {
                                onOfflineItemClicked(it)
                            },
                            onLongClick = {
                                onItemLongClicked(it)
                            },
                            isSelected = it.isSelected
                        )
                        MegaDivider(dividerType = DividerType.BigStartPadding)
                    }
                }
            } else {
                val newList = rememberNodeListForGrid(
                    offlineNodeUIItem = uiState.offlineNodes,
                    spanCount = spanCount
                )
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(spanCount),
                    modifier = Modifier
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(newList) {
                        NodeGridViewItem(
                            isFolderNode = it.offlineNode.isFolder,
                            name = it.offlineNode.name,
                            iconRes = if (it.offlineNode.isFolder) {
                                IconPackR.drawable.ic_folder_medium_solid
                            } else {
                                fileTypeIconMapper(getFileExtension(it.offlineNode.name))
                            },
                            thumbnailData = it.offlineNode.thumbnail,
                            isSelected = it.isSelected,
                            isTakenDown = false,
                            inVisible = it.isInvisible,
                            onClick = {
                                onOfflineItemClicked(it)
                            },
                            onLongClick = {
                                onItemLongClicked(it)
                            },
                            onMenuClick = {}
                        )
                    }
                }
            }
        }
    }
}

/**
 * Remember function for [NodeGridView] to form empty items in case of folders count are not as per
 * span count
 * @param offlineNodeUIItem list of [OfflineNodeUIItem]
 * @param spanCount span count of [NodeGridView]
 */
@Composable
private fun rememberNodeListForGrid(
    offlineNodeUIItem: List<OfflineNodeUIItem>,
    spanCount: Int,
) = remember(spanCount, offlineNodeUIItem) {
    val folderCount = offlineNodeUIItem.count {
        it.offlineNode.isFolder
    }
    val placeholderCount =
        (folderCount % spanCount).takeIf { it != 0 }?.let { spanCount - it } ?: 0
    if (folderCount > 0 && placeholderCount > 0 && folderCount < offlineNodeUIItem.size) {
        val gridItemList = offlineNodeUIItem.toMutableList()
        repeat(placeholderCount) {
            val node = offlineNodeUIItem[folderCount - 1].copy(
                isInvisible = true,
            )
            gridItemList.add(folderCount, node)
        }
        return@remember gridItemList
    }
    offlineNodeUIItem
}

/**
 * Get the description for the offline node
 */
@Composable
internal fun getOfflineNodeDescription(offlineFileInformation: OfflineFileInformation): String {
    val context = LocalContext.current
    return if (offlineFileInformation.isFolder) {
        offlineFileInformation.folderInfo?.let { folderInfo ->
            if (folderInfo.numFolders == 0 && folderInfo.numFiles == 0) {
                stringResource(R.string.file_browser_empty_folder)
            } else if (folderInfo.numFolders == 0 && folderInfo.numFiles > 0) {
                pluralStringResource(
                    R.plurals.num_files_with_parameter,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            } else if (folderInfo.numFiles == 0 && folderInfo.numFolders > 0) {
                pluralStringResource(
                    R.plurals.num_folders_with_parameter,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                )
            } else {
                pluralStringResource(
                    R.plurals.num_folders_num_files,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                ) + pluralStringResource(
                    R.plurals.num_folders_num_files_2,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            }
        } ?: run {
            ""
        }
    } else {
        formatFileSize(offlineFileInformation.totalSize, context)
            .plus(offlineFileInformation.addedTime?.let {
                " · ".plus(
                    formatModifiedDate(
                        java.util.Locale(
                            Locale.current.language, Locale.current.region
                        ),
                        it
                    )
                )
            } ?: "")
    }
}

private fun getFileExtension(name: String) =
    name.substringAfterLast('.', "")


@CombinedThemePreviews
@Composable
private fun OfflineFeatureScreenPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        OfflineFeatureScreen(
            uiState = OfflineUiState(
                isLoading = false,
                offlineNodes = listOf(
                    OfflineNodeUIItem(
                        offlineNode = OfflineFileInformation(
                            name = "Some file.txt",
                            totalSize = 1234,
                            addedTime = System.currentTimeMillis(),
                        )
                    ),
                    OfflineNodeUIItem(
                        offlineNode = OfflineFileInformation(
                            name = "Some file.txt",
                            totalSize = 3456,
                            addedTime = System.currentTimeMillis(),
                        )
                    ),
                    OfflineNodeUIItem(
                        offlineNode = OfflineFileInformation(
                            name = "Some Folder",
                            totalSize = 1234,
                            addedTime = System.currentTimeMillis(),
                            isFolder = true,
                            folderInfo = OfflineFolderInfo(numFiles = 2, numFolders = 3)
                        )
                    ),
                )
            ),
            fileTypeIconMapper = FileTypeIconMapper(),
            onOfflineItemClicked = {},
            onItemLongClicked = {},
            onOptionClicked = {}
        )
    }
}