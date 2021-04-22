import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import CohortBuildPage from 'app/page/cohort-build-page';
import { makeRandomName } from 'utils/str-utils';
import { PhysicalMeasurementsCriteria } from 'app/page/criteria-search-page';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { waitForText } from 'utils/waits-utils';
import CohortReviewModal from 'app/modal/cohort-review-modal';
import { LinkText } from 'app/text-labels';
import CohortReviewPage from 'app/page/cohort-review-page';

describe('Cohort edit tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eEditCohortTest';

  /**
   * Test:
   * Create new workspace if not exists.
   * Find an existing Cohorts or create a new Cohorts.
   * Renaming Cohorts Group 1 name.
   * Delete Cohorts.
   */
  test('Rename group', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const cohortsCard = await dataPage.findOrCreateCohort();
    await cohortsCard.clickResourceName();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Save Total Count for comparison later
    const totalCount = Number((await cohortBuildPage.getTotalCount()).replace(/,/g, ''));

    // Edit Group 1 name successfully
    const newName = makeRandomName();
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.editGroupName(newName);

    // Check new named group
    const groupName = cohortBuildPage.findIncludeParticipantsGroup(newName);
    expect(await groupName.exists()).toBe(true);

    // Check Total Count is unaffected by group name rename
    const newTotalCount = Number((await cohortBuildPage.getTotalCount()).replace(/,/g, ''));
    expect(newTotalCount).toBe(totalCount);
  });

  test('Insert new group', async () => {
    await findCohort();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Save Total Count for comparison later
    const totalCount = Number((await cohortBuildPage.getTotalCount()).replace(/,/g, ''));

    // Insert new Include Participants Group
    const newGroup = cohortBuildPage.findIncludeParticipantsEmptyGroup();
    await newGroup.includePhysicalMeasurement(PhysicalMeasurementsCriteria.Weight, 190);

    // Check Total Count and new Total Count is different
    const newTotalCount = Number((await cohortBuildPage.getTotalCount()).replace(/,/g, ''));
    expect(newTotalCount).toBe(totalCount);

    const newCohortName = await cohortBuildPage.saveCohortAs();

    // Should land on Cohorts Actions page
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();

    await waitForText(page, 'Cohort Saved Successfully');
    await waitForText(page, `The cohort ${newCohortName} has been saved.`);
  });

  test('Review cohort', async () => {
    const cohortName = await findCohort();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    const reviewSetsButton = await cohortBuildPage.getCopyButton();
    await reviewSetsButton.click();

    const modal = new CohortReviewModal(page);
    await modal.waitForLoad();

    const reviewSetNumberOfParticipants = Math.floor(Math.random() * (1000 - 100) + 100);
    await modal.fillInNumberOfParticipants(reviewSetNumberOfParticipants);
    await modal.clickButton(LinkText.CreateSet);

    const cohortReviewPage = new CohortReviewPage(page);
    await cohortReviewPage.waitForLoad();

    await waitForText(page, `Review Sets for ${cohortName}`);

    // Verify table pagination records count.
    const participantsTable = cohortReviewPage.getDataTable();
    const records = await participantsTable.getNumRecords();
    // Table records page numbering is in "1 - 25 of 100 records" format.
    expect(Number(records[2])).toEqual(reviewSetNumberOfParticipants);

    console.log(`Created Review Set with ${reviewSetNumberOfParticipants} participants.`);
  });

  test('Delete cohort', async () => {
    const cohortName = await findCohort();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Delete cohort while inside the Cohort Build page
    const modalContent = await cohortBuildPage.deleteCohort();
    expect(modalContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);
  });

  async function findCohort(): Promise<string> {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const cohortsCard = await dataPage.findOrCreateCohort();
    const cohortName = await cohortsCard.getResourceName();
    await cohortsCard.clickResourceName();
    return cohortName;
  }
});
