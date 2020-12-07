import Checkbox from 'app/element/checkbox';
import Link from 'app/element/link';
import RadioButton from 'app/element/radiobutton';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import GoogleLoginPage from 'app/page/google-login';
import HomePage, {LabelAlias} from 'app/page/home-page';
import {ElementType, XPathOptions} from 'app/xpath-options';
import * as fp from 'lodash/fp';
import {ElementHandle, Page} from 'puppeteer';
import {waitForText, waitWhileLoading} from 'utils/waits-utils';
import WorkspaceCard from 'app/component/workspace-card';
import {WorkspaceAccessLevel} from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import Navigation, {NavLink} from 'app/component/navigation';
import {makeWorkspaceName} from './str-utils';
import {config} from 'resources/workbench-config';

export async function signIn(page: Page, userId?: string, passwd?: string): Promise<void> {
  const loginPage = new GoogleLoginPage(page);
  await loginPage.login(userId, passwd);
  // This element exists in DOM after user has logged in. But it could takes a while.
  await page.waitForFunction(() => !!document.querySelector('app-signed-in'), {timeout: 30000});
  const homePage = new HomePage(page);
  await homePage.waitForLoad();
}

/**
 * Login in new Incognito page.
 * @param page
 * @param {string} userId
 * @param {string} passwd
 */
// @ts-ignore
export async function signInAs(page: Page, userId: string, passwd: string, opts: {reset?: boolean} = {}): Promise<Page> {
  const {reset = true} = opts;
  if (reset) {
    await jestPuppeteer.resetBrowser();
  }
  const newPage = await browser.newPage();
  const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36';
  await newPage.setUserAgent(userAgent);
  await newPage.setDefaultNavigationTimeout(90000);
  await signIn(newPage, userId, passwd);
  return newPage;
}

export async function signOut(page: Page) {
  await Navigation.navMenu(page, NavLink.SIGN_OUT);
  await page.waitFor(1000);
}


export async function click(page: Page, opts: {xpath?: string, css?: string}) {
  const {xpath, css} = opts;
  if (xpath) {
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
    }, xpath);
  }

  if  (css) {
    return page.evaluate((selector) => {
      const node: any = document.querySelector(selector);
      node.click();
    }, css);
  }
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
    Link.findByName(page, {name: LabelAlias.SeeAllWorkspaces}),
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
    await waitWhileLoading(page);
  }
  await waitForText(page, 'Bypass action is complete. Reload the page to continue.', {css: '[data-test-id="self-bypass"]'});
  await page.reload({waitUntil: ['networkidle0', 'domcontentloaded']});
  await waitWhileLoading(page);
}

/**
 * Perform array of UI actions defined.
 * @param fields
 */
export async function performActions(
   page: Page,
   fields: ({ id: {textOption: XPathOptions, affiliated?: ElementType}; value?: string; selected?: boolean })[]) {
  for (const field of fields) {
    await performAction(page, field.id, field.value, field.selected);
  }
}

/**
 * Perform one UI action.
 * @param {Page} page
 * @param { textOption?: XpathOptions, affiliated?: InputType } identifier
 * @param { string } value Set textbox or textarea value if associated UI element is a Checkbox.
 * @param { boolean } selected to True for select Checkbox or Radiobutton. False to unselect.
 */
export async function performAction(
   page: Page,
   identifier: {textOption: XPathOptions, affiliated?: ElementType}, value?: string, selected?: boolean) {

  switch (identifier.textOption.type.toLowerCase()) {
  case 'radio':
    const radioELement = await RadioButton.findByName(page, identifier.textOption);
    await radioELement.select();
    break;
  case 'checkbox':
    const checkboxElement = await Checkbox.findByName(page, identifier.textOption);
    await checkboxElement.toggle(selected);
    if (value) {
        // For Checkbox and its required Textarea or Textbox. Set value in Textbox or Textarea if Checkbox is checked.
      identifier.textOption.type = identifier.affiliated;
      await performAction(page, { textOption: identifier.textOption }, value);
    }
    break;
  case 'text':
    const textboxElement = await Textbox.findByName(page, identifier.textOption);
    await textboxElement.type(value, {delay: 0});
    await textboxElement.pressTab();
    break;
  case 'textarea':
    const textareaElement = await Textarea.findByName(page, identifier.textOption);
    await textareaElement.paste(value);
    await textareaElement.pressTab();
    break;
  default:
    throw new Error(`${identifier} is not recognized.`);
  }

}

