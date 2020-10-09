import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import Button from 'app/element/button';
import {LinkText} from 'app/text-labels';
import ConceptDomainCard, {Domain} from 'app/component/concept-domain-card';
import Link from 'app/element/link';
import AuthenticatedPage from './authenticated-page';
import ConceptSetPage from './conceptset-page';
import ConceptSetSearchPage from './conceptset-search-page';
import DatasetBuildPage from './dataset-build-page';

const PageTitle = 'Concept Set Actions';

export default class ConceptSetActionsPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page)
    ]);
    await Promise.all([
      this.getCreateAnotherConceptSetButton(),
      this.getCreateDatasetButton()
    ]);
    return true;
  }

  async clickCreateAnotherConceptSetButton(): Promise<void> {
    const button = await this.getCreateAnotherConceptSetButton();
    return button.clickAndWait();
  }

  async clickCreateDatasetButton(): Promise<DatasetBuildPage> {
    const button = await this.getCreateDatasetButton();
    await button.clickAndWait();
    const datasetBuildPage = new DatasetBuildPage(this.page);
    return datasetBuildPage.waitForLoad();
  }

  async getCreateAnotherConceptSetButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.CreateAnotherConceptSet});
  }

  async getCreateDatasetButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.CreateDataset});
  }

  async openConceptSet(conceptName: string): Promise<ConceptSetPage> {
    const link = new Link(this.page, `//a[text()="${conceptName}"]`);
    await link.click();
    const conceptSetPage = new ConceptSetPage(this.page);
    await conceptSetPage.waitForLoad();
    return conceptSetPage;
  }

  /**
   * Click Create another Concept Set button.
   * Click Domain card.
   * @param {Domain} domain
   */
  async openConceptSearch(domain: Domain): Promise<ConceptSetSearchPage> {
    await this.clickCreateAnotherConceptSetButton();

    const procedures = await ConceptDomainCard.findDomainCard(this.page, domain);
    await procedures.clickSelectConceptButton();

    const conceptSearchPage = new ConceptSetSearchPage(this.page);
    await conceptSearchPage.waitForLoad();
    return conceptSearchPage;
  }

}
