import {ElementHandle, Page} from 'puppeteer';
import {Option} from 'app/text-labels';
import Container from 'app/container';
import SnowmanMenu, {snowmanIconXpath} from './snowman-menu';


export default abstract class CardBase extends Container {
  protected cardElement: ElementHandle;

  protected constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  asElementHandle(): ElementHandle {
    return this.cardElement.asElement();
  }

  async clickSnowmanIcon(): Promise<this> {
    const iconXpath = `.${snowmanIconXpath}`
    const [snowmanIcon] = await this.asElementHandle().$x(iconXpath)
    await snowmanIcon.click();
    await snowmanIcon.dispose();
    return this;
  }

  async getSnowmanMenu(): Promise<SnowmanMenu> {
    await this.clickSnowmanIcon();
    return new SnowmanMenu(this.page);
  }

  async selectSnowmanMenu(option: Option, opt: { waitForNav?: boolean } = {}): Promise<void> {
    return this.getSnowmanMenu().then(menu => menu.select(option, opt));
  }


}
