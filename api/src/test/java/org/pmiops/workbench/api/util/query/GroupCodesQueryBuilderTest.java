package org.pmiops.workbench.api.util.query;

import com.google.cloud.bigquery.QueryRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.config.TestBigQueryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({GroupCodesQueryBuilder.class})
@SpringBootTest(classes = {TestBigQueryConfig.class})
public class GroupCodesQueryBuilderTest {

    @Autowired
    GroupCodesQueryBuilder queryBuilder;

    @Autowired
    WorkbenchConfig workbenchConfig;

    @Test
    public void buildQueryRequest() throws Exception {
        QueryParameters parameters = new QueryParameters().type("ICD9").codes(Arrays.asList("11.1", "11.2", "11.3"));
        QueryRequest result = queryBuilder.buildQueryRequest(parameters);
        String expected =
                "select code,\n" +
                        "domain_id as domainId\n" +
                        "from `" + getTablePrefix() + ".icd9_criteria`\n" +
                        "where (code like @code0 or code like @code1 or code like @code2)\n" +
                        "and is_selectable = TRUE and is_group = FALSE order by code asc";
        String actual = result.getQuery();
        assertEquals(expected, actual);
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.GROUP_CODES.getName(), queryBuilder.getType());
    }

    private String getTablePrefix() {
        return workbenchConfig.bigquery.projectId + "." + workbenchConfig.bigquery.dataSetId;
    }

}
