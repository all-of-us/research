package org.pmiops.workbench.actionaudit.targetproperties

enum class AccountTargetProperty
    constructor(override val propertyName: String) : SimpleTargetProperty {
    IS_ENABLED("is_enabled"),
    REGISTRATION_STATUS("registration_status"),
    ACKNOWLEDGED_TOS_VERSION("acknowledged_tos_version");
}
