import Container from 'app/container';
import {Page} from 'puppeteer';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import Checkbox from 'app/element/checkbox';
import {savePageToFile, takeScreenshot} from '../../utils/save-file-utils';

export enum ButtonLabel {
  Confirm = 'Confirm',
  KeepEditing = 'Keep Editing',
  Cancel = 'Cancel',
  Calculate = 'Calculate',
  AddThis = 'ADD THIS',
  Finish = 'Finish',
  Rename = 'Rename',
  Save = 'Save',
  Update = 'Update',
  CreateSet = 'CREATE SET',
  DiscardChanges = 'Discard Changes',
}

const Selector = {
  defaultDialog: '//*[@role="dialog"]',
}

export default class Dialog extends Container {

  constructor(page: Page, xpath: string = Selector.defaultDialog) {
    super(page, xpath);
  }

  async waitForLoad(): Promise<this> {
    try {
      await this.page.waitForXPath(this.xpath, {visible: true});
    } catch (e) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      const title = await this.page.title();
      console.error(`${title}: dialog waitForLoad() encountered ${e}`);
      throw e;
    }
    return this;
  }

  async getContent(): Promise<string> {
    const dialog = await this.page.waitForXPath(this.xpath, {visible: true});
    const modalText = await (await dialog.getProperty('innerText')).jsonValue();
    console.debug('dialog: \n' + modalText);
    return modalText.toString();
  }

  async clickButton(buttonLabel: ButtonLabel): Promise<void> {
    const button = await this.waitForButton(buttonLabel);
    await button.waitUntilEnabled();
    return button.click();
  }

  async waitForButton(buttonLabel: ButtonLabel): Promise<Button> {
    return Button.findByName(this.page, {containsText: buttonLabel}, this);
  }

  async waitForTextbox(textboxName: string): Promise<Textbox> {
    return Textbox.findByName(this.page, {name: textboxName}, this);
  }

  async waitForTextarea(textareaName: string): Promise<Textarea> {
    return Textarea.findByName(this.page, {name: textareaName}, this);
  }

  async waitForCheckbox(checkboxName: string): Promise<Checkbox> {
    return Checkbox.findByName(this.page, {name: checkboxName}, this);
  }

  async waitUntilDialogIsClosed(timeOut: number = 60000): Promise<void> {
    await this.page.waitForXPath(this.xpath, {hidden: true, timeout: timeOut});
  }

  async waitUntilVisible(): Promise<void> {
    await this.page.waitForXPath(this.xpath, {visible: true});
  }

  /**
   * Returns true if Dialog exists on page.
   */
  async exists(): Promise<boolean> {
    return (await this.page.$x(this.xpath)).length > 0;
  }

}
