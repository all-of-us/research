import * as React from 'react';

import {Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {EditComponentReact} from 'app/icons/edit';
import {
  disseminateFindings,
  researchOutcomes,
  researchPurposeQuestions,
  SpecificPopulationItems
} from 'app/pages/workspace/workspace-edit-text';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {
  getSelectedResearchPurposeItems
} from 'app/utils/research-purpose';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';

const styles = reactStyles({
  mainHeader: {
    fontSize: '16px', fontWeight: 600, color: colors.primary, marginBottom: '0.5rem',
    display: 'flex', flexDirection: 'row', alignItems: 'center'
  },
  sectionContentContainer: {
    marginLeft: '1rem'
  },
  sectionHeader: {
    fontSize: '16px', fontWeight: 600, color: colors.primary, marginTop: '1rem'
  },
  sectionItemWithBackground: {
    padding: '10px',
    backgroundColor: colors.white,
    color: colors.primary,
    marginLeft: '0.5rem',
    borderRadius: '3px'
  },
  sectionSubHeader: {
    fontSize: '14px', fontWeight: 600, color: colors.primary
  },
  sectionText: {
    fontSize: '14px', lineHeight: '24px', color: colors.primary, marginTop: '0.3rem'
  },
});


export const ResearchPurpose = withCurrentWorkspace()(
  class extends React.Component<
    {workspace: WorkspaceData},
    {workspacePermissions: WorkspacePermissions}> {
    constructor(props) {
      super(props);
      this.state = {
        workspacePermissions: new WorkspacePermissions(props.workspace)
      };
    }

    // TODO: Move this to an abstract function and also change the admin workspace to use it
    getSelectedPopulationsOfInterest() {
      const researchPurpose = this.props.workspace.researchPurpose;
      const categories = SpecificPopulationItems.filter(specificPopulationItem => specificPopulationItem
        .subCategory.filter(item => researchPurpose.populationDetails.includes(item.shortName)).length > 0);
      categories.forEach(category => category.subCategory = category.subCategory
        .filter(subCategoryItem => researchPurpose.populationDetails.includes(subCategoryItem.shortName)));
      return categories;
    }

    render() {
      const {workspace} = this.props;
      const {workspacePermissions} = this.state;
      const selectedResearchPurposeItems = getSelectedResearchPurposeItems(this.props.workspace.researchPurpose);
      const selectedPopulationsOfInterest = this.getSelectedPopulationsOfInterest();
      return <FadeBox>
        <div style={styles.mainHeader}>Primary purpose of project
          <Clickable disabled={!workspacePermissions.canWrite}
                     style={{display: 'flex', alignItems: 'center'}}
                     data-test-id='edit-workspace'
                     onClick={() => navigate(
                       ['workspaces',  workspace.namespace, workspace.id, 'edit'])}>
            <EditComponentReact enableHoverEffect={true}
                                disabled={!workspacePermissions.canWrite}
                                style={{marginTop: '0.1rem', height: 22,
                                  width: 22,
                                  fill: colors.light,
                                  backgroundColor: colors.accent,
                                  cursor: 'pointer',
                                  padding: '5px',
                                  borderRadius: '23px'}}/>
          </Clickable>
        </div>
        <div style={styles.sectionContentContainer}>
          {selectedResearchPurposeItems.map((selectedResearchPurposeItem, i) => <div key={i}>
            <div style={{marginTop: '1rem'}}>{selectedResearchPurposeItem}</div>
          </div>)}
        </div>
        <div style={styles.sectionHeader}>Summary of research purpose</div>
        <div style={styles.sectionContentContainer}>
          <div style={styles.sectionSubHeader}>{researchPurposeQuestions[2].header}</div>
          <div style={{...styles.sectionItemWithBackground, minHeight: '6rem', padding: '15px'}}>
            {workspace.researchPurpose.intendedStudy}</div>
          <div style={styles.sectionSubHeader}>{researchPurposeQuestions[3].header}
          </div>
          <div style={{...styles.sectionItemWithBackground, minHeight: '6rem', padding: '15px'}}>
            {workspace.researchPurpose.scientificApproach}</div>
          <div style={styles.sectionSubHeader}>{researchPurposeQuestions[4].header}
          </div>
          <div style={{...styles.sectionItemWithBackground, minHeight: '6rem', padding: '15px'}}>
            {workspace.researchPurpose.anticipatedFindings}
          </div>
        </div>
        <div style={styles.sectionHeader}>Findings will be disseminate by the following:</div>
        <div style={styles.sectionContentContainer}>
          {workspace.researchPurpose.disseminateResearchFindingList.map(disseminateFinding =>
            <div style={{...styles.sectionItemWithBackground, marginTop: '0.5rem'}}>{disseminateFindings
              .find(finding => finding.shortName === disseminateFinding).label}</div>
          )}
        </div>
        <div style={styles.sectionHeader}>Outcomes anticipated from the research:</div>
        <div style={styles.sectionContentContainer}>
          {workspace.researchPurpose.researchOutcomeList.map(workspaceOutcome =>
            <div style={{...styles.sectionItemWithBackground, marginTop: '0.5rem'}}>{researchOutcomes
              .find(outcome => outcome.shortName === workspaceOutcome).label}</div>
          )}
        </div>
        <div style={styles.sectionHeader}>Population of interest</div>
        <div style={styles.sectionContentContainer}>
          {selectedPopulationsOfInterest.map(selectedPopulationOfInterest => {
            return <React.Fragment>
              <div style={{...styles.sectionSubHeader, marginTop: '0.5rem'}}>{selectedPopulationOfInterest.label}</div>
              {selectedPopulationOfInterest.subCategory.map(subCategory => <div style={{
                ...styles.sectionItemWithBackground, marginTop: '0.5rem'}}>{subCategory.label}</div>)}
            </React.Fragment>;
          })}
        </div>
      </FadeBox>;
    }
  }
);
