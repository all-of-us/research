import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitForNumericalString, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';
import DatasetBuildPage from './dataset-build-page';
import Link from 'app/element/link';
import CohortBuildPage, { FieldSelector } from './cohort-build-page';

const PageTitle = 'Cohort Actions';

export default class CohortActionsPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    await Promise.all([
      this.getCreateAnotherCohortButton().asElementHandle(),
      this.getCreateReviewSetsButton().asElementHandle(),
      this.getCreateDatasetButton().asElementHandle()
    ]);
    return true;
  }

  async clickCreateDatasetButton(): Promise<DatasetBuildPage> {
    const button = this.getCreateDatasetButton();
    await button.clickAndWait();
    return new DatasetBuildPage(this.page).waitForLoad();
  }

  getCreateAnotherCohortButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateAnotherCohort });
  }

  getCreateReviewSetsButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateReviewSets });
  }

  getCreateDatasetButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateDataset });
  }

  // Click cohort link. Open cohort build page.
  async openCohort(cohortName: string): Promise<CohortBuildPage> {
    const cohortLink = Link.findByName(this.page, { name: cohortName });
    await cohortLink.clickAndWait();
    await waitForNumericalString(this.page, FieldSelector.TotalCount, 60000);
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    return cohortBuildPage;
  }
}
