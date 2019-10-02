import {FlexColumn} from 'app/components/flex';
import {
  TextInput
} from 'app/components/inputs';
import {InstitutionalAffiliation} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import {EducationalRole, IndustryRole, NonAcademicAffiliation} from '../../generated/fetch';
import {Clickable} from '../components/buttons';
import {FlexRow} from '../components/flex';
import {ClrIcon} from '../components/icons';
import {ErrorMessage, RadioButton} from '../components/inputs';
import {Section} from '../pages/login/account-creation/account-creation';
import {AccountCreationOptions} from '../pages/login/account-creation/account-creation-options';
import colors, {colorWithWhiteness} from '../styles/colors';


interface AffiliationsRowDetails {
  edit: boolean;
  affiliation: InstitutionalAffiliation;
}

interface Props {
  affiliations: Array<InstitutionalAffiliation>;
  updateProfile: Function;
}

interface State {
  academicAffiliationList: Array<AffiliationsRowDetails>;
  nonAcademicAffiliationList: Array<AffiliationsRowDetails>;
  showRemoveError: boolean;
  showInstitution: boolean;

  nonAcademicAffiliationRole: string;
  showAcademicAffiliation: boolean;
  showNonAcademicAffiliationRole: boolean;
  showNonAcademicAffiliationOther: boolean;
  tempAffiliation: InstitutionalAffiliation;
  rolesOptions: any;
}

export const AdditionalAffiliation = (props) => {
  return <React.Fragment>
    <TextInput style={{width: '16rem', marginBottom: '0.5rem',
      marginTop: '0.5rem'}} value={props.affiliation} disabled={props.disabled}
               onChange={(value) =>
                   props.updateInstitutionAffiliation(value)}>
    </TextInput>
    <TextInput placeholder='Role' style={{width: '16rem', marginBottom: '0.5rem',
      marginTop: '0.5rem'}} value={props.role} disabled={props.disabled}
               onChange={(value) =>
        props.updateRoleAffiliation(value)}>
    </TextInput></React.Fragment>;
};

export const Footer = (props) => {
  return <FlexRow style={{justifyContent: 'flex-end'}}>
    {!props.edit && <Clickable
        style={{alignSelf: 'center'}}
        onClick={() => props.updateEdit()}>
      Edit
    </Clickable>}
    {props.edit && <React.Fragment><Clickable
        style={{alignSelf: 'center', justifyContent: 'flex-end', marginRight: '1rem'}}
        onClick={() => props.remove()}>
      Remove
    </Clickable>
      <Clickable
          style={{alignSelf: 'center', justifyContent: 'flex-end'}}
          onClick={() => props.save()}>
        Save
      </Clickable>
    </React.Fragment>
    }
  </FlexRow>;
};

