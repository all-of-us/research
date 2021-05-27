import * as fp from 'lodash/fp';
import * as React from 'react';

import {maybeDaysRemaining} from 'app/components/access-renewal-notification';
import {withRouteData} from 'app/components/app-router';
import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn } from 'app/components/flex';
import {Arrow, ClrIcon, ExclamationTriangle, withCircleBackground} from 'app/components/icons';
import {RadioButton} from 'app/components/inputs';
import {AoU} from 'app/components/text-wrappers';
import {withProfileErrorModal} from 'app/components/with-error-modal';
import {withResponseHandling, ResponseModal, Result} from 'app/components/modals';
import {styles} from 'app/pages/profile/profile-styles';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {redirectToTraining} from 'app/utils/access-utils'
import {profileApi} from 'app/services/swagger-fetch-clients';
import {
  cond,
  daysFromNow,
  displayDateWithoutHours,
  useId,
  withStyle
} from 'app/utils';
import {navigate, navigateByUrl} from 'app/utils/navigation';
import {profileStore, useStore, withProfileStoreReload} from 'app/utils/stores';

const {useState} = React;
// Lookback period - at what point do we give users the option to update their compliance items?
// In an effort to allow users to sync all of their training, we are setting at 330 to start.
const LOOKBACK_PERIOD = 330;

