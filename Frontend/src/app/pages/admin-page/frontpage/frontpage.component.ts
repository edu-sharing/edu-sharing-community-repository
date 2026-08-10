import { AfterViewInit, Component, EventEmitter, inject, Output, ViewChild } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import {
    ColumnType,
    InteractionType,
    MdsHelperService,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
} from 'ngx-edu-sharing-ui';
import { RestCollectionService, RestNodeService } from '../../../core-module/core.module';
import { MdsService, Node } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { ConfigurationService } from '../../../core-module/rest/services/configuration.service';
import { RestAdminService } from '../../../core-module/rest/services/rest-admin.service';
import { Toast } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { Closable } from '../../../features/dialogs/card-dialog/card-dialog-config';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-admin-frontpage',
    templateUrl: 'frontpage.component.html',
    styleUrls: ['frontpage.component.scss'],
    standalone: false,
})
export class AdminFrontpageComponent implements AfterViewInit {
    private adminService = inject(RestAdminService);
    private collectionService = inject(RestCollectionService);
    private dialogs = inject(DialogsService);
    private formBuilder = inject(UntypedFormBuilder);
    private mdsService = inject(MdsService);
    private mdsHelperService = inject(MdsHelperService);
    private nodeService = inject(RestNodeService);
    private toast = inject(Toast);
    private translate = inject(TranslateService);
    configService = inject(ConfigurationService);

    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;

    @ViewChild(NodeEntriesWrapperComponent) nodeEntries: NodeEntriesWrapperComponent<Node>;

    @Output() openNode = new EventEmitter<NodeClickEvent<NodeEntriesDataType>>();
    previewLoading = true;
    config: any;
    modes = ['collection', 'rating', 'views', 'downloads', 'random'];
    conditionTypes = ['TOOLPERMISSION'];
    form: UntypedFormGroup;
    previewNodesDataSource = new NodeDataSource();
    previewColumns: ColumnType;
    previewError: string;
    collectionName = '';
    chooseCollection = false;
    codeOptions = {
        minimap: { enabled: false },
        language: 'json',
        autoIndent: true,
        automaticLayout: true,
    };
    toolpermissions: any;

    constructor() {
        this.form = this.formBuilder.group(
            {
                totalCount: [
                    '',
                    [Validators.required, Validators.min(1), Validators.pattern('[0-9]*')],
                ],
                displayCount: [
                    '',
                    [Validators.required, Validators.min(1), Validators.pattern('[0-9]*')],
                ],
                timespan: [
                    '',
                    [Validators.required, Validators.min(1), Validators.pattern('[0-9]*')],
                ],
                timespanAll: [],
            },
            { validator: [ValidateForm] },
        );
        this.form.valueChanges.subscribe(() => this.updateFieldStates());
        this.mdsService.getMetadataSet({}).subscribe((set) => {
            this.previewColumns = this.mdsHelperService.getColumns(set, 'search');
        });
        this.adminService
            .getToolpermissions()
            .subscribe((toolpermissions) => (this.toolpermissions = Object.keys(toolpermissions)));
        void this.update();
    }

    ngAfterViewInit(): void {
        void this.nodeEntries.initOptionsGenerator({});
    }

    onModeChange() {
        this.updateFieldStates();
    }

    /**
     * timespan is only relevant for the statistic based modes and totalCount is redundant in random
     * mode (elastic already shuffles the whole pool), so both get disabled - and therefore excluded
     * from the form validation - where they don't apply
     */
    private updateFieldStates() {
        const isRandom = this.config?.frontpage?.mode === 'random';
        this.setEnabled('timespan', !this.form.getRawValue().timespanAll && !isRandom);
        this.setEnabled('totalCount', !isRandom);
    }

    private setEnabled(control: string, enabled: boolean) {
        if (enabled) {
            this.form.get(control).enable({ emitEvent: false });
        } else {
            this.form.get(control).disable({ emitEvent: false });
        }
    }

    save() {
        const values = this.form.getRawValue();
        for (const key of Object.keys(values)) {
            this.config.frontpage[key] = values[key];
        }
        this.toast.showProgressSpinner();
        this.adminService.updateRepositoryConfig(this.config).subscribe(() => {
            void this.update();
            this.toast.toast('ADMIN.FRONTPAGE.SAVED');
        });
    }

    private async update() {
        try {
            this.config = await this.adminService.getRepositoryConfig().toPromise();
            const values = this.form.getRawValue();
            for (const key of Object.keys(values)) {
                values[key] = this.config.frontpage[key];
            }
            // fix if field is disabled, still fetch value
            if (!values.timespan) {
                values.timespan = this.form.get('timespan').value;
            }
            this.form.setValue(values);
            this.updateFieldStates();
            this.toast.closeProgressSpinner();
            if (this.config.frontpage.collection) {
                this.collectionService
                    .getCollection(this.config.frontpage.collection)
                    .subscribe((c) => {
                        this.collectionName = c.collection.title;
                    });
            }
        } catch (e) {
            this.toast.error(e);
            this.toast.closeProgressSpinner();
            const dialogRef = await this.dialogs.openGenericDialog({
                title: 'ADMIN.FRONTPAGE.CONFIG_BROKEN',
                message: 'ADMIN.FRONTPAGE.CONFIG_BROKEN_INFO',
                buttons: [
                    { label: 'CANCEL', config: { color: 'standard' } },
                    { label: 'ADMIN.FRONTPAGE.RESET', config: { color: 'danger' } },
                ],
                closable: Closable.Standard,
                maxWidth: 500,
            });
            dialogRef.afterClosed().subscribe((response) => {
                if (response === 'ADMIN.FRONTPAGE.RESET') {
                    this.toast.showProgressSpinner();
                    this.adminService.updateRepositoryConfig(null).subscribe(() => {
                        void this.update();
                    });
                } else {
                    this.toast.closeProgressSpinner();
                }
            });
        }
        this.updatePreviews();
    }

    updatePreviews() {
        this.previewLoading = true;
        this.previewNodesDataSource.reset();
        this.previewError = null;
        this.nodeService
            .getChildren(RestConstants.NODES_FRONTPAGE, [], {
                propertyFilter: [RestConstants.ALL],
            })
            .subscribe(
                (nodes) => {
                    this.previewLoading = false;
                    this.previewNodesDataSource.setData(nodes.nodes, nodes.pagination);
                },
                (error) => {
                    if (UIHelper.errorContains(error, 'No Elasticsearch instance')) {
                        this.previewError = 'ELASTICSEARCH';
                    } else {
                        this.previewError = 'UNKNOWN';
                    }
                },
            );
    }

    setCollection(collection: Node) {
        this.config.frontpage.collection = collection.ref.id;
        this.collectionName = collection.title;
        this.chooseCollection = false;
    }

    queryHelp() {
        // @TODO: Link to edu-sharing manpage!
    }

    addQueryCondition() {
        if (!this.config.frontpage.queries) this.config.frontpage.queries = [];
        this.config.frontpage.queries.push({
            condition: {
                type: this.conditionTypes[0],
                negate: false,
            },
        });
    }
    removeQueryCondition(query: any) {
        this.config.frontpage.queries.splice(this.config.frontpage.queries.indexOf(query), 1);
    }
}
const ValidateForm: ValidatorFn = (control) => {
    const displayCount = control.get('displayCount');
    const totalCount = control.get('totalCount');

    if (parseInt(displayCount.value, 10) > parseInt(totalCount.value, 10)) {
        totalCount.setErrors({ outOfRange: true });
    }
    return null;
};
