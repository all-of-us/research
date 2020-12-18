import WorkspaceCard from 'src/app/component/workspace-card';
import {Page} from 'puppeteer';
import {createWorkspace, signIn} from 'utils/test-utils';
import {config} from 'resources/workbench-config';
import WorkspaceDataPage from 'src/app/page/workspace-data-page';
import WorkspaceBase from 'src/app/page/workspace-base';
import WorkspaceEditPage from 'src/app/page/workspace-edit-page';
import CdrVersionUpgradeModal from 'src/app/page/cdr-version-upgrade-modal';

describe('Workspace CDR Version Upgrade modal', () => {
    beforeEach(async () => {
        await signIn(page);
    });

   test('Clicking Cancel and Upgrade buttons', async () => {

      const workspaceCard: WorkspaceCard = await createWorkspace(page, config.altCdrVersionName);
      const workspaceName = await workspaceCard.clickWorkspaceName();

      const workspacePage: WorkspaceBase = new WorkspaceDataPage(page);
      const cdrVersion = await workspacePage.getCdrVersion();
      expect(cdrVersion).toBe(config.altCdrVersionName);

      let modal = await launchCdrUpgradeModal(page);

      // Clicking the Cancel
      const modalCancelButton = await modal.getCancelButton();
      await modalCancelButton.click();

      // CDR version flag remains
      await workspacePage.getNewCdrVersionFlag();

      // Clicking the Upgrade button opens the Duplicate Workspace Page
      modal = await launchCdrUpgradeModal(page);
      const upgradeButton = await modal.getUpgradeButton();
      await upgradeButton.click();

      const duplicationPage = new WorkspaceEditPage(page);
      const upgradeMessage = await duplicationPage.getCdrVersionUpgradeMessage();
      expect(upgradeMessage).toContain(workspaceName);
      expect(upgradeMessage).toContain(`${config.altCdrVersionName} to ${config.defaultCdrVersionName}.`);

      const editCancelButton = await duplicationPage.getCancelButton();
      await editCancelButton.clickAndWait();

      // cleanup
      await workspacePage.deleteWorkspace()
    });

});

async function launchCdrUpgradeModal(page: Page): Promise<CdrVersionUpgradeModal> {
   const workspacePage: WorkspaceBase = new WorkspaceDataPage(page);

   // Clicking the CDR version upgrade flag pops up the upgrade modal
   const newVersionFlag = await workspacePage.getNewCdrVersionFlag();
   await newVersionFlag.click();

   const modal = new CdrVersionUpgradeModal(page);
   expect(await modal.isLoaded()).toBe(true);
   return modal;
}
