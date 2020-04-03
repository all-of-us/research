package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbMenuOption;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortbuilder.mappers.CriteriaMapper;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCountListResponse;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.CriteriaAttributeListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.CriteriaMenuOption;
import org.pmiops.workbench.model.CriteriaMenuOptionsListResponse;
import org.pmiops.workbench.model.CriteriaMenuSubOption;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFiltersResponse;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.StandardFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

  private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());
  private static final String BAD_REQUEST_MESSAGE =
      "Bad Request: Please provide a valid %s. %s is not valid.";

  private BigQueryService bigQueryService;
  private CohortQueryBuilder cohortQueryBuilder;
  private CBCriteriaDao cbCriteriaDao;
  private CdrVersionService cdrVersionService;
  private ElasticSearchService elasticSearchService;
  private Provider<WorkbenchConfig> configProvider;
  private CohortBuilderService cohortBuilderService;
  private CriteriaMapper criteriaMapper;

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final BiFunction<String, List<CriteriaMenuSubOption>, CriteriaMenuOption>
      TO_CLIENT_MENU_OPTIONS =
          (domain, types) -> new CriteriaMenuOption().domain(domain).types(types);

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final BiFunction<String, Set<Boolean>, CriteriaMenuSubOption>
      TO_CLIENT_MENU_SUB_OPTIONS =
          (type, standards) ->
              new CriteriaMenuSubOption()
                  .type(type)
                  .standardFlags(
                      standards.stream()
                          .map(s -> new StandardFlag().standard(s))
                          .collect(Collectors.toList()));

  @Autowired
  CohortBuilderController(
      BigQueryService bigQueryService,
      CohortQueryBuilder cohortQueryBuilder,
      CBCriteriaDao cbCriteriaDao,
      CdrVersionService cdrVersionService,
      ElasticSearchService elasticSearchService,
      Provider<WorkbenchConfig> configProvider,
      CohortBuilderService cohortBuilderService,
      CriteriaMapper criteriaMapper) {
    this.bigQueryService = bigQueryService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.cbCriteriaDao = cbCriteriaDao;
    this.cdrVersionService = cdrVersionService;
    this.elasticSearchService = elasticSearchService;
    this.configProvider = configProvider;
    this.cohortBuilderService = cohortBuilderService;
    this.criteriaMapper = criteriaMapper;
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaAutoComplete(
      Long cdrVersionId, String domain, String term, String type, Boolean standard, Integer limit) {
    validateDomain(domain);
    validateType(type);
    validateTerm(term);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findCriteriaAutoComplete(
                    cdrVersionId, domain, term, type, standard, limit)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findDrugBrandOrIngredientByValue(
      Long cdrVersionId, String value, Integer limit) {
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findDrugBrandOrIngredientByValue(cdrVersionId, value, limit)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findDrugIngredientByConceptId(
      Long cdrVersionId, Long conceptId) {
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(cohortBuilderService.findDrugIngredientByConceptId(cdrVersionId, conceptId)));
  }

  @Override
  public ResponseEntity<AgeTypeCountListResponse> findAgeTypeCounts(Long cdrVersionId) {
    return ResponseEntity.ok(
        new AgeTypeCountListResponse().items(cohortBuilderService.findAgeTypeCounts(cdrVersionId)));
  }

  /**
   * This method will return a count of unique subjects defined by the provided {@link
   * SearchRequest}.
   */
  @Override
  public ResponseEntity<Long> countParticipants(Long cdrVersionId, SearchRequest request) {
    DbCdrVersion cdrVersion = cdrVersionService.findAndSetCdrVersion(cdrVersionId);
    if (configProvider.get().elasticsearch.enableElasticsearchBackend
        && !Strings.isNullOrEmpty(cdrVersion.getElasticIndexBaseName())
        && !isApproximate(request)) {
      try {
        return ResponseEntity.ok(elasticSearchService.count(request));
      } catch (IOException e) {
        log.log(Level.SEVERE, "Elastic request failed, falling back to BigQuery", e);
      }
    }
    QueryJobConfiguration qjc =
        bigQueryService.filterBigQueryConfig(
            cohortQueryBuilder.buildParticipantCounterQuery(new ParticipantCriteria(request)));
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<FieldValue> row = result.iterateAll().iterator().next();
    Long count = bigQueryService.getLong(row, rm.get("count"));
    return ResponseEntity.ok(count);
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findCriteriaByDomainAndSearchTerm(
      Long cdrVersionId, String domain, String term, Integer limit) {
    validateDomain(domain);
    validateTerm(term);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findCriteriaByDomainAndSearchTerm(
                    cdrVersionId, domain, term, limit)));
  }

  @Override
  public ResponseEntity<CriteriaMenuOptionsListResponse> findCriteriaMenuOptions(
      Long cdrVersionId) {
    ListMultimap<String, Boolean> typeToStandardOptionsMap = ArrayListMultimap.create();
    ListMultimap<String, String> domainToTypeOptionsMap = ArrayListMultimap.create();
    List<CriteriaMenuSubOption> returnMenuSubOptions = new ArrayList<>();
    List<CriteriaMenuOption> returnMenuOptions = new ArrayList<>();

    cdrVersionService.setCdrVersion(cdrVersionId);
    List<DbMenuOption> options = cbCriteriaDao.findMenuOptions();

    options.forEach(
        o -> {
          typeToStandardOptionsMap.put(o.getType(), o.getStandard());
          domainToTypeOptionsMap.put(o.getDomain(), o.getType());
        });
    for (String domainKey : domainToTypeOptionsMap.keySet()) {
      List<String> typeList =
          domainToTypeOptionsMap.get(domainKey).stream().distinct().collect(Collectors.toList());
      for (String typeKey : typeList) {
        returnMenuSubOptions.add(
            TO_CLIENT_MENU_SUB_OPTIONS.apply(
                typeKey, new HashSet<>(typeToStandardOptionsMap.get(typeKey))));
      }
      returnMenuOptions.add(
          TO_CLIENT_MENU_OPTIONS.apply(
              domainKey,
              returnMenuSubOptions.stream()
                  .sorted(Comparator.comparing(CriteriaMenuSubOption::getType))
                  .collect(Collectors.toList())));
      returnMenuSubOptions.clear();
    }
    CriteriaMenuOptionsListResponse response =
        new CriteriaMenuOptionsListResponse()
            .items(
                returnMenuOptions.stream()
                    .sorted(Comparator.comparing(CriteriaMenuOption::getDomain))
                    .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<DataFiltersResponse> findDataFilters(Long cdrVersionId) {
    return ResponseEntity.ok(
        new DataFiltersResponse().items(cohortBuilderService.findDataFilters(cdrVersionId)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> findStandardCriteriaByDomainAndConceptId(
      Long cdrVersionId, String domain, Long conceptId) {
    validateDomain(domain);
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                cohortBuilderService.findStandardCriteriaByDomainAndConceptId(
                    cdrVersionId, domain, conceptId)));
  }

  @Override
  public ResponseEntity<DemoChartInfoListResponse> getDemoChartInfo(
      Long cdrVersionId, String genderOrSex, String age, SearchRequest request) {
    GenderOrSexType genderOrSexType =
        Optional.ofNullable(genderOrSex)
            .map(GenderOrSexType::fromValue)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Bad Request: Please provide a valid gender or sex at birth parameter"));
    AgeType ageType =
        Optional.ofNullable(age)
            .map(AgeType::fromValue)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Bad Request: Please provide a valid age type parameter"));
    DemoChartInfoListResponse response = new DemoChartInfoListResponse();
    if (request.getIncludes().isEmpty()) {
      return ResponseEntity.ok(response);
    }
    DbCdrVersion cdrVersion = cdrVersionService.findAndSetCdrVersion(cdrVersionId);
    if (configProvider.get().elasticsearch.enableElasticsearchBackend
        && !Strings.isNullOrEmpty(cdrVersion.getElasticIndexBaseName())
        && !isApproximate(request)) {
      try {
        return ResponseEntity.ok(
            response.items(
                elasticSearchService.demoChartInfo(
                    new ParticipantCriteria(request, genderOrSexType, ageType))));
      } catch (IOException e) {
        log.log(Level.SEVERE, "Elastic request failed, falling back to BigQuery", e);
      }
    }
    QueryJobConfiguration qjc =
        bigQueryService.filterBigQueryConfig(
            cohortQueryBuilder.buildDemoChartInfoCounterQuery(
                new ParticipantCriteria(request, genderOrSexType, ageType)));
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(
          new DemoChartInfo()
              .name(bigQueryService.getString(row, rm.get("name")))
              .race(bigQueryService.getString(row, rm.get("race")))
              .ageRange(bigQueryService.getString(row, rm.get("ageRange")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CriteriaAttributeListResponse> findCriteriaAttributeByConceptId(
      Long cdrVersionId, Long conceptId) {
    return ResponseEntity.ok(
        new CriteriaAttributeListResponse()
            .items(cohortBuilderService.findCriteriaAttributeByConceptId(cdrVersionId, conceptId)));
  }

  @Override
  public ResponseEntity<CriteriaListResponse> getCriteriaBy(
      Long cdrVersionId, String domain, String type, Boolean standard, Long parentId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    validateDomain(domain);
    validateType(type);
    List<DbCriteria> criteriaList;
    if (parentId != null) {
      criteriaList =
          cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
              domain, type, standard, parentId);
    } else {
      criteriaList = cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(domain, type);
    }
    return ResponseEntity.ok(
        new CriteriaListResponse()
            .items(
                criteriaList.stream()
                    .map(criteriaMapper::dbModelToClient)
                    .collect(Collectors.toList())));
  }

  @Override
  public ResponseEntity<ParticipantDemographics> getParticipantDemographics(Long cdrVersionId) {
    cdrVersionService.setCdrVersion(cdrVersionId);
    List<DbCriteria> criteriaList = cbCriteriaDao.findGenderRaceEthnicity();
    List<ConceptIdName> genderList = buildConceptIdNameList(criteriaList, FilterColumns.GENDER);
    List<ConceptIdName> raceList = buildConceptIdNameList(criteriaList, FilterColumns.RACE);
    List<ConceptIdName> ethnicityList =
        buildConceptIdNameList(criteriaList, FilterColumns.ETHNICITY);
    return ResponseEntity.ok(
        new ParticipantDemographics()
            .genderList(genderList)
            .raceList(raceList)
            .ethnicityList(ethnicityList));
  }

  @NotNull
  private List<ConceptIdName> buildConceptIdNameList(
      List<DbCriteria> criteriaList, FilterColumns columnName) {
    return criteriaList.stream()
        .filter(c -> c.getType().equals(columnName.toString()))
        .map(
            c -> new ConceptIdName().conceptId(new Long(c.getConceptId())).conceptName(c.getName()))
        .collect(Collectors.toList());
  }

  /**
   * This method helps determine what request can only be approximated by elasticsearch and must
   * fallback to the BQ implementation.
   *
   * @param request
   * @return
   */
  protected boolean isApproximate(SearchRequest request) {
    List<SearchGroup> allGroups =
        ImmutableList.copyOf(Iterables.concat(request.getIncludes(), request.getExcludes()));
    List<SearchParameter> allParams =
        allGroups.stream()
            .flatMap(sg -> sg.getItems().stream())
            .flatMap(sgi -> sgi.getSearchParameters().stream())
            .collect(Collectors.toList());
    return allGroups.stream().anyMatch(sg -> sg.getTemporal())
        || allParams.stream().anyMatch(sp -> CriteriaSubType.BP.toString().equals(sp.getSubtype()));
  }

  /** TODO: freemabd - remove this eventually * */
  private String modifyTermMatch(String term) {
    if (term == null || term.trim().isEmpty()) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Please provide a valid search term: \"%s\" is not valid.", term));
    }
    String[] keywords = term.split("\\W+");
    if (keywords.length == 1 && keywords[0].length() <= 3) {
      return "+\"" + keywords[0];
    }

    return IntStream.range(0, keywords.length)
        .filter(i -> keywords[i].length() > 2)
        .mapToObj(
            i -> {
              if ((i + 1) != keywords.length) {
                return "+\"" + keywords[i] + "\"";
              }
              return "+" + keywords[i] + "*";
            })
        .collect(Collectors.joining());
  }

  private void validateDomain(String domain) {
    Arrays.stream(DomainType.values())
        .filter(domainType -> domainType.toString().equalsIgnoreCase(domain))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "domain", domain)));
  }

  private void validateType(String type) {
    Arrays.stream(CriteriaType.values())
        .filter(critType -> critType.toString().equalsIgnoreCase(type))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "type", type)));
  }

  private void validateTerm(String term) {
    if (term == null || term.trim().isEmpty()) {
      throw new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "search term", term));
    }
  }
}
