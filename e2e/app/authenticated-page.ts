import {performance} from 'perf_hooks';
import {Page} from 'puppeteer';
import Checkbox from 'app/aou-elements/checkbox';
import RadioButton from 'app/aou-elements/radiobutton';
import TextOptions from 'app/aou-elements/text-options';
import Textarea from 'app/aou-elements/textarea';
import Textbox from 'app/aou-elements/textbox';
import {clrIconXpath} from 'app/aou-elements/xpath-defaults';
import {findIcon} from 'app/aou-elements/xpath-finder';
import BasePage from 'app/base-page';
import {pageUrl, sideNavLink} from 'util/enums';

const selectors = {
  signedInIndicator: 'body#body div',
  logo: 'img[src="/assets/images/all-of-us-logo.svg"]'
};


/**
 * AuthenticatedPage represents the base page for any AoU page after user has successfully logged in (aka authenticated).
 * This is the base page for all AoU pages to extends from.
 */
export default abstract class AuthenticatedPage extends BasePage {

  protected constructor(page: Page) {
    super(page);
  }

  protected async isSignedIn(): Promise<boolean> {
    await this.page.waitForSelector(selectors.signedInIndicator);
    await this.page.waitForSelector(selectors.logo, {visible: true});
    return true;
  }

  /**
   * Method to be implemented by children classes.
   * Check whether current page has specified web elements.
   */
  abstract async isLoaded(): Promise<boolean>

  /**
   * Wait until current page is loaded and without spinners spinning.
   */
  async waitForLoad(): Promise<this> {
    if (!await this.isLoaded()) {
      await this.saveHtmlToFile('PageIsNotLoaded');
      await this.takeScreenshot('PageIsNotLoaded');
      throw new Error('Page isLoaded() failed.');
    }
    await this.waitUntilNoSpinner();
    return this;
  }

  /**
   * Load AoU page URL.
   */
  async loadPageUrl(url: pageUrl): Promise<void> {
    await this.gotoUrl(url.toString());
    await this.waitForLoad();
  }

  /**
   * Go to application page.
   * @param targetPage
   */
  async navTo(targetPage: sideNavLink) {
    await this.openSideNav();
    const angleIconXpath = clrIconXpath({}, 'angle');
    await this.page.waitForXPath(angleIconXpath, {timeout: 2000});
    const appLinkXpath = `//*[@role="button" and @tabindex="0"]//span[contains(., "${targetPage}")]`;
    // try to find target sidenav link. Get the first element from ElementHandle array
    const [applink] = await this.page.$x(appLinkXpath);
    if (!applink) {
      // if sidnav link is not found, check to see if it's a link under User or Admin submenu.
      const [username, admin] = await this.page.$x(angleIconXpath);
      if (targetPage === sideNavLink.PROFILE || targetPage === sideNavLink.SIGN_OUT) {
        // Open User submenu if needed
        if (!applink) {
          await username.click();
        }
      } else if (targetPage === sideNavLink.USER_ADMIN) {
        // Open Admin submenu if needed
        if (!applink) {
          await admin.click();
        }
      }
    }
    // find target sidenav link again. If not found, throws exception
    const link = await this.page.waitForXPath(appLinkXpath, {timeout: 2000});
    if (targetPage === sideNavLink.CONTACT_US) {
      await link.click();
    } else {
      await this.clickAndWait(link);
    }

  }

  /**
   * Find the actual displayed User name string in sidenav dropdown.
   */
  async getUserName(): Promise<unknown> {
    const xpath = `//*[child::clr-icon[@shape="angle"]/*[@role="img"]]`;
    const username = (await this.page.$x(xpath))[0];
    const p = await username.getProperty('innerText');
    const value = await p.jsonValue();
    return value;
  }

  /**
   * Open sidenav dropdown.
   */
  async openSideNav() {
    const is = await this.sideNavIsOpen();
    if (!is) {
      // click bars icon to open dropdown
      const barsIcon = await findIcon(this.page, {}, 'bars');
      await barsIcon.click();
    }
  }

  /**
   * <pre>
   * Wait until spinner stops spinning, usually it indicate page is ready.
   * </pre>
   */
  async waitUntilNoSpinner() {
    // wait maximum 1 second for either spinner to show up
    const selectr1 = '.spinner, svg';
    const spinner = await this.page.waitFor((selector) => {
      return document.querySelectorAll(selector).length > 0
    }, {timeout: 1000}, selectr1);
    const jValue = await spinner.jsonValue();

    // wait maximum 90 seconds for spinner disappear if spinner existed
    const selectr2 = 'svg[style*="spin"], .spinner:empty';
    const startTime = performance.now();
    try {
      if (jValue) {
        await this.page.waitFor((selector) => {
          const selectorLength = document.querySelectorAll(selector).length;
          return selectorLength === 0;
        }, {timeout: 90000}, selectr2);
      }
    } catch (err) {
      await this.takeScreenshot('TimedOutWaitForSpinnerStop');
      throw err;
    } finally {
      const finishTime = performance.now();
      const diff = Math.floor(((finishTime - startTime) / 1000) % 60);
      if (diff > 60) {
        // if timeout exceeds 60 seconds without error thrown, it tells me page loading is slower.
        console.warn(`WARNING: waitUntilNoSpinner took ${diff} seconds.`);
      }
    }
    // final 1 second wait for page render to finish
    if (jValue) {
      await this.page.waitFor(1000);
    }
  }

  // Determine the open state by looking for a visible Home icon
  private async sideNavIsOpen(): Promise<boolean> {
    try {
      await findIcon(this.page, {text: 'Home'}, 'home', {visible: true, timeout: 1000});
      return true;
    } catch(err) {
      return false;
    }
  }

  /**
   *
   * @param fields Array
   */
  async performUiActions(
     fields: ({ id: { textOption?: TextOptions; affiliated?: string; type?: string }; value?: string; selected?: boolean })[]) {
    for (const field of fields) {
      await this.performUiAction(field.id, field.value, field.selected);
    }
  }

  /**
   * UI actions helper function.
   *
   * @param { textOption?: TextOptions; affiliated?: string; type?: string } identifier
   * @param { string } value Set textbox or textarea value if associated UI element is a Checkbox.
   * @param { boolean } Set to True for select Checkbox or Radiobutton. False to unselect.
   */
  async performUiAction(identifier: { textOption?: TextOptions; affiliated?: string; type?: string },
                        value?: string,
                        selected?: boolean) {
    switch (identifier.type.toLowerCase()) {
    case 'radiobutton':
      const radioELement = await RadioButton.forLabel(this.page, identifier.textOption);
      await radioELement.toggle(selected);
      break;
    case 'checkbox':
      const checkboxElement = await Checkbox.forLabel(this.page, identifier.textOption);
      await checkboxElement.toggle(selected);
      if (value) {
          // For Checkbox and its required Textarea or Textbox. Set value in Textbox or Textarea if Checkbox is checked.
        await this.performUiAction({ textOption: identifier.textOption, type: identifier.affiliated }, value);
      }
      break;
    case 'textbox':
      const textboxElement = await Textbox.forLabel(this.page, identifier.textOption);
      await textboxElement.type(value);
      await textboxElement.tabKey();
      break;
    case 'textarea':
      const textareaElement = await Textarea.forLabel(this.page, identifier.textOption);
      await textareaElement.type(value);
      await textareaElement.tabKey();
      break;
    default:
      throw new Error(`${identifier} is not recognized.`)
    }
  }


}
