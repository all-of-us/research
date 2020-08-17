package org.pmiops.workbench.cohortbuilder;

import java.util.List;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaListWithCountResponse;
import org.pmiops.workbench.model.CriteriaMenuOption;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;

public interface CohortBuilderService {

  Long countParticipants(SearchRequest request);

  List<AgeTypeCount> findAgeTypeCounts();

  List<CriteriaAttribute> findCriteriaAttributeByConceptId(Long conceptId);

  List<Criteria> findCriteriaAutoComplete(
      String domain, String term, String type, Boolean standard, Integer limit);

  List<Criteria> findCriteriaBy(String domain, String type, Boolean standard, Long parentId);

  CriteriaListWithCountResponse findCriteriaByDomainAndSearchTerm(
      String domain, String term, Integer limit);

  List<CriteriaMenuOption> findCriteriaMenuOptions();

  List<DataFilter> findDataFilters();

  List<DemoChartInfo> findDemoChartInfo(
      GenderOrSexType genderOrSexType, AgeType ageType, SearchRequest request);

  List<Criteria> findDrugBrandOrIngredientByValue(String value, Integer limit);

  List<Criteria> findDrugIngredientByConceptId(Long conceptId);

  ParticipantDemographics findParticipantDemographics();

  List<Criteria> findStandardCriteriaByDomainAndConceptId(String domain, Long conceptId);
}
