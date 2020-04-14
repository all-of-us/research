const PuppeteerEnvironment = require('jest-environment-puppeteer');
const fs = require('fs-extra');
require('jest-circus');


// jest-circus retryTimes
const retryTimes = process.env.RETRY_ATTEMPTS || 0;

class PuppeteerCustomEnvironment extends PuppeteerEnvironment {
  async setup() {
    await super.setup();
  }

  async teardown() {
    // time for screenshots
    await this.global.page.waitFor(1000);
    await super.teardown();
  }

  // Take a screenshot right after failure
  async handleTestEvent(event, state) {
    if (event.name === 'test_fn_failure') {
      if (state.currentlyRunningTest.invocations > retryTimes) {
        console.log(`Test "${event.test.name}" failed.`);
        const testName = state.currentlyRunningTest.name.replace(/\s/g, ''); // remove whitespaces
        const screenshotDir = 'logs/screenshot';
        await fs.ensureDir(screenshotDir);
        // move create-filename to helper.ts
        const timestamp = new Date().getTime();
        const fileName = `${testName}_${timestamp}.png`
        const screenshotFile = `${screenshotDir}/${fileName}`;
        await this.global.page.screenshot({path: screenshotFile, fullPage: true});
        console.log('Saved screenshot ' + screenshotFile);
      }
    }
  }

}

module.exports = PuppeteerCustomEnvironment;