const renewalStyle = {
  h1: {
    fontSize: '0.83rem',
    fontWeight: 600,
    color: colors.primary
  },
  h2: {
    fontSize: '0.75rem',
    fontWeight: 600
  },
  h3: {
    fontSize: '0.675rem',
    fontWeight: 600
  },
  card: {
    backgroundColor: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.8)}`,
    borderRadius: '0.2rem',
    display: 'flex',
    fontSize: '0.58rem',
    fontWeight: 500,
    height: 345,
    lineHeight: '22px',
    margin: 0,
    padding: '0.5rem',
    width: 560
  }
};
const isExpiring = (nextReview: number): boolean => daysFromNow(nextReview) <= LOOKBACK_PERIOD;

const withInvalidDateHandling = date => {
  if (!date) {
    return 'Unavailable';
  } else {
    return displayDateWithoutHours(date);
  }
};

// const confirmPublications = withProfileStoreReload(async () => {
//   try {
//     await profileApi().confirmPublications();
//   } catch {
//     console.log('Error')
//   }
// })

const computeDisplayDates = (lastConfirmedTime, bypassTime, nextReviewTime) => {
  const userCompletedModule = !!lastConfirmedTime;
  const userBypassedModule = !!bypassTime;
  const lastConfirmedDate = withInvalidDateHandling(lastConfirmedTime);
  const nextReviewDate = withInvalidDateHandling(nextReviewTime);
  const bypassDate = withInvalidDateHandling(bypassTime);

  return cond(
    // User has bypassed module
    [userBypassedModule, () => ({lastConfirmedDate: `${bypassDate}`, nextReviewDate: 'Unavailable (bypassed)'})],
    // User never completed training
    [!userCompletedModule && !userBypassedModule, () =>
      ({lastConfirmedDate: 'Unavailable (not completed)', nextReviewDate: 'Unavailable (not completed)'})],
    // User completed training, but is in the lookback window
    [userCompletedModule && isExpiring(nextReviewTime), () => {
      const daysRemaining = daysFromNow(nextReviewTime);
      const daysRemainingDisplay = daysRemaining >= 0 ? `(${daysRemaining} day${daysRemaining !== 1 ? 's' : ''})` : '(expired)';
      return {
        lastConfirmedDate,
        nextReviewDate: `${nextReviewDate} ${daysRemainingDisplay}`
      };
    }],
    // User completed training and is up to date
    [userCompletedModule && !isExpiring(nextReviewTime), () => {
      const daysRemaining = daysFromNow(nextReviewTime);
      return {lastConfirmedDate, nextReviewDate: `${nextReviewDate} (${daysRemaining} day${daysRemaining !== 1 ? 's' : ''})`};
    }]
  );
};


// Helper / Stateless Components
interface CompletedButtonInterface {
  buttonText: string;
  wasBypassed: boolean;
  style?: React.CSSProperties;
}

const CompletedButton = ({buttonText, wasBypassed, style}: CompletedButtonInterface) => <Button disabled={true}
    data-test-id='completed-button'
    style={{
      height: '1.6rem',
      marginTop: 'auto',
      backgroundColor: colors.success,
      width: 'max-content',
      cursor: 'default',
      ...style
    }}>
    <ClrIcon shape='check' style={{marginRight: '0.3rem'}}/>{wasBypassed ? 'Bypassed' : buttonText}
  </Button>;

interface ActionButtonInterface {
  isComplete: boolean;
  wasBypassed: boolean;
  actionButtonText: string;
  completedButtonText: string;
  onClick: Function;
  disabled?: boolean;
  style?: React.CSSProperties;
}
const ActionButton = (
  {isComplete, disabled, wasBypassed, actionButtonText, completedButtonText, onClick, style}: ActionButtonInterface) => {
  return wasBypassed || isComplete
    ? <CompletedButton buttonText={completedButtonText} wasBypassed={wasBypassed} style={style}/>
    : <Button
        onClick={onClick}
        disabled={disabled}
        style={{marginTop: 'auto', height: '1.6rem', width: 'max-content', ...style}}>{actionButtonText}</Button>;
};

const BackArrow = withCircleBackground(() => <Arrow style={{height: 21, width: 18}}/>);

const RenewalCard = withStyle(renewalStyle.card)(
  ({step, TitleComponent, lastCompletionTime, nextReviewTime, bypassTime = null, children, style}) => {
    const {lastConfirmedDate, nextReviewDate} = computeDisplayDates(lastCompletionTime, bypassTime, nextReviewTime);
    return <FlexColumn style={style}>
      <div style={renewalStyle.h3}>STEP {step}</div>
      <div style={renewalStyle.h3}><TitleComponent/></div>
      <div style={{ color: colors.primary, margin: '0.5rem 0', display: 'grid', columnGap: '1rem', gridTemplateColumns: 'auto 1fr'}}>
        <div>Last Updated On:</div>
        <div>Next Review:</div>
        <div>{lastConfirmedDate}</div>
        <div>{nextReviewDate}</div>
      </div>
      {children}
    </FlexColumn>;
  }
);



// Page to render
export const AccessRenewalPage = fp.flow(
  withRouteData,
  withProfileErrorModal
)(() => {
  const {profile: {
    complianceTrainingCompletionTime,
    dataUseAgreementCompletionTime,
    publicationsLastConfirmedTime,
    profileLastConfirmedTime,
    dataUseAgreementBypassTime,
    complianceTrainingBypassTime,
    renewableAccessModules: {modules}},
    profile
  } = useStore(profileStore);
  const [publications, setPublications] = useState<boolean>(null);
  const noReportId = useId();
  const reportId = useId();
  const [confirm, setConfirm] = useState<Result | null>(null);

  const getExpirationTimeFor = moduleName => fp.flow(fp.find({moduleName: moduleName}), fp.get('expirationEpochMillis'))(modules);

  const confirmPublications = withResponseHandling(setConfirm, {
      title: 'Confirmed Publications',
      message: 'You have successfully reported your publications',
      errorTitle: 'Failed to confirm publications', 
      errorMessage: 'An error occured trying to confirm your publications. Please try again.',
      onDismiss: () => console.log('DISMISS')
    }, async () => {
    await profileApi().confirmPublications()
  })

  return <FadeBox style={{margin: '1rem auto 0', color: colors.primary}}>
    <div style={{display: 'grid', gridTemplateColumns: '1.5rem 1fr', alignItems: 'center', columnGap: '.675rem'}}>
      {confirm && <ResponseModal 
        title={confirm.title} 
        message={confirm.message} 
        onDismiss={() => {
          !confirm.error && confirm.onDismiss && confirm.onDismiss();
          setConfirm(null);
        }
      }/>}
      {maybeDaysRemaining(profile) < 0
        ? <React.Fragment>
            <ExclamationTriangle style={{height: '1.5rem', width: '1.5rem'}}/>
            <div style={styles.h1}>Access to the Researcher Workbench revoked.</div>
          </React.Fragment>
        : <React.Fragment>
            <Clickable onClick={() => history.back()}><BackArrow style={{height: '1.5rem', width: '1.5rem'}}/></Clickable>
            <div style={styles.h1}>Yearly Researcher Workbench access renewal</div>
          </React.Fragment>
      }
      <div style={{gridColumnStart: 2}}>Researchers are required to complete a number of steps as part of the annual renewal
        to maintain access to <AoU/> data. Renewal of access will occur on a rolling basis annually (i.e. for each user, access
        renewal will be due 365 days after the date of authorization to access <AoU/> data.
      </div>
    </div>
    <div style={{...renewalStyle.h2, margin: '1rem 0'}}>Please complete the following steps</div>
    <div style={{display: 'grid', gridTemplateColumns: 'auto 1fr', marginBottom: '1rem', alignItems: 'center', gap: '1rem'}}>
      {/* Profile */}
      <RenewalCard step={1}
        TitleComponent={() => 'Update your profile'}
        lastCompletionTime={profileLastConfirmedTime}
        nextReviewTime={getExpirationTimeFor('profileConfirmation')}>
        <div style={{marginBottom: '0.5rem'}}>Please update your profile information if any of it has changed recently.</div>
        <div>Note that you are obliged by the Terms of Use of the Workbench to provide keep your profile
          information up-to-date at all times.
        </div>
        <ActionButton isComplete={!isExpiring(getExpirationTimeFor('profileConfirmation'))}
          actionButtonText='Review'
          completedButtonText='Confirmed'
          onClick={() => navigateByUrl('profile?renewal=1')}
          wasBypassed={false} />
      </RenewalCard>
      {/* Publications */}
      <RenewalCard step={2}
        TitleComponent={() => 'Report any publications or presentations based on your research using the Researcher Workbench'}
        lastCompletionTime={publicationsLastConfirmedTime}
        nextReviewTime={getExpirationTimeFor('publicationConfirmation')}>
        <div>The <AoU/> Publication and Presentation Policy requires that you report any upcoming publication or
             presentation resulting from the use of <AoU/> Research Program Data at least two weeks before the date of publication.
             If you are lead on or part of a publication or presentation that hasn’t been reported to the
             program, <a target='_blank' style={{textDecoration: 'underline'}}
              href={'https://redcap.pmi-ops.org/surveys/?s=MKYL8MRD4N'}>please report it now.</a>
        </div>
        <div style={{marginTop: 'auto', display: 'grid', columnGap: '0.25rem', gridTemplateColumns: 'auto 1rem 1fr', alignItems: 'center'}}>
          <ActionButton isComplete={!isExpiring(getExpirationTimeFor('publicationConfirmation'))}
            actionButtonText='Confirm'
            completedButtonText='Confirmed'
            onClick={confirmPublications}
            wasBypassed={false}
            disabled={publications === null}
            style={{gridRow: '1 / span 2', marginRight: '0.25rem'}}/>
          <RadioButton id={noReportId}
            disabled={!isExpiring(getExpirationTimeFor('publicationConfirmation'))}
            style={{justifySelf: 'end'}} checked={publications === true}
            onChange={() => setPublications(true)}/>
          <label htmlFor={noReportId}> At this time, I have nothing to report </label>
          <RadioButton id={reportId}
            disabled={!isExpiring(getExpirationTimeFor('publicationConfirmation'))}
            style={{justifySelf: 'end'}}
            checked={publications === false}
            onChange={() => setPublications(false)}/>
          <label htmlFor={reportId}>Report submitted</label>
        </div>
      </RenewalCard>
      {/* Compliance Training */}
      <RenewalCard step={3}
        TitleComponent={() => <div><AoU/> Responsible Conduct of Research Training</div>}
        lastCompletionTime={complianceTrainingCompletionTime}
        nextReviewTime={getExpirationTimeFor('complianceTraining')}
        bypassTime={complianceTrainingBypassTime}>
        <div> You are required to complete the refreshed ethics training courses to understand the privacy safeguards and
          the compliance requirements for using the <AoU/> Dataset.
        </div>
        <ActionButton isComplete={!isExpiring(getExpirationTimeFor('complianceTraining'))}
          actionButtonText='Complete Training'
          completedButtonText='Confirmed'
          onClick={redirectToTraining}
          wasBypassed={!!complianceTrainingBypassTime}/>
      </RenewalCard>
      {/* DUCC */}
      <RenewalCard step={4}
        TitleComponent={() => 'Sign Data User Code of Conduct'}
        lastCompletionTime={dataUseAgreementCompletionTime}
        nextReviewTime={getExpirationTimeFor('dataUseAgreement')}
        bypassTime={dataUseAgreementBypassTime}>
        <div>Please review and sign the data user code of conduct consenting to the <AoU/> data use policy.</div>
        <ActionButton isComplete={!isExpiring(getExpirationTimeFor('dataUseAgreement'))}
          actionButtonText='View & Sign'
          completedButtonText='Completed'
          onClick={() => navigateByUrl('data-code-of-conduct?renewal=1')}
          wasBypassed={!!dataUseAgreementBypassTime}/>
      </RenewalCard>
    </div>
  </FadeBox>;
});
