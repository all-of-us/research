import {ElementHandle} from 'puppeteer';
import CreateAccountPage from '../../app/create-account-page';
import GoogleLoginPage from '../../app/google-login';
import PuppeteerLaunch from '../../services/puppeteer-launch';
require('../../driver/waitFuncs');
require('../../driver/puppeteerExtension');
import {waitForText} from '../../driver/waitFuncs';

jest.setTimeout(60 * 1000);

const configs = require('../../resources/config');

describe('User registration tests:', () => {

  let browser;
  let incognitoContext;
  let page;

  beforeAll(async () => {
    browser = await PuppeteerLaunch();
  });

  beforeEach(async () => {
    incognitoContext = await browser.createIncognitoBrowserContext();
    page = await incognitoContext.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
    await page.setDefaultNavigationTimeout(60000);
  });

  afterEach(async () => {
    await incognitoContext.close();
  });

  afterAll(async () => {
    await browser.close();
  });


  test('Can register new user', async () => {
    // Load the landing page for login.
    const url = configs.uiBaseUrl + configs.workspacesUrlPath;
    const loginPage = new GoogleLoginPage(page);

    await loginPage.goto();

    // Click the create account button to start new-user-registration flow.
    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    // Step 1: Enter invitation key.
    const createAccountPage = new CreateAccountPage(page);

    const header = 'Enter your Invitation Key:';
    const headerDisplayed = await waitForText(page, 'h2', header);

    await createAccountPage.fillOutInvitationKey(process.env.INVITATION_KEY);
    await page.waitFor(1000);

    // Step 2: Accepting Terms of Service.
    await page.waitForFunction(() => {
      return document.querySelectorAll('.tos-pdf-page[data-page-number]').length === 9
    }, {timeout: 30000});

    await createAccountPage.acceptTermsOfUseAgreement();
    let nextButton = await createAccountPage.getNextButton();
    await nextButton.click();
    await page.waitFor(1000);

    // Step 3: Enter user information
    // Should be on Create your account: Step 1 of 2 page
    await createAccountPage.fillOutUserInformation();
    nextButton = await createAccountPage.getNextButton();
    await nextButton.click();
    await page.waitFor(1000);

    // Step 4: Enter demographic survey (All Survey Fields are optional)
    // Should be on Demongraphic Survey page
    const demograpicsPageHeader = 'Demographics Survey (All Survey Fields are optional)';
    expect(await waitForText(page, 'h3', demograpicsPageHeader)).toBeTruthy();

    await createAccountPage.fillOutDemographicSurvey();
    const submitButton = await createAccountPage.getSubmitButton();
    await submitButton.click();
    await page.waitFor(1000);

    // Step 5: New account created successfully page.
    const congratHeader = 'Congratulations!';
    await page.waitForSelector('h1', { visible: true });

    const h4List = [];
    const h4Headers: ElementHandle[] = await page.$$('h4');
    for (const h4Header of h4Headers) {
      const txt = await (await h4Header.getProperty('innerText')).jsonValue();
      h4List.push(txt);
    }
    const expectedH4Headers = [
      'Your All of Us research account has been created!',
      'Check your contact email for instructions on getting started.',
      `Your contact email is: ${process.env.CONTACT_EMAIL}`
    ];
    expect(h4List).toEqual(expectedH4Headers);

    const resendButton = await page.waitForXPath('//button[.="Resend Instructions"]');
    expect(resendButton).toBeTruthy();

    const changeEmailButton = await page.waitForXPath('//button[.="Change contact email"]');
    expect(changeEmailButton).toBeTruthy();

    const h3List = [];
    const h3Headers: ElementHandle[] = await page.$$('h3');
    for (const h3Header of h3Headers) {
      const txt = await (await h3Header.getProperty('innerText')).jsonValue();
      h3List.push(txt);
    }
    console.log(h3List);

  });

});
