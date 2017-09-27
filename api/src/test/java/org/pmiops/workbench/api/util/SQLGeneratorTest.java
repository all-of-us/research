package org.pmiops.workbench.api.util;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.config.TestBigQueryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({SQLGenerator.class})
@SpringBootTest(classes = {TestBigQueryConfig.class})
public class SQLGeneratorTest {

    private String showValues(String expected, String actual) {
        return String.format("\nExpected: %s\nActual:   %s", expected, actual);
    }

    @Autowired
    SQLGenerator generator;

    @Autowired
    WorkbenchConfig workbenchConfig;

    @Test
    public void findGroupCodes() throws Exception {
        String TABLE_PREFIX = workbenchConfig.bigquery.projectId + "." + workbenchConfig.bigquery.dataSetId;
        QueryRequest result = generator.findGroupCodes("ICD9", Arrays.asList("11.1", "11.2", "11.3"));
        String expected =
                "SELECT code, domain_id AS domainId FROM `" + TABLE_PREFIX + ".icd9_criteria` " +
                "WHERE (code LIKE @code0 OR code LIKE @code1 OR code LIKE @code2) " +
                "AND is_selectable = TRUE AND is_group = FALSE ORDER BY code ASC";
        String actual = result.getQuery();
        assert actual.equals(expected) : showValues(expected, actual);
    }

    @Test
    public void handleSearch() throws Exception {
        String TABLE_PREFIX = workbenchConfig.bigquery.projectId + "." + workbenchConfig.bigquery.dataSetId;
        List<SearchParameter> params = new ArrayList<>();
        SearchParameter p;

        p = new SearchParameter();
        p.setDomainId("Condition");
        p.setCode("10.1");
        params.add(p);

        p = new SearchParameter();
        p.setDomainId("Condition");
        p.setCode("20.2");
        params.add(p);

        p = new SearchParameter();
        p.setDomainId("Measurement");
        p.setCode("30.3");
        params.add(p);

        /* Check the generated query */
        QueryRequest request = generator.handleSearch("ICD9", params);
        String actual = request.getQuery();
        String expected =
            "SELECT DISTINCT CONCAT(" +
                "CAST(p.person_id AS string), ',', " +
                "p.gender_source_value, ',', " +
                "p.race_source_value) AS val "+
            "FROM `" + TABLE_PREFIX + ".person` p " +
            "WHERE PERSON_ID IN (" +
                "SELECT DISTINCT PERSON_ID " +
                "FROM `" + TABLE_PREFIX + ".condition_occurrence` a, `" + TABLE_PREFIX + ".CONCEPT` b " +
                "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID " +
                "AND b.VOCABULARY_ID IN (@cm,@proc) " +
                "AND b.CONCEPT_CODE IN UNNEST(@Conditioncodes)"+
                " UNION DISTINCT " +
                "SELECT DISTINCT PERSON_ID " +
                "FROM `" + TABLE_PREFIX + ".measurement` a, `" + TABLE_PREFIX + ".CONCEPT` b " +
                "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID " +
                "AND b.VOCABULARY_ID IN (@cm,@proc) " +
                "AND b.CONCEPT_CODE IN UNNEST(@Measurementcodes))";
        assert actual.equals(expected) : showValues(expected, actual);

        /* Check the query parameters */
        QueryParameterValue param;
        Map<String, QueryParameterValue> preparedParams = request.getNamedParameters();
        param = preparedParams.get("Conditioncodes");
        assert param.getArrayValues().get(0).getValue().equals("10.1");
        assert param.getArrayValues().get(1).getValue().equals("20.2");
        param = preparedParams.get("Measurementcodes");
        assert param.getArrayValues().get(0).getValue().equals("30.3");
        param = preparedParams.get("cm");
        assert param.getValue().equals("ICD9CM");
        param = preparedParams.get("proc");
        assert param.getValue().equals("ICD9Proc");
    }

    @Test
    public void findParametersWithEmptyDomainIds() throws Exception {
        final SearchParameter searchParameterCondtion = new SearchParameter();
        searchParameterCondtion.setCode("001");

        final SearchParameter searchParameterCondtion2 = new SearchParameter();
        searchParameterCondtion2.setCode("002");
        searchParameterCondtion2.setDomainId("Condition");

        List<SearchParameter> parameterList = new ArrayList<>();
        parameterList.add(searchParameterCondtion);
        parameterList.add(searchParameterCondtion2);

        SearchGroupItem item = new SearchGroupItem();
        item.setType("ICD9");
        item.setSearchParameters(parameterList);

        assertEquals(Arrays.asList("001%"), generator.findParametersWithEmptyDomainIds(item.getSearchParameters()));
        assertEquals(1, item.getSearchParameters().size());

        SearchParameter searchParameter = new SearchParameter();
        searchParameter.setCode("002");
        searchParameter.setDomainId("Condition");
        assertEquals(searchParameter, item.getSearchParameters().get(0));
    }

    @Test
    public void getSubQuery() throws Exception {
        String TABLE_PREFIX = workbenchConfig.bigquery.projectId + "." + workbenchConfig.bigquery.dataSetId;
        Map<String, String> expectedByKey = new HashMap<String, String>();
        expectedByKey.put("Condition",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".condition_occurrence` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND b.CONCEPT_CODE IN UNNEST(@Conditioncodes)"
        );
        expectedByKey.put("Observation",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".observation` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND b.CONCEPT_CODE IN UNNEST(@Observationcodes)"
        );
        expectedByKey.put("Measurement",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".measurement` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND b.CONCEPT_CODE IN UNNEST(@Measurementcodes)"
        );
        expectedByKey.put("Exposure",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".device_exposure` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.DEVICE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND b.CONCEPT_CODE IN UNNEST(@Exposurecodes)"
        );
        expectedByKey.put("Drug",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".drug_exposure` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.DRUG_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND b.CONCEPT_CODE IN UNNEST(@Drugcodes)"
        );
        expectedByKey.put("Procedure",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".procedure_occurrence` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND b.CONCEPT_CODE IN UNNEST(@Procedurecodes)"
        );

        List<String> keys = Arrays.asList("Condition", "Observation", "Measurement", "Exposure", "Drug", "Procedure");
        String actual, expected;
        for (String key : keys) {
            actual = generator.getSubQuery(key);
            expected = expectedByKey.get(key);
            assert actual.equals(expected) : showValues(expected, actual);
        }
    }

    @Test
    public void getMappedParameters() throws Exception {
        final SearchParameter searchParameterCondtion = new SearchParameter();
        searchParameterCondtion.setDomainId("Condition");
        searchParameterCondtion.setCode("001");

        final SearchParameter searchParameterProc1 = new SearchParameter();
        searchParameterProc1.setDomainId("Procedure");
        searchParameterProc1.setCode("002");

        final SearchParameter searchParameterProc2 = new SearchParameter();
        searchParameterProc2.setDomainId("Procedure");
        searchParameterProc2.setCode("003");

        SearchGroupItem item = new SearchGroupItem();
        item.setType("ICD9");
        item.setSearchParameters(Arrays.asList(searchParameterCondtion, searchParameterProc1, searchParameterProc2));

        List<SearchParameter> parameters = item.getSearchParameters();
        assertEquals(2, generator.getMappedParameters(parameters).keySet().size());
        assertEquals(new HashSet<String>(Arrays.asList("Condition", "Procedure")), generator.getMappedParameters(parameters).keySet());
        assertEquals(Arrays.asList("001"), generator.getMappedParameters(parameters).get("Condition"));
        assertEquals(Arrays.asList("002", "003"), generator.getMappedParameters(parameters).get("Procedure"));
    }
}
