package mega.privacy.android.app.di.ui.toolbaritem

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.Backups
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.CloudDrive
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.IncomingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.Links
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.OutgoingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.RubbishBin
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ClearSelection
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Copy
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DisputeTakeDown
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DownloadToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.GetLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.LeaveShare
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ManageLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Move
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.MultiSelectManageLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.NodeToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShare
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RenameToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RestoreToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SelectAll
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SendToChat
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Share
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ShareFolder
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.TrashToolbarMenuItem

/**
 * Toolbar module
 *
 * Handles the toolbar option inject based on the selected screen
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ToolbarItemModule {

    companion object {

        /**
         * Provide outgoing shares toolbar options
         */
        @Provides
        @ElementsIntoSet
        @OutgoingShares
        fun provideOutgoingSharesToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            download: DownloadToolbarMenuItem,
            removeShare: RemoveShare,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            manageLink: ManageLink,
            removeLinkToolbarMenuItem: RemoveLinkToolbarMenuItem,
            sendToChat: SendToChat,
            shareFolder: ShareFolder,
            share: Share,
            renameToolbarMenuItem: RenameToolbarMenuItem,
            copy: Copy,
            trashToolbarMenuItem: TrashToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                removeShare,
                getLinkToolbarMenuItem,
                manageLink,
                removeLinkToolbarMenuItem,
                sendToChat,
                shareFolder,
                share,
                renameToolbarMenuItem,
                copy,
                trashToolbarMenuItem,
            )
        }

        /**
         * Provide incoming shares toolbar options
         */
        @Provides
        @ElementsIntoSet
        @IncomingShares
        fun provideIncomingSharesToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            leaveShare: LeaveShare,
            download: DownloadToolbarMenuItem,
            move: Move,
            copy: Copy,
            sendToChat: SendToChat,
            renameToolbarMenuItem: RenameToolbarMenuItem,
            trashToolbarMenuItem: TrashToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                leaveShare,
                move,
                sendToChat,
                renameToolbarMenuItem,
                copy,
                trashToolbarMenuItem,
            )
        }

        /**
         * Provide shared links toolbar options
         */
        @Provides
        @ElementsIntoSet
        @Links
        fun provideSharedLinksToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            download: DownloadToolbarMenuItem,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            manageLink: ManageLink,
            removeLinkToolbarMenuItem: RemoveLinkToolbarMenuItem,
            sendToChat: SendToChat,
            renameToolbarMenuItem: RenameToolbarMenuItem,
            copy: Copy,
            trashToolbarMenuItem: TrashToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                getLinkToolbarMenuItem,
                manageLink,
                removeLinkToolbarMenuItem,
                sendToChat,
                renameToolbarMenuItem,
                copy,
                trashToolbarMenuItem,
            )
        }

        /**
         * Provide cloud drive toolbar options
         */
        @Provides
        @ElementsIntoSet
        @CloudDrive
        fun provideCloudDriveToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            renameToolbarMenuItem: RenameToolbarMenuItem,
            manageLink: MultiSelectManageLink,
            getLinkToolbarMenuItem: GetLinkToolbarMenuItem,
            removeLinkToolbarMenuItem: RemoveLinkToolbarMenuItem,
            download: DownloadToolbarMenuItem,
            trashToolbarMenuItem: TrashToolbarMenuItem,
            copy: Copy,
            move: Move,
            sendToChat: SendToChat,
            shareFolder: ShareFolder,
            removeShare: RemoveShare,
            disputeTakeDown: DisputeTakeDown,
            share: Share,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                download,
                disputeTakeDown,
                move,
                getLinkToolbarMenuItem,
                manageLink,
                removeLinkToolbarMenuItem,
                sendToChat,
                shareFolder,
                share,
                renameToolbarMenuItem,
                copy,
                trashToolbarMenuItem,
                removeShare,
            )
        }

        /**
         * Provide backups toolbar options
         */
        @Provides
        @ElementsIntoSet
        @Backups
        fun provideBackupsToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            removeToolbarMenuItem: RemoveToolbarMenuItem,
            restoreToolbarMenuItem: RestoreToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                removeToolbarMenuItem,
                restoreToolbarMenuItem
            )
        }

        /**
         * Provide rubbish bin toolbar options
         */
        @Provides
        @ElementsIntoSet
        @RubbishBin
        fun provideRubbishBinToolbarItems(
            selectAll: SelectAll,
            clearSelection: ClearSelection,
            removeToolbarMenuItem: RemoveToolbarMenuItem,
            restoreToolbarMenuItem: RestoreToolbarMenuItem,
        ): Set<NodeToolbarMenuItem<*>> {
            return setOf(
                selectAll,
                clearSelection,
                removeToolbarMenuItem,
                restoreToolbarMenuItem
            )
        }
    }
}