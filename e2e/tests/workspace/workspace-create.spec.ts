import Link from 'app/element/link';
import DataPage from 'app/page/data-page';
import WorkspacesPage, {FieldSelector} from 'app/page/workspaces-page';
import {signIn, performActions} from 'utils/test-utils';
import Button from 'app/element/button';
import * as testData from 'resources/data/workspace-data';
import {makeWorkspaceName} from 'utils/str-utils';

describe('Creating new workspaces', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Create workspace - NO request for review', async () => {
    const newWorkspaceName = makeWorkspaceName();
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    // create workspace with "No Review Requested" radiobutton selected
    const modalTextContent = await workspacesPage.createWorkspace(newWorkspaceName);

    // Pick out few sentenses to verify
    expect(modalTextContent).toContain('Create Workspace');
    expect(modalTextContent).toContain('Primary purpose of your project (Question 1)Summary of research purpose (Question 2)Population of interest (Question 5)');
    expect(modalTextContent).toContain('You can also make changes to your answers after you create your workspace.');

    await verifyWorkspaceLinkOnDataPage(newWorkspaceName);
  });

  test('User can create a workspace using all inputs', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const createNewWorkspaceButton  = await Button.findByName(page, FieldSelector.CreateNewWorkspaceButton.textOption );
    await createNewWorkspaceButton.clickAndWait();

    // fill out new workspace name
    const newWorkspaceName = await workspacesPage.fillOutWorkspaceName();

    // select the default Synthetic Dataset
    await workspacesPage.selectDataset();

    // select Billing Account
    await workspacesPage.selectBillingAccount('Use All of Us free credits');

    // fill out question #1 - What is the primary purpose of your project?
    await workspacesPage.expandResearchPurposeGroup(true); // First, expand accordion: "Research purpose"
    await performActions(page, testData.defaultAnswersPrimaryPurpose);

    // fill out question #2 - Please provide a summary of your research purpose by responding to the questions.
    await performActions(page, testData.defaultAnswersResearchPurposeSummary);

    // fill out question #3 - The All of Us Research Program encourages researchers to disseminate their research findings...
    await performActions(page, testData.defaultAnswersDisseminateResearchFindings);

    // fill out question #4 - select all of the statements below that describe the outcomes you anticipate from your research.
    await performActions(page, testData.defaultAnswersAnticipatedOutcomesFromResearch);

    // fill out question #5 - Population interest
    await performActions(page, testData.defaultAnswersPopulationOfInterest);

    // fill out question #6 - Request for Review of Research Purpose Description
    // -- No Review Required
    await performActions(page, testData.defaultAnswersRequestForReview);

    const finishButton = await workspacesPage.getCreateWorkspaceButton();
    await finishButton.waitUntilEnabled();
    await workspacesPage.clickCreateFinishButton(finishButton);

    await verifyWorkspaceLinkOnDataPage(newWorkspaceName);
  });

  // helper function to check visible workspace link on Data page
  async function verifyWorkspaceLinkOnDataPage(workspaceName: string) {
    const dataPage = new DataPage(page);
    await dataPage.waitForLoad();

    const workspaceLink = new Link(page, `//a[text()='${workspaceName}']`);
    await workspaceLink.waitForXPath({visible: true});
    expect(await workspaceLink.isVisible()).toBe(true);
  }

});
