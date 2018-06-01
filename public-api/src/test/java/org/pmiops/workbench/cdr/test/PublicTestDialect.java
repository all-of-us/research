package org.pmiops.workbench.cdr.test;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class PublicTestDialect extends MySQLDialect {

    public PublicTestDialect() {
        super();
        // For in-memory tests, use LIKE for full text searches.
        // For some weird reason, we need to have this function use DOUBLE; see
        // https://pavelmakhov.com/2016/09/jpa-custom-function
        registerFunction("match", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE,
                "?1 LIKE ?2"));
    }

}
