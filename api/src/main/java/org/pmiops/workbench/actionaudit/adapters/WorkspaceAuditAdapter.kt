package org.pmiops.workbench.actionaudit.adapters

import org.hibernate.jdbc.Work
import org.pmiops.workbench.db.model.DbWorkspace
import org.pmiops.workbench.model.Workspace

interface WorkspaceAuditAdapter {
    fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long)

    fun fireEditAction(previousWorkspace: Workspace, editedWorkspace: Workspace)

    fun fireDeleteAction(dbWorkspace: DbWorkspace)

    fun fireDuplicateAction(
        sourceWorkspaceDbModel: DbWorkspace,
        destinationWorkspaceDbModel: DbWorkspace
    )

    fun fireCollaborateAction(sourceWorkspaceId: Long, aclStringsByUserId: Map<Long, String>)
}
