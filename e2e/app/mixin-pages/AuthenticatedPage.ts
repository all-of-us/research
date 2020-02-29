import {Page} from 'puppeteer';
import {waitUntilTitleMatch} from '../../driver/waitFuncs';
import AppNavBar from './AppNavBar';
import BasePage from './BasePage';

const selectors = {
  signedInIndicator: 'body#body div',
  logo: 'img[src="/assets/images/all-of-us-logo.svg"]'
};

/**
 * AoU basepage class for extending.
 */
export default abstract class AuthenticatedPage extends BasePage {

  public navigation: AppNavBar;

  constructor(page: Page) {
    super(page);
    this.navigation = new AppNavBar(page);
  }

  /**
   * Take a full-page screenshot, save file in .png format in logs/screenshots directory.
   * @param fileName
   */
  public async takeScreenshot(fileName: string) {
    const timestamp = new Date().getTime();
    const screenshotFile = `screenshots/${fileName}_${timestamp}.png`;
    await this.puppeteerPage.screenshot({path: screenshotFile, fullPage: true});
  }

  public async isLoaded(documentTitle: string): Promise<boolean> {
    await this.puppeteerPage.waitForSelector(selectors.signedInIndicator);
    await this.puppeteerPage.waitForSelector(selectors.logo, {visible: true});
    await waitUntilTitleMatch(this.puppeteerPage, documentTitle);
    return true;
  }

  /**
   * <pre>
   * Wait for spinner to stop to indicated page is ready.
   * </pre>
   */
  protected async waitForSpinner() {
    // wait maximum 1 second for either spinner to show up
    const selectr1 = '.spinner, svg';
    const spinner = await this.puppeteerPage.waitFor((selector) => {
      return document.querySelectorAll(selector).length > 0
    }, {timeout: 1000}, selectr1);
    const jValue = await spinner.jsonValue();

    // wait maximum 60 seconds for spinner disappear if spinner existed
    const selectr2 = 'svg[style*="spin"], .spinner:empty';

    if (jValue) {
      await this.puppeteerPage.waitFor((selector) => {
        return document.querySelectorAll(selector).length === 0
      }, {timeout: 60000}, selectr2);
    }
    // final 1 second wait for page render to finish
    if (jValue) {
      await this.puppeteerPage.waitFor(1000);
    }
  }

}
