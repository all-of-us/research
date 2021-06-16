import CookiePolicyPage from 'app/page/cookie-policy';
import GoogleLoginPage from 'app/page/google-login';
import { config } from 'resources/workbench-config';
import { withBrowser, withPage } from 'libs/browser';

describe('Login tests:', () => {
  test('Cookie banner visible on login page', async () => {
    await withBrowser(async (browser) => {
      await withPage(browser)(async (page) => {
        const loginPage = new GoogleLoginPage(page);
        await loginPage.load();
        expect(await loginPage.loginButton()).toBeTruthy();
        const link = await loginPage.cookiePolicyLink();
        expect(link).toBeTruthy();
        await link.click();

        // This link is a target='_blank', so we need to capture the new page.
        const newTarget = await browser.waitForTarget((target) => target.opener() === page.target());
        const newPage = await newTarget.page();
        const cookiePage = new CookiePolicyPage(newPage);
        await cookiePage.loaded();
      });
    });
  });

  const urls = [
    { text: `${config.uiBaseUrl}${config.workspacesUrlPath}` },
    { text: config.uiBaseUrl },
    { text: `${config.uiBaseUrl}${config.profileUrlPath}` }
  ];

  test.each(urls)('Redirects to login page', async (url) => {
    await withBrowser(async (browser) => {
      await withPage(browser)(async (page) => {
        await page.goto(url.text, { waitUntil: 'networkidle0' });
        const loginPage = new GoogleLoginPage(page);
        expect(await loginPage.loginButton()).toBeTruthy();
      });
    });
  });
});
