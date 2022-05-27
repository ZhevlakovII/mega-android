package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.globalmanagement.SortOrderManagement
import nz.mega.sdk.MegaNode
import javax.inject.Inject


/**
 * Default get children nodes of the browser parent handle
 *
 *  @property getNodeByHandle
 *  @property getChildrenNode
 *  @property getRootNode
 *  @property sortOrderManagement
 */
class DefaultGetBrowserNodeByHandle @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getChildrenNode: GetChildrenNode,
    private val getRootNode: GetRootNode,
    private val sortOrderManagement: SortOrderManagement,
) : GetBrowserNodeByHandle {

    override fun invoke(parentHandle: Long): List<MegaNode>? {
        val node = (if (parentHandle != -1L) getNodeByHandle(parentHandle) else getRootNode())
            ?: return null
        return getChildrenNode(parent = node, order = sortOrderManagement.getOrderCloud())
    }

}