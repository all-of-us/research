package org.pmiops.workbench.workspaces

import org.pmiops.workbench.annotations.NoArg

@NoArg
data class POJOSuperSource(var sub: POJOSource, var superField: String)