import {Page} from 'puppeteer';
import EllipsisMenu from 'app/component/ellipsis-menu';
import {EllipsisMenuAction} from 'app/page-identifiers';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import ClrIconLink from 'app/element/clr-icon-link';
import DataResourceCard from 'app/component/data-resource-card';
import Dialog from 'app/component/dialog';
import Button from 'app/element/button';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import {ElementType} from 'app/xpath-options';
import CohortBuildPage from './cohort-build-page';

export enum LabelAlias {
  Data = 'Data',
  Analysis = 'Analysis',
  About = 'About',
  Cohorts = 'Cohorts',
  Datasets = 'Datasets',
  CohortReviews = 'Cohort Reviews',
  ConceptSets = 'Concept Sets',
  ShowAll = 'Show All',
}

const PageTitle = 'Data Page';

export default class DataPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`DataPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async selectWorkspaceAction(action: EllipsisMenuAction): Promise<void> {
    const ellipsisMenu = new EllipsisMenu(this.page, './/*[@data-test-id="workspace-menu-button"]');
    return ellipsisMenu.clickAction(action);
  }

  /**
   * Select DATA, ANALYSIS or ABOUT page tab.
   * @param tabName
   */
  async openTab(tabName: LabelAlias): Promise<void> {
    const selector = `//*[(@aria-selected | @tabindex) and @role="button" and text()="${tabName}"]`;
    const tab = await this.page.waitForXPath(selector, {visible: true});
    return tab.click();
  }

  async getAddDatasetButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: LabelAlias.Datasets, iconShape: 'plus-circle'});
  }

  async getAddCohortsButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: LabelAlias.Cohorts, iconShape: 'plus-circle'});
  }

  /**
   * Delete cohort by look up its name using Ellipsis menu.
   * @param {string} cohortName
   */
  async deleteCohort(cohortName: string): Promise<string> {
    const cohortCard = await DataResourceCard.findCard(this.page, cohortName);
    const menu = cohortCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.DELETE, false);
    const dialogContent = await (new CohortBuildPage(this.page)).deleteConfirmationDialog();
    console.log(`Cohort "${cohortName}" deleted.`);
    return dialogContent;
  }

  /**
   * Delete Dataset thru Ellipsis menu located inside the Dataset Resource card.
   * @param {string} datasetName
   */
  async deleteDataset(datasetName: string): Promise<string> {
    const dataSetCard = await DataResourceCard.findCard(this.page, datasetName);
    const menu = dataSetCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.DELETE, false);

    const dialog = new Dialog(this.page);
    const dialogContentText = await dialog.getContent();
    const deleteButton = await Button.findByName(this.page, {type: ElementType.Button, normalizeSpace: 'Delete Dataset'}, dialog);
    await Promise.all([
      deleteButton.click(),
      dialog.waitUntilDialogIsClosed(),
    ]);
    await waitWhileLoading(this.page);

    console.log(`Dataset "${datasetName}" deleted.`);
    return dialogContentText;
  }

  /**
   * Rename Dataset thru the Ellipsis menu located inside the Dataset Resource card.
   * @param {string} datasetName
   * @param {string} newDatasetName
   */
  async renameDataset(datasetName: string, newDatasetName: string): Promise<void> {
    const card = await DataResourceCard.findCard(this.page, datasetName);
    const menu = card.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.RenameDataset, false);

    const dialog = new Dialog(this.page);

    const newNameTextbox = new Textbox(this.page, '//*[@id="new-name"]');
    await newNameTextbox.type(newDatasetName);

    const descriptionTextarea = await Textarea.findByName(this.page, {containsText: 'Description:'}, dialog);
    await descriptionTextarea.type('Puppeteer automation rename dataset.');

    const renameButton = await Button.findByName(this.page, {normalizeSpace: 'Rename Dataset'}, dialog);
    await Promise.all([
      renameButton.click(),
      dialog.waitUntilDialogIsClosed(120000), // Rename Dataset can be very slow sometimes.
    ]);
    return waitWhileLoading(this.page);
  }

}
