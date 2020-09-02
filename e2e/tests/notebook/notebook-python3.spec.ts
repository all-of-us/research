import WorkspaceDataPage from 'app/page/workspace-data-page';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook tests in Python language', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Run code from file', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    const cell1OutputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/import-os.py'});
    expect(cell1OutputText).toContain('success');

    const cell2OutputText = await notebook.runCodeCell(2, {codeFile: 'resources/python-code/import-libs.py'});
    expect(cell2OutputText).toContain('success');

    await notebook.runCodeCell(3, {codeFile: 'resources/python-code/simple-pyplot.py'});

    // Verify plot is the output.
    const cell = await notebook.findCell(3);
    const cellOutputElement = await cell.findOutputElementHandle();
    const [imgElement] = await cellOutputElement.$x('./img[@src]');
    expect(imgElement).toBeTruthy(); // plot format is a img.

    const codeSnippet = '!jupyter kernelspec list';
    const codeSnippetOutput = await notebook.runCodeCell(4, {code: codeSnippet});
    expect(codeSnippetOutput).toEqual(expect.stringContaining('/usr/local/share/jupyter/kernels/python3'));

    await notebook.deleteNotebook(notebookName);
  });

})
