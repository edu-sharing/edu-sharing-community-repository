import { Component, OnInit } from '@angular/core';
import {
    RestMediacenterService
} from '../../../core-module/rest/services/rest-mediacenter.service';
import { RestLtiService } from '../../../core-module/rest/services/rest-lti.service';
import { LTIRegistrationTokens } from '../../../core-module/rest/data-object';

@Component({
  selector: 'es-lti-admin',
  templateUrl: './lti-admin.component.html',
  styleUrls: ['./lti-admin.component.scss']
})
export class LtiAdminComponent implements OnInit {
    tokens: LTIRegistrationTokens;
    displayedColumns: string[] = ['token', 'url', 'tsCreated'];

  constructor( private ltiService: RestLtiService) { }

  ngOnInit(): void {
      this.ltiService.getTokensCall(false).subscribe((t: LTIRegistrationTokens) => {this.tokens = t; });
  }

}
