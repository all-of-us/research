import { mount } from 'enzyme';
import * as React from 'react';

import {SettingsReact, SettingsState} from './component';

import {clusterApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {completeApiCall} from 'testing/react-test-helpers';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';

import {Cluster, ClusterApi, ClusterStatus} from 'generated/fetch/api';

describe('SettingsComponent', () => {
  const component = () => {
    return mount<SettingsReact, {}, SettingsState>(<SettingsReact/>);
  };

  beforeAll(() => {
    const popupRoot = document.createElement('div');
    popupRoot.setAttribute('id', 'popup-root');
    document.body.appendChild(popupRoot);
  });

  beforeEach(() => {
    registerApiClient(ClusterApi, new ClusterApiStub());
  });

  afterAll(() => {
    document.removeChild(document.getElementById('popup-root'));
  });

  it('should not open the cluster reset modal when no cluster', () => {
    const wrapper = component();
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
  });

  it('should allow deleting the cluster when there is one', async () => {
    const spy = jest.spyOn(clusterApi(), 'deleteCluster');
    const wrapper = component();
    await completeApiCall(wrapper);
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(1);
    wrapper.find('[data-test-id="reset-cluster-send"]').at(0).simulate('click');
    await completeApiCall(wrapper);
    expect(spy).toHaveBeenCalled();
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
  });
});
