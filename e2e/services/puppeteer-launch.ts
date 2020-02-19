import * as Puppeteer from 'puppeteer';
import {Browser} from 'puppeteer';

const defaultLaunchOpts = {
  headless: true,
  slowMo: 10,
  defaultViewport: null,
  devtools: false,
  ignoreDefaultArgs: ['--disable-extensions'],
  args: [
    '--no-sandbox',
    '--disable-setuid-sandbox',
    '--disable-dev-shm-usage',
    '--disable-gpu',
    '--disable-background-timer-throttling',
    '--disable-backgrounding-occluded-windows',
    '--disable-renderer-backgrounding'
  ]
};

export default async (opts?): Promise<Browser> => {
  const launchOptions = opts || defaultLaunchOpts;
  return await Puppeteer.launch(launchOptions);
};
