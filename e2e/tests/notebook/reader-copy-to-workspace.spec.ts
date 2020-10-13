import DataResourceCard from 'app/component/data-resource-card';
import Modal from 'app/component/modal';
import WorkspaceCard from 'app/component/workspace-card';
import Link from 'app/element/link';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {Option, Language, LinkText, ResourceCard, WorkspaceAccessLevel} from 'app/text-labels';
import {config} from 'resources/workbench-config';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn, signInAs, signOut} from 'utils/test-utils';
import {waitWhileLoading} from 'utils/waits-utils';

jest.setTimeout(20 * 60 * 1000);

describe('Workspace reader Jupyter notebook action tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test: Workspace reader can copy notebook to another Workspace then gain Edit right to the clone notebook.
   * - Create a Workspace and a new notebook. Run simple code in notebook and save notebook.
   * - Share Workspace with collaborator as READER.
   * - Sign in as collaborator.
   * - Create Workspace where collaborator is the OWNER.
   * - Verify Workspace and notebook permissions.
   * - Copy notebook to collaborator Workspace.
   * - Full Edit rights to clone notebook.
   * - Delete clone notebook.
   */
  test('Workspace reader copy notebook to another workspace', async () => {
    const workspaceName = await findWorkspace(page, {create: true}).then(card => card.clickWorkspaceName());

    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName, Language.Python);

     // Run code: 1 + 1.
    const code = 'print(1+1)';
    const cellOutput = await notebook.runCodeCell(1, {code});
    expect(Number.parseInt(cellOutput, 10)).toEqual(2);
    await notebook.save();

    const analysisPage = await notebook.goAnalysisPage();
    await analysisPage.openAboutPage();

    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();

    // Share Workspace to collaborator as READER.
    const shareModal = await aboutPage.openShareModal();
    await shareModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Reader);
    await waitWhileLoading(page);
    await signOut(page);

    // Sign in as collaborator in new Incognito page.
    const newPage = await signInAs(page, config.collaboratorUsername, config.userPassword);

    // Create a new Workspace. This is the copy to workspace.
    const collaboratorWorkspaceName = await findWorkspace(newPage, {create: true}).then(card => card.getWorkspaceName());

    // Verify shared Workspace Access Level is READER.
    const workspaceCard = await WorkspaceCard.findCard(newPage, workspaceName);
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);

    // Verify notebook actions list.
    await workspaceCard.clickWorkspaceName();
    await new WorkspaceDataPage(newPage).openAnalysisPage();

    // Notebook actions Rename, Duplicate and Delete are disabled.
    const dataResourceCard = new DataResourceCard(newPage);
    let notebookCard = await dataResourceCard.findCard(notebookName, ResourceCard.Notebook);
    // open Snowman menu.
    const snowmanMenu = await notebookCard.getSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(Option.Rename)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(Option.Duplicate)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(Option.Delete)).toBe(true);
    // But the Copy to another Workspace action is available for click.
    expect(await snowmanMenu.isOptionDisabled(Option.CopyToAnotherWorkspace)).toBe(false);
    await notebookCard.clickSnowmanIcon(); // close Snowman menu.

    // Copy notebook to another Workspace and give notebook a new name.
    const newAnalysisPage = new WorkspaceAnalysisPage(newPage);
    const copiedNotebookName = `copy-of-${notebookName}`;
    await newAnalysisPage.copyNotebookToWorkspace(notebookName, collaboratorWorkspaceName, copiedNotebookName);

    // Verify Copy Success modal.
    const modal = new Modal(newPage);
    await modal.waitForButton(LinkText.GoToCopiedNotebook);
    const textContent = await modal.getTextContent();
    expect(textContent).toContain('Copy to Workspace');
    const expectedFullMsg = `Successfully copied ${notebookName}  to ${collaboratorWorkspaceName} . Do you want to view the copied Notebook?`
    expect(textContent).toContain(expectedFullMsg);

    // Dismiss modal. Open Copied notebook.
    await modal.clickButton(LinkText.GoToCopiedNotebook, {waitForClose: true});

    // Verify current workspace is collaborator Workspace.
    await newAnalysisPage.waitForLoad();
    const workspaceLink = await Link.findByName(newPage, {name: collaboratorWorkspaceName});
    const linkDisplayed = await workspaceLink.isDisplayed();
    expect(linkDisplayed).toBe(true);

    // Verify copied notebook exists in collaborator Workspace.
    notebookCard = await dataResourceCard.findCard(copiedNotebookName, ResourceCard.Notebook);
    expect(notebookCard).toBeTruthy();

    // Notebook actions Rename, Duplicate, Delete and Copy to another Workspace actions are avaliable to click.
    const copyNotebookCardMenu = await notebookCard.getSnowmanMenu();
    expect(await copyNotebookCardMenu.isOptionDisabled(Option.Rename)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(Option.Duplicate)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(Option.Delete)).toBe(false);
    expect(await copyNotebookCardMenu.isOptionDisabled(Option.CopyToAnotherWorkspace)).toBe(false);
    await notebookCard.clickSnowmanIcon(); // close menu

    await newAnalysisPage.deleteResource(copiedNotebookName, ResourceCard.Notebook);
  })

});
