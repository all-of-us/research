import {ElementHandle, Page} from 'puppeteer';
import {EllipsisMenuAction} from 'app/page-identifiers';
import {GroupAction} from '../page/cohort-participants-group';
import TieredMenu from './tiered-menu';

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

  async clickAction(action: EllipsisMenuAction, waitForNav: boolean = true): Promise<void> {
    await this.clickEllipsis();
    const selector = `${this.rootXpath}//*[@role='button' and text()='${action}']`;
    const link = await this.page.waitForXPath(selector, {visible: true});
    // Select a Workspace action starts page navigation event
    if (waitForNav) {
      await Promise.all([
        this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0']}),
        link.click(),
      ]);
    } else {
      await link.click();
    }
    await link.dispose();
  }

  /**
   * Build Cohort Criteria page: Group ellipsis menu
   * @param {GroupAction} action
   */
  async clickParticipantsGroupAction(action: GroupAction): Promise<void> {
    await this.clickEllipsis();
    const menu = new TieredMenu(this.page);
    return menu.clickMenuItem([action.toString()]);
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
