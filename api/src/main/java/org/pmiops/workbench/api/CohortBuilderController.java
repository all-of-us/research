package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Provider;

import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.dao.CriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cdr.model.CriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.CriteriaAttributeListResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {
  private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());
  private static final Long DEFAULT_LIMIT = 100L;

  private BigQueryService bigQueryService;
  private ParticipantCounter participantCounter;
  private CriteriaDao criteriaDao;
  private CriteriaAttributeDao criteriaAttributeDao;
  private CdrVersionDao cdrVersionDao;
  private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;
  private CdrVersionService cdrVersionService;
  private ElasticSearchService elasticSearchService;
  private Provider<WorkbenchConfig> configProvider;

  /**
   * Converter function from backend representation (used with Hibernate) to
   * client representation (generated by Swagger).
   */
  private static final Function<Criteria, org.pmiops.workbench.model.Criteria>
    TO_CLIENT_CRITERIA =
    new Function<Criteria, org.pmiops.workbench.model.Criteria>() {
      @Override
      public org.pmiops.workbench.model.Criteria apply(Criteria criteria) {
        return new org.pmiops.workbench.model.Criteria()
          .id(criteria.getId())
          .parentId(criteria.getParentId())
          .type(TreeType.fromValue(criteria.getType()))
          .subtype(TreeSubType.fromValue(criteria.getSubtype()))
          .code(criteria.getCode())
          .name(criteria.getName())
          .count(StringUtils.isEmpty(criteria.getCount()) ? null : new Long(criteria.getCount()))
          .group(criteria.getGroup())
          .selectable(criteria.getSelectable())
          .conceptId(StringUtils.isEmpty(criteria.getConceptId()) ? null : new Long(criteria.getConceptId()))
          .domainId(criteria.getDomainId())
          .hasAttributes(criteria.getAttribute())
          .path(criteria.getPath());
      }
    };

  /**
   * Converter function from backend representation (used with Hibernate) to
   * client representation (generated by Swagger).
   */
  private static final Function<CriteriaAttribute, org.pmiops.workbench.model.CriteriaAttribute>
    TO_CLIENT_CRITERIA_ATTRIBUTE =
    new Function<CriteriaAttribute, org.pmiops.workbench.model.CriteriaAttribute>() {
      @Override
      public org.pmiops.workbench.model.CriteriaAttribute apply(CriteriaAttribute criteria) {
        return new org.pmiops.workbench.model.CriteriaAttribute()
          .id(criteria.getId())
          .valueAsConceptId(criteria.getValueAsConceptId())
          .conceptName(criteria.getConceptName())
          .type(criteria.getType())
          .estCount(criteria.getEstCount());
      }
    };

  @Autowired
  CohortBuilderController(BigQueryService bigQueryService,
                          ParticipantCounter participantCounter,
                          CriteriaDao criteriaDao,
                          CriteriaAttributeDao criteriaAttributeDao,
                          CdrVersionDao cdrVersionDao,
                          Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider,
                          CdrVersionService cdrVersionService,
                          ElasticSearchService elasticSearchService,
                          Provider<WorkbenchConfig> configProvider) {
    this.bigQueryService = bigQueryService;
    this.participantCounter = participantCounter;
    this.criteriaDao = criteriaDao;
    this.criteriaAttributeDao = criteriaAttributeDao;
    this.cdrVersionDao = cdrVersionDao;
    this.genderRaceEthnicityConceptProvider = genderRaceEthnicityConceptProvider;
    this.cdrVersionService = cdrVersionService;
    this.elasticSearchService = elasticSearchService;
    this.configProvider = configProvider;
  }

  @Override
  public ResponseEntity<CriteriaListResponse> getCriteriaAutoComplete(Long cdrVersionId,
                                                                      String type,
                                                                      String value,
                                                                      String subtype,
                                                                      Long limit) {
    cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId));
    Long resultLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
    String matchExp = modifyKeywordMatch(value, type);
    List<Criteria> criteriaList;
    if (subtype == null) {
      criteriaList = criteriaDao.findCriteriaByTypeForCodeOrName(type, matchExp, value, new PageRequest(0, resultLimit.intValue()));
    } else {
      criteriaList = type.equals(TreeType.SNOMED.name()) ?
        criteriaDao.findCriteriaByTypeAndSubtypeForName(type, subtype, matchExp, new PageRequest(0, resultLimit.intValue())) :
        criteriaDao.findCriteriaByTypeAndSubtypeForCodeOrName(type, subtype, matchExp, value, new PageRequest(0, resultLimit.intValue()));
    }
    CriteriaListResponse criteriaResponse = new CriteriaListResponse();
    criteriaResponse.setItems(criteriaList.stream().map(TO_CLIENT_CRITERIA).collect(Collectors.toList()));

    return ResponseEntity.ok(criteriaResponse);
  }

  @Override
  public ResponseEntity<CriteriaListResponse> getDrugBrandOrIngredientByValue(Long cdrVersionId,
                                                                             String value,
                                                                             Long limit) {
    cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId));
    Long resultLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
    final List<Criteria> criteriaList = criteriaDao.findDrugBrandOrIngredientByValue(value, resultLimit);

    CriteriaListResponse criteriaResponse = new CriteriaListResponse();
    criteriaResponse.setItems(criteriaList.stream().map(TO_CLIENT_CRITERIA).collect(Collectors.toList()));

    return ResponseEntity.ok(criteriaResponse);
  }

  @Override
  public ResponseEntity<CriteriaListResponse> getDrugIngredientByConceptId(Long cdrVersionId, Long conceptId) {
    cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId));
    final List<Criteria> criteriaList = criteriaDao.findDrugIngredientByConceptId(conceptId);

    CriteriaListResponse criteriaResponse = new CriteriaListResponse();
    criteriaResponse.setItems(criteriaList.stream().map(TO_CLIENT_CRITERIA).collect(Collectors.toList()));

    return ResponseEntity.ok(criteriaResponse);
  }

  /**
   * This method will return a count of unique subjects defined by the provided {@link SearchRequest}.
   */
  @Override
  public ResponseEntity<Long> countParticipants(Long cdrVersionId, SearchRequest request) {
    CdrVersion cdrVersion = cdrVersionDao.findOne(cdrVersionId)
    cdrVersionService.setCdrVersion(cdrVersion);
    if (configProvider.get().elasticsearch.enableElasticsearchBackend &&
        !Strings.isNullOrEmpty(cdrVersion.getElasticBaseName()) && !isApproximate(request)) {
      try {
        return ResponseEntity.ok(elasticSearchService.count(request));
      } catch (IOException e) {
        log.log(Level.SEVERE, "Elastic request failed, falling back to BigQuery", e);
      }
    }
    QueryJobConfiguration qjc = bigQueryService.filterBigQueryConfig(
      participantCounter.buildParticipantCounterQuery(new ParticipantCriteria(request))
    );
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<FieldValue> row = result.iterateAll().iterator().next();
    Long count = bigQueryService.getLong(row, rm.get("count"));
    return ResponseEntity.ok(count);
  }

  @Override
  public ResponseEntity<DemoChartInfoListResponse> getDemoChartInfo(Long cdrVersionId, SearchRequest request) {
    DemoChartInfoListResponse response = new DemoChartInfoListResponse();
    CdrVersion cdrVersion = cdrVersionDao.findOne(cdrVersionId)
    cdrVersionService.setCdrVersion(cdrVersion);
    if (configProvider.get().elasticsearch.enableElasticsearchBackend &&
        !Strings.isNullOrEmpty(cdrVersion.getElasticBaseName()) && !isApproximate(request)) {
      try {
        return ResponseEntity.ok(response.items(elasticSearchService.demoChartInfo(request)));
      } catch (IOException e) {
        log.log(Level.SEVERE, "Elastic request failed, falling back to BigQuery", e);
      }
    }
    QueryJobConfiguration qjc = bigQueryService.filterBigQueryConfig(participantCounter.buildDemoChartInfoCounterQuery(
      new ParticipantCriteria(request)));
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    for (List<FieldValue> row : result.iterateAll()) {
      response.addItemsItem(new DemoChartInfo()
        .gender(bigQueryService.getString(row, rm.get("gender")))
        .race(bigQueryService.getString(row, rm.get("race")))
        .ageRange(bigQueryService.getString(row, rm.get("ageRange")))
        .count(bigQueryService.getLong(row, rm.get("count"))));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CriteriaAttributeListResponse> getCriteriaAttributeByConceptId(Long cdrVersionId, Long conceptId) {
    cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId));
    final List<CriteriaAttribute> criteriaAttributeList = criteriaAttributeDao.findCriteriaAttributeByConceptId(conceptId);

    CriteriaAttributeListResponse criteriaAttributeResponse = new CriteriaAttributeListResponse();
    criteriaAttributeResponse.setItems(criteriaAttributeList.stream().map(TO_CLIENT_CRITERIA_ATTRIBUTE).collect(Collectors.toList()));
    return ResponseEntity.ok(criteriaAttributeResponse);
  }

  @Override
  public ResponseEntity<CriteriaListResponse> getCriteriaBy(Long cdrVersionId,
                                                            String type,
                                                            String subtype,
                                                            Long parentId,
                                                            Boolean allChildren) {
    Optional.ofNullable(type)
      .orElseThrow(() -> new BadRequestException(String.format("Bad Request: Please provide a valid criteria type. %s is not valid.", type )));
    Arrays
      .stream(TreeType.values())
      .filter(treeType -> treeType.name().equalsIgnoreCase(type))
      .findFirst()
      .orElseThrow(() -> new BadRequestException(String.format("Bad Request: Please provide a valid criteria type. %s is not valid.", type )));
    Optional.ofNullable(subtype)
      .ifPresent(st -> Arrays
      .stream(TreeSubType.values())
      .filter(treeSubType -> treeSubType.name().equalsIgnoreCase(st))
      .findFirst()
      .orElseThrow(() -> new BadRequestException(String.format("Bad Request: Please provide a valid criteria subtype. %s is not valid.", st ))));

    cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId));
    List<Criteria> criteriaList;
    if (parentId != null) {
      if (subtype != null) {
        criteriaList = criteriaDao.findCriteriaByTypeAndSubtypeAndParentIdOrderByIdAsc(type, subtype, parentId);
      } else if (allChildren != null) {
        criteriaList = criteriaDao.findCriteriaChildrenByTypeAndParentId(type, parentId);
      } else {
        criteriaList = criteriaDao.findCriteriaByTypeAndParentIdOrderByIdAsc(type, parentId);
      }
    } else if (subtype != null) {
      criteriaList = criteriaDao.findCriteriaByTypeAndSubtypeOrderByIdAsc(type, subtype);
    } else {
      criteriaList = criteriaDao.findCriteriaByType(type);
    }

    CriteriaListResponse criteriaResponse = new CriteriaListResponse();
    criteriaResponse.setItems(criteriaList.stream().map(TO_CLIENT_CRITERIA).collect(Collectors.toList()));

    return ResponseEntity.ok(criteriaResponse);
  }

  @Override
  public ResponseEntity<org.pmiops.workbench.model.Criteria> getPPICriteriaParent(Long cdrVersionId, String type, String conceptId) {
    Optional.ofNullable(type)
      .orElseThrow(() -> new BadRequestException(String.format("Bad Request: Please provide a valid criteria type. %s is not valid.", type )));
    Arrays
      .stream(TreeType.values())
      .filter(treeType -> treeType.name().equalsIgnoreCase(type))
      .findFirst()
      .orElseThrow(() -> new BadRequestException(String.format("Bad Request: Please provide a valid criteria type. %s is not valid.", type )));
    Optional.ofNullable(conceptId)
      .orElseThrow(() -> new BadRequestException(String.format("Bad Request: Please provide a valid conceptId. %s is not valid.", conceptId )));
    cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId));
    Criteria criteria = criteriaDao.findCriteriaByTypeAndConceptIdAndSelectable(type, conceptId, false);
    return ResponseEntity.ok(TO_CLIENT_CRITERIA.apply(criteria));
  }

  @Override
  public ResponseEntity<ParticipantDemographics> getParticipantDemographics(Long cdrVersionId) {
    cdrVersionService.setCdrVersion(cdrVersionDao.findOne(cdrVersionId));

    Map<String, Map<Long, String>> concepts = genderRaceEthnicityConceptProvider.get().getConcepts();
    List<ConceptIdName> genderList = concepts.get(ParticipantCohortStatusColumns.GENDER.name()).entrySet().stream()
      .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
      .collect(Collectors.toList());
    List<ConceptIdName> raceList = concepts.get(ParticipantCohortStatusColumns.RACE.name()).entrySet().stream()
      .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
      .collect(Collectors.toList());
    List<ConceptIdName> ethnicityList = concepts.get(ParticipantCohortStatusColumns.ETHNICITY.name()).entrySet().stream()
      .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
      .collect(Collectors.toList());

    ParticipantDemographics participantDemographics =
      new ParticipantDemographics().genderList(genderList).raceList(raceList).ethnicityList(ethnicityList);
    return ResponseEntity.ok(participantDemographics);
  }

  /**
   * This method helps determine what request can only be approximated by elasticsearch
   * and must fallback to the BQ implementation.
   *
   * @param request
   * @return
   */
  protected boolean isApproximate(SearchRequest request) {
    //currently elasticsearch doesn't implement Temporal/BP/DEC
    return request.getIncludes().stream().anyMatch(sg -> sg.getTemporal())
      || request.getExcludes().stream().anyMatch(sg -> sg.getTemporal())
      || request.getIncludes().stream()
      .flatMap(sg -> sg.getItems().stream())
      .flatMap(sgi -> sgi.getSearchParameters().stream())
      .anyMatch(sp -> TreeSubType.BP.toString().equals(sp.getSubtype()) || TreeSubType.DEC.toString().equals(sp.getSubtype()))
      || request.getExcludes().stream()
      .flatMap(sg -> sg.getItems().stream())
      .flatMap(sgi -> sgi.getSearchParameters().stream())
      .anyMatch(sp -> TreeSubType.BP.toString().equals(sp.getSubtype()) || TreeSubType.DEC.toString().equals(sp.getSubtype()));
  }

  private String modifyKeywordMatch(String value, String type) {
    if (value == null || value.trim().isEmpty()) {
      throw new BadRequestException(
        String.format("Bad Request: Please provide a valid search term: \"%s\" is not valid.", value));
    }
    String[] keywords = value.split("\\W+");
    if (keywords.length == 1 && keywords[0].length() <= 3) {
      return "+\"" + keywords[0] + "\"+\"[rank1]\"";
    }
    String rank1 = TreeType.PPI.name().equals(type) ? "" : "+\"[rank1]\"";

    return IntStream
      .range(0, keywords.length)
      .filter(i -> keywords[i].length() > 2)
      .mapToObj(i -> {
        if ((i + 1) != keywords.length) {
          return "+\"" + keywords[i] + "\"";
        }
        return "+" + keywords[i] + "*";
      })
      .collect(Collectors.joining())
      + rank1;
  }

}
