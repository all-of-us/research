import Button from 'app/element/button';
import HomePage from 'app/page/home-page';
import ProfilePage from 'app/page/profile-page';
import WorkspacesPage from 'app/page/workspaces-page';
import {signIn} from 'utils/test-utils';
import Navigation, {NavLink} from 'app/component/navigation';
import {waitForDocumentTitle} from 'utils/waits-utils';

export const HELP_DESK = {
  ASK_QUESTION: 'Ask a question about the Researcher Workbench',
  REPORT_DATA_PRIVACY_CONCERN: 'Report a data privacy concern',
  TELL_ABOUT_PUBLICATION: 'Tell us about an upcoming publication',
  REQUEST_BILLING_CREDITS: 'Request additional billing credits',
};


describe('Sidebar Navigation', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('SideNav menu works', async () => {
    const homePage = new HomePage(page);
    // Select Profile link
    await Navigation.navMenu(page, NavLink.PROFILE);
    const profilePage = new ProfilePage(page);
    await profilePage.waitForLoad();
    expect(await profilePage.isLoaded()).toBe(true);

    // check user name in dropdown matches names on Profile page
    const fname = await (await profilePage.getFirstName()).getValue();
    const lname = await (await profilePage.getLastName()).getValue();
    await Navigation.openNavMenu(page);
    const displayedUsername = await homePage.getUsername();
    expect(displayedUsername).toBe(`${fname} ${lname}`);

    // Select Your Workspaces link
    await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();
    expect(await workspacesPage.isLoaded()).toBe(true);

    // Select Home link
    await Navigation.navMenu(page, NavLink.HOME);
    await homePage.waitForLoad();
    expect(await homePage.isLoaded()).toBe(true);
  });

  test('User can see the Contact Us form', async () => {
    // Select Contact Us
    await Navigation.navMenu(page, NavLink.CONTACT_US);

    const iframeHandle: any = await page.waitForSelector('iframe[title="Find more information here"]', {visible: true});
    const newIframe = await iframeHandle.contentFrame();

    const askQuestionAboutButton = await Button.findByName(newIframe, {name: HELP_DESK.ASK_QUESTION});
    expect(await askQuestionAboutButton.isVisible()).toBe(true);

    const reportConcernButton = await Button.findByName(newIframe, {name: HELP_DESK.REPORT_DATA_PRIVACY_CONCERN});
    expect(await reportConcernButton.isVisible()).toBe(true);
    await reportConcernButton.dispose();

    const tellAboutPublicationButton = await Button.findByName(newIframe, {name: HELP_DESK.TELL_ABOUT_PUBLICATION});
    expect(await tellAboutPublicationButton.isVisible()).toBe(true);
    await tellAboutPublicationButton.dispose();

    const requestBillingCreditsButton = await Button.findByName(newIframe, {name: HELP_DESK.REQUEST_BILLING_CREDITS});
    expect(await requestBillingCreditsButton.isVisible()).toBe(true);
    await requestBillingCreditsButton.dispose();

    const minimizeButton = await newIframe.$('button[aria-label="Minimize widget"]');
    await minimizeButton.click();

    expect(await askQuestionAboutButton.isVisible()).toBe(false);
    await askQuestionAboutButton.dispose();
  });

  test('User can Sign Out', async () => {
    // Select Sign Out link
    await Navigation.navMenu(page, NavLink.SIGN_OUT);
    await waitForDocumentTitle(page, 'Redirect Notice');
    const href = await page.evaluate(() => location.href);
    expect(href).toEqual(expect.stringMatching(/(\/|%2F)login$/));
  });

});
