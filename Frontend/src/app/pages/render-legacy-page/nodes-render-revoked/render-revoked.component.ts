import { Component, Input } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { SharedModule } from '../../../shared/shared.module';

@Component({
    selector: 'es-render-revoked',
    templateUrl: './render-revoked.component.html',
    styleUrls: ['./render-revoked.component.scss'],
    imports: [SharedModule],
})
export class RenderRevokedComponent {
    @Input() node: Node;
    constructor(private dialogsService: DialogsService) {}
    reportRevokeFeedback() {
        void this.dialogsService.openNodeReportDialog({
            node: this.node,
            mode: 'REVOKE_FEEDBACK',
            showOptions: false,
        });
    }
}
