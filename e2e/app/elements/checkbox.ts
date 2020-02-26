import {Page} from 'puppeteer';
import WebElement from './web-element';
import {findCheckbox} from './xpath-finder';

export default class Checkbox {

  private readonly label: string;
  private readonly page: Page;
  private webElement: WebElement;

  constructor(aPage: Page, label: string) {
    this.page = aPage;
    this.label = label;
  }

  public async get(): Promise<WebElement> {
    if (!!this.webElement) {
      const element = await findCheckbox(this.page, this.label);
      this.webElement = new WebElement(element);
    }
    return this.webElement;
  }

  /**
   * Checked means element does not have a `checked` property
   */
  public async isChecked(): Promise<boolean> {
    const propChecked = (await this.get()).getProperty('checked');
    return !!propChecked;
  }

  /**
   * Make checkbox element checked
   */
  public async check(): Promise<void> {
    const isChecked = await this.isChecked();
    if (!isChecked) {
      (await this.get()).click();
    }
  }

  /**
   * Make checkbox element unchecked
   */
  public async unCheck() {
    const isChecked = await this.isChecked();
    if (isChecked) {
      (await this.get()).click();
    }
  }


}
