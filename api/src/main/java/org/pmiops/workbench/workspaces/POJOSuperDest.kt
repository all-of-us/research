package org.pmiops.workbench.workspaces

import org.pmiops.workbench.annotations.NoArg

@NoArg
data class POJOSuperDest(var sub: POJODest, var superField: String)