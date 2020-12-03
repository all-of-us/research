import {ClickOptions, ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {getAttrValue, getPropValue} from 'utils/element-utils';


/**
 * BaseElement represents a web element in the DOM.
 * It implements useful methods for querying and interacting with this element.
 */
export default class BaseElement extends Container {

  static asBaseElement(page: Page, elementHandle: ElementHandle, xpath?: string): BaseElement {
    const baseElement = new BaseElement(page, xpath);
    baseElement.setElementHandle(elementHandle);
    return baseElement;
  }

  private element: ElementHandle;

  constructor(protected readonly page: Page, protected readonly xpath?: string) {
    super(page, xpath);
  }

  protected setElementHandle(element: ElementHandle) {
    this.element = element;
  }

  /**
   * Find first element matching xpath selector.
   * If there is no element matching xpath selector, null is returned.
   * @param {WaitForSelectorOptions} waitOptions
   */
  async waitForXPath(waitOptions: WaitForSelectorOptions = {visible: true}): Promise<ElementHandle> {
    if (this.element !== undefined) return this.element.asElement();
    try {
      return this.page.waitForXPath(this.xpath, waitOptions).then(elemt => this.element = elemt.asElement());
    } catch (err) {
      console.error(`waitForXpath('${this.xpath}') encountered ${err}`);
      // Debugging pause
      // await jestPuppeteer.debug();
      throw err;
    }
  }

  /**
   * Find all elements matching xpath selector.
   */
  async findAllElements(): Promise<ElementHandle[]> {
    return this.page.$x(this.xpath);
  }

  /**
   * Find descendant elements matching xpath selector.
   * @param {string} descendantXpath Be sure to begin xpath with a dot. e.g. ".//div".
   */
  async findDescendant(descendantXpath: string): Promise<ElementHandle[]> {
    return this.asElementHandle()
      .then(elemt => {
        return elemt.$x(descendantXpath);
      });
  }

  /**
   * Finds the value of a property for this element.
   *
   * Alternative:
   *  const handle = await page.evaluateHandle((elem, prop) => {
   *    return elem[prop];
   *  }, element, property);
   *  return (await handle.jsonValue()).toString();
   */
  async getProperty<T>(propertyName: string): Promise<T> {
    return this.asElementHandle()
      .then(element => {
        return getPropValue<T>(element, propertyName);
      });
  }

  /**
   * Finds the value of an attribute
   * @param attribute name
   */
  async getAttribute(attributeName: string): Promise<string> {
    return this.asElementHandle()
      .then(element => {
        return getAttrValue(this.page, element, attributeName);
      });
  }

  /**
   * Does attribute exists for this element?
   *
   * @param attribute name
   */
  async hasAttribute(attributeName: string): Promise<boolean> {
    return this.getAttribute(attributeName)
      .then(value => {
        return value !== null;
      });
  }

  /**
   * Is element disabled or readonly?
   * Disabled means element has `disabled` attribute but without a value.
   */
  async isDisabled(): Promise<boolean> {
    return this.getProperty<boolean>('disabled');
  }

  /**
   * <pre>
   *  Check if the element is visible
   * </pre>
   * @param {Page} page
   * @param {ElementHandle} element
   */
  async isVisible(): Promise<boolean> {
    return this.asElementHandle()
      .then(elemt => {
        return elemt.boxModel();
      })
      .then(box => {
        return box !== null;
      })
  }

  /**
   * Check both boxModel and style for visibility.
   */
  async isDisplayed(): Promise<boolean> {
    const elemt = await this.asElementHandle();
    const isVisibleHandle = await this.page.evaluateHandle((e) =>
    {
      const style = window.getComputedStyle(e);
      return (style && style.display !== 'none' &&
         style.visibility !== 'hidden' && style.opacity !== '0');
    }, elemt);
    const jValue = await isVisibleHandle.jsonValue();
    const boxModelValue = await elemt.boxModel();
    return jValue && boxModelValue !== null;
  }

  async click(options?: ClickOptions): Promise<void> {
    return this.asElementHandle()
      .then(elemt => {
        return elemt.click(options);
      });
  }

  /**
   * Clear existing value in textbox then type new text value.
   * @param textValue The text string.
   * @param options The typing options.
   */
  async type(textValue: string, options?: { delay: number }): Promise<this> {

    const clearAndType = async (txt: string, opts?: { delay: number }): Promise<string> => {
      await this.clear();
      await this.asElementHandle().then((handle: ElementHandle) => handle.type(txt, opts));
      return this.getProperty<string>('value');
    }

    let maxRetries = 1;
    const typeAndCheck = async () => {
      const actualValue = (await clearAndType(textValue, options));
      if (actualValue === textValue) {
        return; // success
      }
      if (maxRetries <= 0) {
        throw new Error(`BaseElement.type("${textValue}") failed. Actual text: "${actualValue}"`);
      }
      maxRetries--;
      return await this.page.waitForTimeout(1000).then(typeAndCheck); // one second pause and retry type
    };

    await typeAndCheck();
    return this;
  }

  async pressKeyboard(key: string, options?: { text?: string, delay?: number }): Promise<void> {
    return this.asElementHandle()
      .then(elemt => {
        return elemt.press(key, options);
      });
  }

  async pressReturn(): Promise<void> {
    return this.pressKeyboard(String.fromCharCode(13));
  }

  /**
   * Press keyboard "tab".
   */
  async pressTab(): Promise<void> {
    return this.pressKeyboard('Tab', { delay: 100 });
  }

  /**
   * Clear value in textbox.
   */
  async clear(options: ClickOptions = { clickCount: 3 }): Promise<void> {
    console.log(this.xpath);
    const elemt = await this.asElementHandle();
    await elemt.focus();
    await elemt.click(options);
    await this.page.keyboard.press('Backspace');
  }

  async clearTextInput(): Promise<void> {
    return this.asElementHandle()
      .then(element => {
        return this.page.evaluate((elemt, textValue) => {
          // Refer to https://stackoverflow.com/a/46012210/440432
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
          nativeInputValueSetter.call(elemt, textValue);
          const event = new Event('input', { bubbles: true });
          elemt.dispatchEvent(event);
        }, element, '');
      });
  }

  /**
   * Calling focus() and hover() together.
   */
  async focus(): Promise<void> {
    return this.asElementHandle()
      .then(elemt => {
        Promise.all([
          elemt.focus(),
          elemt.hover(),
        ]);
      });
  }

  /**
   * <pre>
   * Get the textContent property value for a element.
   * </pre>
   */
  async getTextContent(): Promise<string> {
    return this.asElementHandle()
      .then(elemt => {
        return this.page.evaluate((element: HTMLElement) => (element.textContent ? element.textContent.trim() : ''), elemt,);
      });
  }

  /**
   * Get the value of property 'value' for this element.
   * Alternative: await page.evaluate(elem => elem.value, element);
   */
  async getValue(): Promise<string> {
    return this.getProperty<string>('value');
  }

  async getComputedStyle(styleName: string): Promise<string> {
    const handle = await this.asElementHandle();
    const attrStyle = await handle.evaluateHandle((e) => {
      const style = window.getComputedStyle(e);
      return style;
    }, this.element);
    const propValue = await attrStyle.getProperty(styleName);
    return (await propValue.jsonValue()).toString();
  }

  /**
   * Determine if cursor is disabled (== " not-allowed ") by checking style 'cursor' value.
   */
  async isCursorNotAllowed(): Promise<boolean> {
    return this.getComputedStyle('cursor')
      .then(cursor => {
        return cursor === 'not-allowed';
      });
  }

  /**
   * Finds visible element's bounding box size.
   */
  async getSize(): Promise<{ width: number; height: number }> {
    const box = await this.asElementHandle()
      .then(elemt => {
        return elemt.boundingBox();
      })
    if (box === null) {
      // if element is not visible, returns size of (0, 0).
      return { width: 0, height: 0 };
    }
    const { width, height } = box;
    return { width, height };
  }

  async dispose(): Promise<void> {
    return this.element.dispose();
  }

  // try this method when click() is not working
  async clickWithEval(): Promise<void> {
    return this.asElementHandle()
      .then(elemt => {
        return this.page.evaluate( elem => elem.click(), elemt );
      });
  }

  /**
   * Click on element then wait for page navigation to finish.
   */
  async clickAndWait(): Promise<void> {
    await Promise.all([
      this.page.waitForNavigation({ waitUntil: ['load', 'domcontentloaded', 'networkidle0'] }),
      this.click(),
    ]);
  }

  /**
   * Paste texts in textarea instead type one char at a time. Very fast.
   * @param text
   */
  async paste(text: string): Promise<void> {
    return this.asElementHandle()
      .then(element => {
        return this.page.evaluate((elemt, textValue) => {
          // Refer to https://stackoverflow.com/a/46012210/440432
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value').set;
          nativeInputValueSetter.call(elemt, textValue);
          const event = new Event('input', { bubbles: true });
          elemt.dispatchEvent(event);
        }, element, text);
      });
  }

  /**
   * Clear texts in textarea.
   */
  async clearTextArea(): Promise<void> {
    return this.paste('');
  }

  /**
   * Returns ElementHandle.
   */
  async asElementHandle(): Promise<ElementHandle> {
    return this.waitForXPath();
  }

}
