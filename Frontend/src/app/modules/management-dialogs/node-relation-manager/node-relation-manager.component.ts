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
    RestConnectorService,
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
    @Input() nodes: Node[];
    @Output() close = new EventEmitter<void>();
}
