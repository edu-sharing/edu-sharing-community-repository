import {
    Component,
    computed,
    Input,
    OnInit,
    Signal,
    signal,
    ViewChild,
    WritableSignal,
} from '@angular/core';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';
import {
    HOME_REPOSITORY,
    MdsQueryCriteria,
    Node,
    NodeService,
    PROPERTY_FILTER_ALL,
    ROOT,
    SearchRequestParams,
    SearchResults,
    SearchService,
} from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    CanDrop,
    ColumnType,
    DragData,
    DropSource,
    FetchEvent,
    InteractionType,
    ListItem,
    MdsHelperService,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesService,
    NodeEntriesWrapperComponent,
    Scope,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import {
    CollectionReference,
    CollectionSubcollections,
} from '../../../core-module/rest/data-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { AddMaterialDialogResult } from '../../../features/dialogs/dialog-modules/add-material-dialog/add-material-dialog-data';
import { AddMaterialDialogModule } from '../../../features/dialogs/dialog-modules/add-material-dialog/add-material-dialog.module';

import { NodeHelperService } from '../../../services/node-helper.service';
import { Toast } from '../../../services/toast';
import { UploadDialogService } from '../../../services/upload-dialog.service';
import { SharedModule } from '../../../shared/shared.module';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ShareDialogChooseDateComponent } from '../../../features/dialogs/dialog-modules/share-dialog/permission/choose-date/choose-date.component';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';

@Component({
    selector: 'es-manage-assignment',
    templateUrl: 'manage-assignment.component.html',
    styleUrls: ['manage-assignment.component.scss'],
    imports: [SharedModule, MatStepperModule, MatFormFieldModule, ShareDialogChooseDateComponent],
})
export class ManageAssignmentComponent {
    now = new Date().getTime();
    dateTime = new Date().getTime() + 1000 * 3600 * 24 * 5;
    @ViewChild('dateChooser') dateChooserRef: ShareDialogChooseDateComponent;
    // @TODO: assignment data type
    @Input() assignment = signal<any>(null);
    mainDataFormGroup: FormGroup;

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
    }

    showFileDialog() {
        this.editorialSidebarService.showOption({
            option: 'SORT_INTO',
            trap: true,
        });
    }
}
