import ClrIconLink from 'app/element/clr-icon-link';
import Link from 'app/element/link';
import BaseElement from 'app/element/base-element';
import HomePage, {LABEL_ALIAS as HOME_PAGE_LABEL_ALIAS} from 'app/page/home-page';
import WorkspaceCard from 'app/component/workspace-card';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import WorkspacesPage from 'app/page/workspaces-page';
import {signIn} from 'utils/app-utils';


describe('Home page ui tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Check visibility of Workspace cards', async () => {
    const cards = await WorkspaceCard.getAllCards(page);
    let width;
    let height;
    for (const card of cards) {
      const cardElem = new BaseElement(page, card.asElementHandle());
      expect(await cardElem.isVisible()).toBe(true);
      const size = await cardElem.getSize();
      expect(size).toBeTruthy();
      expect(size.height).toBeGreaterThan(1);
      expect(size.width).toBeGreaterThan(1);

      if (width === undefined || height === undefined) {
        width = size.width; // Initialize width and height with first card element's size, compare with rest cards
        height = size.height;
      } else {
        expect(size.height).toEqual(height);
        expect(size.width).toEqual(width);
      }

      // check workspace name has characters
      const cardName = await card.getWorkspaceName();
      expect(cardName).toMatch(new RegExp(/^[a-zA-Z]+/));

      // check Workspace Action menu for listed actions
      const ellipsis = card.getEllipsis();
      // Assumption: test user is workspace Owner.
      // Check Workspace Actions ellipsis dropdown displayes the right set of options
      const links = await ellipsis.getAvaliableActions();
      expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
    }
  });

   // Click See All workspaces link => Opens Your Workspaces page
  test('Click on See All workspace link', async () => {
    const seeAllWorkspacesLink = await Link.forLabel(page, HOME_PAGE_LABEL_ALIAS.SEE_ALL_WORKSPACES);
    await seeAllWorkspacesLink.click();
    const workspaces = new WorkspacesPage(page);
    await workspaces.waitForLoad();
    expect(await workspaces.isLoaded()).toBe(true);
    await seeAllWorkspacesLink.dispose();
  });

   // Click Create New Workspace link => Opens Create Workspace page
  test('Click on Create New Workspace link', async () => {
    const home = new HomePage(page);
    await home.getCreateNewWorkspaceLink().then((link) => link.click());

    const workspaceEdit = new WorkspaceEditPage(page);
    await workspaceEdit.waitForLoad();
    // expect Workspace name Input textfield exists
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
  });

  test('Check Create New Workspace link on Home page', async () => {
    const plusIcon = await ClrIconLink.forLabel(page, {normalizeSpace: HOME_PAGE_LABEL_ALIAS.CREATE_NEW_WORKSPACE}, 'plus-circle');
    expect(plusIcon).toBeTruthy();
    const classname = await plusIcon.getProperty('className');
    expect(classname).toBe('is-solid');
    const shape = await plusIcon.getAttribute('shape');
    expect(shape).toBe('plus-circle');
    const hasShape = await plusIcon.hasAttribute('shape');
    expect(hasShape).toBe(true);
    const disabled = await plusIcon.isDisabled();
    expect(disabled).toBe(false);
    const cursor = await plusIcon.getComputedStyle('cursor');
    expect(cursor).toBe('pointer');
    expect(await plusIcon.isVisible()).toBe(true);

    await plusIcon.dispose();
    expect(await plusIcon.isVisible()).toBe(false);
  });


});
