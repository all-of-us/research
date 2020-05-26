import {JSHandle, Page} from 'puppeteer';
import Checkbox from 'app/element/checkbox';
import RadioButton from 'app/element/radiobutton';
import TextOptions from 'app/element/text-options';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import GoogleLoginPage from 'app/page/google-login';
import HomePage, {LABEL_ALIAS} from 'app/page/home-page';
import Link from 'app/element/link';
import {waitForText} from 'utils/waits-utils';

export async function signIn(page: Page): Promise<void> {
  const loginPage = new GoogleLoginPage(page);
  await loginPage.login();
  // this element exists in DOM after user has logged in
  await page.waitFor(() => document.querySelector('app-signed-in') !== null);
  const homePage = new HomePage(page);
  await homePage.waitForLoad();
}

/**
 * <pre>
 * Wait while the page is loading (spinner is spinning and visible). Waiting stops when spinner stops spinning or when timed out.
 * It usually indicates the page is ready for user interaction.
 * </pre>
 */
export async function waitWhileLoading(page: Page, timeOut: number = 60000): Promise<void> {
  // wait maximum 1 second for either spinner to show up
  const spinSelector = '.spinner, svg';
  let spinner: JSHandle;
  try {
    spinner = await page.waitFor((selector) => {
      return document.querySelectorAll(selector).length > 0
    }, {timeout: 1000}, spinSelector);
  } catch (err) {
    console.info('waitUntilNoSpinner does not find any spin elements.');
  }
  const jValue = await spinner.jsonValue();

  // wait maximum 90 seconds for spinner disappear if spinner existed
  const spinAnimationSelector = 'svg[style*="spin"], .spinner:empty';
  // const startTime = performance.now();
  try {
    if (jValue) {
      await page.waitFor((selector) => {
        return document.querySelectorAll(selector).length === 0;
      }, {timeout: timeOut}, spinAnimationSelector);
    }
  } catch (err) {
    throw new Error(err);
  }
  // final 1 second wait for page render to finish
  if (jValue) {
    await page.waitFor(1000);
  }
}

export async function clickEvalXpath(page: Page, xpathSelector: string) {
  return page.evaluate((selector) => {
    const node: any = document.evaluate(
       selector,
       document,
       null,
       XPathResult.FIRST_ORDERED_NODE_TYPE,
       null
    ).singleNodeValue;
    document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
    node.click();
  }, xpathSelector);
}

export async function clickEvalCss(page: Page, cssSelector: string) {
  return page.evaluate((selector) => {
    const node: any = document.querySelector(selector);
    node.click();
  }, cssSelector);
}

/**
 * Is there a element located by CSS selector?
 * @param page Puppeteer.Page
 * @param selector CSS selector
 */
export async function exists(page: Page, selector: string) {
  return !!(await page.$(`${selector}`));
}

export async function clickRecaptcha(page: Page) {
  const css = '[id="recaptcha-anchor"][role="checkbox"]';
  await page.frames().find(async (frame) => {
    for(const childFrame of frame.childFrames()) {
      const recaptcha = await childFrame.$$(css);
      if (recaptcha.length > 0) {
        await recaptcha[0].click();
        return;
      }
    }
  });
}

export async function newUserRegistrationSelfBypass(page: Page) {
  const selfBypassXpath = '//*[@data-test-id="self-bypass"]';
  await Promise.race([
    page.waitForXPath(selfBypassXpath, {visible: true, timeout: 60000}),
    Link.forLabel(page, LABEL_ALIAS.SEE_ALL_WORKSPACES, {visible: true, timeout: 60000}),
  ]);

  // check to see if it is the Self-Bypass link
  const bypassLink = await page.$x(selfBypassXpath);
  if (bypassLink.length === 0) {
    return;
  }

  // Click Self-Bypass button to continue
  const selfBypass = await page.waitForXPath(`${selfBypassXpath}//div[@role="button"]`, {visible: true});
  await selfBypass.click();
  try {
    await waitWhileLoading(page);
  } catch (timeouterr) {
    // wait more if 60 seconds wait time wasn't enough.
    await waitWhileLoading(page, 120000);
  }
  await waitForText(page, 'Bypass action is complete. Reload the page to continue.', {css: '[data-test-id="self-bypass"]'}, 60000);
  await page.reload({waitUntil: ['networkidle0', 'domcontentloaded']});
  await waitWhileLoading(page);
}

/**
 * Perform array of UI actions defined.
 * @param fields
 */
export async function performActions(
   page: Page,
   fields: ({ id: {textOption?: TextOptions; affiliated?: string; type?: string}; value?: string; selected?: boolean })[]) {
  for (const field of fields) {
    await performAction(page, field.id, field.value, field.selected);
  }
}

/**
 * Perform one UI action.
 *
 * @param { textOption?: TextOptions; affiliated?: string; type?: string } identifier
 * @param { string } value Set textbox or textarea value if associated UI element is a Checkbox.
 * @param { boolean } Set to True for select Checkbox or Radiobutton. False to unselect.
 */
export async function performAction(
   page: Page,
   identifier: {textOption?: TextOptions; affiliated?: string; type?: string}, value?: string, selected?: boolean) {

  switch (identifier.type.toLowerCase()) {
  case 'radiobutton':
    const radioELement = await RadioButton.forLabel(page, identifier.textOption);
    await radioELement.select();
    break;
  case 'checkbox':
    const checkboxElement = await Checkbox.forLabel(page, identifier.textOption);
    await checkboxElement.toggle(selected);
    if (value) {
        // For Checkbox and its required Textarea or Textbox. Set value in Textbox or Textarea if Checkbox is checked.
      await performAction(page, { textOption: identifier.textOption, type: identifier.affiliated }, value);
    }
    break;
  case 'textbox':
    const textboxElement = await Textbox.forLabel(page, identifier.textOption);
    await textboxElement.type(value, {delay: 0});
    await textboxElement.tabKey();
    break;
  case 'textarea':
    const textareaElement = await Textarea.forLabel(page, identifier.textOption);
    await textareaElement.paste(value);
    await textareaElement.tabKey();
    break;
  default:
    throw new Error(`${identifier} is not recognized.`);
  }
}
