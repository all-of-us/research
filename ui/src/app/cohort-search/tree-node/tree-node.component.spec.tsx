import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {CohortBuilderApi, DomainType} from 'generated/fetch';
import defaultServerConfig from 'testing/default-server-config';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {NodeProp, TreeNode} from './tree-node.component';

const treeNodeStub = {
  children: [],
  code: '',
  conceptId: 903133,
  count: 0,
  domainId: 'Measurement',
  group: false,
  hasAttributes: true,
  id: 316305,
  name: 'Height Detail',
  parentId: 0,
  predefinedAttributes: null,
  selectable: true,
  subtype: 'HEIGHT',
  type: 'PM'
} as NodeProp;

const surveyCOPETreeNodeStub = {
  children: [],
  code: '',
  conceptId: 1333342,
  count: 0,
  domainId: DomainType.SURVEY.toString(),
  group: true,
  hasAttributes: false,
  id: 328232,
  name: 'COVID-19 Related Symptoms',
  parentId: 0,
  predefinedAttributes: null,
  selectable: true,
  subtype: 'HEIGHT',
  type: 'PM'
} as NodeProp;
describe('TreeNode', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
    serverConfigStore.next({
      ...defaultServerConfig,
      enableCohortBuilderV2: false
    });
  });
  it('should create', () => {
    const wrapper = mount(<TreeNode autocompleteSelection={undefined}
                                      groupSelections={[]}
                                      node={treeNodeStub}
                                      scrollToMatch={() => {}}
                                      searchTerms={''}
                                      select={() => {}}
                                      selectedIds={[]}
                                      setAttributes={() => {}}/>);
    expect(wrapper).toBeTruthy();
  });
  it('should display Versioned if SURVEY is COPE', () => {
    const wrapper = mount(<TreeNode autocompleteSelection={undefined}
                                    groupSelections={[]}
                                    node={surveyCOPETreeNodeStub}
                                    scrollToMatch={() => {}}
                                    searchTerms={''}
                                    select={() => {}}
                                    selectedIds={[]}
                                    setAttributes={() => {}}/>);
    expect(wrapper).toBeTruthy();
    expect(wrapper.find('[data-test-id="displayName"]').text()).toContain('COVID-19 Related Symptoms -  Versioned');
  });
});
