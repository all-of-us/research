import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import DataResourceCard from 'app/component/data-resource-card';
import Dialog from 'app/component/dialog';
import Button from 'app/element/button';
import {EllipsisMenuAction} from 'app/page-identifiers';
import AuthenticatedPage from './authenticated-page';

const PageTitle = 'View Notebooks';

export default class AnalysisPage extends AuthenticatedPage {

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
    } catch (err) {
      console.log(`AnalysisPage isLoaded() encountered ${err}`);
      return false;
    }
  }

   /**
    * Delete notebook thru Ellipsis menu located inside the Notebook resource card.
    * @param {string} notebookName
    */
  async deleteNotebook(notebookName: string): Promise<string> {
    const resourceCard = await DataResourceCard.findCard(this.page, notebookName);
    const menu = resourceCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Delete, false);

    const dialog = new Dialog(this.page);
    const dialogContentText = await dialog.getContent();
    const deleteButton = await Button.findByName(this.page, {normalizeSpace: 'Delete Notebook'}, dialog);
    await Promise.all([
      deleteButton.click(),
      dialog.waitUntilDialogIsClosed(),
    ]);
    await waitWhileLoading(this.page);

    console.log(`Deleted Notebook "${notebookName}"`);
    return dialogContentText;
  }

}
