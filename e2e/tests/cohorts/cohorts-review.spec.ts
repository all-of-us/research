import {createWorkspace, isValidDate, signIn} from 'utils/test-utils';
import {Option, LinkText, ResourceCard} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import CohortBuildPage from 'app/page/cohort-build-page';
import CohortParticipantDetailPage from 'app/page/cohort-participant-detail-page';
import CohortReviewModal from 'app/page/cohort-review-modal';
import CohortReviewPage from 'app/page/cohort-review-page';
import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {waitForText, waitWhileLoading} from 'utils/waits-utils';
import {getPropValue} from 'utils/element-utils';
import SidebarContent, {ReviewStatus} from 'app/component/sidebar-content';
import AnnotationFieldModal from 'app/component/annotation-field-modal';


describe('Cohort review tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * Find an existing workspace or create a new workspace if none exists.
   * Create a new Cohort from Criteria: Visits -> Out-Patient Visit.
   * Create a Review Set for 100 participants.
   * Verify some UI.
   * Delete Cohort in Cohort Build page.
   */
  test('Create Cohort and a Review Set for 100 participants', async () => {
    const reviewSetNumberOfParticipants = 100;

    await createWorkspace(page).then(card => card.clickWorkspaceName());

    const dataPage = new WorkspaceDataPage(page);
    const cohortCard = await dataPage.createCohort();
    const cohortName = await cohortCard.getResourceName();
    console.log(`Created Cohort: "${cohortName}"`);

    await cohortCard.selectSnowmanMenu(Option.Review);
    const modal = new CohortReviewModal(page);
    await modal.fillInNumberOfPartcipants(reviewSetNumberOfParticipants);
    await modal.clickButton(LinkText.CreateSet);
    console.log(`Created Review Set with ${reviewSetNumberOfParticipants} participants.`);

    const cohortReviewPage = new CohortReviewPage(page);
    await cohortReviewPage.waitForLoad();
    await waitForText(page, `Review Sets for ${cohortName}`);

    // Verify table pagination records count.
    const participantsTable = cohortReviewPage.getDataTable();
    const records = await participantsTable.getNumRecords();
    // Table records page numbering is in "1 - 25 of 100 records" format.
    expect(Number(records[0])).toEqual(1);
    expect(Number(records[1])).toEqual(25);
    expect(Number(records[2])).toEqual(reviewSetNumberOfParticipants);

    // Verify table column names match.
    const columns = ['Participant ID', 'Date of Birth', 'Deceased', 'Sex at Birth', 'Gender', 'Race', 'Ethnicity', 'Status'];
    const columnNames = await participantsTable.getColumnNames();
    expect(columnNames).toHaveLength(columns.length);
    expect(columnNames.sort()).toEqual(columns.sort());

    // Get Date of Birth in row 2.
    const dobCell = await participantsTable.getCell(2, 2);
    const cellValue = await getPropValue<string>(dobCell, 'textContent');
    // Check birth date is valid format.
    isValidDate(cellValue);

    // Check table row link navigation works. Click ParticipantId link in the second row.
    const dataTablepid1 = await cohortReviewPage.clickParticipantLink(2);
    
    // Not checking anything in Participant Detail page.
    const participantDetailPage = new CohortParticipantDetailPage(page);
     await participantDetailPage.waitForLoad();

    // click on the pen icon to open the participant
     await participantDetailPage.clickPenIconHelpSideBar();
    // confirm that the sidebar-content opened
    
    const sidebarContent = new SidebarContent(page);
    const reviewParticipantid1 = await sidebarContent.getParticipantID(); 
    console.log(`reviewParticipantid1: ${reviewParticipantid1}`);

    expect(dataTablepid1).toEqual(reviewParticipantid1);
    
    // select review status from dropdown option
    const participantStatus1 = await sidebarContent.selectReviewStatus(ReviewStatus.Excluded);
    
    // click on the plus-icon next to annotations 
    await sidebarContent.getAnnotationsButton().then(btn => btn.click());
    // the annotations modal displays
    const annotationFieldModal = new AnnotationFieldModal(page);    
    // click cancel button onthe annotation modal
    await annotationFieldModal.cancelAnnotationButton().then(btn => btn.click());
    // close the sidebar content 
    await participantDetailPage.clickPenIconHelpSideBar();

    // navigate to the next participant
    await participantDetailPage.goToTheNextParticipant();
    await participantDetailPage.waitForLoad();
   
    // get the participant ID on the detail page
    const detailPageParticipantid = await participantDetailPage.getParticipantIDnum();
     // click on the pen icon to open the sidebar
    await participantDetailPage.clickPenIconHelpSideBar();
    await waitWhileLoading(page); 
    // get the participant ID on the sidebar content
    const reviewParticipantid2 = await sidebarContent.getParticipantID(); 
    console.log(`reviewParticipantid2: ${reviewParticipantid2}`);
    // validate that the participant ID on detail page and the sidebar content match
    expect(detailPageParticipantid).toEqual(reviewParticipantid2);

    // select a review status
    const participantStatus2 = await sidebarContent.selectReviewStatus(ReviewStatus.Included);

    // navigate to review set page and check if the status column is displaying the review status for both participants
    await participantDetailPage.getBackToReviewSetButton().then(btn => btn.click());
    await waitWhileLoading(page);

    // Get the status of participant1
    const statusCell1 = await participantsTable.getCell(2, 8);
    const statusValue1 = await getPropValue<string>(statusCell1, 'textContent');
    expect(statusValue1).toEqual(participantStatus1);
    console.log(`${reviewParticipantid1}: ${statusValue1}`);

    // Get the status of participant2
    const statusCell2 = await participantsTable.getCell(3, 8);
    const statusValue2 = await getPropValue<string>(statusCell2, 'textContent');
    expect(statusValue2).toEqual(participantStatus2);
    console.log(`${reviewParticipantid2}: ${statusValue2}`);

    // return to cohort review page
    await cohortReviewPage.getBackToCohortButton().then(btn => btn.clickAndWait());

    // Land on Cohort Build page
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Land on the Data Page & click the Cohort Reviews SubTab
    await dataPage.openCohortReviewsSubtab();

    // Rename Cohort Review 
    const newCohortReviewName = makeRandomName();
    await dataPage.renameResource(cohortName, newCohortReviewName, ResourceCard.CohortReview);

    // Verify Rename Cohort Review is successful.
    expect(await DataResourceCard.findCard(page, newCohortReviewName)).toBeTruthy();

     // Delete Cohort Review
     const modalTextContent = await dataPage.deleteResource(newCohortReviewName, ResourceCard.CohortReview);

     // Verify Delete Cohort Review dialog content text
     expect(modalTextContent).toContain(`Are you sure you want to delete Cohort Review: ${newCohortReviewName}?`);
 
     // Verify Delete Cohort Review successful.
     expect(await DataResourceCard.findCard(page, newCohortReviewName, 5000)).toBeFalsy();

     // Delete workspace
    await dataPage.deleteWorkspace();
  });

});
