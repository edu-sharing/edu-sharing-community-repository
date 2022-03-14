import { trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    ViewChild,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import {
    DialogButton,
    LoginResult,
    Node,
    RestConnectorService, RestConstants,
    RestIamService,
    RestNodeService,
} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { Toast } from '../../../core-ui-module/toast';

@Component({
    selector: 'es-node-relation-manager',
    templateUrl: 'node-relation-manager.component.html',
    styleUrls: ['node-relation-manager.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeRelationManagerComponent {
    readonly Relations = Object.values(Relations);
    readonly RelationsInverted = {
        [Relations.isBasedOn]: 'isBasisFor',
        [Relations.isPartOf]: 'hasPart',
        [Relations.references]: 'references'
    };
    _nodes: Node[];
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
    }
    @Output() onClose = new EventEmitter<void>();

    readonly form = new FormGroup({
        relation: new FormControl(Relations.isBasedOn, Validators.required),
    });
    readonly buttons = DialogButton.getSingleButton('CLOSE',
        () => this.onClose.emit(),
        DialogButton.TYPE_CANCEL);
    permissions = [RestConstants.PERMISSION_WRITE];
    target: Node;

    swap() {
        const tmp = this.target;
        this.target = this._nodes[0];
        this._nodes[0] = tmp;
    }
}
export enum Relations {
    isBasedOn = 'isBasedOn',
    isPartOf = 'isPartOf',
    references = 'references'
}
