import { Component, OnInit } from '@angular/core';

import { RestLtiService } from '../../../core-module/rest/services/rest-lti.service';
import { LTIRegistrationToken, LTIRegistrationTokens } from '../../../core-module/rest/data-object';

@Component({
  selector: 'es-lti-admin',
  templateUrl: './lti-admin.component.html',
  styleUrls: ['./lti-admin.component.scss']
})
export class LtiAdminComponent implements OnInit {
    tokens: LTIRegistrationTokens;
    displayedColumns: string[] = ['url', 'tsCreated', 'delete'];

  constructor( private ltiService: RestLtiService) { }

  ngOnInit(): void {
      this.refresh();
  }

    remove(element: LTIRegistrationToken) {
      console.log('remove called');
      this.ltiService.removeToken(element.token).subscribe((t: void) => {
          this.refresh();
      });

    }

    refresh() {
        this.ltiService.getTokensCall(false).subscribe((t: LTIRegistrationTokens) => {this.tokens = t; });
    }
}
