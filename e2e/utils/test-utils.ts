import {Page} from 'puppeteer';
import Checkbox from 'app/element/checkbox';
import RadioButton from 'app/element/radiobutton';
import TextOptions from 'app/element/text-options';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';

/**
 * Perform array of UI actions defined.
 * @param fields
 */
export async function performActions(
   page: Page,
   fields: ({ id: {textOption?: TextOptions; affiliated?: string; type?: string}; value?: string; selected?: boolean })[]) {
  for (const field of fields) {
    await performAction(page, field.id, field.value, field.selected);
  }
}

/**
 * Perform one UI action.
 *
 * @param { textOption?: TextOptions; affiliated?: string; type?: string } identifier
 * @param { string } value Set textbox or textarea value if associated UI element is a Checkbox.
 * @param { boolean } Set to True for select Checkbox or Radiobutton. False to unselect.
 */
export async function performAction(
   page: Page,
   identifier: {textOption?: TextOptions; affiliated?: string; type?: string}, value?: string, selected?: boolean) {

  switch (identifier.type.toLowerCase()) {
  case 'radiobutton':
    const radioELement = await RadioButton.forLabel(page, identifier.textOption);
    await radioELement.select();
    break;
  case 'checkbox':
    const checkboxElement = await Checkbox.forLabel(page, identifier.textOption);
    await checkboxElement.toggle(selected);
    if (value) {
      // For Checkbox and its required Textarea or Textbox. Set value in Textbox or Textarea if Checkbox is checked.
      await performAction(page, { textOption: identifier.textOption, type: identifier.affiliated }, value);
    }
    break;
  case 'textbox':
    const textboxElement = await Textbox.forLabel(page, identifier.textOption);
    await textboxElement.type(value, {delay: 0});
    await textboxElement.tabKey();
    break;
  case 'textarea':
    const textareaElement = await Textarea.forLabel(page, identifier.textOption);
    await textareaElement.type(value);
    await textareaElement.tabKey();
    break;
  default:
    throw new Error(`${identifier} is not recognized.`);
  }
}
