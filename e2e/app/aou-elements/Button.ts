import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findButton} from './xpath-finder';

export default class Button extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementName: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.name = aElementName;
    throwErr = throwErr || true;
    try {
      this.element = await findButton(this.page, this.name, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Button: "${this.name}".`);
        throw e;
      }
    }
    return this.element;
  }

  /**
   * Determine if button is disabled by checking style 'cursor'.
   */
  public async isCursorAllowed(): Promise<boolean> {
    const cursor = await this.getComputedStyle('cursor');
    return cursor === 'not-allowed';
  }

}
