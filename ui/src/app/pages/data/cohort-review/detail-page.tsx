import * as fp from 'lodash/fp';
import * as React from 'react';
import {from} from 'rxjs/observable/from';

import {SpinnerOverlay} from 'app/components/spinners';
import {DetailHeader} from 'app/pages/data/cohort-review/detail-header.component';
import {DetailTabs} from 'app/pages/data/cohort-review/detail-tabs.component';
import {cohortReviewStore, getVocabOptions, participantStore, vocabOptions} from 'app/services/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ParticipantCohortStatus, SortOrder} from 'generated/fetch';
import {WithSpinnerOverlayProps} from "app/components/with-spinner-overlay";

interface Props extends WithSpinnerOverlayProps {
  workspace: WorkspaceData;
}

interface State {
  participant: ParticipantCohortStatus;
}

export const DetailPage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {participant: null};
    }

    async componentDidMount() {
      const {workspace: {cdrVersionId, id, namespace}, hideSpinner} = this.props;
      hideSpinner();
      const {ns, wsid, cid} = urlParamsStore.getValue();
      if (!cohortReviewStore.getValue()) {
        await cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, {
          page: 0,
          pageSize: 25,
          sortOrder: SortOrder.Asc,
          filters: {items: []}
        }).then(response => cohortReviewStore.next(response.cohortReview));
      }
      this.subscription = urlParamsStore.distinctUntilChanged(fp.isEqual)
        .filter(params => !!params.pid)
        .switchMap(({pid}) => {
          return from(cohortReviewApi()
            .getParticipantCohortStatus(ns, wsid, cohortReviewStore.getValue().cohortReviewId, +pid))
            .do(ps => participantStore.next(ps));
        })
        .subscribe();
      if (!vocabOptions.getValue()) {
        const {cohortReviewId} = cohortReviewStore.getValue();
        getVocabOptions(namespace, id, cohortReviewId);
      }
      this.subscription.add(participantStore.subscribe(participant => this.setState({participant})));
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    render() {
      const {participant} = this.state;
      return <div style={{
        minHeight: 'calc(100vh - calc(4rem + 60px))',
        padding: '1rem',
        position: 'relative',
        marginRight: '45px'
      }}>
        {!!participant
          ? <React.Fragment>
            <DetailHeader participant={participant} />
            <DetailTabs />
          </React.Fragment>
          : <SpinnerOverlay />
        }
      </div>;
    }
  }
);
