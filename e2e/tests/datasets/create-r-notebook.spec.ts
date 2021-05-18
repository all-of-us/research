import Link from 'app/element/link';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { Ethnicity } from 'app/page/cohort-participants-group';
import { Language, LinkText, ResourceCard } from 'app/text-labels';

describe('Create dataset and export to notebook at same time', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eDataSetsCreateRNotebookTest';

  /**
   * Create new Cohort thru Dataset Build page. Cohort built from demographics -> Ethnicity.
   * Create new Dataset with Cohort, then export to notebook in R language.
   * Delete Cohort, Dataset, and Notebook.
   */
  test('Jupyter Notebook for R programming language can be created', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    // Click Add Datasets button
    const dataPage = new WorkspaceDataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();
    const cohortBuildPage = await datasetBuildPage.clickAddCohortsButton();

    // Include Participants Group 1: Add Criteria: Ethnicity
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeEthnicity([Ethnicity.HispanicOrLatino, Ethnicity.NotHispanicOrLatino]);

    // Check Group 1 Count.
    const group1Count = await group1.getGroupCount();
    expect(group1Count).toBeGreaterThan(1);
    console.log(`Include Participants Group 1: ${group1Count}`);

    // Save new Cohort.
    const newCohortName = await cohortBuildPage.createCohort();
    await waitForText(page, 'Cohort Saved Successfully');
    console.log(`Created Cohort "${newCohortName}"`);

    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.clickCreateDatasetButton();

    await datasetBuildPage.selectCohorts([newCohortName]);
    await datasetBuildPage.selectConceptSets([LinkText.Demographics]);
    const saveModal = await datasetBuildPage.clickSaveAndAnalyzeButton();
    const newNotebookName = makeRandomName();
    const newDatasetName = await saveModal.saveDataset({
      exportToNotebook: true,
      notebookName: newNotebookName,
      lang: Language.R
    });
    await waitWhileLoading(page);

    // Verify Notebook preview. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${newNotebookName}.ipynb`);

    const code = await notebookPreviewPage.getFormattedCode();
    expect(code).toContain('library(bigrquery)');

    // Navigate to Workspace Data page.
    const notebooksLink = Link.findByName(page, { name: workspace });
    await notebooksLink.clickAndWait();
    await dataPage.waitForLoad();

    // Delete test data sequence is: Delete Notebook, then Dataset, finally Cohort.
    // Delete Notebook
    await dataPage.openAnalysisPage();

    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();
    await analysisPage.deleteResource(newNotebookName, ResourceCard.Notebook);

    // Delete Dataset
    await dataPage.openDatasetsSubtab();

    const datasetDeleteDialogText = await dataPage.deleteResource(newDatasetName, ResourceCard.Dataset);
    expect(datasetDeleteDialogText).toContain(`Are you sure you want to delete Dataset: ${newDatasetName}?`);

    // Delete Cohort
    await dataPage.openCohortsSubtab();

    const cohortDeleteDialogText = await dataPage.deleteResource(newCohortName, ResourceCard.Cohort);
    expect(cohortDeleteDialogText).toContain(`Are you sure you want to delete Cohort: ${newCohortName}?`);
  });
});
