import {Page} from 'puppeteer';
import Button from 'app/element/button';
import {PageUrl} from 'app/page-identifiers';
import WorkspaceEditPage, {FIELD as EDIT_FIELD} from 'app/page/workspace-edit-page';
import {makeWorkspaceName} from 'utils/str-utils';
import RadioButton from 'app/element/radiobutton';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle, waitForPageContainsText} from 'utils/wait-utils';
const faker = require('faker/locale/en_US');

export const PAGE = {
  TITLE: 'View Workspace',
};

export const LABEL_ALIAS = {
  CREATE_A_NEW_WORKSPACE: 'Create a New Workspace',
};

export const FIELD = {
  createNewWorkspaceButton: {
    textOption: {
      normalizeSpace: LABEL_ALIAS.CREATE_A_NEW_WORKSPACE
    }
  }
};

export default class WorkspacesPage extends WorkspaceEditPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.puppeteerPage, PAGE.TITLE),
        this.puppeteerPage.waitForXPath('//a[text()="Workspaces"]', {visible: true}),
        this.puppeteerPage.waitForXPath('//h3[normalize-space(text())="Workspaces"]', {visible: true}),  // Texts above Filter By Select
        waitWhileLoading(this.puppeteerPage),
      ]);
      return true;
    } catch (err) {
      console.log(`WorkspacesPage isLoaded() encountered ${err}`);
      return false;
    }
  }

  /**
   * Load 'Your Workspaces' page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(PageUrl.WORKSPACES);
    return this;
  }

  // tests helpers: combined a number of steps in one function

 /**
  * Perform following steps:
  *
  * 1: go to My Workspaces page
  * 2: click Create New Workspace link (button)
  * 3: wait until Edit page is loaded and ready
  * 4: return
  */
  async clickCreateNewWorkspace(): Promise<WorkspaceEditPage> {
    const link = await Button.forLabel({puppeteerPage: this.puppeteerPage}, FIELD.createNewWorkspaceButton.textOption );
    await link.clickAndWait();
    const workspaceEdit = new WorkspaceEditPage(this.puppeteerPage);
    await workspaceEdit.waitForLoad();
    return workspaceEdit;
  }

  /**
   * Create a simple and basic new workspace end-to-end.
   */
  async createWorkspace(workspaceName: string, billingAccount: string, reviewRequest: boolean = false): Promise<string> {

    const editPage = await this.clickCreateNewWorkspace();
    // wait for Billing Account default selected value
    await waitForPageContainsText(this.puppeteerPage, 'Use All of Us free credits');

    await (await editPage.getWorkspaceNameTextbox()).type(workspaceName);
    await (await editPage.getWorkspaceNameTextbox()).tabKey();

    // select Synthetic Data Set 2
    await editPage.selectDataSet('2');

    // select Billing Account
    await editPage.selectBillingAccount(billingAccount);

    // 1. What is the primary purpose of your project?
    // check Educational Purpose checkbox
    const educationPurpose = editPage.question1_educationalPurpose();
    await (await educationPurpose.asCheckBox()).check();

    // 2. Please provide a summary of your research purpose by responding to the questions below.
    const scientificQuestions = editPage.question2_scientificQuestionsIntendToStudy();
    await (await scientificQuestions.asTextArea()).paste(faker.lorem.paragraph());

    const scientificApproaches = editPage.question2_scientificApproaches();
    await (await scientificApproaches.asTextArea()).paste(faker.lorem.paragraph());

    const anticipatedFindings = editPage.question2_anticipatedFindings();
    await (await anticipatedFindings.asTextArea()).paste(faker.lorem.paragraph());

    // 3. The All of Us Research Program encourages researchers to disseminate ....
    const publicationInJournal = editPage.publicationInJournal();
    await (await publicationInJournal.asCheckBox()).check();

    // 4. The All of Us Research Program would like to understand how ....
    const increaseWellness = editPage.increaseWellnessResilience();
    await (await increaseWellness.asCheckBox()).check();

    // 5. Population of interest: use default values. Using default value
    const noRadiobutton = await RadioButton.forLabel({puppeteerPage: this.puppeteerPage},
       EDIT_FIELD.POPULATION_OF_INTEREST.noUnderrepresentedPopulationRadiobutton.textOption);
    await noRadiobutton.select();

    // 6. Request for Review of Research Purpose Description. Using default value
    await editPage.requestForReviewRadiobutton(reviewRequest);

    // click CREATE WORKSPACE button
    const createButton = await this.getCreateWorkspaceButton();
    await createButton.waitUntilEnabled();
    return await editPage.clickCreateFinishButton(createButton);
  }

  /**
   * Type in new workspace name.
   * @return {string} new workspace name
   */
  async fillOutWorkspaceName(): Promise<string> {
    const newWorkspaceName = makeWorkspaceName();
    await (await this.getWorkspaceNameTextbox()).type(newWorkspaceName);
    await (await this.getWorkspaceNameTextbox()).tabKey();
    return newWorkspaceName;
  }

}
