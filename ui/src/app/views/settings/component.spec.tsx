import { mount } from 'enzyme';
import * as React from 'react';

import {SettingsReact, SettingsState} from './component';

import {clusterApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';

import {Cluster, ClusterApi, ClusterStatus} from 'generated/fetch/api';

describe('SettingsComponent', () => {
  const component = () => {
    return mount<SettingsReact, {}, SettingsState>(<SettingsReact/>);
  };

  beforeEach(() => {
    registerApiClient(ClusterApi, new ClusterApiStub());
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
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(1);
    wrapper.find('[data-test-id="reset-cluster-send"]').at(0).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(spy).toHaveBeenCalled();
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
  });
});
