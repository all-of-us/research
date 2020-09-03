import ProfilePage, {MissingErrorAlias} from 'app/page/profile-page';
import {signIn} from 'utils/test-utils';
import navigation, {NavLink} from 'app/component/navigation';
import {makeString, makeUrl} from 'utils/str-utils';
import Button from 'app/element/button';

describe('Profile', () => {
  // initialized in beforeEach()
  let profilePage: ProfilePage;

  async function waitForSaveButton(isActive: boolean): Promise<Button> {
    const button = await profilePage.getSaveProfileButton();
    const isCursorEnabled = ! (await button.isCursorNotAllowed());
    expect(isCursorEnabled).toBe(isActive);
    return button;
  }

  // TODO generalize - promote to a Div Element?
  async function isDivWithTextPresent(text: string): Promise<boolean> {
    const selector = `//div[normalize-space(text())="${text}"]`;
    const elements = await page.$x(selector);
    return elements.length > 0;
  }

  async function isMissingErrorPresent(fieldText: string): Promise<boolean> {
    return isDivWithTextPresent(`${fieldText} can't be blank`);
  }


  beforeEach(async () => {
    await signIn(page);
    await navigation.navMenu(page, NavLink.PROFILE);
    profilePage = new ProfilePage(page);
    await profilePage.waitForLoad();

    // Verify "Save Profile" button is disabled first time page is opened.
    await waitForSaveButton(false);
  });

  test('Edit single field of existing user profile', async () => {
    const testText = makeString(50);

    // Type in Research Background textarea
    const researchBackground = await profilePage.getResearchBackgroundTextarea();
    await researchBackground.paste(testText);

    // profile update should enable Save button
    const saveButton = await waitForSaveButton(true);
    await saveButton.click();
    await profilePage.waitForLoad();

    expect(await researchBackground.getValue()).toBe(testText);
  });

  test('Edit all fields of existing user profile', async () => {
    const testTextFirstName = makeString(10);
    const testTextLastName = makeString(10);
    const testTextURL = makeUrl(10);
    const testTextResearchBackground = makeString(10);
    const testTextAddress1 = makeString(10);
    const testTextAddress2 = makeString(10);
    const testTextCity = makeString(10);
    const testTextState = makeString(10);
    const testTextZip = makeString(10);
    const testTextCountry = makeString(10);

    const firstName = await profilePage.getFirstNameInput();
    const lastName = await profilePage.getLastNameInput();
    const url = await profilePage.getProfessionalUrlInput();
    const researchBackground = await profilePage.getResearchBackgroundTextarea();
    const address1 = await profilePage.getAddress1Input();
    const address2 = await profilePage.getAddress2Input();
    const city = await profilePage.getCityInput();
    const state = await profilePage.getStateInput();
    const zip = await profilePage.getZipCodeInput();
    const country = await profilePage.getCountryInput();

    await firstName.type(testTextFirstName);
    await lastName.type(testTextLastName);
    await url.type(testTextURL);
    await researchBackground.type(testTextResearchBackground);
    await address1.type(testTextAddress1);
    await address2.type(testTextAddress2);
    await city.type(testTextCity);
    await state.type(testTextState);
    await zip.type(testTextZip);
    await country.type(testTextCountry);

    // profile update should enable Save button
    const saveButton = await waitForSaveButton(true);
    await saveButton.click();
    await profilePage.waitForLoad();

    expect(await firstName.getValue()).toBe(testTextFirstName);
    expect(await lastName.getValue()).toBe(testTextLastName);
    expect(await url.getValue()).toBe(testTextURL);
    expect(await researchBackground.getValue()).toBe(testTextResearchBackground);
    expect(await address1.getValue()).toBe(testTextAddress1);
    expect(await address2.getValue()).toBe(testTextAddress2);
    expect(await city.getValue()).toBe(testTextCity);
    expect(await state.getValue()).toBe(testTextState);
    expect(await zip.getValue()).toBe(testTextZip);
    expect(await country.getValue()).toBe(testTextCountry);
  });

  test('A missing required field disables the save button', async () => {
    const researchBackground = await profilePage.getResearchBackgroundTextarea();

    // make a change, causing the Save button to activate
    await researchBackground.paste(makeString(10));

    // save button is enabled and no error message is displayed
    await waitForSaveButton(true);
    expect(await isMissingErrorPresent(MissingErrorAlias.ResearchBackground)).toBeFalsy();

    // remove text from Research Background textarea
    await researchBackground.clear();

    // save button is disabled and error message is displayed
    await waitForSaveButton(false);
    expect(await isMissingErrorPresent(MissingErrorAlias.ResearchBackground)).toBeTruthy();
  });

  test('Each missing required field individually disables the save button', async () => {
    const firstName = await profilePage.getFirstNameInput();
    const lastName = await profilePage.getLastNameInput();
    const researchBackground = await profilePage.getResearchBackgroundTextarea();
    const address1 = await profilePage.getAddress1Input();
    const city = await profilePage.getCityInput();
    const state = await profilePage.getStateInput();
    const zip = await profilePage.getZipCodeInput();
    const country = await profilePage.getCountryInput();

    // note: Professional URL and Address2 are optional fields

    const testText = makeString(10);

    for (const {element, missingError} of [
      {element: firstName, missingError: MissingErrorAlias.FirstName},
      {element: lastName, missingError: MissingErrorAlias.LastName},
      {element: researchBackground, missingError: MissingErrorAlias.ResearchBackground},
      {element: address1, missingError: MissingErrorAlias.Address1},
      {element: city, missingError: MissingErrorAlias.City},
      {element: state, missingError: MissingErrorAlias.State},
      {element: zip, missingError: MissingErrorAlias.Zip},
      {element: country, missingError: MissingErrorAlias.Country},
    ]) {
      const originalValue = await element.getValue();

      // make a change, causing the Save button to activate
      await element.type(testText);

      // save button is enabled and no error message is displayed
      await waitForSaveButton(true);
      expect(await isMissingErrorPresent(missingError)).toBeFalsy();

      // remove text from First Name
      await element.clear();

      // save button is disabled and error message is displayed
      await waitForSaveButton(false);
      expect(await isMissingErrorPresent(missingError)).toBeTruthy();

      // restore state for next loop
      await element.type(originalValue);
    }
  });


});
