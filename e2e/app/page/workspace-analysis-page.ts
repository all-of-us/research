import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import Modal from 'app/component/modal';
import NewNotebookModal from 'app/component/new-notebook-modal';
import Button from 'app/element/button';
import Link from 'app/element/link';
import Textbox from 'app/element/textbox';
import {EllipsisMenuAction, Language, LinkText} from 'app/text-labels';
import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import AuthenticatedPage from './authenticated-page';

const PageTitle = 'View Notebooks';

export default class WorkspaceAnalysisPage extends AuthenticatedPage {

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
      console.log(`WorkspaceAnalysisPage isLoaded() encountered ${err}`);
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
    await menu.clickAction(EllipsisMenuAction.Delete, {waitForNav: false});

    const modal = new Modal(this.page);
    const modalContentText = await modal.getContent();
    const deleteButton = await Button.findByName(this.page, {normalizeSpace: LinkText.DeleteNotebook}, modal);
    await Promise.all([
      deleteButton.click(),
      modal.waitUntilClose(),
    ]);
    await waitWhileLoading(this.page);

    console.log(`Deleted Notebook "${notebookName}"`);
    return modalContentText;
  }

  /**
   * Create a new notebook.
   * - Click "Create a New Notebook" link in Analysis page.
   * - Fill in Notebook name and choose language in New Notebook modal.
   * - Wait for Jupyter notebook page load.
   * @param {string} notebookName New notebook name.
   * @param {Language} language Notebook language.
   */
  async createNotebook(notebookName: string, language: Language): Promise<void> {
    const link = await this.createNewNotebookLink();
    await link.click();
    const modal = new NewNotebookModal(this.page);
    await modal.waitForLoad();
    await modal.fillInModal(notebookName, language);

    // Waiting up to 15 minutes
    console.log(`Waiting for "${notebookName}" notebook server to start ...`);
    await waitWhileLoading(this.page, 60 * 15 * 1000);
  }

  async createNewNotebookLink(): Promise<Link> {
    return Link.findByName(this.page, {normalizeSpace: LinkText.CreateNewNotebook});
  }

  async renameNotebook(notebookName: string, newNotebookName: string): Promise<string> {
    const notebookCard = await DataResourceCard.findCard(this.page, notebookName);
    const menu = notebookCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Rename, {waitForNav: false});
    const modal = new Modal(this.page);
    const modalTextContents = await modal.getContent();
    const newNameInput = new Textbox(this.page, `${modal.getXpath()}//*[@id="new-name"]`);
    await newNameInput.type(newNotebookName);
    await modal.clickButton(LinkText.RenameNotebook);
    await modal.waitUntilClose();
    await waitWhileLoading(this.page);
    console.log(`Notebook "${notebookName}" renamed to "${newNotebookName}"`);
    return modalTextContents;
  }

  /**
   * Duplicate notebook using Ellipsis menu in Workspace Analysis page.
   * @param {string} notebookName The notebook name to clone from.
   */
  async duplicateNotebook(notebookName: string): Promise<void> {
    const resourceCard = new DataResourceCard(this.page);
    const notebookCard = await resourceCard.findCard(notebookName, CardType.Notebook);
    const menu = notebookCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Duplicate, {waitForNav: false});
    await waitWhileLoading(this.page);
  }

}
