import {Button, IconButton} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Toggle} from 'app/components/inputs';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';
import {AccessModule, Profile} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';

const styles = reactStyles({
  infoIcon: {
    color: colors.accent,
    cursor: 'pointer',
    marginBottom: '0.5rem',
    height: '16px',
    width: '16px',
    alignSelf: 'center'
  }
});

export class AdminUserBypass extends React.Component<
    { profile: Profile},
    { initialModules: AccessModule[], selectedModules: AccessModule[],
      open: boolean} > {

  constructor(props) {
    super(props);
    const {profile} = props;
    const initialModules = [
      ...(profile.betaAccessBypassTime ? [AccessModule.BETAACCESS] : []),
      ...(profile.complianceTrainingBypassTime ? [AccessModule.COMPLIANCETRAINING] : []),
      ...(profile.dataUseAgreementBypassTime ? [AccessModule.DATAUSEAGREEMENT] : []),
      ...(profile.eraCommonsBypassTime ? [AccessModule.ERACOMMONS] : []),
      ...(profile.twoFactorAuthBypassTime ? [AccessModule.TWOFACTORAUTH] : []),
    ];
    this.state = {
      open: false,
      initialModules: initialModules,
      selectedModules: initialModules
    };
  }

  cancel() {
    const {initialModules} = this.state;
    this.setState({selectedModules: initialModules});
  }

  save() {
    const {initialModules, selectedModules} = this.state;
    const {profile} = this.props;
    const changedModules = fp.xor(initialModules, selectedModules);
    changedModules.forEach(async module => {
      await profileApi()
        .bypassAccessRequirement(profile.userId,
          {isBypassed: selectedModules.includes(module), moduleName: module});
    });

    this.setState({initialModules: selectedModules});
  }

  hasEdited(): boolean {
    return fp.xor(this.state.selectedModules, this.state.initialModules).length !== 0;
  }

  render() {
    const {selectedModules, open} = this.state;
    const {enableBetaAccess,
      enableComplianceTraining,
      enableEraCommons,
      enableDataUseAgreement} = serverConfigStore.getValue();
    return <PopupTrigger
        side='bottom'
        onClose={() => {this.cancel(); this.setState({open: false}); }}
        onOpen={() => this.setState({open: true})}
        content={<FlexColumn style={{padding: '1rem'}}>
          {enableBetaAccess && <FlexRow style={{justifyContent: 'space-between'}}>
            <Toggle name='Beta Access'
                    initialValue={selectedModules.includes(AccessModule.BETAACCESS)}
                    data-test-id='beta-access-toggle'
                    onToggle={() => {this.setState({selectedModules:
                      fp.xor(selectedModules, [AccessModule.BETAACCESS])}); } }
                    height={28}
                    width={56}
            />
            <TooltipTrigger content={'Grant beta access to a user.  This replaces verify/reject.'}>
              <ClrIcon shape='info' className='is-solid' style={styles.infoIcon}/>
            </TooltipTrigger>
          </FlexRow>}
          {enableBetaAccess && <hr style={{width: '100%', marginBottom: '0.5rem'}}/>}
          {enableComplianceTraining && <Toggle name='Compliance Training'
                  initialValue={selectedModules.includes(AccessModule.COMPLIANCETRAINING)}
                  data-test-id='compliance-training-toggle'
                  onToggle={() => {this.setState({selectedModules:
                      fp.xor(selectedModules, [AccessModule.COMPLIANCETRAINING])}); } }
                  height={28}
                  width={56}
          />}
          {enableEraCommons && <Toggle name='eRA Commons Linking'
                  initialValue={selectedModules.includes(AccessModule.ERACOMMONS)}
                  data-test-id='era-commons-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.ERACOMMONS])}); } }
                  height={28}
                  width={56}
          />}
          <Toggle name='Two Factor Auth'
                  initialValue={selectedModules.includes(AccessModule.TWOFACTORAUTH)}
                  data-test-id='two-factor-auth-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.TWOFACTORAUTH])}); }}
                  height={28}
                  width={56}
          />
          {enableDataUseAgreement && <Toggle name='Data Use Agreement'
                  initialValue={selectedModules.includes(AccessModule.DATAUSEAGREEMENT)}
                  data-test-id='data-use-agreement-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.DATAUSEAGREEMENT])}); }}
                  height={28}
                  width={56}
          />}
          <div style={{display: 'flex', justifyContent: 'flex-end'}}>
            <IconButton icon='times'
                        onClick={() => this.cancel()}
                        disabled={!this.hasEdited()}/>
            <IconButton icon='check'
                        data-test-id='toggle-save'
                        onClick={() => this.save()}
                        disabled={!this.hasEdited()}/>
          </div>
        </FlexColumn>}>
      <Button type='secondaryLight' data-test-id='bypass-popup'>
        <ClrIcon shape={open ? 'caret down' : 'caret right'} size={19}
                 style={{color: colors.accent, marginRight: '1px', cursor: 'pointer'}}/>
        Bypass
      </Button>
    </PopupTrigger>;
  }

}
