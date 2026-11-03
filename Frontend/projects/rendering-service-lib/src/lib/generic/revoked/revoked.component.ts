import { Component, Input, inject, ChangeDetectionStrategy } from '@angular/core';
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
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [MatButtonModule, EduSharingUiCommonModule, TranslateModule],
})
export class RevokedComponent {
    dialogsService = inject(DialogsService, { optional: true });

    @Input() node: Node | undefined;
    reportRevokeFeedback() {
        void this.dialogsService.openNodeReportDialog({
            node: this.node!!,
            mode: 'REVOKE_FEEDBACK',
            showOptions: false,
        });
    }
}
