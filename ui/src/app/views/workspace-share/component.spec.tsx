import {mount} from 'enzyme';
import * as React from 'react';
import * as Lolex from 'lolex';
import * as fp from 'lodash/fp';
import Select from 'react-select';

import {WorkspaceShare, WorkspaceShareProps, WorkspaceShareState} from './component';

import {registerApiClient, workspacesApi} from 'app/services/swagger-fetch-clients';
import {
  User,
  UserApi,
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesApi
} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {UserApiStub} from 'testing/stubs/user-api-stub'
import {WorkspacesApiStub} from "../../../testing/stubs/workspaces-api-stub";

describe('WorkspaceShareComponent', () => {
  let props: WorkspaceShareProps;

  const component = () => {
    return mount<WorkspaceShare, WorkspaceShareProps, WorkspaceShareState>
    (<WorkspaceShare {...props}/>);
  };
  const harry: User = {givenName: 'Harry', familyName: 'Potter', email: 'harry.potter@hogwarts.edu'};
  const harryRole: UserRole = {...harry, email: harry.email, role: WorkspaceAccessLevel.OWNER};
  const hermione: User = {givenName: 'Hermione', familyName: 'Granger', email: 'hermione.granger@hogwarts.edu'};
  const hermioneRole: UserRole = {...hermione, email: hermione.email, role: WorkspaceAccessLevel.WRITER};
  const ron: User = {givenName: 'Ronald', familyName: 'Weasley', email: 'ron.weasley@hogwarts.edu'};
  const ronRole: UserRole = {...ron, email: ron.email, role: WorkspaceAccessLevel.READER};
  const luna: User = {givenName: 'Luna', familyName: 'Lovegood', email: 'luna.lovegood@hogwarts.edu'};
  const lunaRole: UserRole = {...luna, email: luna.email, role: WorkspaceAccessLevel.NOACCESS};

  const tomRiddleDiary = {namespace: 'Horcrux', name: 'The Diary of Tom Marvolo Riddle',
    userRoles: [harryRole, hermioneRole, ronRole], etag: '1', id: 'The Diary of Tom Marvolo Riddle'} as Workspace;

  const getSelectString = (user: UserRole) => {
    return '.' + (user.email).replace(/[@\.]/g, '')
      + '-user-role__single-value';
  };

  beforeEach(() => {
    registerApiClient(UserApi, new UserApiStub([harryRole, hermioneRole, ronRole, lunaRole]));
    registerApiClient(WorkspacesApi, new WorkspacesApiStub([tomRiddleDiary]));
    props = {
      closeFunction: () => {},
      workspace: tomRiddleDiary,
      accessLevel: WorkspaceAccessLevel.OWNER,
      userEmail: 'harry.potter@hogwarts.edu',
      sharing: true
    };
  });

  it('display correct users', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    expect(wrapper.find('[data-test-id="collab-user-name"]').length).toBe(3);
    expect(wrapper.find('[data-test-id="collab-user-email"]').length).toBe(3);
    const expectedNames = [harry, hermione, ron].map(u => u.givenName + ' ' + u.familyName);
    expect(wrapper.find('[data-test-id="collab-user-name"]').map(el => el.text())).toEqual(expectedNames);
    const expectedEmails = [harry, hermione, ron].map(u => u.email);
    expect(wrapper.find('[data-test-id="collab-user-email"]').map(el => el.text())).toEqual(expectedEmails);
  });

  it('displays correct role info', () => {
    const wrapper = component();
    expect(wrapper.find(getSelectString(harryRole)).text()).toEqual(fp.capitalize(WorkspaceAccessLevel[harryRole.role]));
    expect(wrapper.find(getSelectString(hermioneRole)).text()).toEqual(fp.capitalize(WorkspaceAccessLevel[hermioneRole.role]));
    expect(wrapper.find(getSelectString(ronRole)).text()).toEqual(fp.capitalize(WorkspaceAccessLevel[ronRole.role]));
    expect(wrapper.find(getSelectString(lunaRole)).length).toBe(0);
  });

  it('removes user correctly', () => {
    const wrapper = component();
    wrapper.find('[data-test-id="remove-collab-ron.weasley@hogwarts.edu"]').first().simulate('click');
    const expectedNames = [harry, hermione].map(u => u.givenName + ' ' + u.familyName);
    expect(wrapper.find('[data-test-id="collab-user-name"]').map(el => el.text())).toEqual(expectedNames);
  });

  it('adds user correctly', async() => {
    let clock = Lolex.install({shouldAdvanceTime: true});
    const wrapper = component();
    wrapper.find('[data-test-id="search"]').simulate('change', {target: {value: 'luna'}});
    clock.tick(401);
    await waitOneTickAndUpdate(wrapper);
    wrapper.update();
    clock.uninstall();
    wrapper.find('[data-test-id="add-collab-luna.lovegood@hogwarts.edu"]').first().simulate('click');
    const expectedNames = [harry, hermione, ron, luna].map(u => u.givenName + ' ' + u.familyName);
    expect(wrapper.find('[data-test-id="collab-user-name"]').map(el => el.text())).toEqual(expectedNames);
  });

  it('does not allow self-removal', () => {
    const wrapper = component();
    const dataString = '[data-test-id="remove-collab-harry.potter@hogwarts.edu"]';
    expect(wrapper.find(dataString).length).toBe(0);
  });

  it('does not allow self role change', () => {
    const wrapper = component();
    expect(wrapper.find('[data-test-id="harry.potter@hogwarts.edu-user-role"]').first().props()['isDisabled']).toBe(true);
  });

  it('saves acl correctly after changes made', async() => {
    let clock = Lolex.install({shouldAdvanceTime: true});
    const wrapper = component();
    wrapper.find('[data-test-id="search"]').simulate('change', {target: {value: 'luna'}});
    clock.tick(401);
    await waitOneTickAndUpdate(wrapper);
    wrapper.update();
    clock.uninstall();
    // add luna to acl
    wrapper.find('[data-test-id="add-collab-luna.lovegood@hogwarts.edu"]').first()
      .simulate('click');
    // remove ron from acl
    wrapper.find('[data-test-id="remove-collab-ron.weasley@hogwarts.edu"]').first()
      .simulate('click');
    // change hermione's access to owner
    let selectComponent = wrapper.find('[data-test-id="hermione.granger@hogwarts.edu-user-role"]')
      .find(Select);
    selectComponent.instance().setState({menuIsOpen: true});
    wrapper.update();
    wrapper.find('.hermionegrangerhogwartsedu-user-role__option')
      .findWhere(n => n.text() === fp.capitalize(WorkspaceAccessLevel[WorkspaceAccessLevel.OWNER]))
      .simulate('click');
    wrapper.update();
    const spy = jest.spyOn(workspacesApi(), 'shareWorkspace');
    wrapper.find('[data-test-id="save"]').first().simulate('click');
    expect(spy).toHaveBeenCalledWith(tomRiddleDiary.namespace, tomRiddleDiary.name,
      {workspaceEtag: tomRiddleDiary.etag,
        items: [harryRole, {...hermioneRole, role: WorkspaceAccessLevel.OWNER},
          {...lunaRole, role: WorkspaceAccessLevel.READER}]});
  });

});
