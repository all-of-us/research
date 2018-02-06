import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfileEditComponent implements OnInit {
  profile: Profile;
  profileLoaded = false;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private route: ActivatedRoute,
      private router: Router,
  ) {}

  ngOnInit(): void {
    this.errorHandlingService.retryApi(this.profileService.getMe()).subscribe(
        (profile: Profile) => {
      this.profile = profile;
      this.profileLoaded = true;
    });
  }

  submitChanges(): void {
    this.errorHandlingService.retryApi(
        this.profileService.updateProfile(this.profile)).subscribe(() => {
        this.router.navigate(['../'], {relativeTo : this.route});
      }
    );
  }
}
