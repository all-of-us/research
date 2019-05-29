import {Component, OnDestroy, OnInit} from '@angular/core';
import {
  cohortReviewStore,
  filterStateStore,
  initialFilterState,
  multiOptions,
  visitsFilterOptions,
  vocabOptions
} from 'app/cohort-review/review-state.service';
import {cohortReviewApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {CohortBuilderService, TreeType} from 'generated';
import {PageFilterType, ReviewStatus, SortOrder, WorkspaceAccessLevel} from 'generated/fetch';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit, OnDestroy {
  reviewPresent: boolean;
  cohortLoaded = false;
  readonly = false;
  constructor(private builderApi: CohortBuilderService) {}

  ngOnInit() {
    const {ns, wsid, cid} = urlParamsStore.getValue();
    const {accessLevel, cdrVersionId} = currentWorkspaceStore.getValue();
    this.readonly = accessLevel === WorkspaceAccessLevel.READER;
    cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, {
      page: 0,
      pageSize: 25,
      sortOrder: SortOrder.Asc,
      pageFilterType: PageFilterType.ParticipantCohortStatuses,
    }).then(review => {
      cohortReviewStore.next(review);
      this.reviewPresent = review.reviewStatus !== ReviewStatus.NONE;
      if (this.reviewPresent) {
        navigate(['workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
      }
    });
    cohortsApi().getCohort(ns, wsid, cid).then(cohort => {
      // This effectively makes the 'current cohort' available to child components, by using
      // the `withCurrentCohort` HOC. In addition, this store is used to render the breadcrumb.
      currentCohortStore.next(cohort);
      this.cohortLoaded = true;
    });
    if (!visitsFilterOptions.getValue()) {
      this.builderApi.getCriteriaBy(+cdrVersionId, TreeType[TreeType.VISIT], null, true, 0)
        .toPromise()
        .then(response => {
          visitsFilterOptions.next([
            {value: null, label: 'Any'},
            ...response.items.map(option => {
              return {value: option.name, label: option.name};
            })
          ]);
        });
    }
  }

  ngOnDestroy() {
    currentCohortStore.next(undefined);
    multiOptions.next(null);
    vocabOptions.next(null);
    filterStateStore.next(JSON.parse(JSON.stringify(initialFilterState)));
  }

  reviewCreated = () => {
    this.reviewPresent = true;
  }

  returnToCohorts() {
    const {ns, wsid} = urlParamsStore.getValue();
    navigate(['workspaces', ns, wsid, 'cohorts']);
  }

  get ableToReview() {
    return this.cohortLoaded && this.reviewPresent === false && !this.readonly;
  }

  get unableToReview() {
    return this.cohortLoaded && this.reviewPresent === false && this.readonly;
  }
}
