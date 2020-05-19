import {Frame, Page} from 'puppeteer';
import {defaultFieldValues} from 'resources/data/user-registration-data';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import IconLink from 'app/element/icon-link';
import SelectMenu from 'app/component/select-menu';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import BasePage from 'app/page/base-page';
import {config} from 'resources/workbench-config';
import {waitForPageContainsText} from 'utils/wait-utils';
import IFrame from 'app/component/iframe';
const faker = require('faker/locale/en_US');

export const INSTITUTION_VALUE = {
  VANDERBILT: 'Vanderbilt University Medical Center',
  BROAD: 'Broad Institute',
  VERILY: 'Verily LLC',
  NATIONAL_INSTITUTE_HEALTH: 'National Institute of Health',
  WONDROS: 'Wondros'
};

export const INSTITUTION_ROLE_VALUE = {
  EARLY_CAREER_TENURE_TRACK_RESEARCHER: 'Early career tenure-track researcher',
  UNDERGRADUATE_STUDENT: 'Undergraduate (Bachelor level) student',
  INDUSTRY: 'Industry',
  RESEARCH_ASSISTANT: 'Research Assistant (pre-doctoral)',
  RESEARCH_ASSOCIATE: 'Research associate (post-doctoral; early/mid career)',
  SENIOR_RESEARCHER: 'Senior Researcher (PI/Team Lead, senior scientist)',
};

export const EDUCATION_LEVEL_VALUE = {
  DOCTORATE: 'Doctorate',
  // other option values here
};

export const LABEL_ALIAS = {
  READ_UNDERSTAND_PRIVACY_STATEMENT: 'I have read, understand, and agree to the All of Us Program Privacy Statement.',
  READ_UNDERSTAND_TERMS_OF_USE: 'I have read, understand, and agree to the Terms described above.',
  INSTITUTION_NAME: 'Institution Name',
  ARE_YOU_AFFILIATED: 'Are you affiliated with an Academic Research Institution',
  RESEARCH_BACKGROUND: 'Your research background, experience, and research interests',
  EDUCATION_LEVEL: 'Highest Level of Education Completed', // Highest Level of Education Completed
  YEAR_OF_BIRTH: 'Year of Birth',
  INSTITUTION_EMAIL: 'Your institutional email address',
};

