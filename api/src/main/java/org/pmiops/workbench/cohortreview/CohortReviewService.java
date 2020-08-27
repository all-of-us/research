package org.pmiops.workbench.cohortreview;

import java.util.List;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;

public interface CohortReviewService {

  /** Find the {@link DbCohort} for the specified cohortId. */
  DbCohort findCohort(long cohortId);

  /** Find the {@link DbCohortReview} for the specified cohortId and cdrVersionId. */
  DbCohortReview findCohortReview(Long cohortId, Long cdrVersionId);

  /** Find the {@link DbCohortReview} for the specified cohortReviewId. */
  DbCohortReview findCohortReview(Long cohortReviewId);

  /** Find the {@link DbCohortReview} for the specified ns, firecloudName and cohortReviewId */
  DbCohortReview findCohortReview(String ns, String firecloudName, Long cohortReviewId);

  /** Delete the specified cohort review. */
  void deleteCohortReview(DbCohortReview cohortReview);

  /** Find the {@link DbCohortReview} for the specified ns and firecloudName. */
  List<DbCohortReview> getRequiredWithCohortReviews(String ns, String firecloudName);

  /** Save the specified {@link DbCohortReview}. */
  DbCohortReview saveCohortReview(DbCohortReview cohortReview);

  /**
   * Save the {@link DbCohortReview} as well as the collection of {@link DbParticipantCohortStatus}.
   */
  void saveFullCohortReview(
      DbCohortReview cohortReview, List<DbParticipantCohortStatus> participantCohortStatuses);

  /** Save the specified {@link DbParticipantCohortStatus}. */
  DbParticipantCohortStatus saveParticipantCohortStatus(
      DbParticipantCohortStatus participantCohortStatus);

  /**
   * Find the {@link DbParticipantCohortStatus} for the specified cohortReviewId and participantId.
   */
  DbParticipantCohortStatus findParticipantCohortStatus(Long cohortReviewId, Long participantId);

  /**
   * Find a list of {@link DbParticipantCohortStatus} for the specified cohortReviewId, filtering
   * and paging.
   */
  List<DbParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest);

  /** Find count for the specified cohortReviewId, filtering and paging. */
  Long findCount(Long cohortReviewId, PageRequest pageRequest);

  /** Save the specified {@link DbParticipantCohortAnnotation}. */
  ParticipantCohortAnnotation saveParticipantCohortAnnotation(
      Long cohortReviewId, ParticipantCohortAnnotation participantCohortAnnotation);

  /**
   * Save the {@link DbParticipantCohortAnnotation} for the specified participantId, annotationId
   * and {@link ModifyParticipantCohortAnnotationRequest}.
   */
  ParticipantCohortAnnotation updateParticipantCohortAnnotation(
      Long annotationId,
      Long cohortReviewId,
      Long participantId,
      ModifyParticipantCohortAnnotationRequest modifyParticipantCohortAnnotationRequest);

  /**
   * Find the {@link DbCohortAnnotationDefinition} for the specified cohortAnnotationDefinitionId.
   */
  DbCohortAnnotationDefinition findCohortAnnotationDefinition(Long cohortAnnotationDefinitionId);

  /**
   * Delete the {@link DbParticipantCohortAnnotation} for the specified annotationId, cohortReviewId
   * and participantId.
   */
  void deleteParticipantCohortAnnotation(
      Long annotationId, Long cohortReviewId, Long participantId);

  /**
   * Find a list of {@link DbParticipantCohortAnnotation} for the specified cohortReviewId and
   * participantId.
   */
  List<ParticipantCohortAnnotation> findParticipantCohortAnnotations(
      Long cohortReviewId, Long participantId);
}
