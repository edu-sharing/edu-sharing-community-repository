import { Component, Input, Optional } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { DialogsService } from '../../services/abstract/dialogs.service';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';

import { MatButton, MatButtonModule } from '@angular/material/button';

@Component({
    selector: 'rs-revoked',
    templateUrl: './revoked.component.html',
    styleUrls: ['./revoked.component.scss'],
    standalone: true,
    imports: [MatButtonModule, EduSharingUiCommonModule, TranslateModule],
})
export class RevokedComponent {
    @Input() node: Node | undefined;
    constructor(@Optional() public dialogsService: DialogsService) {}
    reportRevokeFeedback() {
        void this.dialogsService.openNodeReportDialog({
            node: this.node!!,
            mode: 'REVOKE_FEEDBACK',
            showOptions: false,
        });
    }
}
