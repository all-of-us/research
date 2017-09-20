package org.pmiops.workbench.api.util;

import com.google.cloud.bigquery.QueryRequest;
import org.junit.Test;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;

import java.util.*;

import static org.junit.Assert.*;

public class SQLGeneratorTest {
    static String TABLE_PREFIX = "pmi-drc-api-test.synpuf";

    private String showValues(String expected, String actual) {
        return String.format("\nExpected: %s\nActual:   %s", expected, actual);
    }

    @Test
    public void findGroupCodes() throws Exception {
        SQLGenerator generator = new SQLGenerator();
        QueryRequest result = generator.findGroupCodes("ICD9", Arrays.asList("11.1", "11.2", "11.3"));
        String expected =
                "SELECT code, domain_id AS domainId FROM `" + TABLE_PREFIX + ".ICD9_criteria` " +
                "WHERE (code LIKE @11.1 OR code LIKE @11.2 OR code LIKE @11.3) " +
                "AND is_selectable = 1 AND is_group = 0 ORDER BY code ASC";
        String actual = result.getQuery();
        assert actual.equals(expected) : showValues(expected, actual);
    }

    @Test
    public void handleICD9Search() throws Exception {
        SQLGenerator generator = new SQLGenerator();
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

        String actual = generator.handleICD9Search(params).getQuery();
        String expected = 
            "SELECT PERSON_ID || ',' || gender_source_value || ',' || x_race_ui AS val " +
            "FROM `" + TABLE_PREFIX + ".PERSON` " +
            "WHERE PERSON_ID IN " +
            "(SELECT PERSON_ID FROM (" +
                "SELECT DISTINCT PERSON_ID, CONDITION_START_DATE as ENTRY_DATE " +
                "FROM `" + TABLE_PREFIX + ".CONDITION_OCCURRENCE` a, `" + TABLE_PREFIX + ".CONCEPT` b " +
                "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID " +
                "AND b.VOCABULARY_ID IN (@cm,@proc) " +
                "AND CONDITION_SOURCE_VALUE IN (@Conditioncodes)"+
                " UNION " +
                "SELECT DISTINCT PERSON_ID, MEASUREMENT_DATE as ENTRY_DATE "+
                "FROM `" + TABLE_PREFIX + ".MEASUREMENT` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
                "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
                "AND b.VOCABULARY_ID IN (@cm,@proc) "+
                "AND MEASUREMENT_SOURCE_VALUE IN (@Measurementcodes)" +
            "))";
        assert actual.equals(expected) : showValues(expected, actual);
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

        SearchRequest request = new SearchRequest();
        request.setType("ICD9");
        request.setSearchParameters(parameterList);

        SQLGenerator controller = new SQLGenerator();
        assertEquals(Arrays.asList("001%"), controller.findParametersWithEmptyDomainIds(request.getSearchParameters()));
        assertEquals(1, request.getSearchParameters().size());

        SearchParameter searchParameter = new SearchParameter();
        searchParameter.setCode("002");
        searchParameter.setDomainId("Condition");
        assertEquals(searchParameter, request.getSearchParameters().get(0));
    }

    @Test
    public void getSubQuery() throws Exception {
        Map<String, String> expectedByKey = new HashMap<String, String>();
        expectedByKey.put("Condition",
            "SELECT DISTINCT PERSON_ID, CONDITION_START_DATE as ENTRY_DATE "+
            "FROM `" + TABLE_PREFIX + ".CONDITION_OCCURRENCE` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND CONDITION_SOURCE_VALUE IN (@Conditioncodes)"
        );
        expectedByKey.put("Observation",
            "SELECT DISTINCT PERSON_ID, OBSERVATION_DATE as ENTRY_DATE "+
            "FROM `" + TABLE_PREFIX + ".OBSERVATION` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND OBSERVATION_SOURCE_VALUE IN (@Observationcodes)"
        );
        expectedByKey.put("Measurement",
            "SELECT DISTINCT PERSON_ID, MEASUREMENT_DATE as ENTRY_DATE "+
            "FROM `" + TABLE_PREFIX + ".MEASUREMENT` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND MEASUREMENT_SOURCE_VALUE IN (@Measurementcodes)"
        );
        expectedByKey.put("Exposure",
            "SELECT DISTINCT PERSON_ID, DEVICE_EXPOSURE_START_DATE as ENTRY_DATE "+
            "FROM `" + TABLE_PREFIX + ".DEVICE_EXPOSURE` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.DEVICE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND DEVICE_SOURCE_VALUE IN (@Exposurecodes)"
        );
        expectedByKey.put("Drug",
            "SELECT DISTINCT PERSON_ID, DRUG_EXPOSURE_START_DATE as ENTRY_DATE "+
            "FROM `" + TABLE_PREFIX + ".DRUG_EXPOSURE` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.DRUG_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND DRUG_SOURCE_VALUE IN (@Drugcodes)"
        );
        expectedByKey.put("Procedure",
            "SELECT DISTINCT PERSON_ID, PROCEDURE_DATE as ENTRY_DATE "+
            "FROM `" + TABLE_PREFIX + ".PROCEDURE_OCCURRENCE` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND PROCEDURE_SOURCE_VALUE IN (@Procedurecodes)"
        );

        List<String> keys = Arrays.asList("Condition", "Observation", "Measurement", "Exposure", "Drug", "Procedure");
        SQLGenerator generator = new SQLGenerator();
        String actual, expected;
        for (String key : keys) {
            actual = generator.getSubQuery(key);
            expected = expectedByKey.get(key);
            assert actual.equals(expected) : showValues(expected, actual);
        }
    }

    @Test
    public void getTablePrefix() throws Exception {
        // FYI: This is basically a Stub
        SQLGenerator generator = new SQLGenerator();
        String actual = generator.getTablePrefix();
        assert actual.equals(TABLE_PREFIX) : showValues(TABLE_PREFIX, actual);
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

        SearchRequest request = new SearchRequest();
        request.setType("ICD9");
        request.setSearchParameters(Arrays.asList(searchParameterCondtion, searchParameterProc1, searchParameterProc2));

        SQLGenerator controller = new SQLGenerator();
        List<SearchParameter> parameters = request.getSearchParameters();
        assertEquals(2, controller.getMappedParameters(parameters).size());
        assertEquals(new HashSet<String>(Arrays.asList("Condition", "Procedure")), controller.getMappedParameters(parameters).keySet());
        assertEquals(Arrays.asList("001"), controller.getMappedParameters(parameters).get("Condition"));
        assertEquals(Arrays.asList("002", "003"), controller.getMappedParameters(parameters).get("Procedure"));
    }
}
