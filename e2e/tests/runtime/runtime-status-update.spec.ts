import HelpSidebar, {HelpSidebarTab} from 'app/component/help-sidebar';
import RuntimePanel, {
  ComputeType,
  RuntimePreset,
  StartStopIconState
} from 'app/component/runtime-panel';
import {config} from 'resources/workbench-config';
import {createWorkspace, signIn} from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {makeRandomName} from 'utils/str-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import NotebookPage from '../../app/page/notebook-page';
import WorkspaceAnalysisPage from '../../app/page/workspace-analysis-page';

describe('Updating runtime parameters', () => {
  beforeEach(async () => {
    await signIn(page);
    const workspaceCard = await createWorkspace(page, config.altCdrVersionName);
    await workspaceCard.clickWorkspaceName();

    // Pause a bit to wait for getRuntime to complete
    await page.waitForTimeout(2000);
  });

  test('Create a default runtime', async() => {
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCreateButton();

    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
  });

  test('Create a custom runtime', async() => {
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCustomizeButton();

    await runtimePanel.pickCpus(8);
    await runtimePanel.pickRamGbs(30);
    await runtimePanel.pickDiskGbs(60);
    await runtimePanel.clickCreateButton();

    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
  });

  test('Create a dataproc runtime', async() => {
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCustomizeButton();

    await runtimePanel.pickCpus(8);
    await runtimePanel.pickRamGbs(30);
    await runtimePanel.pickDiskGbs(60);
    await runtimePanel.pickComputeType(ComputeType.Dataproc);
    await runtimePanel.pickDataprocNumWorkers(3);
    await runtimePanel.pickDataprocNumPreemptibleWorkers(1);
    await runtimePanel.pickWorkerCpus(2);
    await runtimePanel.pickWorkerRamGbs(13);
    await runtimePanel.pickWorkerDisk(60);
    await runtimePanel.clickCreateButton();

    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
  });


  test('Switch from GCE to dataproc', async() => {
    // This one is going to take a long time.
    jest.setTimeout(60 * 20 * 1000);
    // Open the runtime panel
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);

    // Click “create“ , from the default “create panel”
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCreateButton();

    // Wait until status shows green in side-nav
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Open a Python notebook
    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run some Python commands to validate the VM configuration
    const cpusOutputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/count-cpus.py'});
    // Default CPU count is 4
    expect(parseInt(cpusOutputText)).toBe(4);
    // This gets the amount of memory available to Python in bytes
    const memoryOutputText = await notebook.runCodeCell(2, {codeFile: 'resources/python-code/count-memory.py'});
    // Default memory is 15 gibibytes, we'll check that it is between 14 billion and 16 billion bytes
    expect(parseInt(memoryOutputText)).toBeGreaterThanOrEqual(14 * 1000 * 1000 * 1000);
    expect(parseInt(memoryOutputText)).toBeLessThanOrEqual(16 * 1000 * 1000 * 1000);
    // This gets the disk space in bytes
    const diskOutputText = await notebook.runCodeCell(3, {codeFile: 'resources/python-code/count-disk-space.py'});
    // Default disk is 50 gibibytes, we'll check that it is between 45 and 55 billion bytes
    expect(parseInt(diskOutputText)).toBeGreaterThanOrEqual(45 * 1000 * 1000 * 1000);
    expect(parseInt(diskOutputText)).toBeLessThanOrEqual(55 * 1000 * 1000 * 1000);

    // Go back to the analysis page in the hopes that maybe the dataproc dropdown will behave
    const notebookPage = new NotebookPage(page, notebookName);
    await notebookPage.goAnalysisPage();

    // Open runtime panel
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);

    // Switch to dataproc cluster with custom settings (e.g. 1 preemptible worker)
    await runtimePanel.pickComputeType(ComputeType.Dataproc);
    await runtimePanel.pickDataprocNumWorkers(3);
    await runtimePanel.pickDataprocNumPreemptibleWorkers(1);
    await runtimePanel.pickWorkerCpus(1);
    await runtimePanel.pickWorkerDisk(60);

    // Wait for indicator to go green in side-nav
    await runtimePanel.clickNextButton();
    await runtimePanel.clickApplyAndRecreateButton();
    await page.waitForTimeout(2000);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Stopping);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Starting);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Go back to the notebook:
    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.openNotebook(notebookName);
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.openEditMode(notebookName);

    // Run notebook to validate runtime settings. Use import hail … hail.spark_context() in Python to verify Spark (see above for other settings)
    const sparkOutputText = await notebook.runCodeCell(4, {codeFile: 'resources/python-code/spark-context.py'});
    // TODO: analyze this
    console.log(sparkOutputText);

    // Open runtime panel
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);

    // Click 'delete environment'
    await runtimePanel.clickDeleteEnvironmentButton();
    await runtimePanel.clickDeleteButton();

    // wait until status indicator disappears
    await helpSidebar.waitForRuntimeStatusIconHidden();

    // Refresh page, and reopen the panel
    await page.reload({ waitUntil: ['networkidle0', 'domcontentloaded'] });
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);

    // Verify that dataproc settings are still shown
    expect(await runtimePanel.getDataprocNumWorkers()).toBe(3);
    expect(await runtimePanel.getDataprocNumPreemptibleWorkers()).toBe(1);
    expect(await runtimePanel.getWorkerCpus()).toBe("1");
    expect(await runtimePanel.getWorkerRamGbs()).toBe("3.75");
    expect(await runtimePanel.getWorkerDisk()).toBe(60);
  });

  test('Switch from dataproc to GCE', async() => {
    // This one is going to take a long time.
    jest.setTimeout(60 * 20 * 1000);

    // Open the runtime panel
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);

    // Click “customize“ , from the default “create panel”
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.clickCustomizeButton();

    // Use the preset selector to pick “Hail genomics analysis“
    runtimePanel.pickRuntimePreset(RuntimePreset.HailGenomicsAnalysis);
    await runtimePanel.clickCreateButton();

    // Wait until status shows green in side-nav
    await page.waitForTimeout(2000);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Open a notebook
    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run notebook to validate runtime settings. Use import hail … hail.spark_context() in Python to verify Spark (see above for other settings)
    const sparkOutputText = await notebook.runCodeCell(1, {codeFile: 'resources/python-code/spark-context.py'});
    // TODO: analyze this
    console.log(sparkOutputText);

    // Open runtime panel
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);

    // Switch to a GCE VM with custom settings
    await runtimePanel.pickComputeType(ComputeType.Standard);
    await runtimePanel.pickCpus(8);
    await runtimePanel.pickRamGbs(30);
    await runtimePanel.pickDiskGbs(60);
    await runtimePanel.clickNextButton();
    await runtimePanel.clickApplyAndRecreateButton();

    // Wait for indicator to go green in side-nav
    await page.waitForTimeout(2000);
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Go back to the notebook:
    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.openNotebook(notebookName);
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.openEditMode(notebookName);

    // Run notebook to validate runtime settings (cpu, disk, memory)
    const cpusOutputText = await notebook.runCodeCell(2, {codeFile: 'resources/python-code/count-cpus.py'});
    // Default CPU count is 4
    expect(parseInt(cpusOutputText)).toBe(4);
    // This gets the amount of memory available to Python in bytes
    const memoryOutputText = await notebook.runCodeCell(3, {codeFile: 'resources/python-code/count-memory.py'});
    // Default memory is 15 gibibytes, we'll check that it is between 14 billion and 16 billion bytes
    expect(parseInt(memoryOutputText)).toBeGreaterThanOrEqual(14 * 1000 * 1000 * 1000);
    expect(parseInt(memoryOutputText)).toBeLessThanOrEqual(16 * 1000 * 1000 * 1000);
    // This gets the disk space in bytes
    const diskOutputText = await notebook.runCodeCell(4, {codeFile: 'resources/python-code/count-disk-space.py'});
    // Default disk is 50 gibibytes, we'll check that it is between 45 and 55 billion bytes
    expect(parseInt(diskOutputText)).toBeGreaterThanOrEqual(45 * 1000 * 1000 * 1000);
    expect(parseInt(diskOutputText)).toBeLessThanOrEqual(55 * 1000 * 1000 * 1000);

    // Open runtime panel
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Running);

    // Click ''delete environment”
    await runtimePanel.clickDeleteEnvironmentButton();
    await runtimePanel.clickDeleteButton();

    // wait until status indicator disappears
    await helpSidebar.waitForRuntimeStatusIconHidden();

    // Refresh page, reopen the panel
    await page.reload({ waitUntil: ['networkidle0', 'domcontentloaded'] });
    await helpSidebar.clickSidebarTab(HelpSidebarTab.ComputeConfiguration);

    // Verify GCE custom settings are still shown
    expect(await runtimePanel.getCpus()).toBe("8");
    expect(await runtimePanel.getRamGbs()).toBe("30");
    expect(await runtimePanel.getDiskGbs()).toBe(60);
  });
});