export default class CreateAccountPage extends BasePage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForPageContainsText(this.puppeteerPage, 'Sign In'),
      ]);
      return true;
    } catch (err) {
      console.log(`CreateAccountPage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async getSubmitButton(): Promise<Button> {
    return await Button.forLabel({puppeteerPage: this.puppeteerPage}, {text: 'Submit'});
  }

  async getNextButton(): Promise<Button> {
    return await Button.forLabel({puppeteerPage: this.puppeteerPage}, {text: 'Next'});
  }

  async agreementLoaded(): Promise<boolean> {
    const iframe = await IFrame.find(this.puppeteerPage, 'terms of service agreement')
    const bodyHandle = await iframe.$('body')
    return await iframe.evaluate(body => body.scrollHeight > 0, bodyHandle)
  }

  async readAgreement(): Promise<Frame> {
    const iframe = await IFrame.find(this.puppeteerPage, 'terms of service agreement')
    const bodyHandle = await iframe.$('body')
    await iframe.evaluate(body =>  body.scrollTo(0, body.scrollHeight), bodyHandle)
    return iframe;
  }

  async getPrivacyStatementCheckbox(): Promise<Checkbox> {
    return await Checkbox.forLabel({puppeteerPage: this.puppeteerPage}, {normalizeSpace: LABEL_ALIAS.READ_UNDERSTAND_PRIVACY_STATEMENT});
  }

  async getTermsOfUseCheckbox(): Promise<Checkbox> {
    return await Checkbox.forLabel({puppeteerPage: this.puppeteerPage}, {normalizeSpace: LABEL_ALIAS.READ_UNDERSTAND_TERMS_OF_USE});
  }

  async getInstitutionNameInput(): Promise<Textbox> {
    return await Textbox.forLabel({puppeteerPage: this.puppeteerPage}, {text: LABEL_ALIAS.INSTITUTION_NAME});
  }

  async getResearchBackgroundTextarea(): Promise<Textarea> {
    return await Textarea.forLabel({puppeteerPage: this.puppeteerPage}, {normalizeSpace: LABEL_ALIAS.RESEARCH_BACKGROUND});
  }

  async getUsernameDomain(): Promise<unknown> {
    const elem = await this.puppeteerPage.waitForXPath('//*[./input[@id="username"]]/i');
    return await (await elem.getProperty('innerText')).jsonValue();
  }

  async fillInFormFields(fields: { label: string; value: string; }[]): Promise<string> {
    let newUserName;
    for (const field of fields) {
      const textbox = await Textbox.forLabel({puppeteerPage: this.puppeteerPage}, {text: field.label});
      await textbox.type(field.value);
      await textbox.tabKey();
      if (field.label === 'New Username') {
        await IconLink.forLabel({puppeteerPage: this.puppeteerPage}, {text: field.label}, 'success-standard');
        newUserName = field.value; // store new username for return
      }
    }
    return newUserName;
  }

  // select Institution Affiliation from a dropdown
  async selectInstitution(selectTextValue: string) {
    const dropdown = new SelectMenu({puppeteerPage: this.puppeteerPage});
    await dropdown.select(selectTextValue);
  }

  async getInstitutionValue() {
    const dropdown = new SelectMenu({puppeteerPage: this.puppeteerPage});
    return await dropdown.getSelectedValue();
  }

  // select Education Level from a dropdown
  async selectEducationLevel(selectTextValue: string) {
    const dropdown = new SelectMenu({puppeteerPage: this.puppeteerPage}, {label: LABEL_ALIAS.EDUCATION_LEVEL, nodeLevel: 2});
    await dropdown.select(selectTextValue);
  }

  // select Year of Birth from a dropdown
  async selectYearOfBirth(year: string) {
    const dropdown = new SelectMenu({puppeteerPage: this.puppeteerPage}, {label: LABEL_ALIAS.YEAR_OF_BIRTH, nodeLevel: 2});
    await dropdown.select(year);
  }

  // Combined steps to make test code cleaner and shorter

  // Step 1: Fill out institution affiliation details
  async fillOutInstitution() {
    const institutionSelect = new SelectMenu({puppeteerPage: this.puppeteerPage}, {label: 'Select your institution', nodeLevel: 2});
    await institutionSelect.select(INSTITUTION_VALUE.BROAD);
    const emailAddressTextbox = await Textbox.forLabel({puppeteerPage: this.puppeteerPage},
       {contains: LABEL_ALIAS.INSTITUTION_EMAIL, ancestorNodeLevel: 2});
    await emailAddressTextbox.type(config.broadInstitutionContactEmail);
    await emailAddressTextbox.tabKey(); // tab out to start email validation
    await IconLink.forLabel({puppeteerPage: this.puppeteerPage},
       {contains: LABEL_ALIAS.INSTITUTION_EMAIL, ancestorNodeLevel: 2}, 'success-standard');
    const roleSelect = new SelectMenu({puppeteerPage: this.puppeteerPage}, {label: 'describes your role', nodeLevel: 2});
    await roleSelect.select(INSTITUTION_ROLE_VALUE.UNDERGRADUATE_STUDENT);
  }

  // Step 3: Accepting Terms of Use and Privacy statement.
  async acceptTermsOfUseAgreement() {
    await this.getPrivacyStatementCheckbox();
    await this.getTermsOfUseCheckbox();
    await this.getNextButton();

    await this.readAgreement();

    // check by click on label works
    await (await this.getPrivacyStatementCheckbox()).check();
    await (await this.getTermsOfUseCheckbox()).check();
  }

  // Step 3: Fill out user information with default values
  async fillOutUserInformation() {
    const newUserName = await this.fillInFormFields(defaultFieldValues);
    await (await this.getResearchBackgroundTextarea()).type(faker.lorem.word());
    return newUserName;
  }

  // Step 4: Fill out demographic survey information with default values
  async fillOutDemographicSurvey() {
    await waitForPageContainsText(this.puppeteerPage, 'Optional Demographics Survey');
    // Find and check on all checkboxes with same label: Prefer not to answer
    const targetXpath = '//*[normalize-space(text())="Prefer not to answer"]/ancestor::node()[1]/input[@type="checkbox"]';
    await this.puppeteerPage.waitForXPath(targetXpath, { visible: true });
    const checkboxes = await this.puppeteerPage.$x(targetXpath);
    for (const ck of checkboxes) {
      await ck.click();
    }
    await this.selectYearOfBirth('1955');
    await this.selectEducationLevel(EDUCATION_LEVEL_VALUE.DOCTORATE);
  }

}
