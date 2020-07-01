package org.pmiops.workbench.conceptset;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.ConceptSetsController;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.ConceptSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptSetService {

  private static final int CONCEPT_SET_VERSION = 1;
  private ConceptSetDao conceptSetDao;
  private ConceptBigQueryService conceptBigQueryService;
  private ConceptService conceptService;
  private ConceptSetMapper conceptSetMapper;

  @Autowired
  public ConceptSetService(
      ConceptSetDao conceptSetDao,
      ConceptBigQueryService conceptBigQueryService,
      ConceptService conceptService,
      ConceptSetMapper conceptSetMapper) {
    this.conceptSetDao = conceptSetDao;
    this.conceptBigQueryService = conceptBigQueryService;
    this.conceptService = conceptService;
    this.conceptSetMapper = conceptSetMapper;
  }

  public DbConceptSet save(DbConceptSet dbConceptSet) {
    return conceptSetDao.save(dbConceptSet);
  }

  public void delete(Long conceptSetId) {
    conceptSetDao.delete(conceptSetId);
  }

  public Optional<DbConceptSet> findOne(Long conceptSetId) {
    return Optional.of(conceptSetDao.findOne(conceptSetId));
  }

  public ConceptSet toClientConceptSet(DbConceptSet dbConceptSet) {
    ConceptSet result = conceptSetMapper.dbModelToClient(dbConceptSet);
    return result.concepts(
        conceptService.findAll(
            dbConceptSet.getConceptIds(), ConceptSetsController.CONCEPT_NAME_ORDERING));
  }

  public List<ConceptSet> findAll(List<Long> conceptSetIds) {
    return ((List<DbConceptSet>) conceptSetDao.findAll(conceptSetIds))
        .stream()
            .map(conceptSet -> conceptSetMapper.dbModelToClient(conceptSet))
            .collect(Collectors.toList());
  }

  public DbConceptSet findOne(Long conceptSetId, DbWorkspace workspace) {
    return conceptSetDao
        .findByConceptSetIdAndWorkspaceId(conceptSetId, workspace.getWorkspaceId())
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format(
                        "No concept set with ID %s in workspace %s.",
                        conceptSetId, workspace.getFirecloudName())));
  }

  public List<DbConceptSet> findByWorkspaceId(long workspaceId) {
    return conceptSetDao.findByWorkspaceId(workspaceId);
  }

  public List<DbConceptSet> findByWorkspaceIdAndSurvey(long workspaceId, short surveyId) {
    return conceptSetDao.findByWorkspaceIdAndSurvey(workspaceId, surveyId);
  }

  @Transactional
  public DbConceptSet cloneConceptSetAndConceptIds(
      DbConceptSet conceptSet, DbWorkspace targetWorkspace, boolean cdrVersionChanged) {
    DbConceptSet dbConceptSet = new DbConceptSet(conceptSet);
    if (cdrVersionChanged) {
      String omopTable = BigQueryTableInfo.getTableName(conceptSet.getDomainEnum());
      dbConceptSet.setParticipantCount(
          conceptBigQueryService.getParticipantCountForConcepts(
              conceptSet.getDomainEnum(), omopTable, conceptSet.getConceptIds()));
    }
    dbConceptSet.setWorkspaceId(targetWorkspace.getWorkspaceId());
    dbConceptSet.setCreator(targetWorkspace.getCreator());
    dbConceptSet.setLastModifiedTime(targetWorkspace.getLastModifiedTime());
    dbConceptSet.setCreationTime(targetWorkspace.getCreationTime());
    dbConceptSet.setVersion(CONCEPT_SET_VERSION);
    return conceptSetDao.save(dbConceptSet);
  }

  public List<DbConceptSet> getConceptSets(DbWorkspace workspace) {
    // Allows for fetching concept sets for a workspace once its collection is no longer
    // bound to a session.
    return conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
  }
}
