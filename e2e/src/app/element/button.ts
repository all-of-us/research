import {Page} from 'puppeteer';
import Container from 'src/app/container';
import {ElementType, XPathOptions} from 'src/app/xpath-options';
import BaseElement from './base-element';
import {buildXPath} from 'src/app/xpath-builders';

export default class Button extends BaseElement {

  /**
   * @param {Page} page Puppeteer Page.
   * @param {XPathOptions} xOpt Convert XpathOptions to Xpath string.
   * @param {Container} container Parent node if one exists. Normally, it is a Dialog or Modal window.
   * @param {WaitForSelectorOptions} waitOptions.
   */
  static async findByName(page: Page, xOpt: XPathOptions, container?: Container): Promise<Button> {
    xOpt.type = ElementType.Button;
    const butnXpath = buildXPath(xOpt, container);
    const button = new Button(page, butnXpath);
    return button;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  /**
   * Wait until button is clickable (enabled).
   * @param {string} xpathSelector (Optional) Button Xpath selector.
   * @throws Timeout exception if button is not enabled after waiting.
   */
  async waitUntilEnabled(xpathSelector?: string): Promise<void> {
    const selector = xpathSelector || this.getXpath();
    await this.page.waitForFunction(xpath => {
      const elemt = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      const style = window.getComputedStyle(elemt as Element);
      const propValue = style.getPropertyValue('cursor');
      return propValue === 'pointer';
    }, {}, selector).catch((err) => {throw err;});
  }

}
