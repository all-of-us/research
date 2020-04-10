import {ElementHandle, Page} from 'puppeteer';
import Link from 'app/aou-elements/link';
import {findIcon} from 'app/aou-elements/xpath-finder';
import AuthenticatedPage from 'app/authenticated-page';
import {pageUrl} from 'util/enums';

export const PAGE = {
  TITLE: 'Homepage',
  HEADER: 'Workspaces',
};

export const FIELD_LABEL = {
  SEE_ALL_WORKSPACES: 'See all Workspaces',
  CREATE_A_NEW_WORKSPACE: 'Workspaces',
};


export default class HomePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitUntilTitleMatch(PAGE.TITLE);
      await this.waitForTextExists(PAGE.HEADER);
      await Link.forLabel(this.page, FIELD_LABEL.SEE_ALL_WORKSPACES);
      return true;
    } catch (e) {
      return false;
    }
  }

  async getCreateNewWorkspaceLink(): Promise<ElementHandle> {
    return findIcon(this.page, {text: FIELD_LABEL.CREATE_A_NEW_WORKSPACE}, 'plus-circle');
  }

  /**
   * Load Home page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(pageUrl.HOME);
    return this;
  }

}
