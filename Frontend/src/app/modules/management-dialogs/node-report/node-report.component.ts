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
    selector: 'es-node-report',
    templateUrl: 'node-report.component.html',
    styleUrls: ['node-report.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeReportComponent {
    readonly reasons = ['UNAVAILABLE', 'INAPPROPRIATE_CONTENT', 'INVALID_METADATA', 'OTHER'];

    private _node: Node;
    @Input() set node(node: Node) {
        this._node = node;
    }
    get node() {
        return this._node;
    }

    @Output() onCancel = new EventEmitter();
    @Output() onLoading = new EventEmitter();
    @Output() onDone = new EventEmitter();

    @ViewChild('formElement') formRef: ElementRef<HTMLFormElement>;

    buttons: DialogButton[];

    readonly form = new FormGroup({
        reason: new FormControl('', Validators.required),
        comment: new FormControl(''),
        email: new FormControl('', [Validators.email, Validators.required]),
    });

    constructor(
        private connector: RestConnectorService,
        private iam: RestIamService,
        private translate: TranslateService,
        private toast: Toast,
        private nodeApi: RestNodeService,
    ) {
        this.buttons = [
            new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.cancel()),
            new DialogButton('NODE_REPORT.REPORT', DialogButton.TYPE_PRIMARY, () => this.report()),
        ];
        this.connector.isLoggedIn().subscribe((data: LoginResult) => {
            if (!data.isGuest) {
                this.form.get('email').disable();
                this.iam.getCurrentUserAsync().then((user) => {
                    this.form.patchValue({ email: user.person.profile.email });
                });
            }
        });
    }

    cancel() {
        this.onCancel.emit();
    }

    done() {
        this.onDone.emit();
    }

    shouldShowError(field: string) {
        const fieldControl = this.form.get(field);
        return fieldControl.touched && !fieldControl.valid;
    }

    report() {
        if (this.form.valid) {
            // Include value for possibly disabled email field.
            const value = this.form.getRawValue();
            this.onLoading.emit(true);
            this.nodeApi
                .reportNode(
                    this.node.ref.id,
                    this.getReasonAsString(value.reason),
                    value.email,
                    value.comment,
                    this.node.ref.repo,
                )
                .subscribe(
                    () => {
                        this.toast.toast('NODE_REPORT.DONE');
                        this.onLoading.emit(false);
                        this.onDone.emit();
                    },
                    (error: any) => {
                        this.onLoading.emit(false);
                        this.toast.error(error);
                    },
                );
        } else {
            for (const field of ['reason', 'email']) {
                const control = this.form.get(field);
                if (!control.valid) {
                    control.markAsTouched();
                    this.focusField(field);
                    break;
                }
            }
        }
    }

    private getReasonAsString(reason: string) {
        return `${this.translate.instant('NODE_REPORT.REASONS.' + reason)} (${reason})`;
    }

    private focusField(field: string) {
        const form = this.formRef.nativeElement;
        const element = form.elements.namedItem(field);
        if (element instanceof HTMLElement) {
            element.focus();
        } else if (element instanceof RadioNodeList) {
            (element[0] as HTMLElement).focus();
        }
    }
}
