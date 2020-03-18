import CreateAccountPage from '../../app/create-account-page';
import GoogleLoginPage from '../../app/google-login';

// set timeout globally per suite, not per test.
jest.setTimeout(2 * 60 * 1000);

describe('User registration tests:', () => {

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });

  test('Can register new user', async () => {
    // Load the landing page for login.
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    // Click the create account button to start new-user-registration flow.
    const createAccountButton = await loginPage.createAccountButton();
    await createAccountButton.click();

    const createAccountPage = new CreateAccountPage(page);

    // Step 1: Enter invitation key.
    await createAccountPage.waitForTextExists('Enter your Invitation Key:');
    await createAccountPage.fillOutInvitationKey(process.env.INVITATION_KEY);

    // Step 2: Accepting Terms of Service.
    await page.waitForFunction(() => {
      return document.querySelectorAll('.tos-pdf-page[data-page-number]').length > 1
    }, {timeout: 5000});

    await createAccountPage.acceptTermsOfUseAgreement();
    let nextButton = await createAccountPage.getNextButton();
    await nextButton.waitForEnabled();
    await nextButton.click();

    // Step 3: Enter Institution
    await createAccountPage.fillOutInstitution();
    nextButton = await createAccountPage.getNextButton();
    await nextButton.waitForEnabled();
    await nextButton.click();

    // Step 4: Enter user information
    await createAccountPage.fillOutUserInformation();
    nextButton = await createAccountPage.getNextButton();
    await nextButton.waitForEnabled();
    await nextButton.click();

    // Step 5: Enter demographic survey (All Survey Fields are optional)
    await createAccountPage.fillOutDemographicSurvey();

    // TODO uncomment after disable recaptcha
    // const submitButton = await createAccountPage.getSubmitButton();
    // await submitButton.click();

    // Step 5: New account created successfully page.
    // await createAccountPage.waitForTextExists('Congratulations!');
    // await createAccountPage.waitForTextExists('Your new research workbench account');

    // const resendButton = await findClickable(page, 'Resend Instructions');
    // expect(resendButton).toBeTruthy();
  });

});
