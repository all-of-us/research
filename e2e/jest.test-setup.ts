const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

/**
 * Set up page common properties:
 * - Page view port
 * - Page user-agent
 * - Page navigation timeout
 * - waitFor functions timeout
 */
beforeEach(async () => {

  // URL string begins with "https://api-dot-all-of-us-workbench-test.appspot.com/v1"
  const isAoUApi = (url: string): boolean => {
    return url.startsWith('https://api-dot-all-of-us-workbench-test.appspot.com/v1');
  }

  // Whether to write request response to test log.
  const skipResponse = (url: string): boolean => {
    return url.endsWith('/readonly');
  }

  await jestPuppeteer.resetPage();
  await jestPuppeteer.resetBrowser();
  await page.setUserAgent(userAgent);
  await page.setViewport({width: 1300, height: 0});
  page.setDefaultNavigationTimeout(60000); // Puppeteer default timeout is 30 seconds.
  page.setDefaultTimeout(30000);

  await page.setRequestInterception(true);
  page.on('request', (request) => {
    try {
      if (isAoUApi(request.url())) {
        const method = request.method();
        if (method !== 'OPTIONS') {
          const data = request.postData();
          const dataString = (data === undefined) ? '' : JSON.stringify(JSON.parse(data), null, 2);
          console.debug(`❗Request issued: ${method} ${request.url()}\n${dataString}`);
        }
      }
      request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  })
  .on('requestfinished', async (request) => {
    try {
      // response body can only be accessed for non-redirect responses.
      if (request.redirectChain().length === 0) {
        const method = request.method();
        const response = await request.response();
        if (response != null) {
          if (isAoUApi(request.url())) {
            const status = response.status();
            const failure = request.failure();
            const responseText = (await response.buffer()).toString();
            let responseTextJson = JSON.stringify(JSON.parse(responseText), null, 2);
            if (failure != null || !response.ok()) {
              const errorText = JSON.stringify(JSON.parse(failure.errorText), null, 2);
              const errorTextJson = JSON.stringify(JSON.parse(await response.text()), null, 2);
              console.debug(`❗Request failed: ${status} ${method} ${request.url()}\n${errorText}\n${errorTextJson}\n${responseTextJson}`);
              return;
            }
            if (skipResponse(request.url())) {
              console.debug(`❗Request finished: ${status} ${method} ${request.url()}\n`);
            } else {
              if (request.url().endsWith('/v1/workspaces')) {
                // Additional log processer: truncate response json because could be too many workspaces.
                const jsonBody = JSON.parse(responseText);
                responseTextJson = JSON.stringify(jsonBody.items.slice(0, 3), null, 2);
              }
              console.debug(`❗Request finished: ${status} ${method} ${request.url()}\n${responseTextJson}`);
            }
          }
        }
      }
      await request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  })
  .on('console', async (message) => {
    if (!message.args().length) {
      return;
    }
    try {
      const texts = await Promise.all(
        message.args().map(async (arg) =>
          await arg.executionContext().evaluate((txt) => {
            if (txt instanceof Error) {
              return txt.stack;
            }
            return txt.toString();
          }, arg))
      )
      const type = message.type();
      // Don't log "log", "info", "debug"
      switch (type) {
        case 'error':
        case 'warning':
          console.debug(`❗Page console ${message.type()}: ${texts}`);
          break;
        }
      // tslint:disable-next-line:no-empty
    } catch (err) {
    }
  })
  .on('error', (error) => {
    console.debug(`❗Page error: ${error}`);
  })
  .on('pageerror', (error) => {
    try {
      console.debug(`❗Page error: ${error}`);
      // tslint:disable-next-line:no-empty
    } catch (err) {
    }
  })

});

afterEach(async () => {
  await page.setRequestInterception(false);
});
