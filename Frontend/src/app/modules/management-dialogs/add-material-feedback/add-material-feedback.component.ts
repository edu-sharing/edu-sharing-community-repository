import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Node, RestConstants } from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { FeedbackV1Service } from 'ngx-edu-sharing-api';
import { Values } from '../../../features/mds/types/types';
import { ActivatedRoute, Params } from '@angular/router';

@Component({
    selector: 'es-add-material-feedback',
    templateUrl: 'add-material-feedback.component.html',
    styleUrls: ['add-material-feedback.component.scss'],
})
export class AddMaterialFeedbackComponent {
    private queryParams: Params;
    @Input() set node(node: Node) {
        this._node = node;
    }
    @Output() onClose = new EventEmitter<void>();
    _node: Node;
    constructor(
        private route: ActivatedRoute,
        private feedbackService: FeedbackV1Service,
        private toast: Toast,
    ) {
        this.route.queryParams.subscribe((queryParams) => (this.queryParams = queryParams));
    }

    async addFeedback(values: Node[] | Values) {
        this.toast.showProgressDialog();
        try {
            const result = await this.feedbackService
                .addFeedback({
                    repository: RestConstants.HOME_REPOSITORY,
                    node: this._node.ref.id,
                    body: values as Values,
                })
                .toPromise();
            this.toast.toast('FEEDBACK.TOAST');
        } catch (e) {
            this.toast.error(e);
        }
        this.toast.closeModalDialog();
        this.onClose.emit();
        if (this.queryParams.feedbackClose) {
            window.close();
        }
    }
}
