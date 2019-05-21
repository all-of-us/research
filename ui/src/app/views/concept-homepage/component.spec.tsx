import {mount} from 'enzyme';
import * as React from 'react';

import {conceptsApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {ConceptHomepage} from 'app/views/concept-homepage/component';
import {ConceptsApi, ConceptSetsApi, DomainInfo, StandardConceptFilter, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {ConceptsApiStub, ConceptStubVariables, DomainStubVariables} from 'testing/stubs/concepts-api-stub';
import {workspaceDataStub, WorkspacesApiStub, WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';


function isSelectedDomain(
  domain: DomainInfo, wrapper): boolean {
  return wrapper.find('[data-test-id="active-domain"]').key() === domain.domain;
}

function conceptsCountInDomain(domain: DomainInfo, isStandardConcepts: boolean): number {
  const conceptsInDomain = ConceptStubVariables.STUB_CONCEPTS
    .filter(c => c.domainId === domain.name);
  if (isStandardConcepts) {
    return conceptsInDomain.filter(c => c.standardConcept === isStandardConcepts).length;
  } else {
    return conceptsInDomain.length;
  }
}

function searchTable(searchTerm: string, wrapper) {
  const searchInput = wrapper.find('[data-test-id="concept-search-input"]')
    .find('input').getDOMNode() as HTMLInputElement;
  searchInput.value = searchTerm;
  wrapper.find('[data-test-id="concept-search-input"]')
    .find('input').simulate('keydown', {keyCode: 13});
}

describe('ConceptHomepage', () => {

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    const wrapper = mount(<ConceptHomepage />);
    expect(wrapper).toBeTruthy();
  });

  it('should have one card per domain.', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="domain-box-name"]').length)
      .toBe(DomainStubVariables.STUB_DOMAINS.length);
  });

  it('should default to standard concepts only, and performs a full search', async() => {
    const searchTerm = 'test';
    const spy = jest.spyOn(conceptsApi(), 'searchConcepts');
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable(searchTerm, wrapper);
    await waitOneTickAndUpdate(wrapper);

    DomainStubVariables.STUB_DOMAINS.forEach((domain) => {
      const includeDomainCounts = isSelectedDomain(domain, wrapper);
      const expectedRequest = {
        query: searchTerm,
        // Tests that it searches only standard concepts.
        standardConceptFilter: StandardConceptFilter.STANDARDCONCEPTS,
        domain: domain.domain,
        includeDomainCounts: includeDomainCounts,
        includeVocabularyCounts: true,
        maxResults: 100
      };
      expect(spy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        expectedRequest
      );
    });

    // Test that it makes a call for each domain
    expect(spy).toHaveBeenCalledTimes(DomainStubVariables.STUB_DOMAINS.length);

    // Test that it switches to the table view
    expect(wrapper.find('[data-test-id="conceptTable"]').length).toBeGreaterThan(0);
    const firstDomainRowName = wrapper.find('[data-test-id="conceptName"]').at(1).text();
    await waitOneTickAndUpdate(wrapper);

    // Test that it changes the table when a new domain is selected
    const unselectedDomainName = DomainStubVariables.STUB_DOMAINS[1].name;
    wrapper.find('[data-test-id="domain-header-' + unselectedDomainName + '"]')
      .first().simulate('click');
    expect( wrapper.find('[data-test-id="conceptName"]').at(1).text())
      .not.toBe(firstDomainRowName);

  });

  it('should changes search criteria when standard only not checked', async() => {
    const spy = jest.spyOn(conceptsApi(), 'searchConcepts');
    const searchTerm = 'test';
    const selectedDomain = DomainStubVariables.STUB_DOMAINS[1];
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="standardConceptsCheckBox"]').first()
      .simulate('change', { target: { checked: true } });
    await waitOneTickAndUpdate(wrapper);
    searchTable(searchTerm, wrapper);
    await waitOneTickAndUpdate(wrapper);

    DomainStubVariables.STUB_DOMAINS.forEach((domain) => {
      const includeDomainCounts = isSelectedDomain(domain, wrapper);
      const expectedRequest = {
        query: searchTerm,
        // Tests that it searches only standard concepts.
        standardConceptFilter: StandardConceptFilter.ALLCONCEPTS,
        domain: domain.domain,
        includeDomainCounts: includeDomainCounts,
        includeVocabularyCounts: true,
        maxResults: 100
      };
      expect(spy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        expectedRequest
      );
    });
    // check number of rows in table plus header row
    expect(wrapper.find('[data-test-id="conceptName"]').length)
      .toBe(conceptsCountInDomain(selectedDomain, false) + 1);
  });

  it('should display the selected concepts on header', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable('test', wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('span.p-checkbox-icon.p-clickable').at(1).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="selectedConcepts"]').text()).toBe('1');
  });

  it('should display the selected concepts on sliding button', async () => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable('test', wrapper);
    await waitOneTickAndUpdate(wrapper);

    // before anything is selected, the sliding button should be disabled
    expect(wrapper.find('[data-test-id="sliding-button"]')
      .parent().props()['disable']).toBeTruthy();

    wrapper.find('span.p-checkbox-icon.p-clickable').at(1).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="sliding-button"]')
      .parent().props()['disable']).toBeFalsy();
    expect(wrapper.find('[data-test-id="sliding-button"]').text()).toBe('Add (1) to set');
  });

  it('should clear search and selected concepts', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable('test', wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('span.p-checkbox-icon.p-clickable').at(1).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="selectedConcepts"]').text()).toBe('1');

    wrapper.find('[data-test-id="clear-search"]').first().simulate('click');
    expect(wrapper.find('[data-test-id="selectedConcepts"]').length).toEqual(0);
  });

  it('should clear selected concepts after adding', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable('test', wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('span.p-checkbox-icon.p-clickable').at(1).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="selectedConcepts"]').text()).toBe('1');

    wrapper.find('[data-test-id="sliding-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="toggle-new-set"]').first().simulate('click');
    wrapper.find('[data-test-id="create-new-set-name"]').first()
      .simulate('change', {target: {value: 'test-set'}});
    wrapper.find('[data-test-id="save-concept-set"]').first().simulate('click');

    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="selectedConcepts"]').length).toEqual(0);
  });

});
