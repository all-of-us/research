import {ElementHandle, Page} from 'puppeteer';
import {WorkspaceAction} from 'app/page-identifiers';

export default class EllipsisMenu {

  readonly rootXpath = '//*[@id="popup-root"]';

  constructor(private readonly page: Page,
               private readonly xpath: string,
               private readonly parentNode?: ElementHandle) {}

   /**
    *  Get all availabe Workspace Actions in a opened Ellipsis dropdown.
    */
  async getAvaliableActions(): Promise<string[]> {
    await this.clickEllipsis();
    const selector = `${this.rootXpath}//*[@role='button']/text()`;
    await this.page.waitForXPath(selector, {visible: true});
    const elements = await this.page.$x(selector);
    const actionTextsArray = [];
    for (const elem of elements) {
      actionTextsArray.push(await (await elem.getProperty('textContent')).jsonValue());
      await elem.dispose();
    }
    return actionTextsArray;
  }

  async clickAction(action: WorkspaceAction) {
    await this.clickEllipsis();
    const selector = `${this.rootXpath}//*[@role='button' and text()='${action}']`;
    const link = await this.page.waitForXPath(selector, {visible: true});
    // Select a Workspace action starts page navigation event
    await Promise.all([
      this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0'], timeout: 0}),
      link.click(),
    ]);
    await link.dispose();
  }

  private async clickEllipsis(): Promise<void> {
    const element = await this.getEllipsisIcon();
    await element.click();
    await element.dispose();
  }

  private async getEllipsisIcon(): Promise<ElementHandle> {
    if (this.parentNode === undefined) {
      return this.page.waitForXPath(this.xpath, {visible: true});
    }
    const [elemt] = await this.parentNode.$x(this.xpath);
    return elemt;
  }

}