export async function createWorkspace(page: Page, workspaceName: string = makeWorkspaceName(), cdrVersionName: string = config.defaultCdrVersionName): Promise<WorkspaceCard> {
  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();

  await workspacesPage.createWorkspace(workspaceName, cdrVersionName);
  console.log(`Created workspace "${workspaceName}" with CDR version "${cdrVersionName}"`);
  await workspacesPage.load();

  const workspaceCard = new WorkspaceCard(page);
  return workspaceCard.findCard(workspaceName);
}

/**
 * Find a suitable existing workspace, or create one if it does not exist.
 *
 * If the caller specifies a workspace name and it can be found, return it.
 *
 * If the workspace is not found (or no name is given), search for a workspace where the user
 * has Owner access.
 *
 * If no such workspace exists or the caller specifies alwaysCreate, create a new workspace and return it.
 *
 * Else choose one of the suitable workspaces randomly.
 *
 * @param page
 * @param workspaceName Returns the workspace with this name if it can be found.
 *  Otherwise, create a new workspace with this name.
 */
export async function findOrCreateWorkspace(page: Page, opts: { workspaceName?: string, openWorkspace?: boolean } = {} ): Promise<WorkspaceCard | null> {

  const {openWorkspace = true, workspaceName} = opts;

  const workspacesPage = new WorkspacesPage(page);
  await workspacesPage.load();

  const workspaceCard = new WorkspaceCard(page);
  // Returns specified workspaceName Workspace card if exists.
  if (workspaceName !== undefined) {
    const cardFound = await workspaceCard.findCard(workspaceName);
    if (cardFound != null) {
      console.log(`Found workspace "${workspaceName}"`);
      if (openWorkspace) await cardFound.clickWorkspaceName(); else return cardFound;
    }
    console.warn(`Failed to find workspace "${workspaceName}"`);
  }

  // Workspace name not found, or none was specified
  const existingWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(WorkspaceAccessLevel.Owner);
  if (existingWorkspaces.length === 0) {
    // Create new workspace
    const name = workspaceName || makeWorkspaceName();
    await workspacesPage.createWorkspace(name);
    console.log(`Created workspace "${name}"`);
    if (!openWorkspace) {
      await workspacesPage.load();
      return workspaceCard.findCard(name);
    }
  }

  // Default behavior: Returns one random selected Workspace card.
  const oneWorkspaceCard = fp.shuffle(existingWorkspaces)[0];
  const workspaceCardName = await oneWorkspaceCard.getWorkspaceName();
  console.log(`Found workspace "${workspaceCardName}"`);
  if (openWorkspace) await oneWorkspaceCard.clickWorkspaceName(); else return oneWorkspaceCard;
}

export async function centerPoint(element: ElementHandle): Promise<[number, number]> {
  const box = await element.boundingBox();
  const { x, y, height, width } = box;
  const cx = (x + x + width) / 2;
  const cy = (y + y + height) / 2;
  return [cx, cy];
}

export async function dragDrop(page: Page, element: ElementHandle, destinationPoint: {x, y}) {
  const [x0, y0] = await centerPoint(element);
  const {x, y} = destinationPoint;
  const mouse = page.mouse;
  await mouse.move(x0, y0);
  await page.waitFor(100);
  await mouse.down();
  await page.waitFor(100);
  await mouse.move(x, y, {steps: 10});
  await page.waitFor(100);
  await mouse.up();
  await page.waitFor(1000);
}

/**
 * Validate a date string.
 * @param {string} date
 */
// See: https://stackoverflow.com/questions/18758772/how-do-i-validate-a-date-in-this-format-yyyy-mm-dd-using-jquery
export function isValidDate(date: string) {
  const regex = /^\d{4}-\d{2}-\d{2}$/;
  if (!date.match(regex)) {
    return false;
  }
  const d = new Date(date);
  const dNum = d.getTime();
  if(!dNum && dNum !== 0) {
    return false;
  }
  return d.toISOString().slice(0,10) === date;
}
