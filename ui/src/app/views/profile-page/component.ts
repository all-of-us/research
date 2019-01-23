import {Component, OnInit, ViewChild} from '@angular/core';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {SignInService} from 'app/services/sign-in.service';
import {deepCopy} from 'app/utils/index';
import {BugReportComponent} from 'app/views/bug-report/component';

import {
  ErrorResponse,
  IdVerificationStatus,
  InstitutionalAffiliation,
  Profile,
  ProfileService,
} from 'generated';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ProfilePageComponent implements OnInit {
  profile: Profile;
  workingProfile: Profile;
  profileImage: string;
  profileLoaded = false;
  errorText: string;
  editing = false;
  view: any[] = [200, 200];
  numberOfTotalTasks = 4;
  completedTasksName = 'Completed';
  unfinishedTasksName = 'Unfinished';
  colorScheme = {
    domain: ['#8BC990', '#C7C8C8']
  };
  spinnerValues = [
    {
      'name': this.completedTasksName,
      'value': this.completedTasks
    },
    {
      'name': this.unfinishedTasksName,
      'value': this.numberOfTotalTasks - this.completedTasks
    }
  ];
  termsOfServiceError = false;
  demographicSurveyError = false;
  ethicsTrainingError = false;
  idVerificationError = false;

  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;

  static validLength(val: string, len: number): boolean {
    if (val) {
      return val.length <= len;
    }
    return true;
  }

  static notEmpty(val: string): boolean {
    if (val) {
      return val.length > 0;
    }
    return false;
  }

  constructor(
    private profileService: ProfileService,
    private profileStorageService: ProfileStorageService,
    private signInService: SignInService
  ) {}

  ngOnInit(): void {
    this.profileImage = this.signInService.profileImage;
    this.errorText = null;

    this.profileStorageService.profile$.subscribe(profile => {
      this.profile = profile;
      if (!this.editing) {
        this.workingProfile = <Profile> deepCopy(profile);
      }
      this.profileLoaded = true;
      this.reloadSpinner();
    });

    this.profileStorageService.reload();
  }

  get isGivenNameValidationError(): boolean {
    if (this.workingProfile && this.workingProfile.givenName) {
      return this.workingProfile.givenName.length > 80;
    }
    return false;
  }

  get givenNameValid(): boolean {
    if (this.workingProfile) {
      return ProfilePageComponent.validLength(this.workingProfile.givenName, 80);
    }
    return false;
  }

  get givenNameNotEmpty(): boolean {
    if (this.workingProfile) {
      return ProfilePageComponent.notEmpty(this.workingProfile.givenName);
    }
    return false;
  }

  get familyNameValid(): boolean {
    if (this.workingProfile) {
      return ProfilePageComponent.validLength(this.workingProfile.familyName, 80);
    }
    return false;
  }

  get familyNameNotEmpty(): boolean {
    if (this.workingProfile) {
      return ProfilePageComponent.notEmpty(this.workingProfile.familyName);
    }
    return false;
  }

  get currentPositionValid(): boolean {
    if (this.workingProfile) {
      return ProfilePageComponent.validLength(this.workingProfile.currentPosition, 255);
    }
    return false;
  }

  get currentPositionNotEmpty(): boolean {
    if (this.workingProfile) {
      return (ProfilePageComponent.notEmpty(this.workingProfile.currentPosition));
    }
    return false;
  }

  get organizationValid(): boolean {
    if (this.workingProfile) {
      return ProfilePageComponent.validLength(this.workingProfile.organization, 255);
    }
    return false;
  }

  get organizationNotEmpty(): boolean {
    if (this.workingProfile) {
      return ProfilePageComponent.notEmpty(this.workingProfile.organization);
    }
    return false;
  }

  get currentResearchNotEmpty(): boolean {
    if (this.workingProfile) {
      return ProfilePageComponent.notEmpty(this.workingProfile.areaOfResearch);
    }
    return false;
  }

  get allFieldsValid(): boolean {
    return this.givenNameValid && this.givenNameNotEmpty && this.familyNameValid
        && this.familyNameNotEmpty && this.currentPositionValid && this.currentPositionNotEmpty
        && this.organizationValid && this.organizationNotEmpty && this.currentResearchNotEmpty;
  }

  submitChanges(): void {

    this.profileService.updateProfile(this.workingProfile).subscribe(
      () => {
        this.profile = <Profile> deepCopy(this.workingProfile);
        this.editing = false;
        this.profileStorageService.reload();
      },
      error => {
        const response: ErrorResponse = ErrorHandlingService.convertAPIError(error);
        this.errorText = '';
      });
  }

  public get completedTasks() {
    let completedTasks = 0;
    if (this.profile === undefined) {
      return completedTasks;
    }
    if (this.profile.idVerificationStatus === IdVerificationStatus.VERIFIED) {
      completedTasks += 1;
    }
    if (this.profile.demographicSurveyCompletionTime !== null) {
      completedTasks += 1;
    }
    if (this.profile.ethicsTrainingCompletionTime !== null) {
      completedTasks += 1;
    }
    if (this.profile.termsOfServiceCompletionTime !== null) {
      completedTasks += 1;
    }
    return completedTasks;
  }

  public get completedTasksAsPercentage() {
    return this.completedTasks / this.numberOfTotalTasks * 100;
  }

  reloadSpinner(): void {
    this.spinnerValues = [
      {
        'name': this.completedTasksName,
        'value': this.completedTasks
      },
      {
        'name': this.unfinishedTasksName,
        'value': this.numberOfTotalTasks - this.completedTasks
      }
    ];
  }

  submitTermsOfService(): void {
    this.profileService.submitTermsOfService().subscribe((profile) => {
      this.profile.termsOfServiceCompletionTime = profile.termsOfServiceCompletionTime;
      this.workingProfile.termsOfServiceCompletionTime = profile.termsOfServiceCompletionTime;
      this.reloadSpinner();
    }, () => {
      this.termsOfServiceError = true;
    });
  }


  completeEthicsTraining(): void {
    this.profileService.completeEthicsTraining().subscribe((profile) => {
      this.profile.ethicsTrainingCompletionTime = profile.ethicsTrainingCompletionTime;
      this.workingProfile.ethicsTrainingCompletionTime = profile.ethicsTrainingCompletionTime;
      this.reloadSpinner();
    }, () => {
      this.ethicsTrainingError = true;
    });
  }

  submitDemographicSurvey(): void {
    this.profileService.submitDemographicsSurvey().subscribe((profile) => {
      this.profile.demographicSurveyCompletionTime = profile.demographicSurveyCompletionTime;
      this.workingProfile.demographicSurveyCompletionTime = profile.demographicSurveyCompletionTime;
      this.reloadSpinner();
    }, () => {
      this.demographicSurveyError = true;
    });
  }

  reloadProfile(): void {
    this.workingProfile = <Profile> deepCopy(this.profile);
    this.editing = false;
  }

  pushAffiliation(): void {
    if (!this.editing) {
      return;
    }
    if (this.workingProfile) {
      if (this.workingProfile.institutionalAffiliations === undefined) {
        this.workingProfile.institutionalAffiliations = [];
      }
      this.workingProfile.institutionalAffiliations.push(
        {
          role: '',
          institution: '',
        }
      );
    }
  }

  removeAffiliation(affiliation: InstitutionalAffiliation): void {
    if (!this.editing) {
      return;
    }
    if (this.workingProfile) {
      const positionOfValue = this.workingProfile.institutionalAffiliations
        .findIndex(item => item === affiliation);
      if (positionOfValue !== -1) {
        this.workingProfile.institutionalAffiliations.splice(positionOfValue, 1);
      }
    }
  }

  requestVerification(): void {
    this.profileService.submitIdVerification().subscribe((profile) => {
      this.profile.requestedIdVerification = profile.requestedIdVerification;
      this.workingProfile.requestedIdVerification = profile.requestedIdVerification;
    }, () => {
      this.idVerificationError = true;
    });
  }

  submitTermsOfServiceBugReport(): void {
    this.termsOfServiceError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Error submitting terms of service';
  }

  submitEthicsTrainingBugReport(): void {
    this.ethicsTrainingError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Error submitting ethics training';
  }

  submitDemographicsSurveyBugReport(): void {
    this.demographicSurveyError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Error submitting demographics survey';
  }

  submitIdVerificationBugReport(): void {
    this.idVerificationError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Error submitting identity verification';
  }
}
