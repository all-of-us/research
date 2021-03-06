import RadioButton from 'app/element/radiobutton';
import Textbox from 'app/element/textbox';
import { Page } from 'puppeteer';
import { Language, LinkText } from 'app/text-labels';
import Modal from './modal';
import { waitForText } from 'utils/waits-utils';

const title = 'Export Dataset';

export default class ExportToNotebookModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { xpath: this.getXpath() });
    await this.getNotebookNameInput().asElementHandle();
    return true;
  }

  getNotebookNameInput(): Textbox {
    const selector = `${this.getXpath()}//*[@data-test-id="notebook-name-input"]`;
    return new Textbox(this.page, selector);
  }

  getPythonRadioButton(): RadioButton {
    const selector = `${this.getXpath()}//label[contains(normalize-space(),"Python")]//input[@type="radio"]`;
    return new RadioButton(this.page, selector);
  }

  getRRadioButton(): RadioButton {
    const selector = `${this.getXpath()}//label[contains(normalize-space(),"R")]//input[@type="radio"]`;
    return new RadioButton(this.page, selector);
  }

  async enterNotebookName(notebookName: string): Promise<Textbox> {
    const notebookNameInput = this.getNotebookNameInput();
    return notebookNameInput.type(notebookName);
  }

  async pickLanguage(language: Language = Language.Python): Promise<void> {
    const radio = language === Language.Python ? this.getPythonRadioButton() : this.getRRadioButton();
    return radio.select();
  }

  async clickExportButton(): Promise<void> {
    return this.clickButton(LinkText.Export, { waitForClose: true });
  }

  /**
   * Fill out Export Notebook modal to create a new notebook:
   * - Type notebook name.
   * - Select notebook language.
   * - Click "Export and Open" button.
   * @param {string} notebookName Notebook name.
   * @param {Language} language Notebook programming language. Default value is Python.
   */
  async fillInModal(notebookName: string, language: Language = Language.Python): Promise<void> {
    await this.enterNotebookName(notebookName);
    await this.pickLanguage(language);
    return await this.clickExportButton();
  }
}
