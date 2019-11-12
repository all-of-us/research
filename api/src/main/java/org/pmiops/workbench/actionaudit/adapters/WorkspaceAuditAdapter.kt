package org.pmiops.workbench.actionaudit.adapters

import org.pmiops.workbench.db.model.DbWorkspace
import org.pmiops.workbench.model.Workspace

interface WorkspaceAuditAdapter : AuditAdapter<Workspace> {
    fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long)

    fun fireDeleteAction(dbWorkspace: DbWorkspace)

    fun fireDuplicateAction(
        sourceWorkspaceDbModel: DbWorkspace,
        destinationWorkspaceDbModel: DbWorkspace
    )

    fun fireCollaborateAction(sourceWorkspaceId: Long, aclStringsByUserId: Map<Long, String>)
}
