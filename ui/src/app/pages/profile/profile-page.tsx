import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button, Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {TextArea, TextInput, ValidationError} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import { getRegistrationTasksMap } from 'app/pages/homepage/registration-dashboard';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {Profile} from 'generated/fetch';
import {Gender, Race} from 'generated/fetch';
import {
  InstitutionAffiliation
} from '../../utils/institutionAffiliation';
import {AccountCreationSurvey} from '../login/account-creation/account-creation-survey';
import {ProfileRegistrationStepStatus} from './profile-registration-step-status';


const styles = reactStyles({
  h1: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 500,
    lineHeight: '24px'
  },
  inputLabel: {
    color: colors.primary,
    fontSize: 14, lineHeight: '18px',
    marginBottom: 6
  },
  inputStyle: {
    width: 250,
    marginRight: 20
  },
  longInputStyle: {
    height: 175, width: 420,
    resize: 'both'
  },
  box: {
    backgroundColor: colors.white,
    borderRadius: 8,
    padding: 21
  }
});

// validators for validate.js
const required = {presence: {allowEmpty: false}};
const notTooLong = maxLength => ({
  length: {
    maximum: maxLength,
    tooLong: 'must be %{count} characters or less'
  }
});
const validators = {
  givenName: {...required, ...notTooLong(80)},
  familyName: {...required, ...notTooLong(80)},
  currentPosition: {...required, ...notTooLong(255)},
  organization: {...required, ...notTooLong(255)},
  areaOfResearch: required,
};

export interface ProfileProp {
  profileState: { profile: Profile, reload: Function };
}

export interface ProfileState {
  profileEdits: Profile;
  showSurvey: boolean;
  updating: boolean;
}

