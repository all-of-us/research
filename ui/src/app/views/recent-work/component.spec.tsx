import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {RecentWork} from 'app/views/recent-work/component';
import {CohortsApi, ConceptSetsApi, UserMetricsApi, WorkspacesApi} from 'generated/fetch';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {UserMetricsApiStub} from 'testing/stubs/user-metrics-api-stub';
import {WorkspacesApiStub, workspaceDataStub} from 'testing/stubs/workspaces-api-stub';

describe('RecentWorkComponent', () => {
  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(UserMetricsApi, new UserMetricsApiStub());
  });

  it('should render outside of a workspace', () => {
    currentWorkspaceStore.next(undefined);
    const wrapper = mount(<RecentWork />);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should render in a workspace', () => {
    currentWorkspaceStore.next(workspaceDataStub);
    const wrapper = mount(<RecentWork />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
