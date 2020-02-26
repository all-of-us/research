import {ElementHandle, JSHandle, Page} from 'puppeteer';
import {waitUntilTitleMatch} from '../driver/waitFuncs';
import RadioButton from './elements/radiobutton';
import WebComponent from "./elements/web-component";
import {findButton, findTextbox} from "./elements/xpath-finder";
import authenticatedpage from './mixin/authenticatedpage';
require('../driver/waitFuncs');

const configs = require('../resources/config.js');

const selectors = {
  pageTitleRegex: '/(Create|View) Workspaces?/i',
  // select - Synthetic DataSet
  dataSet: 'input[type=text][placeholder="Workspace Name"] + div[id] > select', // css
  // button CREATE WORKSPACE
  createWorkspaceButton: 'Create Workspace',
  // button CANCEL
  cancelButton: `Cancel`,
  // input textbox - Workspace Name
  workspaceName: 'Workspace Name',
};


export default class WorkspaceEditPage extends authenticatedpage {

  public async getCreateWorkspaceButton(): Promise<ElementHandle> {
    return findButton(this.puppeteerPage, selectors.createWorkspaceButton);
  }

  public async getCancelButton(): Promise<ElementHandle> {
    return findButton(this.puppeteerPage, selectors.cancelButton);
  }

  public async getWorkspaceNameTextbox(): Promise<ElementHandle> {
    return findTextbox(this.puppeteerPage, selectors.workspaceName);
  }

  public async getDataSetSelectOption(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitFor(selectors.dataSet, { visible: true });
  }

  public question1_researchPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Research purpose');
  }

  public question1_educationalPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Educational Purpose');
  }

  public question1_forProfitPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'For-Profit Purpose');
  }

  public question1_otherPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Other Purpose');
  }

  public question1_diseaseFocusedResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Disease-focused research');
  }

  public question1_populationHealth(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Population Health/Public Health Research');
  }

  public question1_methodsDevelopmentValidationStudy(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Methods development/validation study');
  }

  public question1_drugTherapeuticsDevelopmentResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Drug/Therapeutics Development Research');
  }

  public question1_researchControl(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Research Control');
  }

  public question1_geneticResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Genetic Research');
  }

  public question1_socialBehavioralResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Social/Behavioral Research');
  }

  public question1_ethicalLegalSocialImplicationsResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'Ethical, Legal, and Social Implications (ELSI) Research');
  }

  public question2_scientificQuestionsIntendToStudy(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'What are the specific scientific question(s) you intend to study');
  }

  public question2_scientificApproaches(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'What are the scientific approaches you plan to use for your study');
  }

  public question2_anticipatedFindings(): WebComponent {
    return new WebComponent(this.puppeteerPage, 'What are the anticipated findings from the study');
  }


  public radioButtonRequestReviewYes(): RadioButton {
    return new RadioButton(this.puppeteerPage, 'Yes, I would like to request a review');
  }

  public radioButtonRequestReviewNo(): RadioButton {
    return new RadioButton(this.puppeteerPage, 'No, I have no concerns at this time');
  }

  public radioButtonNotCenterOnUnrepresentedPopulation(): RadioButton {
    return new RadioButton(this.puppeteerPage, 'No, my study will not center on underrepresented populations.');
  }

  public async waitForReady(): Promise<WorkspaceEditPage> {
    await super.isLoaded(selectors.pageTitleRegex);
    return this;
  }

  /**
   * go directly to the URL of My Workspaces page
   */
  public async goURL(): Promise<WorkspaceEditPage> {
    await this.puppeteerPage.goto(configs.uiBaseUrl + configs.workspacesUrlPath, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.puppeteerPage.waitForXPath('//h3[normalize-space(text())="Workspaces"]', {visible: true});
    await this.waitForSpinner();
    return this;
  }

  /**
   * Find and Click "Create a New Workspace" button.
   */
  public async click_button_CreateNewWorkspace(): Promise<void> {
    const buttonSelectr = '//*[@role="button" and normalize-space(.)="Create a New Workspace"]';
    const button = await this.puppeteerPage.waitForXPath(buttonSelectr, { visible: true });
    await button.click();
  }


  /**
   * Find all visible Workspace names.
   */
  public async getAllWorkspaceNames(): Promise<any[]> {
    return await this.puppeteerPage.evaluate(() => {
      return Array.from(document.querySelectorAll(`*[data-test-id="workspace-card-name"]`)).map(a =>a.textContent)
    })
  }

  /**
   * Find workspace access level.
   * @param workspaceName
   */
  public async getWorkspaceAccessLevel(workspaceName: string) : Promise<JSHandle<string>> {
    const element = await this.puppeteerPage.waitForXPath(this.accessLevel(workspaceName), {visible: true});
    return await element.getProperty('innerText');
  }

  /**
   * Find element with specified workspace name on the page.
   * @param {string} workspaceName
   */
  public async getWorkspaceLink(workspaceName: string) : Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(this.workspaceLink(workspaceName));
  }

  public async waitUntilPageReady() {
    await waitUntilTitleMatch(this.puppeteerPage, 'Create Workspace');
    await this.waitForSpinner();
    await this.puppeteerPage.waitForXPath('//*[normalize-space(.)="Create a new Workspace"]', {visible: true});
    await this.getDataSetSelectOption();
    await this.getCreateWorkspaceButton();

  }

  public async getResearchPurposeExpandIcon(): Promise<ElementHandle[]> {
    return await this.puppeteerPage.$x('//*[child::*/*[contains(normalize-space(text()),"Research purpose")]]//i[contains(@class,"pi-angle-right")]')
  }

  private workspaceLink(workspaceName: string) {
    return `//*[@role='button'][./*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]`
  }

  private accessLevel(workspaceName: string) {
    return `//*[.//*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]/*[@data-test-id='workspace-access-level']`;
  }

}
