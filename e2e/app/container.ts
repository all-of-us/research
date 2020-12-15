import {ElementHandle, Page} from 'puppeteer';

/**
 * This is the super base class.
 * Every element needs a Page object and a xpath for locating the element.
 */
export default class Container {

  constructor(protected readonly page: Page,
              protected xpath?: string,
              protected readonly container?: Container) { }

  getXpath(): string {
    if (this.container === undefined) {
      return this.xpath;
    }
    return `${this.container.getXpath()}//${this.xpath}`;
  }

  setXpath(newXpath: string): void {
    this.xpath = newXpath;
  }

  async waitUntilVisible(timeout?: number): Promise<ElementHandle> {
    return this.page.waitForXPath(this.xpath, {visible: true, timeout});
  }

}
