import {ClickOptions, ElementHandle, Page, Response, WaitForSelectorOptions} from 'puppeteer';
import {ElementInterface} from './ElementInterface';

export default class WebElement implements ElementInterface {
  protected readonly page: Page;
  protected labelText: string;
  protected css: string;
  protected xpath: string;
  protected element: ElementHandle;

  constructor(aPage: Page) {
    this.page = aPage;
  }

  public async withCss(aCssSelector: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
    this.css = aCssSelector;
    this.element = await this.waitForCss(options);
    return this.element;
  }

  public async withXpath(aXpathSelector: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
    this.xpath = aXpathSelector;
    this.element = await this.waitForXpath(options);
    return this.element;
  }

  public async waitForXpath(options?: WaitForSelectorOptions): Promise<ElementHandle> {
    if (this.xpath === undefined) { throw new Error('Xpath selector is undefined.'); }
    this.element = await this.page.waitForXPath(this.xpath, options);
    return this.element
  }

  public async waitForCss(options?: WaitForSelectorOptions): Promise<ElementHandle> {
    if (this.css === undefined) { throw new Error('CSS selector is undefined.'); }
    this.element = await this.page.waitForSelector(this.css, options);
    return this.element;
  }

   /**
    * Find element without waiting for.
    */
  public async findByCss(): Promise<ElementHandle | null> {
    if (this.css === undefined) { throw new Error('CSS selector is undefined.'); }
    this.element = await this.page.$(this.css);
    return this.element;
  }

   /**
    * Find element without waiting for.
    */
  public async findByXpath(): Promise<ElementHandle> {
    if (this.xpath === undefined) { throw new Error('Xpath selector is undefined.'); }
    this.element = (await this.page.$x(this.xpath))[0];
    return this.element;
  }

  /**
   * Finds the value of a property
   */
  public async getProperty(propertyName: string): Promise<unknown> {
    if (this.element === undefined) { throw new Error('The element is undefined.'); }
    const p = await this.element.asElement().getProperty(propertyName);
    return await p.jsonValue();
  }

  /**
   * Finds the value of an attribute
   */
  public async getAttribute(attributeName: string): Promise<string | null> {
    if (this.element === undefined) { throw new Error('The element is undefined.'); }
    const elem = this.element.asElement();
    const attributeValue = await this.page.evaluate(
       (link, attr) => link.getAttribute(attr), elem, attributeName
    );
    return attributeValue;
  }

  /**
   * Call one of following functions before call this function.
   * findByCss
   *
   * @param attributeName
   */
  public async hasAttribute(attributeName: string): Promise<boolean> {
    if (this.element === undefined) { throw new Error('The element is undefined.'); }
    return await this.getAttribute(attributeName) !== null;
  }

  /**
   * Disabled means element has `disabled` attribute.
   */
  public async isDisabled(): Promise<boolean> {
    const disabled = await this.getProperty('disabled');
    return !!disabled ;
  }

  public async isVisible(): Promise<boolean> {
    const eHandle = this.element.asElement();
    if (eHandle === null) { return false; }
    return await eHandle.boundingBox() !== null;
  }

  public async click(options?: ClickOptions): Promise<void> {
    await this.element.asElement().click(options);
  }

  public async type(text: string, options?: { delay: number }): Promise<void> {
    await this.focus();
    await this.element.asElement().type(text, options);
  }

  public async pressKeyboard(key: string, options?: { text?: string, delay?: number }): Promise<void> {
    await this.element.asElement().press(key, options);
  }

  /**
   * Calling focus() and hover() together.
   */
  public async focus(): Promise<void> {
    const handle = this.element.asElement();
    await Promise.all([
      handle.focus(),
      handle.hover()
    ]);
  }

  public async getTextContent(): Promise<string> {
    const handle = await this.element.asElement();
    return await handle.evaluate(
       (element: HTMLElement) => (element.textContent ? element.textContent.trim() : ''), this.element,
    );
  }

  public async getValue(): Promise<unknown> {
    return await this.getProperty('value');
  }

  public async getComputedStyle(styleName: string): Promise<unknown> {
    const handle = this.element.asElement();
    const attrStyle = await handle.evaluateHandle((e) => {
      const style = window.getComputedStyle(e);
      return style;
    }, this.element);

    return (await attrStyle.getProperty(styleName)).jsonValue()
  }

  public async reloadPage(): Promise<Response> {
    return await this.page.reload( { waitUntil: ['networkidle0', 'domcontentloaded'] } );
  }

  /**
   * non-async..
   */
  public exists(): boolean {
    return this.element.asElement() !== null;
  }

}
