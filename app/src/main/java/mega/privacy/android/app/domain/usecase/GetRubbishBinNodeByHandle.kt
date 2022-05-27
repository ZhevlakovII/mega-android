package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get children nodes of the rubbish bin parent handle
 */
interface GetRubbishBinNodeByHandle {
    /**
     * Get children nodes of the rubbish bin parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, null if cannot be retrieved
     */
    operator fun invoke(parentHandle: Long): List<MegaNode>?
}