import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import {Language, ResourceCard} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter Notebook tests in Python language', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Save and open notebook', async () => {

    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openAnalysisPage();

    const notebookName = makeRandomName('py-notebook');
    const analysisPage = new WorkspaceAnalysisPage(page);
    const notebook = await analysisPage.createNotebook(notebookName, Language.Python);

    const kernelName = await notebook.getKernelName();
    expect(kernelName).toBe('Python 3');

    // Import python file to run code.
    const origCellOutput = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/import-os.py'});

    // Save, exit notebook then come back from Analysis page.
    await notebook.save();
    await notebook.goAnalysisPage();

    // Find and open saved notebook and verify notebook contents match.
    const resourceCard = new DataResourceCard(page);
    const notebookCard = await resourceCard.findCard(notebookName, ResourceCard.Notebook);
    await notebookCard.clickResourceName();

    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    await notebookPreviewPage.openEditMode(notebookName);

    // Get Code cell [1] input and output.
    const [, newCellOutput] = await notebook.getCellInputOutput(1);

    // Delete Python notebook from Workspace Analysis page.
    await notebook.deleteNotebook(notebookName);

    // Verify Code cell [1] input and output.
    expect(newCellOutput).toEqual(origCellOutput);
  });

})
