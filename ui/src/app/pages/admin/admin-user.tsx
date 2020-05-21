import * as fp from 'lodash/fp';
import * as React from 'react';

import {Component} from '@angular/core';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {TextInput, Toggle} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {institutionApi, profileApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  displayDateWithoutHours,
  reactStyles,
  ReactWrapperBase,
  withUrlParams
} from 'app/utils';

import {navigate, serverConfigStore} from 'app/utils/navigation';
import {Profile} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';

const styles = reactStyles({
  semiBold: {
    fontWeight: 600
  }
});

const freeCreditLimitOptions = [
  {label: '$300', value: 300},
  {label: '$350', value: 350},
  {label: '$400', value: 400},
  {label: '$450', value: 450},
  {label: '$500', value: 500},
  {label: '$550', value: 550},
  {label: '$600', value: 600},
  {label: '$650', value: 650},
  {label: '$700', value: 700},
  {label: '$750', value: 750},
  {label: '$800', value: 800}
];

const ReadonlyInputWithLabel = ({label, content, dataTestId, inputStyle = {}}) => {
  return <FlexColumn data-test-id={dataTestId} style={{marginTop: '1rem'}}>
    <label style={styles.semiBold}>{label}</label>
    <TextInput
        value={content || ''} // react yells at me if this is null
        disabled
        style={{
          backgroundColor: colorWithWhiteness(colors.primary, .95),
          opacity: '100%',
          width: '17.5rem',
          ...inputStyle
        }}
    />
  </FlexColumn>;
};

const DropdownWithLabel = ({label, options, initialValue, onChange, disabled, dataTestId, dropdownStyle = {}}) => {
  return <FlexColumn data-test-id={dataTestId} style={{marginTop: '1rem'}}>
    <label style={styles.semiBold}>{label}</label>
    <Dropdown
        style={{
          minWidth: '70px',
          width: '14rem',
          ...dropdownStyle
        }}
        options={options}
        onChange={(e) => onChange(e)}
        value={initialValue}
        disabled={disabled}
    />
  </FlexColumn>;
};

const ToggleWithLabelAndToggledText = ({label, initialValue, disabled, onToggle, dataTestId}) => {
  return <FlexColumn data-test-id={dataTestId} style={{width: '8rem', flex: '0 0 auto'}}>
    <label>{label}</label>
    <Toggle
        name={initialValue ? 'BYPASSED' : ''}
        checked={initialValue}
        disabled={disabled}
        onToggle={(checked) => onToggle(checked)}
        height={18}
        width={33}
    />
  </FlexColumn>;
};

interface Props {
  // From withUrlParams
  urlParams: {
    usernameWithoutGsuiteDomain: string
  };
}

interface State {
  institutionsLoadingError: string;
  loading: boolean;
  profile: Profile;
  profileLoadingError: string;
  saveDisabled: boolean;
  verifiedInstitutionOptions: Array<{label: string, value: {displayName: string, shortName: string}}>;
}


