import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {ResourceCard} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findOrCreateWorkspace, signIn} from 'utils/test-utils';

describe('Dataset test', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Create a new Dataset from All Participants and All Surveys.
   * - Save dataset without Export to Notebook.
   * - Rename dataset.
   * - Delete dataset.
   */
  // disabled temporarily to merge fitbit concept sets, will fix with RW-5932
  xtest('Can create and rename Dataset', async () => {

    await findOrCreateWorkspace(page);

    // Click Add Datasets button
    const dataPage = new WorkspaceDataPage(page);
    const datasetPage = await dataPage.clickAddDatasetButton();

    await datasetPage.selectCohorts(['All Participants']);
    await datasetPage.selectConceptSets(['Demographics', 'All Surveys']);
    const saveModal = await datasetPage.clickSaveAndAnalyzeButton();
    const datasetName = await saveModal.saveDataset();

    // Verify create successful
    const resourceCard = new DataResourceCard(page);
    const dataSetExists = await resourceCard.cardExists(datasetName, ResourceCard.Dataset);
    expect(dataSetExists).toBe(true);

    // Rename Dataset
    const newDatasetName = makeRandomName();
    await dataPage.renameResource(datasetName, newDatasetName, ResourceCard.Dataset);

    await dataPage.openDatasetsSubtab();

    // Verify rename successful
    const newDatasetExists = await resourceCard.cardExists(newDatasetName, ResourceCard.Dataset);
    expect(newDatasetExists).toBe(true);

    const oldDatasetExists = await resourceCard.cardExists(datasetName, ResourceCard.Dataset);
    expect(oldDatasetExists).toBe(false);

    // Delete Dataset
    const textContent = await dataPage.deleteResource(newDatasetName, ResourceCard.Dataset);
    expect(textContent).toContain(`Are you sure you want to delete Dataset: ${newDatasetName}?`);
  });


});
