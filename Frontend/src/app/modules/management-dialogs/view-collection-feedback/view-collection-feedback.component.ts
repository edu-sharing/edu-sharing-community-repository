import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {CollectionFeedback, DialogButton, RestCollectionService, RestConnectorService} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {RestNodeService} from "../../../core-module/core.module";
import {
  NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission,
  LoginResult, View, STREAM_STATUS, IamUser, AuthorityProfile
} from "../../../core-module/core.module";
import {ConfigurationService} from "../../../core-module/core.module";
import {UIHelper} from "../../../core-ui-module/ui-helper";
import {RestIamService} from "../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {MdsComponent} from "../../../features/mds/legacy/mds/mds.component";
import {RestConstants} from "../../../core-module/core.module";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {trigger} from "@angular/animations";
import {RestStreamService} from "../../../core-module/core.module";
import {RestHelper} from "../../../core-module/core.module";
import {Helper} from "../../../core-module/rest/helper";

@Component({
  selector: 'es-view-collection-feedback',
  templateUrl: 'view-collection-feedback.component.html',
  styleUrls: ['view-collection-feedback.component.scss'],
  animations: [
    trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class ViewCollectionFeedbackComponent  {
  @Input() set collection (collection: Node) {
    this.toast.showProgressDialog();
    this.collectionService
        .getFeedbacks(collection.ref.id)
        .subscribe(
            data => {
              this._collection = collection;
              this.feedbacks = data.reverse();
              this.toast.closeModalDialog();
            },
            error => {
              this.toast.error(error);
              this.toast.closeModalDialog();
            },
        );  }
  @Output() onClose = new EventEmitter<void>();
  _collection: Node;
  feedbacks: CollectionFeedback[];
  feedbackViewButtons: DialogButton[];
  constructor(
      private collectionService: RestCollectionService,
      private toast: Toast
  ) {
    this.feedbackViewButtons = DialogButton.getSingleButton(
        'CLOSE',
        () => this.onClose.emit(),
        'standard',
    );
  }
}
