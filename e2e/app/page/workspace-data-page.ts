import DataResourceCard from 'app/component/data-resource-card';
import ClrIconLink from 'app/element/clr-icon-link';
import {Option, Language, ResourceCard} from 'app/text-labels';
import {ElementHandle, Page} from 'puppeteer';
import {makeRandomName} from 'utils/str-utils';
import {waitForDocumentTitle, waitWhileLoading} from 'utils/waits-utils';
import CohortActionsPage from './cohort-actions-page';
import CohortBuildPage from './cohort-build-page';
import {Visits} from './criteria-search-page';
import ConceptSetSearchPage from './conceptset-search-page';
import DatasetBuildPage from './dataset-build-page';
import NotebookPage from './notebook-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import WorkspaceBase from './workspace-base';

const PageTitle = 'Data Page';

export default class WorkspaceDataPage extends WorkspaceBase {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page)
    ]);
    await this.imgDiagramLoaded();
    return true;
  }

  async imgDiagramLoaded(): Promise<ElementHandle[]> {
    return Promise.all<ElementHandle, ElementHandle>([
      this.page.waitForXPath('//img[@src="/assets/images/dataset-diagram.svg"]', {visible: true}),
      this.page.waitForXPath('//img[@src="/assets/images/cohort-diagram.svg"]', {visible: true}),
    ]);
  }

  async getAddDatasetButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: 'Datasets', iconShape: 'plus-circle'});
  }

  async getAddCohortsButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: 'Cohorts', iconShape: 'plus-circle'});
  }

  // Click Add Datasets button.
  async clickAddDatasetButton(): Promise<DatasetBuildPage> {
    const addDatasetButton = await this.getAddDatasetButton();
    await addDatasetButton.clickAndWait();
    await waitWhileLoading(this.page);

    // wait for Dataset Build page load and ready.
    const datasetPage = new DatasetBuildPage(this.page);
    await datasetPage.waitForLoad();
    return datasetPage;
  }

  /**
   * Export Dataset to notebook thru the Ellipsis menu located inside the Dataset Resource card.
   * @param {string} datasetName Dataset name.
   * @param {string} notebookName Notebook name.
   */
  async exportToNotebook(datasetName: string, notebookName: string): Promise<void> {
    const resourceCard = new DataResourceCard(this.page);
    const datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    await datasetCard.selectSnowmanMenu(Option.exportToNotebook, {waitForNav: false});
    console.log(`Exported Dataset "${datasetName}" to notebook "${notebookName}"`);
  }

  async findCohortCard(cohortName?: string): Promise<DataResourceCard> {
    await this.openCohortsSubtab();
    if (cohortName === undefined) {
      // if cohort name isn't specified, find any existing cohort.
      return DataResourceCard.findAnyCard(this.page);
    } else {
      // find cohort matching name.
      return DataResourceCard.findCard(this.page, cohortName, 2000);
    }
  }

  /**
   * Create a simple Cohort from Out-Patient Visit criteria.
   * @param {string} cohortName New Cohort name.
   */
  async createCohort(cohortName?: string): Promise<DataResourceCard> {
    await this.getAddCohortsButton().then((butn) => butn.clickAndWait());
    // Land on Build Cohort page.
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const searchPage = await group1.includeVisits();
    await searchPage.addVisits([Visits.OutpatientVisit]);
    // Open selection list and click Save Criteria button
    await group1.viewAndSaveCriteria();
    await waitWhileLoading(this.page);
    await cohortBuildPage.getTotalCount();
    const name = (cohortName === undefined) ? makeRandomName() : cohortName;
    await cohortBuildPage.saveCohortAs(name);
    await (new CohortActionsPage(this.page)).waitForLoad();
    return this.findCohortCard(name);
  }

  /**
   * Click Add Dataset button.
   * Click Add Concept Set button.
   * Click Domain card.
   * @param {Domain} domain
   */
  async openConceptSetSearch(): Promise<ConceptSetSearchPage> {
    // Click Add Datasets button.
    const datasetBuildPage = await this.clickAddDatasetButton();

    // Click Add Concept Sets button.
    return await datasetBuildPage.clickAddConceptSetsButton();
  }

  /**
   * Create a new empty notebook. Wait for Notebook server start.
   * - Open Analysis tab.
   * - Click "Create a New Notebook" link.
   * - Fill out New Notebook modal.
   * @param notebookName The notebook name.
   * @param {Language} lang The notebook language.
   */
  async createNotebook(notebookName: string, lang: Language = Language.Python): Promise<NotebookPage> {
    await this.openAnalysisPage();
    const analysisPage = new WorkspaceAnalysisPage(this.page);
    await analysisPage.waitForLoad();
    return analysisPage.createNotebook(notebookName, lang);
  }

}
