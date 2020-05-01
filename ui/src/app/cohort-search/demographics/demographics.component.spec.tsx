import {shallow} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {CohortBuilderApi, CriteriaType, DomainType} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {Demographics} from './demographics.component';

const wizardStub = {
  domain: DomainType.PERSON,
  type: CriteriaType.GENDER,
  item: {modifiers: [], searchParameters: []}
};
describe('Demographics', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.next({gsuiteDomain: 'fake-research-aou.org', enableCBAgeTypeOptions: false});
  });

  it('should create', () => {
    const wrapper = shallow(<Demographics select={() => {}} selectedIds={[]} selections={[]} wizard={wizardStub}/>)
    expect(wrapper).toBeTruthy();
  });
});
