import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, findWorkspaceCard, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import Modal from 'app/modal/modal';

// re-run one more time if test has failed
jest.retryTimes(1);

/**
 * Test:
 * - Create new Workspace as the copy-to destination Workspace.
 * - Create new Workspace as copy-from Workspace and create new notebook in this Workspace.
 * - Run code to print WORKSPACE_NAMESPACE. It should match Workspace namespace from Workspace URL.
 * - Copy notebook to destination Workspace and give copied notebook a new name.
 * - Verify copied notebook is in destination Workspace.
 * - Open copied notebook and run code to print WORKSPACE_NAMESPACE. It should match destination Workspace namespace.
 * - Delete notebooks.
 */
describe('Copy notebook', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const srcWorkspaceName = 'e2eOwnerCopyNotebookToWorkspaceTest_SourceWorkspace';
  const destWorkspaceName = 'e2eOwnerCopyNotebookToWorkspaceTest_DestinationWorkspace';

  test(
    'Copy notebook to another Workspace when CDR versions match',
    async () => {
      // Create src and dest workspaces with default CDR version
      await findOrCreateWorkspace(page, {
        workspaceName: srcWorkspaceName,
        cdrVersion: config.defaultCdrVersionName
      });
      await findOrCreateWorkspace(page, {
        workspaceName: destWorkspaceName,
        cdrVersion: config.defaultCdrVersionName
      });

      // Find and open source workspace Data page.
      await findWorkspaceCard(page, srcWorkspaceName).then((card) => card.clickWorkspaceName());

      // Create notebook in source workspace.
      const sourceNotebookName = makeRandomName('py');
      const dataPage = new WorkspaceDataPage(page);
      const sourceWorkspacePage = await dataPage.createNotebook(sourceNotebookName);

      // Exit notebook and returns to the Workspace Analysis tab.
      const analysisPage = await sourceWorkspacePage.goAnalysisPage();

      // Copy to destination Workspace and give notebook a new name.
      const copyNewNotebookName = makeRandomName('pyCopy');
      await analysisPage.copyNotebookToWorkspace(sourceNotebookName, destWorkspaceName, copyNewNotebookName);

      // Verify Copy Success modal.
      const modal = new Modal(page);
      await modal.waitForLoad();
      const textContent = await modal.getTextContent();
      const successMsg =
        `Successfully copied ${sourceNotebookName}  to ${destWorkspaceName} . ` +
        'Do you want to view the copied Notebook?';
      expect(textContent).toContain(successMsg);
      // Dismiss modal.
      await modal.clickButton(LinkText.StayHere, { waitForClose: true });

      // Delete notebook
      const deleteModalTextContent = await analysisPage.deleteResource(sourceNotebookName, ResourceCard.Notebook);
      expect(deleteModalTextContent).toContain(`Are you sure you want to delete Notebook: ${sourceNotebookName}?`);

      // Perform actions in copied notebook.
      // Open destination Workspace
      await findWorkspaceCard(page, destWorkspaceName).then((card) => card.clickWorkspaceName());

      // Verify copy-to notebook exists in destination Workspace
      await dataPage.openAnalysisPage();
      const dataResourceCard = new DataResourceCard(page);
      const notebookCard = await dataResourceCard.findCard(copyNewNotebookName, ResourceCard.Notebook);
      expect(notebookCard).toBeTruthy();

      // Delete notebook
      const modalTextContent = await analysisPage.deleteResource(copyNewNotebookName, ResourceCard.Notebook);
      expect(modalTextContent).toContain('This will permanently delete the Notebook.');
    },
    30 * 60 * 1000
  );
});
