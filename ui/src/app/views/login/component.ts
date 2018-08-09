import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {
  ActivatedRoute,
  Router,
} from '@angular/router';

import {SignInService} from 'app/services/sign-in.service';

@Component({
  selector: 'app-signed-out',
  styleUrls: ['./component.css',
              '../../styles/buttons.css',
              '../../styles/headers.css'],
  templateUrl: './component.html'
})
export class LoginComponent implements OnInit {
  showCreateAccount = false;
  backgroundImgSrc = '/assets/images/login-group.png';
  smallerBackgroundImgSrc = '/assets/images/login-standing.png';
  googleIcon = '/assets/icons/google-icon.png';

  constructor(
    /* Ours */
    private signInService: SignInService,
    /* Angular's */
    private router: Router,
  ) {}

  ngOnInit(): void {
    document.body.style.backgroundColor = '#e2e3e5';

    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn) {
        this.router.navigateByUrl('/');
      }
    });
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
