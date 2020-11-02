import DataResourceCard from 'app/component/data-resource-card';
import ClrIconLink from 'app/element/clr-icon-link';
import CohortBuildPage from 'app/page/cohort-build-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {Option, ResourceCard} from 'app/text-labels';
import {findOrCreateWorkspace, signIn} from 'utils/test-utils';
import {waitForText, waitWhileLoading} from 'utils/waits-utils';

describe.skip('Dataset test', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Create a new Cohort from searching for drug name "hydroxychloroquine".
   * - Create a new Dataset from All Participants and All Surveys.
   * - Save dataset without Export to Notebook.
   * - Edit dataset. Save dataset without Export to Notebook.
   * - Delete Dataset.
   */
  // RW-5751 Search for condition is failing in CI.
  xtest('Can create Dataset with user-defined Cohorts', async () => {
    const workspaceCard = await findOrCreateWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Click Add Cohorts button
    const dataPage = new WorkspaceDataPage(page);
    const addCohortsButton = await dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // Build new Cohort
    const cohortBuildPage = new CohortBuildPage(page);
    // Include Participants Group 1: Add a Condition
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const searchPage = await group1.includeDrugs();

    // Search for drug hydroxychloroquine
    const searchResultsTable = await searchPage.searchCondition('hydroxychloroquine');
    // Add a drug in Result table first row. Drug name ignored.
    const nameValue = await searchResultsTable.getCellValue(1, 1);
    const addIcon = await ClrIconLink.findByName(page, {containsText: nameValue, iconShape: 'plus-circle'}, searchResultsTable);
    await addIcon.click();

    // Open selection list and click Save Criteria button
    await searchPage.viewAndSaveCriteria();
    await cohortBuildPage.getTotalCount();

    // Save new cohort.
    const cohortName = await cohortBuildPage.saveCohortAs();
    await waitForText(page, 'Cohort Saved Successfully');
    console.log(`Created Cohort "${cohortName}"`);

    await dataPage.openDataPage();

    // Click Add Datasets button.
    const datasetPage = await dataPage.clickAddDatasetButton();

    await datasetPage.selectCohorts([cohortName]);
    await datasetPage.selectConceptSets(['Demographics']);
    const saveModal = await datasetPage.clickSaveAndAnalyzeButton();
    let datasetName = await saveModal.saveDataset({exportToNotebook: false});

    // Verify create successful.
    await dataPage.openDatasetsSubtab();

    const resourceCard = new DataResourceCard(page);
    const dataSetExists = await resourceCard.cardExists(datasetName, ResourceCard.Dataset);
    expect(dataSetExists).toBe(true);

    // Edit the dataset to include "All Participants".
    const datasetCard = await resourceCard.findCard(datasetName)
    await datasetCard.selectSnowmanMenu(Option.Edit);
    await waitWhileLoading(page);

    await datasetPage.selectCohorts(['All Participants']);
    await datasetPage.clickAnalyzeButton();

    // Save Dataset in a new name.
    datasetName = await saveModal.saveDataset({exportToNotebook: false}, true);
    await dataPage.waitForLoad();

    await dataPage.openDatasetsSubtab();
    await dataPage.deleteResource(datasetName, ResourceCard.Dataset);
  });

});
