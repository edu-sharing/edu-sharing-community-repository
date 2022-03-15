import { Component, Input, OnInit } from '@angular/core';

import { RestLtiService } from '../../../core-module/rest/services/rest-lti.service';
import { LTIRegistrationToken, LTIRegistrationTokens } from '../../../core-module/rest/data-object';
import { Toast } from '../../../core-ui-module/toast';
import { AdminComponent } from '../admin.component';

@Component({
  selector: 'es-lti-admin',
  templateUrl: './lti-admin.component.html',
  styleUrls: ['./lti-admin.component.scss']
})
export class LtiAdminComponent implements OnInit {

    @Input()
    adminComponent: AdminComponent;

    /**
     * dynamic
     */
    tokens: LTIRegistrationTokens;
    displayedColumns: string[] = ['url', 'tsCreated', 'delete'];

    /**
     * advanced
     *
     */
    platformId: string;
    clientId: string;
    deploymentId: string;
    authenticationRequestUrl: string;
    keysetUrl: string;
    keyId: string;
    authTokenUrl: string;

  constructor( private ltiService: RestLtiService, private toast: Toast) { }

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

    generate() {
        this.ltiService.getTokensCall(true).subscribe((t: LTIRegistrationTokens) => {this.tokens = t; });
    }

    saveAdvanced() {
        this.ltiService.registrationAdvanced(this.platformId, this.clientId, this.deploymentId,
            this.authenticationRequestUrl, this.keysetUrl, this.keyId, this.authTokenUrl).subscribe( (t: void) => {
            this.toast.toast('ADMIN.LTI.DATA.CREATED', null);
            this.toast.closeModalDialog();
            this.adminComponent.refreshAppList();
        }, (error: any) => {
            this.toast.error(error);
            this.toast.closeModalDialog(); } );
    }
}
