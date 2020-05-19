export default class TextOptions {
  text?: string;
  contains?: string;
  normalizeSpace?: string;
  ancestorNodeLevel?: number; // ancestor node level is used to find the closest common parent for the label element and lookfor element.
  inputType?: string;
}
