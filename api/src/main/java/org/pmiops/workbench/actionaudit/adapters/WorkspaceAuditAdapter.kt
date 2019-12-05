package org.pmiops.workbench.actionaudit.adapters

import org.pmiops.workbench.db.model.DbWorkspace
import org.pmiops.workbench.model.Workspace

interface WorkspaceAuditAdapter {
    fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long)

    fun fireEditAction(
        previousWorkspace: Workspace?,
        editedWorkspace: Workspace?,
        workspaceId: Long
    )

    fun fireDeleteAction(dbWorkspace: DbWorkspace)

    fun fireDuplicateAction(
        sourceWorkspaceId: Long,
        destinationWorkspaceId: Long,
        destinationWorkspace: Workspace
    )

    fun fireCollaborateAction(sourceWorkspaceId: Long, aclStringsByUserId: Map<Long, String>)
}
