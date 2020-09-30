import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';

export function iframeXpath(label: string) {
  return `//body[@id='body']//*[contains(@aria-label, '${label}')]//iframe`;
}

/**
 * Build a XPath expression from XPathOptions parameter.
 * @param {XPathOptions} xOpts
 * @param container
 */
export function buildXPath(xOpts: XPathOptions, container?: Container): string {

  const  { type, name, containsText, normalizeSpace, ancestorLevel = 1, iconShape, startsWith, dataTestId } = xOpts;

  // optional function parameters check
  if (type === 'icon') {
    if (iconShape === undefined) {
      throw new Error(`Incorrect XPathOptions configuration for Icon: set "iconShape" value.`);
    }
  }

  let str = '';
  if (dataTestId !== undefined) {
    str = `[@data-test-id="${dataTestId}"]`;
  } else if (name !== undefined) {
    str = `[text()="${name}" or @aria-label="${name}" or @placeholder="${name}" or @value="${name}"]`;
  } else if (containsText !== undefined) {
    str = `[contains(text(), "${containsText}") or contains(@aria-label, "${containsText}") or contains(@placeholder, "${containsText}")]`;
  } else if (normalizeSpace !== undefined) {
    str = `[contains(normalize-space(), "${normalizeSpace}")]`;
  } else if (startsWith !== undefined) {
    str = `[starts-with(normalize-space(), "${startsWith}")]`;
  }

  const nodeLevel = (ancestorLevel === 0) ? '' : `/ancestor::node()[${ancestorLevel}]`;
  const containerXpath = (container === undefined) ? '' : container.getXpath();
  // empty str means element is not tied to a specific label.
  const textExpr = (str === '') ? `${containerXpath}` : `(${containerXpath}//label | ${containerXpath}//*)${str}${nodeLevel}`;

  let selector;
  switch (type) {
  case ElementType.Button:
    selector = `(${containerXpath}//button | ${containerXpath}//*[@role="button"])${str}`;
    break;
  case ElementType.Icon: // clickable icon
    const tag = (iconShape === undefined) ? '*' : `clr-icon[@shape="${iconShape}"]`;
    selector = `${textExpr}//${tag}[*[@role="img"]]`;
    break;
  case ElementType.Number:
  case ElementType.Checkbox:
  case ElementType.RadioButton:
  case ElementType.Textbox:
    selector = `${textExpr}//input[@type="${type}"]`;
    break;
  case ElementType.Link:
    selector = `(${containerXpath}//a | ${containerXpath}//span | ${containerXpath}//*[@role="button"])${str}`;
    break;
  case ElementType.Textarea:
  case ElementType.Select:
    selector = `${textExpr}//${type}`;
    break;
  case ElementType.Dropdown:
    selector = `${textExpr}${nodeLevel}//*[${classNameContains('p-dropdown')} and not(${classNameContains('p-disabled')})]`;
    break;
  case ElementType.Tab:
    selector = `//*[(@aria-selected | @tabindex) and @role="button" and text()="${name}"]`;
    break;
  default:
    console.debug(`Implement unhandled type: ${type}. 
      XPathOptions configuration: 
        "type": ${type}, 
        "name": ${name}, 
        "containsText": ${containsText}, 
        "normalizeSpace: ${normalizeSpace}, 
        "ancestorLevel": ${ancestorLevel}, 
        "iconShape": ${iconShape}`);
    throw new Error(`Implement unhandled type: ${type}`);
  }

  return this.xpath = selector;
}

function classNameContains(attrName: string): string {
  return `contains(concat(" ", normalize-space(@class), " "), " ${attrName} ")`
}
