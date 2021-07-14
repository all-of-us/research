import { Page } from 'puppeteer';
import Container from 'app/container';
import { MenuOption } from 'app/text-labels';
import { waitWhileLoading } from 'utils/waits-utils';
import { getPropValue } from 'utils/element-utils';
import Link from 'app/element/link';
import Checkbox from 'app/element/checkbox';

export default abstract class BaseMenu extends Container {
  protected abstract getMenuItemXpath(menuItemText: string): string;

  async waitUntilVisible(): Promise<void> {
    await this.page.waitForXPath(this.getXpath(), { visible: true });
    await waitWhileLoading(this.page);
  }

  protected constructor(page: Page, xpath: string) {
    super(page, xpath);
  }

  /**
   * Select menuitems.
   * @param menuSelections
   * @param opt
   */
  async select(menuSelections: string | MenuOption | MenuOption[], opt: { waitForNav?: boolean } = {}): Promise<void> {
    const { waitForNav = false } = opt;

    let selections = [];
    // Handle case when menuSelections is not array.
    if (typeof menuSelections === 'string') {
      selections.push(menuSelections);
    } else {
      selections = [...menuSelections];
    }

    // Wait for top-level menu dropdown open.
    await this.waitUntilVisible();
    await this.page.waitForTimeout(500);

    // Iterate orderly.
    let rootXpath = this.getXpath();
    const len = selections.length;
    for (let i = 0; i < len; i++) {
      const menuItemLink = await this.findMenuItemLink(selections[i], rootXpath);
      if (i === len - 1) {
        // If it is the last menu item, click on it.
        if (waitForNav) {
          await Promise.all([
            this.page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] }),
            menuItemLink.click()
          ]);
        } else {
          await menuItemLink.click();
        }
      }
      rootXpath = `${rootXpath}/ul`; // submenu xpath
    }

    // Wait for menu close and disappear.
    await this.waitUntilClose();
    await waitWhileLoading(this.page);
  }

  /**
   *  Get all menu items texts.
   */
  protected async getMenuItemTexts(selector: string): Promise<string[]> {
    await this.page.waitForXPath(selector, { visible: true });
    const menuItemLinks = await this.page.$x(selector);
    const actionTextsArray: string[] = [];
    for (const element of menuItemLinks) {
      actionTextsArray.push(await getPropValue<string>(element, 'textContent'));
    }
    return actionTextsArray;
  }

  /**
   *  Get toggle status all modules
   */
     protected async getToggleStatus(selector: string): Promise<boolean[]> {
      await this.page.waitForXPath(selector, { visible: true });
      const inputs = await this.page.$x(selector);
      const toggleStatus = [];
      for (const element of inputs) {
        new Checkbox(this.page, selector);
        toggleStatus.push(await getPropValue<boolean>(element, 'checked'));
      }
      return toggleStatus ;
    }

  /**
   *  Get individual toggle status of each modules 
   */
     protected async getEachToggleStatus(selector: string): Promise<boolean> {
      await this.page.waitForXPath(selector, { visible: true });
       await this.page.$x(selector);
       const btn =  new Checkbox(this.page, selector);
       return await getPropValue<boolean>(await btn.asElementHandle(), 'checked')
    }
  
  protected async isOpen(): Promise<boolean> {
    try {
      await this.page.waitForXPath(this.getXpath(), { visible: true, timeout: 2000 });
      return true;
    } catch (err) {
      return false;
    }
  }

  // Find and hover over menu item
  protected async findMenuItemLink(menuItemText: string, menuParentXpath: string): Promise<Link> {
    const menuItemSelector = `${menuParentXpath}${this.getMenuItemXpath(menuItemText)}`;
    const link = new Link(this.page, menuItemSelector);
    await link.focus(60000);
    await this.page.waitForTimeout(500);
    return link;
  }
}
