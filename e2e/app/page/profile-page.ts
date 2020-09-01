import {Page, WaitForSelectorOptions} from 'puppeteer';
import Textbox from 'app/element/textbox';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle, waitForUrl} from 'utils/waits-utils';
import Button from '../element/button';
import Textarea from '../element/textarea';
import BaseElement from '../element/base-element';

export const PageTitle = 'Profile';

export const LabelAlias = {
  ResearchBackground: 'Your research background, experience and research interests',
  SaveProfile: 'Save Profile',
};


export const DataTestIdAlias = {
  FirstName: 'givenName',
  LastName: 'familyName',
  ProfessionalUrl: 'professionalUrl',
  Address1: 'streetAddress1',
  Address2: 'streetAddress2',
  City: 'city',
  State: 'state',
  Zip: 'zipCode',
  Country: 'country',
};

export default class ProfilePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForUrl(this.page, '/profile'),
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (err) {
      console.log(`ProfilePage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async getFirstNameInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.FirstName});
  }

  async getLastNameInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.LastName});
  }

  async getProfessionalUrlInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.ProfessionalUrl});
  }

  async getResearchBackgroundTextarea(): Promise<Textarea> {
    return Textarea.findByName(this.page, {normalizeSpace: LabelAlias.ResearchBackground});
  }

  async getAddress1Input(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.Address1});
  }

  async getAddress2Input(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.Address2});
  }

  async getCityInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.City});
  }

  async getStateInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.State});
  }

  async getZipCodeInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.Zip});
  }

  async getCountryInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.Country});
  }

  async getSaveProfileButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LabelAlias.SaveProfile});
  }

  // TODO generalize - promote to a Div Element?
  async getDivWithText(text: string, options?: WaitForSelectorOptions): Promise<BaseElement> {
    const selector = `//div[normalize-space(text())="${text}"]`;
    const handle = await this.page.waitForXPath(selector, options);
    return BaseElement.asBaseElement(this.page, handle);
  }

  async expectResearchPurposeErrorMissing(): Promise<void> {
    const text = 'Current Research can\'t be blank';
    await this.getDivWithText(text, {hidden: true});
  }

  async expectResearchPurposeErrorPresent(): Promise<void> {
    const text = 'Current Research can\'t be blank';
    await this.getDivWithText(text, {visible: true});
  }
}
