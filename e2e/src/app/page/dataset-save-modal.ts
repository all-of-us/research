import {Page} from 'puppeteer';
import Modal from 'src/app/component/modal';
import {makeRandomName} from 'utils/str-utils';
import RadioButton from 'src/app/element/radiobutton';
import Textbox from 'src/app/element/textbox';
import {Language, LinkText} from 'src/app/text-labels';
import Button from 'src/app/element/button';
import {waitUntilChanged} from 'utils/element-utils';
import {waitForPropertyExists, waitWhileLoading} from 'utils/waits-utils';
import Textarea from 'src/app/element/textarea';

export default class DatasetSaveModal extends Modal {

  constructor(page: Page) {
    super(page);
  }

  /**
   * Handle Save or Update dialog.
   * @param notebookOpts {}
   * @param {boolean} isUpdate If true, click Update button. If False, click Save button.
   *
   * <pre>
   * {boolean} exportToNotebook If True, select Export To Notebook checkbox.
   * {string} notebookName New notebook name to be created or select an existing notebook.
   * {String} lang Notebook programming language.
   * </pre>
   */
  async saveDataset(notebookOpts: {exportToNotebook?: boolean, notebookName?: string, lang?: Language} = {},
                    isUpdate: boolean = false): Promise<string> {

    const {exportToNotebook = false, notebookName, lang = Language.Python} = notebookOpts;
    const newDatasetName = makeRandomName();

    const nameTextbox = await this.waitForTextbox('Dataset Name');
    await nameTextbox.clearTextInput();
    await nameTextbox.type(newDatasetName);

    // Export to Notebook checkbox is checked by default
    const exportCheckbox = await this.waitForCheckbox('Export to notebook');

    if (exportToNotebook) {
      // Export to notebook
      const notebookNameTextbox = new Textbox(this.page, `${this.getXpath()}//*[@data-test-id="notebook-name-input"]`);
      await notebookNameTextbox.type(notebookName);
      console.log(`Notebook language: ` + lang);
      const radioBtn = await RadioButton.findByName(this.page, {name: lang, ancestorLevel: 0}, this);
      await radioBtn.select();
    } else {
      // Not export to notebook
      await exportCheckbox.unCheck();
    }
    await waitWhileLoading(this.page);

    if (isUpdate) {
      await this.clickButton(LinkText.Update, {waitForClose: true, waitForNav: true});
    } else {
      await this.clickButton(LinkText.Save, {waitForClose: true, waitForNav: true});
    }
    await waitWhileLoading(this.page);

    if (isUpdate) {
      console.log(`Updated Dataset "${newDatasetName}"`);
    } else {
      console.log(`Created Dataset "${newDatasetName}"`);
    }
    if (exportToNotebook) {
      console.log(`Created Notebook "${notebookName}"`);
    }
    return newDatasetName;
  }

  /**
   * Click 'See Code Preview' button. Returns code contents.
   */
  async previewCode(): Promise<string> {
    // Click 'See Code Preview' button.
    const previewButton = await Button.findByName(this.page, {name: LinkText.SeeCodePreview}, this);
    await previewButton.click();
    await waitUntilChanged(this.page, await previewButton.asElementHandle());

    // Find Preview Code
    const selector = `${this.getXpath()}//textarea[@data-test-id="code-text-box"]`;
    const previewTextArea = new Textarea(this.page, selector);
    // Has 'disabled' property
    await waitForPropertyExists(this.page, selector, 'disabled');
    return previewTextArea.getTextContent();
  }

}
