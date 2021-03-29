import WorkspaceCard from 'app/component/workspace-card';
import HomePage from 'app/page/home-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspacesPage from 'app/page/workspaces-page';
import { WorkspaceAccessLevel } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { signInWithAccessToken, signInAs, signOut, findOrCreateWorkspace } from 'utils/test-utils';
import { makeWorkspaceName } from 'utils/str-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { waitWhileLoading } from 'utils/waits-utils';

const assigns = [
  { accessRole: WorkspaceAccessLevel.Writer },
  { accessRole: WorkspaceAccessLevel.Reader },
  { accessRole: WorkspaceAccessLevel.Owner }
];

const newWorkspaceName: string = makeWorkspaceName();
let newWorkspaceCard: WorkspaceCard;

describe('Share workspace', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
    newWorkspaceCard = await findOrCreateWorkspace(page, { workspaceName: newWorkspaceName, alwaysCreate: true });
    await newWorkspaceCard.clickWorkspaceName();
  });

  /**
   * Test:
   * - Create a new workspace.
   * - Share with another user - assigns role Writer/Reader/Owner
   * - Log in as another user.
   * - Workspace share action should be disabled.
   */
  test.each(assigns)('As %s, user cannot share, edit or delete workspace', async (assign) => {
    await removeCollaborators();

    // Share with collaborator in Workspace Data page.
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openDataPage();
    await dataPage.verifyWorkspaceNameOnDataPage(newWorkspaceName);

    const shareWorkspaceModal = await dataPage.shareWorkspace();
    await shareWorkspaceModal.shareWithUser(config.collaboratorUsername, assign.accessRole);

    await waitWhileLoading(page);
    await signOut(page);

    // To verify access level is assigned correctly, user with WRITER/READER/OWNER role will sign in in new Incognito page.
    const newPage = await signInAs(config.collaboratorUsername, config.userPassword);

    const homePage = new HomePage(newPage);
    await homePage.getSeeAllWorkspacesLink().then((link) => link.click());

    const workspacesPage1 = new WorkspacesPage(newPage);
    await workspacesPage1.waitForLoad();

    // Verify Workspace Access Level is WRITER/READER/OWNER.
    const workspaceCard = await WorkspaceCard.findCard(newPage, newWorkspaceName);
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(assign.accessRole);

    // Share, Edit and Delete actions are disabled for Writer & Reader and enabled for Owner.
    await workspaceCard.verifyWorkspaceCardMenuOptions();

    // Make sure the Search input-field in Share modal is disabled for Writer & Reader and enabled for Owner.
    await workspaceCard.clickWorkspaceName();
    await new WorkspaceDataPage(newPage).openAboutPage();
    const aboutPage = new WorkspaceAboutPage(newPage);
    await aboutPage.waitForLoad();

    const accessLevel1 = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
    expect(accessLevel1).toBe(assign.accessRole);

    // verify if the search input field is disabled for Writer/reader and enabled for Owner
    await aboutPage.verifyCollabInputField();
  });

  test('As OWNER, user can share workspace', async () => {
    await removeCollaborators();

    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();

    let shareModal = await aboutPage.openShareModal();
    await shareModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Owner);
    // Collab list is refreshed.
    await waitWhileLoading(page);

    let accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
    expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);

    shareModal = await aboutPage.openShareModal();
    await shareModal.removeUser(config.collaboratorUsername);
    await waitWhileLoading(page);

    accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
    expect(accessLevel).toBeNull();
  });
});

// Remove existing collaborators in Workspace About page.
async function removeCollaborators() {
  const dataPage = new WorkspaceDataPage(page);
  await dataPage.waitForLoad();
  await dataPage.openAboutPage();
  const aboutPage = new WorkspaceAboutPage(page);
  await aboutPage.waitForLoad();
  await aboutPage.removeCollab();
  await waitWhileLoading(page);
}
