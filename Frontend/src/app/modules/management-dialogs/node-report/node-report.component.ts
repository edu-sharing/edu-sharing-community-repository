import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {DialogButton, RestConnectorService} from '../../../core-module/core.module';
import {Toast} from '../../../core-ui-module/toast';
import {RestNodeService} from '../../../core-module/core.module';
import {
    NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission,
    LoginResult
} from '../../../core-module/core.module';
import {ConfigurationService} from '../../../core-module/core.module';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {RestIamService} from '../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';

@Component({
    selector: 'node-report',
    templateUrl: 'node-report.component.html',
    styleUrls: ['node-report.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation())
    ]
})
export class NodeReportComponent  {
    public reasons = [
        'UNAVAILABLE', 'INAPPROPRIATE_CONTENT', 'INVALID_METADATA', 'OTHER'
    ];
    public _node: Node;
    isGuest: boolean;
    form: FormGroup;
    @Input() set node(node: Node) {
        this._node = node;
    }
    @Output() onCancel= new EventEmitter();
    @Output() onLoading= new EventEmitter();
    @Output() onDone= new EventEmitter();
    buttons: DialogButton[];
    constructor(
        private connector: RestConnectorService,
        private iam: RestIamService,
        private translate: TranslateService,
        private config: ConfigurationService,
        private formBuilder: FormBuilder,
        private toast: Toast,
        private nodeApi: RestNodeService) {
        this.buttons = [
            new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.cancel()),
            new DialogButton('NODE_REPORT.REPORT', DialogButton.TYPE_PRIMARY, () => this.report()),
        ];
        this.connector.isLoggedIn().subscribe((data: LoginResult) => {
            this.isGuest = data.isGuest;
            this.isGuest ? this.form.get('email').enable() : this.form.get('email').disable();
            if (!data.isGuest) {
                this.iam.getCurrentUserAsync().then((user) => {
                    this.form.get('email').setValue(user.person.profile.email);
                });
            }
        });
        this.form = this.formBuilder.group({
            reason : ['',[Validators.required]],
            comment : [''],
            email : ['',[Validators.required,Validators.email]]
        });
    }
    public cancel() {
        this.onCancel.emit();
    }
    public done() {
        this.onDone.emit();
    }
    public report() {
        if(!this.form.valid) {
            this.form.markAllAsTouched();
            for (const control of Object.keys(this.form.controls)) {
                if(!this.form.get(control).valid) {
                    (this.form.get(control) as any).nativeElement?.focus();
                    break;
                }
            }
            return;
        }
        this.onLoading.emit(true);
        this.nodeApi.reportNode(this._node.ref.id,
            this.getReasonAsString(),
            this.form.value.email || this.iam.getCurrentUser().profile.email,
            this.form.value.comment, this._node.ref.repo).subscribe(() => {
            this.toast.toast('NODE_REPORT.DONE');
            this.onLoading.emit(false);
            this.onDone.emit();
        }, (error: any) => {
            this.onLoading.emit(false);
            this.toast.error(error);
        });
    }

    private getReasonAsString() {
        return this.translate.instant('NODE_REPORT.REASONS.' + this.form.value.reason) + ' (' + this.form.value.reason + ')';
    }
}
