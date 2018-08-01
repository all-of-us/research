package org.pmiops.workbench.cdr;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class CommonTestDialect extends MySQLDialect {

    public CommonTestDialect() {
        super();
        // For in-memory tests, use LIKE for full text searches.
        // For some weird reason, we need to have this function use DOUBLE; see
        // https://pavelmakhov.com/2016/09/jpa-custom-function
        registerFunction("match", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE,
                "?1 LIKE ?2"));
    }

    @Override
    public boolean dropConstraints() {
        // We don't need to drop constraints before dropping tables, that just leads to error
        // messages about missing tables when we don't have a schema in the database
        return false;
    }

}