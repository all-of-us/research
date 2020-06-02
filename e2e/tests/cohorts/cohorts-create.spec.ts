import {PhysicalMeasurementsCriteria} from 'app/page/cohort-criteria-modal';
import {ButtonLabel} from 'app/component/dialog';
import WorkspaceCard from 'app/component/workspace-card';
import ClrIconLink from 'app/element/clr-icon-link';
import Link from 'app/element/link';
import {EllipsisMenuAction, WorkspaceAccessLevel} from 'app/page-identifiers';
import CohortBuildPage, {FieldSelector} from 'app/page/cohort-build-page';
import DataPage, {LabelAlias} from 'app/page/data-page';
import WorkspacesPage from 'app/page/workspaces-page';
import * as fp from 'lodash/fp';
import {makeWorkspaceName} from 'utils/str-utils';
import {signIn, waitWhileLoading} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';
import DataResourceCard from 'app/component/data-resource-card';


describe('User can create new Cohorts', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * Create a new Workspace.
   * Add criteria in Group 1: Physical Measurements criteria => BMI (>= 30).
   * Add criteria in Group 2: Demographics => Deceased.
   * Checking counts.
   * Renaming Group 1 and 2 names.
   */
  test('Add Cohort of Physical Measurements BMI', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    // Create new workspace for new Cohort.
    const newWorkspaceName = makeWorkspaceName();
    await workspacesPage.createWorkspace(newWorkspaceName);

    // Wait for the Data page.
    const dataPage = new DataPage(page);
    await dataPage.waitForLoad();

    // No cohorts in new workspace.
    const cardsCount = (await DataResourceCard.findAllCards(page)).length;
    expect(cardsCount).toBe(0);

    // Click Add Cohorts button
    const addCohortsButton = await dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // In Build Cohort Criteria page.
    const cohortPage = new CohortBuildPage(page);
    await cohortPage.waitForLoad();

    // Include Participants Group 1.
    const group1 = cohortPage.findIncludeParticipantsGroup('Group 1');
    const group1Count = await group1.includePhysicalMeasurement(PhysicalMeasurementsCriteria.BMI, 30);

    // Checking Group 1 Count. should match Group 1 participants count.
    await waitForText(page, group1Count, {xpath: group1.getGroupCountXpath()});
    const group1CountInt = Number(group1Count.replace(/,/g, ''));
    expect(group1CountInt).toBeGreaterThan(1);
    console.log('Group 1: Physical Measurement -> BMI count: ' + group1CountInt);

    // Checking Total Count: should match Group 1 participants count.
    await waitForText(page, group1Count, {xpath: FieldSelector.TotalCount});
    console.log('Total Count: ' + group1CountInt);

    // Include Participants Group 2: Select menu Demographics -> Deceased
    const group2 = cohortPage.findIncludeParticipantsGroup('Group 2');
    const group2Count = await group2.includeDemographicsDeceased();
    const group2CountInt = Number(group2Count.replace(/,/g, ''));
    expect(group2CountInt).toBeGreaterThan(1);
    console.log('Group 2: Demographics -> Deceased count: ' + group2CountInt);

    // Compare the new Total Count with the old Total Count.
    const newTotalCount = await cohortPage.getTotalCount();
    const newTotalCountInt = Number(newTotalCount.replace(/,/g, ''));
    // Adding additional group decreased Total Count.
    expect(newTotalCountInt).toBeLessThan(group1CountInt);
    console.log('New Total Count: ' + newTotalCountInt);

    // Save new cohort.
    const cohortName = await cohortPage.saveCohortAs();
    console.log(`Cohort ${cohortName} created successfully.`);

    // Open Cohort details.
    const cohortLink = await Link.findByName(page, {name: cohortName});
    await cohortLink.clickAndWait();
    // Wait for page ready
    await waitWhileLoading(page);
    await waitForText(page, newTotalCount, {xpath: FieldSelector.TotalCount});

    // Modify Cohort: Edit Group 1 name successfully.
    const newName1 = 'Group 1: BMI';
    await group1.editGroupName(newName1);
    // Check new named group
    const groupBMI = cohortPage.findIncludeParticipantsGroup(newName1);
    expect(await groupBMI.exists()).toBe(true);

    // Modify Cohort: Edit Group 2 name successfully.
    const newName2 = 'Group 2: Deceased';
    await group2.editGroupName(newName2);
    // Check new name
    const groupDeceased = cohortPage.findIncludeParticipantsGroup(newName2);
    expect(await groupDeceased.exists()).toBe(true);

    // Check Total Count is unaffected by group name rename.
    const newTotalCount2 = await cohortPage.getTotalCount();
    const newTotalCountInt2 = Number(newTotalCount2.replace(/,/g, ''));
    expect(newTotalCountInt2).toBe(newTotalCountInt);

    // Clean up: delete cohort
    const dialogContent = await cohortPage.deleteCohort();
    // Verify dialog content text
    expect(dialogContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);
    console.log(`Cohort "${cohortName}" deleted.`);
  });

  /**
   * Test:
   * Find an existing workspace.
   * Create new cohort with Condition = EKG.
   * Check Group and Total Count.
   * Check cohort open okay.
   * Duplicate cohort.
   * Delete cohort.
   */
  test('Add Cohort of EKG condition with modifiers', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    // Choose one existing workspace on "Your Workspaces" page
    const workspaceCard = new WorkspaceCard(page);
    const retrievedWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(WorkspaceAccessLevel.OWNER);
    const oneWorkspaceCard: WorkspaceCard = fp.shuffle(retrievedWorkspaces)[0];
    await oneWorkspaceCard.clickWorkspaceName();

    // Wait for the Data page
    const dataPage = new DataPage(page);
    await dataPage.waitForLoad();
    const workspaceDataUrl = await page.url();

    // Click Add Cohorts button
    const addCohortsButton = await dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // Create new Cohort
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Include Participants Group 1: Add a Condition
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const modal = await group1.includeConditions();

    // First, search for non-existent condition, expect returns no results.
    const search1ResultsTable = await modal.searchCondition('allergist');
    expect(await search1ResultsTable.exists()).toBe(false);

    // Next, search for condition EKG
    const search2ResultsTable = await modal.searchCondition('EKG');
    // Check cell value in column "Code" (column #2)
    const codeValue = await search2ResultsTable.findCellValue(1, 2);
    expect(Number(codeValue)).not.toBeNaN();

    // Add the condition in first row. We don't know what the condition name is, so we get the cell value first.
    const nameValue = await search2ResultsTable.findCellValue(1, 1);
    const addIcon = await ClrIconLink.findByName(page, {containsText: nameValue, iconShape: 'plus-circle'}, modal);
    await addIcon.click();

    // Bug RW-5017
/*
    // Click Next button to add modifier
    const nextButton = await Button.findByName(page, {name: 'Next'}, modal);
    await nextButton.waitUntilEnabled();
    await nextButton.click();

    // Add Condition Modifiers: Age At Event >= 50
    await modal.ageModifier(FilterSign.GreaterThanEqualTo, 50);
*/
    // Click FINISH button. Criteria dialog closes.
    await modal.clickButton(ButtonLabel.Finish);

    // Check Group 1 Count.
    const group1Count = await group1.getGroupCount();
    const group1CountInt = Number(group1Count.replace(/,/g, ''));
    expect(group1CountInt).toBeGreaterThan(1);
    console.log('Group 1: ' + group1CountInt);

    // Check Total Count.
    const totalCount = await cohortBuildPage.getTotalCount();
    const totalCountInt = Number(totalCount.replace(/,/g, ''));
    expect(totalCountInt).toBe(group1CountInt);
    console.log('Total Count: ' + totalCountInt);

    // Save new cohort.
    const cohortName = await cohortBuildPage.saveCohortAs();
    await waitForText(page, 'Cohort Saved Successfully');
    console.log(`Cohort ${cohortName} created successfully.`);

    // Open Workspace, search for created cohort.
    await page.goto(workspaceDataUrl);
    await dataPage.waitForLoad();

    // Cohort can be opened from resource card link.
    let cohortCard = await DataResourceCard.findCard(page, cohortName);
    await cohortCard.clickResourceName();
    // Wait for page ready
    await cohortBuildPage.waitForLoad();
    await waitWhileLoading(page);

    await dataPage.openTab(LabelAlias.Data);
    await waitWhileLoading(page);

    // Duplicate cohort using Ellipsis menu.
    const origCardsCount = (await DataResourceCard.findAllCards(page)).length;
    cohortCard = await DataResourceCard.findCard(page, cohortName);
    const menu = await cohortCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.DUPLICATE, false);
    await waitWhileLoading(page);
    const newCardsCount = (await DataResourceCard.findAllCards(page)).length;
    // cards count increase by 1.
    expect(newCardsCount).toBe(origCardsCount + 1);

    // Delete duplicated cohort.
    let dialogContent = await dataPage.deleteCohort(`Duplicate of ${cohortName}`);
    expect(dialogContent).toContain(`Are you sure you want to delete Cohort: Duplicate of ${cohortName}?`);

    // Delete new cohort.
    dialogContent = await dataPage.deleteCohort(cohortName);
    expect(dialogContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

  });


});
