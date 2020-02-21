import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {FormSection} from 'app/components/forms';
import {ListPageHeader} from 'app/components/headers';
import {CheckBox, RadioButton} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {toggleIncludes} from 'app/utils';
import {Profile} from 'generated/fetch';
import {Section, TextInputWithLabel} from './account-creation';
import {AccountCreationOptions} from './account-creation-options';

const styles = {
  checkbox: {height: 17, width: 17, marginTop: '0.15rem'},
  checkboxWrapper: {display: 'flex', alignItems: 'flex-start', width: '13rem', marginBottom: '0.5rem'},
  checkboxLabel: {
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    fontWeight: 400,
    paddingLeft: '0.25rem',
    paddingRight: '0.5rem'
  },
  checkboxAreaContainer: {
    justifyContent: 'flex-start',
    flexWrap: 'wrap',
    height: '9rem',
    width: '26rem'
  }
};

export const DropDownSection = (props) => {
  return <Section header={props.header}>
    <Dropdown placeholder='Select' options={props.options} style={{width: '50%'}}
              value={props.value}
              onChange={(e) => props.onChange(e.value)}/>
  </Section>;
};

export interface AccountCreationSurveyProps {
  invitationKey: string;
  termsOfServiceVersion?: number;
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}

export interface AccountCreationState {
  creatingAccount: boolean;
  profile: Profile;
}

export class AccountCreationSurvey extends React.Component<AccountCreationSurveyProps, AccountCreationState> {
  constructor(props: any) {
    super(props);
    this.state = {
      creatingAccount: false,
      profile: {...this.props.profile},
    };
  }

  // TODO: we should probably bump this logic out of the survey component and either into its own
  // component or into the top-level SignIn component. The fact that we're awkwardly passing the
  // invitation key and tos version into this component (for the sole purpose of relaying this data
  // to the backend) is a telltale sign that this should be refactored.
  createAccount(): void {
    const {invitationKey, termsOfServiceVersion, onComplete} = this.props;
    this.setState({creatingAccount: true});
    profileApi().createAccount({
      profile: this.state.profile,
      invitationKey: invitationKey,
      termsOfServiceVersion: termsOfServiceVersion
    })
      .then((savedProfile) => {
        this.setState({profile: savedProfile, creatingAccount: false});
        onComplete(savedProfile);
      }).catch(error => {
        console.log(error);
        this.setState({creatingAccount: false});
        // TODO: we need to show some user-facing error message when this fails.
      });
  }

  updateList(key, value) {
    // Toggle Includes removes the element if it already exist and adds if not
    const attributeList = toggleIncludes(value, this.state.profile.demographicSurvey[key] || []);
    this.updateDemographicAttribute(key, attributeList);
  }

  updateDemographicAttribute(attribute, value) {
    this.setState(fp.set(['profile', 'demographicSurvey', attribute], value));
  }

  createOptionCheckbox(optionKey: string, optionObject: any) {
    const {profile: {demographicSurvey}} = this.state;
    const initialValue = demographicSurvey[optionKey] && demographicSurvey[optionKey].includes(optionObject.value);

    return <CheckBox label={optionObject.label} data-test-id={'checkbox-' + optionObject.value.toString()}
                     style={styles.checkbox} key={optionObject.value.toString()}
                     checked={initialValue}
                     wrapperStyle={styles.checkboxWrapper} labelStyle={styles.checkboxLabel}
                     onChange={(value) => this.updateList(optionKey, optionObject.value)}
    />;
  }

