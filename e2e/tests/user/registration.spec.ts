import CreateAccountPage from 'app/page/create-account-page';
import GoogleLoginPage from 'app/page/google-login';


describe('User registration tests:', () => {

  test('Can register new user', async () => {
    // Load the landing page for login.
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    // Click the create account button to start new-user-registration flow.
    await loginPage.clickCreateAccountButton();

    const createAccountPage = new CreateAccountPage(page);
    await createAccountPage.isLoaded();

    // Step 1: Terms of Service.
    await createAccountPage.acceptTermsOfUseAgreement();
    let nextButton = await createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.click();

    // Step 2: Enter institution affiliation details
    await createAccountPage.fillOutInstitution();
    nextButton = await createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.click();

    // Step 3: Enter user information
    await createAccountPage.fillOutUserInformation();
    nextButton = await createAccountPage.getNextButton();
    await nextButton.waitUntilEnabled();
    await nextButton.click();

    // Step 4: Enter demographic survey (All Survey Fields are optional)
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
