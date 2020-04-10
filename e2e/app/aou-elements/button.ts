import {Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findButton} from './xpath-finder';

export default class Button extends BaseElement {

  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     waitOptions: WaitForSelectorOptions = { visible: true },
     throwErr = true): Promise<Button> {

    let element: Button;
    try {
      const buttonElement = await findButton(page, textOptions, waitOptions);
      element = new Button(page, buttonElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Button: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return element;
  }

  /**
   * Wait until button is clickable (enabled).
   */
  async waitUntilEnabled(selector?: string): Promise<unknown> {
    // works with either a xpath selector or a Element
    if (selector === undefined) {
      const retrievedValue = await this.page.waitForFunction((e) => {
        const style = window.getComputedStyle(e);
        return style.getPropertyValue('cursor') === 'pointer';
      }, { polling: 'mutation' }, this.element);
      return retrievedValue.jsonValue();
    }

    const booleanValue = await this.page.waitForFunction(xpathSelector => {
      const elemt = document.evaluate(xpathSelector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      const style = window.getComputedStyle(elemt as Element);
      const propValue = style.getPropertyValue('cursor');
      return propValue === 'pointer';
    }, { polling: 'mutation' }, selector);
    return booleanValue.jsonValue();
  }

}
