import { trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Inject,
    OnInit,
    ViewChild,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { filter, take } from 'rxjs/operators';
import {
    DialogButton,
    LoginResult,
    Node,
    RestConnectorService,
    RestIamService,
    RestNodeService,
} from '../../../../core-module/core.module';
import { UIAnimation } from '../../../../../../projects/edu-sharing-ui/src/lib/util/ui-animation';
import { Toast } from '../../../../core-ui-module/toast';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';

export interface NodeReportDialogData {
    node: Node;
}

@Component({
    selector: 'es-node-report-dialog',
    templateUrl: 'node-report-dialog.component.html',
    styleUrls: ['node-report-dialog.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeReportDialogComponent implements OnInit {
    readonly reasons = ['UNAVAILABLE', 'INAPPROPRIATE_CONTENT', 'INVALID_METADATA', 'OTHER'];

    @ViewChild('formElement') formRef: ElementRef<HTMLFormElement>;

    readonly form = new FormGroup({
        reason: new FormControl('', Validators.required),
        comment: new FormControl(''),
        email: new FormControl('', [Validators.email, Validators.required]),
    });

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: NodeReportDialogData,
        private dialogRef: CardDialogRef,
        private connector: RestConnectorService,
        private iam: RestIamService,
        private translate: TranslateService,
        private toast: Toast,
        private nodeApi: RestNodeService,
        private cdr: ChangeDetectorRef,
    ) {}

    ngOnInit(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
                new DialogButton('NODE_REPORT.REPORT', { color: 'primary' }, () => this.report()),
            ],
        });
        // Pre-fill the email field for logged-in users.
        this.connector.isLoggedIn().subscribe((data: LoginResult) => {
            if (!data.isGuest) {
                this.form.get('email').disable();
                this.iam.getCurrentUserAsync().then((user) => {
                    this.form.patchValue({ email: user.person.profile.email });
                });
            }
        });
        // Disable close by backdrop click as soon as the user enters any value .
        this.form.valueChanges
            .pipe(
                filter((values) => Object.values(values).some((value) => !!value)),
                take(1),
            )
            .subscribe(() => this.dialogRef.patchConfig({ closable: Closable.Standard }));
    }

    cancel() {
        this.dialogRef.close();
    }

    shouldShowError(field: string) {
        const fieldControl = this.form.get(field);
        return !fieldControl.disabled && fieldControl.touched && !fieldControl.valid;
    }

    report() {
        if (this.form.valid) {
            // Include value for possibly disabled email field.
            const value = this.form.getRawValue();
            this.setLoading(true);
            this.nodeApi
                .reportNode(
                    this.data.node.ref.id,
                    this.getReasonAsString(value.reason),
                    value.email,
                    value.comment,
                    this.data.node.ref.repo,
                )
                .subscribe(
                    () => {
                        this.toast.toast('NODE_REPORT.DONE');
                        this.dialogRef.close();
                    },
                    (error: any) => {
                        this.setLoading(false);
                        this.toast.error(error);
                    },
                );
        } else {
            for (const field of ['reason', 'email']) {
                const control = this.form.get(field);
                if (!control.valid) {
                    control.markAsTouched();
                    this.focusField(field);
                    this.cdr.detectChanges();
                    break;
                }
            }
        }
    }

    setLoading(isLoading: boolean): void {
        this.dialogRef.patchState({ isLoading });
        if (isLoading) {
            this.form.disable();
        } else {
            this.form.enable();
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
