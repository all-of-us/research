import WorkspacesPage from 'app/page/workspaces-page';
import {signIn} from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import ReactSelect from '../../app/element/react-select';

describe('Workspaces Filter Select menu tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Open "Your Workspaces" page.
   * - Select Filter by Access Level.
   * - Find visible Workspace cards, get their Access Level.
   * - Access level should matchs filter value.
   *
   */
  test('Display workspaces by access levels', async () => {

    const accessLevels = [
      'Owner',
      'Writer',
      'Reader'
    ];

    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    // Default Filter by Select menu value is 'All'.
    const selectMenu = new ReactSelect(page, {name: 'Filter by'});
    const defaultSelectedValue = await selectMenu.getSelectedOption();
    expect(defaultSelectedValue).toEqual('All');

    // Change Filter by value.
    for (const accessLevel of accessLevels) {
      const selectedValue = await workspacesPage.filterByAccessLevel(accessLevel);
      expect(selectedValue).toEqual(accessLevel);
      const cards = await WorkspaceCard.findAllCards(page);
      // If any card exists, get its Access Level and compare with filter level.
      for (const card of cards) {
        const cardLevel = await card.getWorkspaceAccessLevel();
        expect(cardLevel).toEqual(accessLevel.toUpperCase());
      }
    }

  });


});