const AdminUser = withUrlParams()(class extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      institutionsLoadingError: '',
      loading: true,
      profile: null,
      profileLoadingError: '',
      saveDisabled: true,
      verifiedInstitutionOptions: []
    };
  }

  async componentDidMount() {
    try {
      await this.getUser();
      await this.getInstitutions();
    } finally {
      this.setState({loading: false});
    }
  }

  async getUser() {
    const {gsuiteDomain} = serverConfigStore.getValue();
    try {
      const profile = await profileApi().getUserByUsername(this.props.urlParams.usernameWithoutGsuiteDomain + "@" + gsuiteDomain);
      this.setState({profile: profile})
    } catch(error) {
      this.setState({profileLoadingError: 'Could not find user - please check spelling of username and try again'});
    }
  }

  async getInstitutions() {
    try {
      const institutionsResponse = await institutionApi().getInstitutions();
      const options = fp.map(
          institution => {
            return {
              'label': institution.displayName ? institution.displayName : institution.shortName,
              'value': {displayName: institution.displayName, shortName: institution.shortName}
            };
          },
          institutionsResponse.institutions
      );
      this.setState({verifiedInstitutionOptions: options});
    } catch (error) {
      this.setState({institutionsLoadingError: 'Could not get list of verified institutions - please try again later'});
    }
  }

  render() {
    const {profile} = this.state;
    return <FadeBox
        style={{
          margin: 'auto',
          paddingTop: '1rem',
          width: '96.25%',
          minWidth: '1232px',
          color: colors.primary
        }}
    >
      {this.state.institutionsLoadingError && <div>{this.state.institutionsLoadingError}</div>}
      {this.state.profileLoadingError && <div>{this.state.profileLoadingError}</div>}
      {this.state.profile && <FlexColumn>
        <FlexRow style={{alignItems: 'center'}}>
          <a onClick={() => navigate(['admin', 'users'])}>
            <ClrIcon
              shape='arrow'
              size={37}
              style={{
                backgroundColor: colorWithWhiteness(colors.accent, .85),
                color: colors.accent,
                borderRadius: '18px',
                transform: 'rotate(270deg)'
              }}
            />
          </a>
          <SmallHeader style={{marginTop: 0, marginLeft: '0.5rem'}}>
            User Profile Information
          </SmallHeader>
        </FlexRow>
        <FlexRow style={{width: '100%', marginTop: '1rem', alignItems: 'center', justifyContent: 'space-between'}}>
          <FlexRow
              style={{
                alignItems: 'center',
                backgroundColor: colorWithWhiteness(colors.primary, .85),
                borderRadius: '5px',
                padding: '0 .5rem',
                height: '1.625rem',
                width: '17.5rem'
              }}
          >
            <label style={{fontWeight: 600}}>
              Account access
            </label>
            <Toggle
                name={profile.disabled ? 'Disabled' : 'Enabled'}
                checked={!profile.disabled}
                disabled={true}
                data-test-id='account-access-toggle'
                onToggle={() => this.setState({saveDisabled: false})}
                style={{marginLeft: 'auto', paddingBottom: '0px'}}
                height={18}
                width={33}
            />
          </FlexRow>
          <Button type='primary' disabled={this.state.saveDisabled}>
            Save
          </Button>
        </FlexRow>
        <FlexRow>
          <FlexColumn style={{width: '33%', marginRight: '1rem'}}>
            <ReadonlyInputWithLabel
                label={'User name'}
                content={profile.givenName + ' ' + profile.familyName}
                dataTestId={'userFullName'}
            />
            <ReadonlyInputWithLabel
                label={'Registration state'}
                content={fp.capitalize(profile.dataAccessLevel.toString())}
                dataTestId={'registrationState'}
            />
            <ReadonlyInputWithLabel
                label={'Registration date'}
                content={profile.firstRegistrationCompletionTime ? displayDateWithoutHours(profile.firstRegistrationCompletionTime) : ''}
                dataTestId={'firstRegistrationCompletionTime'}
            />
            <ReadonlyInputWithLabel
                label={'Username'}
                content={profile.username}
                dataTestId={'username'}
            />
            <ReadonlyInputWithLabel
                label={'Contact email'}
                content={profile.contactEmail}
                dataTestId={'contactEmail'}
            />
            <ReadonlyInputWithLabel
                label={'Free credits used'}
                content={profile.freeTierUsage}
                inputStyle={{width: '6.5rem'}}
                dataTestId={'freeTierUsage'}
            />
          </FlexColumn>
          <FlexColumn style={{width: '33%'}}>
            <DropdownWithLabel
                label={'Free credit limit'}
                options={freeCreditLimitOptions}
                onChange={() => this.setState({saveDisabled: false})}
                initialValue={profile.freeTierDollarQuota}
                dropdownStyle={{width: '3rem'}}
                disabled={true}
                dataTestId={'freeTierDollarQuota'}
            />
            {this.state.verifiedInstitutionOptions && <DropdownWithLabel
                label={'Verified institution'}
                options={this.state.verifiedInstitutionOptions}
                onChange={() => this.setState({saveDisabled: false})}
                initialValue={
                  profile.verifiedInstitutionalAffiliation
                      ? profile.verifiedInstitutionalAffiliation.institutionShortName
                      : undefined
                }
                disabled={true}
                dataTestId={'verifiedInstitution'}
            />}
            <div style={{marginTop: '1rem', width: '15rem'}}>
              <label style={{fontWeight: 600}}>Bypass access to:</label>
              <FlexRow style={{marginTop: '.5rem'}}>
                <ToggleWithLabelAndToggledText
                    label={'2-factor auth'}
                    initialValue={!!profile.twoFactorAuthBypassTime}
                    disabled={true}
                    onToggle={() => this.setState({saveDisabled: false})}
                    dataTestId={'twoFactorAuthBypassToggle'}
                />
                <ToggleWithLabelAndToggledText
                    label={'Compliance training'}
                    initialValue={!!profile.complianceTrainingBypassTime}
                    disabled={true}
                    onToggle={() => this.setState({saveDisabled: false})}
                    dataTestId={'complianceTrainingBypassToggle'}
                />
              </FlexRow>
              <FlexRow style={{marginTop: '1rem'}}>
                <ToggleWithLabelAndToggledText
                    label={'eRA Commons'}
                    initialValue={!!profile.eraCommonsBypassTime}
                    disabled={true}
                    onToggle={(checked) => checked}
                    dataTestId={'eraCommonsBypassToggle'}
                />
                <ToggleWithLabelAndToggledText
                    label={'Data User Code of Conduct'}
                    initialValue={!!profile.dataUseAgreementBypassTime}
                    disabled={true}
                    onToggle={() => this.setState({saveDisabled: false})}
                    dataTestId={'dataUseAgreementBypassToggle'}
                />
              </FlexRow>
            </div>
          </FlexColumn>
        </FlexRow>
      </FlexColumn>}
      {this.state.loading && <SpinnerOverlay/>}
    </FadeBox>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class AdminUserComponent extends ReactWrapperBase {
  constructor() {
    super(AdminUser, []);
  }
}
