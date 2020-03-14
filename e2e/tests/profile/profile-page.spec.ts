import {SideNavLink} from '../../app/authenticated-page';
import HomePage from '../../app/home-page';
import ProfilePage from '../../app/profile-page';
import {signIn} from '../app';


// set timeout globally per suite, not per test.
jest.setTimeout(2 * 60 * 1000);

describe.skip('Profile', () => {
  let page;

  beforeEach(async () => {
    page = await context.newPage();
    await signIn(page);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });

  test('Click First and Last name fields on Profile page', async () => {
    const homePage = new HomePage(page);
    await homePage.waitForLoad();
    await homePage.navTo(SideNavLink.PROFILE);
    const profilePage = new ProfilePage(page);
    const fname = await (await profilePage.getFirstName()).getValue();
    const lname = await (await profilePage.getLastName()).getValue();
      // check last and first name textbox is not empty
    expect(fname).toMatch(new RegExp(/^[a-zA-Z]+/));
    expect(lname).toMatch(new RegExp(/^[a-zA-Z]+/));
    expect(lname).not.toEqual(fname);
  });

});