export const ProfilePage = withUserProfile()(class extends React.Component<ProfileProp, ProfileState> {
  static displayName = 'ProfilePage';

  constructor(props) {
    super(props);

    this.state = {
      profileEdits: props.profileState.profile || {
        demographicSurvey: {
          race: [] as Race[],
          ethnicity: undefined,
          gender: [] as Gender[],
          yearOfBirth: 0,
          education: undefined,
          disability: false
        }
      },
      showSurvey: false,
      updating: false
    };
  }

  navigateToTraining(): void {
    window.location.assign(
      environment.trainingUrl + '/static/data-researcher.html?saml=on');
  }

  componentDidUpdate(prevProps) {
    const {profileState: {profile}} = this.props;

    if (!fp.isEqual(prevProps.profileState.profile, profile)) {
      this.setState({profileEdits: profile}); // for when profile loads after component load
    }
  }

  setSurvey(profile) {
    if (profile) {
      const demographicSurvey = profile.demographicSurvey;
      const profileEdits = {
        ...this.state.profileEdits,
        demographicSurvey
      };
      this.setState({profileEdits: profileEdits, showSurvey: false}, () => this.saveProfile());
    } else {
      this.setState({showSurvey: false});
    }
  }


  async saveProfile() {
    const {profileState: {reload}} = this.props;

    this.setState({updating: true});

    try {
      await profileApi().updateProfile(this.state.profileEdits);
      await reload();
    } catch (e) {
      console.error(e);
    } finally {
      this.setState({updating: false});
    }
  }

  updateAffiliations(affiliations) {
    this.setState(fp.set(['profileEdits', 'institutionalAffiliations'], affiliations));
    this.saveProfile();
  }


  get surveyComplete() {
    const demographicSurvey = this.props.profileState.profile.demographicSurvey;
    return demographicSurvey && demographicSurvey.gender !== null && demographicSurvey.gender.length > 0
         && demographicSurvey.yearOfBirth !== null &&
        demographicSurvey.race !== null && demographicSurvey.race.length > 0 &&
        demographicSurvey.ethnicity !== null && demographicSurvey.education !== null;
  }

  render() {
    const {profileState: {profile}} = this.props;
    const {profileEdits, updating} = this.state;
    const {
      givenName, familyName, currentPosition, organization, areaOfResearch,
      institutionalAffiliations = []
    } = profileEdits;
    const errors = validate({
      givenName, familyName, currentPosition, organization, areaOfResearch
    }, validators, {
      prettify: v => ({
        givenName: 'First Name',
        familyName: 'Last Name',
        areaOfResearch: 'Current Research'
      }[v] || validate.prettify(v))
    });

    const makeProfileInput = ({title, valueKey, isLong = false, ...props}) => {
      const errorText = profile && errors && errors[valueKey];

      const inputProps = {
        value: fp.get(valueKey, profileEdits) || '',
        onChange: v => this.setState(fp.set(['profileEdits', ...valueKey], v)),
        invalid: !!errorText,
        ...props
      };

      return <div style={{marginBottom: 40}}>
        <div style={styles.inputLabel}>{title}</div>
        {isLong ?
          <TextArea
            style={styles.longInputStyle}
            {...inputProps}
          /> :
          <TextInput
            style={styles.inputStyle}
            {...inputProps}
          />}
        <ValidationError>{errorText}</ValidationError>
      </div>;
    };

    const Section = (props) => {
      return <div style={{marginBottom: '1.2rem'}}>
        <div style={{...styles.h1, marginBottom: '0.5rem'}}>{props.header}</div>
        <label style={{color: colors.primary}}>{props.children}</label>
      </div>;
    };

    return <div style={{margin: '35px 35px 100px 45px'}}>
      {(!profile || updating) && <SpinnerOverlay/>}
      <div style={{...styles.h1, marginBottom: 30}}>Profile</div>
      {!this.state.showSurvey && <div style={{display: 'flex'}}>

      {!environment.enableAccountPages && <div style={{flex: '1 0 520px', paddingRight: 26}}>
        <div style={{display: 'flex'}}>
          {makeProfileInput({
            title: 'First Name',
            valueKey: 'givenName'
          })}
          {makeProfileInput({
            title: 'Last Name',
            valueKey: 'familyName'
          })}
        </div>
        {makeProfileInput({
          title: 'Contact Email',
          valueKey: 'contactEmail',
          disabled: true
        })}
        <div style={styles.inputLabel}>Username</div>
          <div style={{
            paddingLeft: '0.5rem', marginBottom: 20,
            height: '1.5rem',
            color: colors.primary
          }}>
            {profile && profile.username}
          </div>
          {makeProfileInput({
            title: 'Your Current Position',
            valueKey: 'currentPosition'
          })}
          {makeProfileInput({
            title: 'Your Organization',
            valueKey: 'organization'
          })}
          {makeProfileInput({
            title: <React.Fragment>
              Current Research Work
              <TooltipTrigger
                side='right'
                content='You are required to describe your current research in order to help
                  All of Us improve the Researcher Workbench.'
              >
                <ClrIcon
                  shape='info-standard'
                  className='is-solid'
                  style={{marginLeft: 10, verticalAlign: 'middle', color: colors.accent}}
                />
              </TooltipTrigger>
            </React.Fragment>,
            valueKey: 'areaOfResearch',
            isLong: true
          })}
          {makeProfileInput({
            title: 'About You',
            valueKey: 'aboutYou',
            isLong: true
          })}
          <div style={{...styles.h1, marginBottom: 24}}>Institution Affiliations</div>
          {institutionalAffiliations.map((v, i) =>
            <div style={{display: 'flex'}} key={`institution${i}`}>
              {makeProfileInput({
                title: 'Institution',
                valueKey: ['institutionalAffiliations', i, 'institution']
              })}
              {makeProfileInput({
                title: 'Role',
                valueKey: ['institutionalAffiliations', i, 'role']
              })}
              <Clickable
                style={{alignSelf: 'center'}}
                onClick={() => this.setState(fp.update(
                  ['profileEdits', 'institutionalAffiliations'],
                  fp.pull(v)
                ))}
              >
                <ClrIcon
                  shape='times'
                  size='24'
                  style={{color: colors.accent, marginBottom: 17}}
                />
              </Clickable>
            </div>
          )}
          <div style={{display: 'flex', width: 520, alignItems: 'center'}}>
            <div style={{border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`, flex: 1}}/>
            <Clickable
              onClick={() => this.setState(fp.update(
                ['profileEdits', 'institutionalAffiliations'],
                v => fp.concat(v, {institution: '', role: ''})
              ))}
            >
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
          <div style={{marginTop: 100, display: 'flex'}}>
            <Button type='link'
              onClick={() => this.setState({profileEdits: profile})}
            >
              Discard Changes
            </Button>
            <TooltipTrigger
              side='top'
              content={!!errors && 'You must correct errors before saving.'}
            >
              <Button
                data-test-id='save profile'
                type='purplePrimary'
                style={{marginLeft: 40}}
                onClick={() => this.saveProfile()}
                disabled={!!errors || fp.isEqual(profile, profileEdits)}
              >
                Save Profile
              </Button>
            </TooltipTrigger>
          </div>
        </div>}
      {environment.enableAccountPages && <div style={{flex: '1 0 520px', paddingRight: 26}}>
          <Section header='Name'>
            {profile.givenName} {profile.familyName}
          </Section>
          <Section header='Email'>
            {profile.contactEmail}
          </Section>
          <Section header='Address'>
            {profile && profile.address && profile.address.streetAddress1}
            {profile && profile.address && profile.address.streetAddress2}
            {profile && profile.address && profile.address.city}
            {profile && profile.address && profile.address.state}
            {profile && profile.address && profile.address.zipCode}
            {profile && profile.address && profile.address.country}
          </Section>
          <Section header='User name'>
            {profile && profile.username}
          </Section>
          <Section header='Institution Affiliation'>
            <InstitutionAffiliation affiliations={profile.institutionalAffiliations}
            updateProfile={(affiliations) => this.updateAffiliations(affiliations)}></InstitutionAffiliation>
          </Section>
        </div>}

        <div>
          <ProfileRegistrationStepStatus
            title='Google 2-Step Verification'
            wasBypassed={!!profile.twoFactorAuthBypassTime}
            incompleteButtonText='Set Up'
            completedButtonText={getRegistrationTasksMap()['twoFactorAuth'].completedText}
            completionTimestamp={getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(profile)}
            isComplete={!!(getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(profile))}
            completeStep={getRegistrationTasksMap()['twoFactorAuth'].onClick  } />

          <ProfileRegistrationStepStatus
            title='Access Training'
            wasBypassed={!!profile.complianceTrainingBypassTime}
            incompleteButtonText='Access Training'
            completedButtonText={getRegistrationTasksMap()['complianceTraining'].completedText}
            completionTimestamp={getRegistrationTasksMap()['complianceTraining'].completionTimestamp(profile)}
            isComplete={!!(getRegistrationTasksMap()['complianceTraining'].completionTimestamp(profile))}
            completeStep={getRegistrationTasksMap()['complianceTraining'].onClick} />

          {serverConfigStore.getValue().enableEraCommons && <ProfileRegistrationStepStatus
            title='eRA Commons Account'
            wasBypassed={!!profile.eraCommonsBypassTime}
            incompleteButtonText='Link'
            completedButtonText={getRegistrationTasksMap()['eraCommons'].completedText}
            completionTimestamp={getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile)}
            isComplete={!!(getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile))}
            completeStep={getRegistrationTasksMap()['eraCommons'].onClick} >
            <div>
              {profile.eraCommonsLinkedNihUsername != null && <React.Fragment>
                <div> Username: </div>
                <div> { profile.eraCommonsLinkedNihUsername } </div>
              </React.Fragment>}
              {profile.eraCommonsLinkExpireTime != null &&
              //  Firecloud returns eraCommons link expiration as 0 if there is no linked account.
              profile.eraCommonsLinkExpireTime !== 0
              && <React.Fragment>
                <div> Link Expiration: </div>
                <div>
                  { moment.unix(profile.eraCommonsLinkExpireTime)
                    .format('MMMM Do, YYYY, h:mm:ss A') }
                </div>
              </React.Fragment>}
            </div>
          </ProfileRegistrationStepStatus>}
          {serverConfigStore.getValue().enableDataUseAgreement && <ProfileRegistrationStepStatus
            title='Data Use Agreement'
            wasBypassed={!!profile.dataUseAgreementBypassTime}
            incompleteButtonText='Sign'
            completedButtonText={getRegistrationTasksMap()['dataUseAgreement'].completedText}
            completionTimestamp={getRegistrationTasksMap()['dataUseAgreement'].completionTimestamp(profile)}
            isComplete={!!(getRegistrationTasksMap()['dataUseAgreement'].completionTimestamp(profile))}
            completeStep={getRegistrationTasksMap()['dataUseAgreement'].onClick} >
            {profile.dataUseAgreementCompletionTime != null && <React.Fragment>
              <div> Agreement Renewal: </div>
              <div>
                { moment.unix(profile.dataUseAgreementCompletionTime / 1000)
                    .add(1, 'year')
                    .format('MMMM Do, YYYY') }
              </div>
            </React.Fragment>}
            <a
              onClick={getRegistrationTasksMap()['dataUseAgreement'].onClick}>
              View current agreement
            </a>
          </ProfileRegistrationStepStatus>}
          {/*TO DO CHANGE THE DATE TIME*/}
          {environment.enableAccountPages && <ProfileRegistrationStepStatus
              completeStep={() => this.setState({showSurvey: true})}
              title='Demographic Survey' incompleteButtonText='Complete Survey'
              wasBypassed={false} isComplete={this.surveyComplete} completedButtonText='Completed'
              completionTimestamp={''}/>}
        </div>
      </div>}
      {this.state.showSurvey && <AccountCreationSurvey invitationKey='' profile={profileEdits}
                                                       setProfile={(obj) => this.setSurvey(obj)}/>}
    </div>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class ProfilePageComponent extends ReactWrapperBase {
  constructor() {
    super(ProfilePage, []);
  }
}
