import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DialogButton, RestConstants } from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { FeedbackData, FeedbackV1Service, Node } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-view-material-feedback',
    templateUrl: 'view-material-feedback.component.html',
    styleUrls: ['view-material-feedback.component.scss'],
    standalone: false,
})
export class ViewMaterialFeedbackComponent {
    private feedbackService = inject(FeedbackV1Service);
    private toast = inject(Toast);

    @Input()
    set node(node: Node) {
        this._node = node;
        void this.fetch();
    }
    @Output() closeFeedback = new EventEmitter<void>();
    _node: Node;
    feedbacks: FeedbackData[];
    feedbackViewButtons: DialogButton[];
    constructor() {
        this.feedbackViewButtons = DialogButton.getSingleButton(
            'CLOSE',
            () => this.closeFeedback.emit(),
            'standard',
        );
    }

    private async fetch() {
        if (!this._node) {
            this.feedbacks = null;
            return;
        }
        this.toast.showProgressSpinner();
        try {
            this.feedbacks = await this.feedbackService
                .getFeedbacks({
                    repository: RestConstants.HOME_REPOSITORY,
                    node: this._node.ref.id,
                })
                .toPromise();
        } catch (e) {
            this.closeFeedback.emit();
        }
        this.toast.closeProgressSpinner();
    }
}
