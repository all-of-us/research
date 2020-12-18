import HomePage from 'src/app/page/home-page';
import ProfilePage from 'src/app/page/profile-page';
import WorkspacesPage from 'src/app/page/workspaces-page';
import {signIn, signOut} from 'utils/test-utils';
import Navigation, {NavLink} from 'src/app/component/navigation';
import {waitForDocumentTitle} from 'utils/waits-utils';

describe('Sidebar Navigation', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('SideNav menu', async () => {
    const homePage = new HomePage(page);
    // Select Profile link
    await Navigation.navMenu(page, NavLink.PROFILE);
    const profilePage = new ProfilePage(page);
    await profilePage.waitForLoad();
    expect(await profilePage.isLoaded()).toBe(true);

    // check user name in dropdown matches names on Profile page
    const fname = await profilePage.getFirstNameInput()
        .then(elm => elm.getValue())
        .then(s => s.trim());
    const lname = await profilePage.getLastNameInput()
        .then(elm => elm.getValue())
        .then(s => s.trim());
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

    // Select Sign Out link
    await signOut(page);
    await waitForDocumentTitle(page, 'Redirect Notice');
    const href = await page.evaluate(() => location.href);
    expect(href).toEqual(expect.stringMatching(/(\/|%2F)login$/));
  });

});
