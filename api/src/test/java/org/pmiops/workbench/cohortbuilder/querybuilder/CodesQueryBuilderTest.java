package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.common.collect.ListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({CodesQueryBuilder.class})
public class CodesQueryBuilderTest {

    @Autowired
    CodesQueryBuilder queryBuilder;

    @Test
    public void getMappedParameters() throws Exception {
        final SearchParameter searchParam1 = new SearchParameter().type(TreeType.ICD9.name()).subtype(TreeSubType.CM.name()).group(false).domainId("Condition").value("001").conceptId(1L);
        final SearchParameter searchParam2 = new SearchParameter().type(TreeType.ICD9.name()).subtype(TreeSubType.PROC.name()).group(false).domainId("Procedure").value("002").conceptId(1L);
        final SearchParameter searchParam3 = new SearchParameter().type(TreeType.ICD9.name()).subtype(TreeSubType.PROC.name()).group(false).domainId("Procedure").value("003").conceptId(1L);
        final SearchParameter searchParam4 = new SearchParameter().type(TreeType.ICD9.name()).subtype(TreeSubType.PROC.name()).group(true).domainId("Procedure").value("0").conceptId(1L);
        final SearchParameter searchParam5 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.CM.name()).group(false).domainId("Condition").value("A001").conceptId(1L);
        final SearchParameter searchParam6 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.PROC.name()).group(false).domainId("Procedure").value("A002").conceptId(1L);
        final SearchParameter searchParam7 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.PROC.name()).group(false).domainId("Procedure").value("A003").conceptId(1L);
        final SearchParameter searchParam8 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.PROC.name()).group(true).domainId("Procedure").value("A0").conceptId(1L);
        SearchGroupItem item = new SearchGroupItem()
          .type(TreeType.ICD9.name())
          .searchParameters(
            Arrays.asList(
              searchParam1,
              searchParam2,
              searchParam3,
              searchParam4,
              searchParam5,
              searchParam6,
              searchParam7,
              searchParam8));

        ListMultimap<CodesQueryBuilder.MultiKey, SearchParameter> mappedParemeters =
          queryBuilder.getMappedParameters(item.getSearchParameters());
        assertEquals(6, mappedParemeters.keySet().size());
        assertEquals(
          new HashSet<CodesQueryBuilder.MultiKey>(
            Arrays.asList(
              queryBuilder.new MultiKey(searchParam1),
              queryBuilder.new MultiKey(searchParam2),
              queryBuilder.new MultiKey(searchParam4),
              queryBuilder.new MultiKey(searchParam5),
              queryBuilder.new MultiKey(searchParam6),
              queryBuilder.new MultiKey(searchParam8)
            )
          ), mappedParemeters.keySet());
        assertEquals(Arrays.asList(searchParam1), mappedParemeters.get(queryBuilder.new MultiKey(searchParam1)));
        assertEquals(Arrays.asList(searchParam2, searchParam3), mappedParemeters.get(queryBuilder.new MultiKey(searchParam2)));
        assertEquals(Arrays.asList(searchParam4), mappedParemeters.get(queryBuilder.new MultiKey(searchParam4)));
        assertEquals(Arrays.asList(searchParam5), mappedParemeters.get(queryBuilder.new MultiKey(searchParam5)));
        assertEquals(Arrays.asList(searchParam6, searchParam7), mappedParemeters.get(queryBuilder.new MultiKey(searchParam6)));
        assertEquals(Arrays.asList(searchParam8), mappedParemeters.get(queryBuilder.new MultiKey(searchParam8)));
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.CODES, queryBuilder.getType());
    }
}
