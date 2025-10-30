import { Component, Input, signal, ViewChild } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { MatStepperModule } from '@angular/material/stepper';
import { FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ShareDialogChooseDateComponent } from '../../../features/dialogs/dialog-modules/share-dialog/permission/choose-date/choose-date.component';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';
import {
    ManageAssignmentNodesComponent,
    NodeWithRole,
} from '../manage-assignment-nodes/manage-assignment-nodes.component';
import { Node } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-manage-assignment',
    templateUrl: 'manage-assignment.component.html',
    styleUrls: ['manage-assignment.component.scss'],
    imports: [
        SharedModule,
        MatStepperModule,
        MatFormFieldModule,
        ShareDialogChooseDateComponent,
        ManageAssignmentNodesComponent,
    ],
})
export class ManageAssignmentComponent {
    now = new Date().getTime();
    dateTime = new Date().getTime() + 1000 * 3600 * 24 * 5;
    @ViewChild('dateChooser') dateChooserRef: ShareDialogChooseDateComponent;
    // @TODO: assignment data type
    @Input() assignment = signal<any>(null);
    mainDataFormGroup: FormGroup;
    nodes = signal<Node[]>(null);
    validateMainForm(group: FormGroup): ValidationErrors | null {
        if (group.get('allowDelayedSubmission')?.value === true) {
            return null;
        }
        return null;
    }

    constructor(
        private formBuilder: FormBuilder,
        private editorialSidebarService: EditorialSidebarService,
    ) {
        this.mainDataFormGroup = this.formBuilder.group(
            {
                title: ['', [Validators.required]],
                description: ['', [Validators.required]],
                allowDelayedSubmission: [false, [Validators.required]],
                allowAdditionalDocumentSubmission: [false, [Validators.required]],
            },
            { validators: this.validateMainForm },
        );
        this.editorialSidebarService.applyNodeEmitted.subscribe(({ nodes }) => {
            console.log(nodes);
            this.nodes.set(
                (this.nodes() || []).concat(
                    nodes
                        .filter((n) => !(this.nodes() || []).some((n2) => n2.ref?.id === n.ref?.id))
                        .map((node) => {
                            return {
                                ...node,
                                documentRole: 'SUPPLEMENTARY',
                            } as NodeWithRole;
                        }),
                ),
            );
            console.log(this.nodes());
        });
    }

    showFileDialog() {
        this.editorialSidebarService.showOption({
            option: 'SORT_INTO',
            trap: true,
        });
    }
}
