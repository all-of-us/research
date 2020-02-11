import Home from '../../app/home';
const Chrome = require('../../driver/ChromeDriver');

jest.setTimeout(60 * 1000);

const configs = require('../../config/config');

describe.skip('Workspace creation tests:', () => {

  let page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });

  test('Create a new workspace from the Home page', async () => {
    const home = new Home(page);
    const link = await home.getCreateNewWorkspaceLink();
    await link.click();
    await home.takeScreenshot('createnewworkspacelink')
    // TODO
  });

});
