import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './TextOptions';
import WebElement from './WebElement';
import {findTextArea} from './xpath-finder';

export default class TextArea extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(
     textOptions?: TextOptions, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {

    throwErr = throwErr || true;
    try {
      this.element = await findTextArea(this.page, textOptions, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding TextArea: "${textOptions}".`);
        throw e;
      }
    }
    return this.element;
  }

}
