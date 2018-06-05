import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {Analysis} from '../../../publicGenerated/model/analysis';
import {Concept} from '../../../publicGenerated/model/concept';
import {DbDomain} from '../../../publicGenerated/model/dbDomain';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';
import {QuestionConcept} from '../../../publicGenerated/model/questionConcept';
import {QuestionConceptListResponse} from '../../../publicGenerated/model/questionConceptListResponse';
import {ChartComponent} from '../../data-browser/chart/chart.component';

@Component({
  selector: 'app-ehr-view',
  templateUrl: './ehr-view.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './ehr-view.component.css']
})
export class EhrViewComponent implements OnInit {
  domainId: string;
  title ;
  subTitle;
  dbDomain;
  searchText = null;
  searchResults = [];
  loading = true;
  minParticipantCount = 0;
  totalParticipants;


  constructor(private route: ActivatedRoute, private api: DataBrowserService) {
    this.route.params.subscribe(params => {
      this.domainId = params.id;
    });
  }

  ngOnInit() {
    this.api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue);

    // Get search result from localStorage
    this.searchText = localStorage.getItem('searchText');
    if (!this.searchText) {
      this.searchText = '';
    }
    const obj = localStorage.getItem('dbDomain');
    if (obj) {
      this.dbDomain = JSON.parse(obj);
      this.subTitle = 'Keyword: ' + this.searchText;
      this.title = 'View Full Results: ' + this.dbDomain.domainDisplay;
    } else {
      /* Error. We need a db Domain object. */
      this.title   = 'Keyword: ' + this.searchText;
      this.title = 'View Full Results: ' + 'Error - no result domain selected';
    }

    // Run search filter to domain
    this.searchDomain();

  }

  searchDomain() {
    if (!this.searchText || this.searchText.length === 0) {
      this.searchText = null;
      console.log('null search text');
    }

    this.api.getConceptsSearch(this.searchText, 'S', this.dbDomain.domainId).subscribe(results =>  {
      this.searchResults = results.items;
      // Set our min partipant count
      if (this.searchResults.length > 0 ) {
        this.minParticipantCount = this.searchResults[0].countValue;
      }
      this.loading = false;
    } );
  }

}
