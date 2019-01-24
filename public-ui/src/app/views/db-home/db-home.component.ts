import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-db-home',
  templateUrl: './db-home.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './db-home.component.css']
})
export class DbHomeComponent implements OnInit {
  dbLogo = '/assets/db-images/Data_Browser_Logo.svg';
  subTitle = 'The Data Browser provides interactive views of the publically available ' +
    'All of Us Research Program participant data. ' +
    'Currently, participant provided information, including surveys and ' +
    'physical measurements taken at the time of participant enrollment ' +
    '(“program physical measurements”), ' +
    'as well as electronic health record (EHR) data are available.' +
    ' The All of Us Research Program data resource will grow to include more data types over time.';

  constructor() { }

  ngOnInit() {
  }

}