  render() {
    const {profile: {demographicSurvey}, creatingAccount} = this.state;
    const validationCheck = {
      lgbtqIdentity: {
        length: {
          maximum: 255,
          tooLong: 'is too long for our system. Please reduce to 255 or fewer characters.'
        }
      },
    };
    const errors = validate(demographicSurvey, validationCheck);

    return <div style={{marginTop: '1rem', paddingLeft: '3rem', width: '26rem'}}>
      <label style={{color: colors.primary, fontSize: 16}}>
        Please complete Step 2 of 2
      </label>
      <ListPageHeader>
        Demographics Survey <label style={{fontSize: '12px', fontWeight: 400}}>
        (All Survey Fields are optional)</label>
      </ListPageHeader>

      {/*Race section*/}
      <Section header='Race'>
        <FlexColumn style={styles.checkboxAreaContainer}>
          {AccountCreationOptions.race.map((race) => {
            return this.createOptionCheckbox('race', race);
          })}
        </FlexColumn>
      </Section>

      {/*Ethnicity section*/}
      <DropDownSection data-test-id='dropdown-ethnicity'
                       header='Ethnicity' options={AccountCreationOptions.ethnicity}
                       value={demographicSurvey.ethnicity}
                       onChange={(e) => this.updateDemographicAttribute('ethnicity', e)}/>
      <Section header='Do you identify as lesbian, gay, bisexual, transgender, queer (LGBTQ),
or another sexual and/or gender minority?'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-yes' id='identifiesAsLgbtqYes' onChange={
              (e) => this.updateDemographicAttribute('identifiesAsLgbtq', true)}
                         checked={demographicSurvey.identifiesAsLgbtq === true}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='identifiesAsLgbtqYes' style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-no' id='identifiesAsLgbtqNo' onChange={(e) => this.updateDemographicAttribute('identifiesAsLgbtq', false)}
                         checked={demographicSurvey.identifiesAsLgbtq === false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='identifiesAsLgbtqNo' style={{color: colors.primary}}>No</label>
          </FlexRow>
        </FlexColumn>
        <label></label>
        <TextInputWithLabel labelText='If yes, please tell us about your LGBTQ+ identity'
                            value={demographicSurvey.lgbtqIdentity} inputName='lgbtqIdentity'
                            containerStyle={{width: '26rem', marginTop: '0.5rem'}} inputStyle={{width: '26rem'}}
                            onChange={(value) => this.updateDemographicAttribute('lgbtqIdentity', value)}
                            disabled={!demographicSurvey.identifiesAsLgbtq}/>
      </Section>

      {/*Gender Identity section*/}
      <Section header='Gender Identity'>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          {AccountCreationOptions.genderIdentity.map((genderIdentity) => {
            return this.createOptionCheckbox('genderIdentityList', genderIdentity);
          })}
        </FlexColumn>
      </Section>

      {/*Sex at birth section*/}
      <Section header='Sex at birth'>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          {AccountCreationOptions.sexAtBirth.map((sexAtBirth) => {
            return this.createOptionCheckbox('sexAtBirth', sexAtBirth);
          })}
        </FlexColumn>
      </Section>

      {/*Year of birth section*/}
      <DropDownSection header='Year of Birth' options={AccountCreationOptions.Years}
                       value={demographicSurvey.yearOfBirth}
                       onChange={(e) => this.updateDemographicAttribute('yearOfBirth', e)}
      />
      {/*Disability section*/}
      <Section header='Do you have a Physical or Cognitive disability?'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='disabilityYes' onChange={
              (e) => this.updateDemographicAttribute('disability', true)}
                         checked={demographicSurvey.disability === true}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='disabilityYes' style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='disabilityNo' onChange={(e) => this.updateDemographicAttribute('disability', false)}
                         checked={demographicSurvey.disability === false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='disabilityNo' style={{color: colors.primary}}>No</label>
          </FlexRow>
        </FlexColumn>
      </Section>
      {/*Education section*/}
      <DropDownSection header='Highest Level of Education Completed'
                       options={AccountCreationOptions.levelOfEducation}
                       value={demographicSurvey.education}
                       onChange={
                         (e) => this.updateDemographicAttribute('education', e)}/>
      <FormSection style={{paddingBottom: '1rem'}}>
        <Button type='secondary' style={{marginRight: '1rem'}} disabled={creatingAccount}
                onClick={() => this.props.onPreviousClick(this.state.profile)}>
          Previous
        </Button>
        <TooltipTrigger content={errors && <React.Fragment>
            <div>Please review the following: </div>
            <ul>
              {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
            </ul>
        </React.Fragment>}>
          <Button type='primary' disabled={creatingAccount || creatingAccount || errors}
                  onClick={() => this.createAccount()}>
            Submit
          </Button>
        </TooltipTrigger>
      </FormSection>
      {creatingAccount && <SpinnerOverlay />}
    </div>;
  }
}