export class InstitutionAffiliation extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      academicAffiliationList: [],
      nonAcademicAffiliationList: [],
      showRemoveError: false,
      showInstitution: true,
      rolesOptions: [],
      nonAcademicAffiliationRole: '',
      showAcademicAffiliation : true,
      showNonAcademicAffiliationRole: false,
      showNonAcademicAffiliationOther: false,
      tempAffiliation: undefined
    };
  }

  componentDidMount() {
    if (this.props.affiliations.length > 0) {

      const academicAffiliations = fp.cloneDeep(this.props.affiliations.filter((aff) =>
          aff.institution != null && aff.institution !== ''));
      const nonAcademicAffiliations = fp.cloneDeep(this.props.affiliations.filter((aff) =>
          aff.institution === null || aff.institution === ''));

      const editAcademic = [];
      const editNonAcademic = [];
      academicAffiliations.forEach((academic) => editAcademic.push({edit: false, affiliation: academic}));

      nonAcademicAffiliations.forEach((nonAcademic) => editNonAcademic.push({edit: false, affiliation: nonAcademic}));

      this.setState({academicAffiliationList : editAcademic, nonAcademicAffiliationList: editNonAcademic});
    }
  }

  updateInstitutionAffiliation(attribute, value, index) {
    const affiliationsList = this.state.showInstitution ? this.state.academicAffiliationList : this.state.nonAcademicAffiliationList;

    if (!affiliationsList || affiliationsList.length === 0) {
      const affiliationObj = {institution: '', nonAcademicAffiliation: undefined, role: '', other: ''};
      affiliationsList[index] = {affiliation: affiliationObj, edit: false};
    } else {
      const propsIndex = fp.findIndex(affiliationsList[index].affiliation, this.props.affiliations);
      if (propsIndex > -1) {
        this.props.affiliations[index][attribute] = value;
      }
    }
    affiliationsList[index].affiliation[attribute] = value;

    this.state.showInstitution ?
      this.setState({academicAffiliationList: affiliationsList})
        : this.setState({nonAcademicAffiliationList: affiliationsList});
  }

  selectNonAcademicAffiliationRoles(role) {
    if (this.showFreeTextField(role)) {
      this.setState({nonAcademicAffiliationRole: role, showNonAcademicAffiliationOther: true});
    } else {
      this.setState({nonAcademicAffiliationRole: role, showNonAcademicAffiliationOther: false});
    }
    this.temporaryList('role', role);

  }

  updateNonAcademicAffiliationRoles(nonAcademicAffiliation) {
    this.setState({showNonAcademicAffiliationRole: false, showNonAcademicAffiliationOther: false});
    if (nonAcademicAffiliation === NonAcademicAffiliation.INDUSTRY) {
      this.setState({rolesOptions: AccountCreationOptions.industryRole,
        showNonAcademicAffiliationRole: true});
    } else if (nonAcademicAffiliation === NonAcademicAffiliation.EDUCATIONALINSTITUTION) {
      this.setState({rolesOptions: AccountCreationOptions.educationRole, showNonAcademicAffiliationRole: true});
    } else if (this.showFreeTextField(nonAcademicAffiliation)) {
      this.setState({showNonAcademicAffiliationOther: true});
      return;
    }
    this.selectNonAcademicAffiliationRoles(this.state.nonAcademicAffiliationRole);
    this.temporaryList('nonAcademicAffiliation', nonAcademicAffiliation);

  }

  showFreeTextField(option) {
    return option === NonAcademicAffiliation.FREETEXT || option === IndustryRole.FREETEXT ||
        option === EducationalRole.FREETEXT;
  }

  isOnlyAcademicAffiliations() {
    return !this.state.academicAffiliationList || this.state.academicAffiliationList.length === 0;
  }

  // change name
  isUnique() {
    return this.state.showInstitution && this.isOnlyAcademicAffiliations() ||
        !this.state.showInstitution && this.isOnlyNonAcademicAffiliations();
  }

  isOnlyNonAcademicAffiliations() {
   return !this.state.nonAcademicAffiliationList || this.state.nonAcademicAffiliationList.length === 0;
 }

  updateEdit(index) {
    const edit = this.state.showInstitution ? this.state.academicAffiliationList : this.state.nonAcademicAffiliationList;
    edit[index].edit = !edit[index].edit;
    this.state.showInstitution ? this.setState({academicAffiliationList: edit}) : this.setState({nonAcademicAffiliationList: edit});
  }

  addNewAffiliations() {
    if (this.state.showInstitution) {
      this.setState(fp.update(
          ['academicAffiliationList'],
        v => fp.concat(v, {affiliation: {institution: '', role: '', nonAcademicAffiliation: '', other: ''}, edit: true})
      ));
    } else {
      this.setState(fp.update(
          ['nonAcademicAffiliationList'],
        v => fp.concat(v, {affiliation: {institution: '', role: '', nonAcademicAffiliation: '', other: ''}, edit: true})
      ));
    }
  }

  save(index) {
    const {affiliations} = this.props;
    affiliations.push(this.state.showInstitution ? this.state.academicAffiliationList[index].affiliation :
        this.state.nonAcademicAffiliationList[index].affiliation);
    this.props.updateProfile(affiliations);
  }

  remove(index) {
    if ((this.state.showInstitution && this.state.academicAffiliationList.length === 1) ||
        (!this.state.showInstitution && this.state.nonAcademicAffiliationList.length === 1) ) {
      setTimeout(() => {
        if (this.state.showRemoveError) {
          this.setState({showRemoveError: false});
          return;
        }
        return false;
      }, 1000);
      this.setState({showRemoveError: true});
      return;

    }
    const {affiliations} = this.props;
    const existingAffiliationsList = this.state.showInstitution ? this.state.academicAffiliationList : this.state.nonAcademicAffiliationList;
    const affilationToRemove = existingAffiliationsList[index].affiliation;
    const propIndex = fp.findIndex(affilationToRemove, affiliations);
    existingAffiliationsList.splice(index, 1);
    if (propIndex > -1) {
      this.props.affiliations.splice(propIndex, 1);
      this.props.updateProfile(existingAffiliationsList.map((affiliationList) => affiliationList.affiliation));
    }

    this.state.showInstitution ? this.setState({academicAffiliationList: existingAffiliationsList}) :
        this.setState({nonAcademicAffiliationList: existingAffiliationsList}) ;
  }

  saveTemp() {
    const {affiliations} = this.props;
    affiliations.push(this.state.tempAffiliation);
    this.props.updateProfile(affiliations);
  }

  temporaryList(attribute, value) {
    let existingAffiliationsList = {institution: '', role: '', other: '', nonAcademicAffiliation: 0} as InstitutionalAffiliation;
    if (this.state.tempAffiliation && (this.state.tempAffiliation.institution || this.state.tempAffiliation.nonAcademicAffiliation)) {
      existingAffiliationsList = this.state.tempAffiliation;
    }
    existingAffiliationsList[attribute] = value;

    this.setState({tempAffiliation: existingAffiliationsList});
  }

  render() {
    const {showRemoveError, academicAffiliationList,
      nonAcademicAffiliationList, showInstitution,
      tempAffiliation} = this.state;
    return <div><Section header='Institutional Affiliation'>
      <label style={{color: colors.primary, fontSize: 16}}>
        Are you affiliated with an Academic Research Institution?
      </label>
      {showRemoveError && <ErrorMessage>Cannot have an empty list please edit the values</ErrorMessage>}
      <div style={{paddingTop: '0.5rem'}}>
        <RadioButton onChange={() => {this.setState({showInstitution: true}); }}
                     onClick={() => this.setState({tempAffiliation: {}})}
                     checked={showInstitution} style={{marginRight: '0.5rem'}}/>
        <label style={{paddingRight: '3rem', color: colors.primary}}>
          Yes
        </label>
        <RadioButton onChange={() => {this.setState({showInstitution: false}); }}
                     onClick={() => this.setState({tempAffiliation: {}})}
                     checked={!showInstitution} style={{marginRight: '0.5rem'}}/>
        <label style={{color: colors.primary}}>No</label>
      </div>
    </Section>

      {/* Display Academic affiliation with no entries for institution name show dropdowns */}

      {showInstitution &&
      this.isOnlyAcademicAffiliations() && academicAffiliationList.length <= 1 &&
        <FlexColumn style={{justifyContent: 'space-between', width: '55%'}}>
            <TextInput data-test-id='institutionname' style={{width: '16rem', marginBottom: '0.5rem',
              marginTop: '0.5rem'}}
                       value={tempAffiliation ? tempAffiliation.institution : ''}
                       placeholder='Institution Name'
                       onChange={value => this.temporaryList('institution', value)}
            ></TextInput>
            <Dropdown data-test-id='institutionRole' value={tempAffiliation ? tempAffiliation.role : ''}
                      onChange={e => this.temporaryList('role', e.value)}
                      placeholder='Which of the following describes your role'
                      style={{width: '16rem'}} options={AccountCreationOptions.roles}/>
          <FlexRow style={{justifyContent: 'flex-end'}}>
           <Clickable
                style={{alignSelf: 'center', justifyContent: 'flex-end'}}
                onClick={() => this.saveTemp()}>
              Save
            </Clickable>
          </FlexRow>
          </FlexColumn>

     }
      {/*Show the list of academic affiliations*/}
      {showInstitution && academicAffiliationList && academicAffiliationList.map((af, index) => {
        return <FlexColumn style={{justifyContent: 'space-between', width: '57%'}}>
          <AdditionalAffiliation affiliation={af.affiliation.institution}
                               role={af.affiliation.role}
                               updateInstitutionAffiliation={(value) =>
                                   this.updateInstitutionAffiliation('institution', value, index)}
                               updateRoleAffiliation={(value) =>
                                   this.updateInstitutionAffiliation('role', value, index)}
          disabled={!af.edit}/>
          <Footer edit={af.edit} updateEdit={() => this.updateEdit(index)}
                  remove={() => this.remove(index)} save={() => this.save(index)}/>
        </FlexColumn>;
      })}

      {/*Non academic affilations list*/}
      {!showInstitution && nonAcademicAffiliationList && nonAcademicAffiliationList.map((af, index) => {
        return <FlexColumn style={{justifyContent: 'space-between', width: '57%'}}>
          <AdditionalAffiliation affiliation={af.affiliation.nonAcademicAffiliation}
                               role={af.affiliation.role}
                               updateInstitutionAffiliation={(value) =>
                                   this.updateInstitutionAffiliation('nonAcademicAffiliation', value, index)}
                               updateRoleAffiliation={(value) =>
                                   this.updateInstitutionAffiliation('role', value, index)}
                               disabled={!af.edit}/>
          <Footer edit={af.edit}
                  updateEdit={() => this.updateEdit(index)} remove={() => this.remove(index)} save={() => this.save(index)}/>
        </FlexColumn>;
      })}

      {/*Non academic affiliation with no entries, show dropdowns*/}
      {!showInstitution && this.isOnlyNonAcademicAffiliations() && nonAcademicAffiliationList.length < 1
      && <FlexColumn style={{justifyContent: 'space-between', width: '55%'}}>
        <Dropdown data-test-id='affiliation'
                  style={{width: '18rem', marginBottom: '0.5rem', marginTop: '0.5rem'}}
                  value={tempAffiliation ? tempAffiliation.nonAcademicAffiliation : ''}
                  options={AccountCreationOptions.nonAcademicAffiliations}
                  onChange={e => this.updateNonAcademicAffiliationRoles(e.value)}
                  placeholder='Which of the following better describes your affiliation?'/>
        {this.state.showNonAcademicAffiliationRole &&
        <Dropdown data-test-id='affiliationrole' placeholder='Which of the following describes your role'
                  options={this.state.rolesOptions} value={tempAffiliation
        ? tempAffiliation.role : ''}
                  onChange={e => this.selectNonAcademicAffiliationRoles(e.value)}
                  style={{width: '18rem'}}/>}
        {this.state.showNonAcademicAffiliationOther &&
        <TextInput value={tempAffiliation ? tempAffiliation.other : ''}
                   onChange={value => this.temporaryList('other', value)}
                   style={{marginTop: '1rem', width: '18rem'}}/>}
        <FlexRow style={{justifyContent: 'flex-end'}}>
          <Clickable
              style={{alignSelf: 'center', justifyContent: 'flex-end'}}
              onClick={() => this.saveTemp()}>
            Save
          </Clickable>
        </FlexRow>
      </FlexColumn>}

      {!this.isUnique() && <React.Fragment>
        <div style={{
          color: colors.primary,
          fontSize: 20,
          fontWeight: 500,
          lineHeight: '24px', marginBottom: 24
        }}>
          Additional Affiliations
        </div>
        <div style={{display: 'flex', width: 520, alignItems: 'center'}}>
          <div style={{border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`, flex: 1}}/>
          <Clickable
              onClick={() => this.addNewAffiliations()}>
            <ClrIcon
                shape='plus-circle'
                size='19'
                style={{
                  color: colors.accent,
                  margin: '0 14px',
                  flex: 'none', verticalAlign: 'text-bottom' // text-bottom makes it centered...?
                }}
            />
          </Clickable>
          <div style={{border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`, flex: 1}}/>
        </div>
      </React.Fragment>
      }


    </div>;
  }
}
