import {ElementHandle, Page, WaitForSelectorOptions, Frame} from 'puppeteer';
import TextOptions from './text-options';
import * as xpathDefaults from './xpath-defaults';

const waitForFn = async ({ fn, interval = 2000, timeout = 10000 }) => {
  const readyState = new Promise<{success?: Frame, intervalId: NodeJS.Timeout}>(resolve => {
    const start = Date.now()
    const currentInterval = setInterval(() => {
      const succeeded = fn()
      if (success) {
        resolve({ success: succeeded, intervalId: currentInterval })
      } 
      if (Date.now() - start > timeout) {
        resolve({ intervalId: currentInterval })
      }
    }, interval)
  })

  const { success, intervalId } = await readyState
  clearInterval(intervalId)
  return success
}

/**
 * Find a LINK or BUTTON element with a specified label.
 * @param {string} label
 */
export async function findClickable(page: Page, label: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.clickableXpath(label);
  return page.waitForXPath(selector, options);
}

/**
 * Find SELECT element with a specified label.
 * @param {string} label
 */
export async function findSelect(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  // ancestor node level is used to find the closest common parent for the label element and Select element.
  // For most cases of Select, closest parent element is two level up from label. Thus for the default value 2.
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 2;
  }
  const selector = `${xpathDefaults.labelXpath(textOptions)}/ancestor::node()[${textOptions.ancestorNodeLevel}]//select`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find CHECKBOX element with a specified label.
 * @param {string} label
 */
export async function findCheckbox(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  // ancestor node level is used to find the closest common parent for the label element and checkbox element.
  // For most cases of checkbox, closest parent element is one level up from label. Thus for the default value 1.
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 1;
  }
  textOptions.inputType = 'checkbox';
  const selector = `${xpathDefaults.inputXpath(textOptions)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find BUTTON element with a specified label.
 * @param label: Button label partial text
 */
export async function findButton(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.buttonXpath(textOptions);
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find TEXTAREA element with a specified label.
 * @param {string} label: Textarea label partial text
 */
export async function findTextarea(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  // ancestor node level is used to find the closest common parent for the label element and textarea element.
  // For most cases of textarea, closest parent element is two level up from label. Thus for the default value 2.
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 2;
  }
  const selector = `${xpathDefaults.labelXpath(textOptions)}/ancestor::node()[${textOptions.ancestorNodeLevel}]//textarea`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find visible text label on the page.
 * @param {string} label:
 */
export async function findLabel(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.labelXpath(textOptions);
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find TEXTINPUT element with a specified label.
 * @param {string} label
 */
export async function findTextbox(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  // ancestor node level is used to find the closest common parent for the label element and textbox element.
  // For most cases of textbox, closest parent element is one level up from label. Thus for the default value 1.
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 1;
  }
  textOptions.inputType = 'text';
  const selector = `${xpathDefaults.inputXpath(textOptions)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find RADIOBUTTON element with a specified label.
 * @param {string} label
 */
export async function findRadiobutton(page: Page, textOptions: TextOptions, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  // ancestor node level is used to find the closest common parent for the label element and radiobutton element.
  // For most cases of radiobutton, closest parent element is one level up from label. Thus for the default value 1.
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 1;
  }
  textOptions.inputType = 'radio';
  const selector = `${xpathDefaults.inputXpath(textOptions)}`;
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find clr-icon element with a specified label. A clr-icon is clickable link AoU app.
 * @param page
 * @param label
 */
export async function findIcon(page: Page, textOptions: TextOptions, shape: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.clrIconXpath(textOptions, shape);
  if (waitOptions === undefined) {
    waitOptions = {visible: true};
  }
  return page.waitForXPath(selector, waitOptions);
}

/**
 * Find IMAGE element that is displayed next to a specified label.
 * @param {string} label
 */
export async function findImage(page: Page, label: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
  const selector = xpathDefaults.imageXpath(label);
  if (waitOptions === undefined) {
    waitOptions = {visible: true};
  }
  return page.waitForXPath(selector, waitOptions);
}

export async function findIframe(page: Page, label: string): Promise<Frame> {
  const iframeNode = await page.waitForXPath(xpathDefaults.iframeXpath(label))
  const srcHandle = await iframeNode.getProperty('src')
  const src = await srcHandle.jsonValue()
  const hasFrame = (): Frame => page.frames().find(frame => frame.url() === src)

  return hasFrame() || await waitForFn({ fn: hasFrame })
}
