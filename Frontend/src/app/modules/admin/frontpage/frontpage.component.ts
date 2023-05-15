import { RestAdminService } from '../../../core-module/rest/services/rest-admin.service';
import { Component, EventEmitter, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node } from '../../../core-module/rest/data-object';
import { ListItem } from '../../../core-module/ui/list-item';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { ConfigurationService } from '../../../core-module/rest/services/configuration.service';
import {
    DialogButton,
    RestCollectionService,
    RestIamService,
    RestMdsService,
    RestNodeService,
} from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { MdsHelper } from '../../../core-module/rest/mds-helper';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { NodeDataSource } from '../../../features/node-entries/node-data-source';
import {
    InteractionType,
    NodeEntriesDisplayType,
} from '../../../features/node-entries/entries-model';

@Component({
    selector: 'es-admin-frontpage',
    templateUrl: 'frontpage.component.html',
    styleUrls: ['frontpage.component.scss'],
})
export class AdminFrontpageComponent {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    @Output() onOpenNode = new EventEmitter();
    previewLoading = true;
    config: any;
    modes = ['collection', 'rating', 'views', 'downloads'];
    conditionTypes = ['TOOLPERMISSION'];
    form: FormGroup;
    previewNodesDataSource = new NodeDataSource();
    previewColumns: ListItem[] = [];
    previewError: string;
    collectionName = '';
    chooseCollection = false;
    codeOptions = { minimap: { enabled: false }, language: 'json' };
    toolpermissions: any;

    /*
  totalCountFormControl = new FormControl('', [
    Validators.required,
    Validators.pattern("[0-9]*"),
    this.totalCountValidator
  ]);
  displayCountFormControl = new FormControl('', [
    Validators.required,
    //this.totalCountValidator
  ]);
  */

    constructor(
        private formBuilder: FormBuilder,
        private adminService: RestAdminService,
        private iamService: RestIamService,
        private translate: TranslateService,
        private nodeService: RestNodeService,
        private collectionService: RestCollectionService,
        public configService: ConfigurationService,
        private toast: Toast,
        private mdsService: RestMdsService,
    ) {
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
        this.form.valueChanges.subscribe((values) => {
            values.timespanAll
                ? this.form.get('timespan').disable({ emitEvent: false })
                : this.form.get('timespan').enable({ emitEvent: false });
        });
        this.mdsService.getSet().subscribe((set) => {
            this.previewColumns = MdsHelper.getColumns(this.translate, set, 'search');
        });
        this.adminService
            .getToolpermissions()
            .subscribe((toolpermissions) => (this.toolpermissions = Object.keys(toolpermissions)));
        this.update();
    }

    save() {
        for (const key of Object.keys(this.form.value)) {
            this.config.frontpage[key] = this.form.value[key];
        }
        this.toast.showProgressDialog();
        this.adminService.updateRepositoryConfig(this.config).subscribe(() => {
            this.update();
            this.toast.toast('ADMIN.FRONTPAGE.SAVED');
        });
    }

    private async update() {
        try {
            this.config = await this.adminService.getRepositoryConfig().toPromise();
            const values = this.form.value;
            for (const key of Object.keys(values)) {
                values[key] = this.config.frontpage[key];
            }
            // fix if field is disabled, still fetch value
            if (!values.timespan) {
                values.timespan = this.form.get('timespan').value;
            }
            this.form.setValue(values);
            this.toast.closeModalDialog();
            if (this.config.frontpage.collection) {
                this.collectionService
                    .getCollection(this.config.frontpage.collection)
                    .subscribe((c) => {
                        this.collectionName = c.collection.title;
                    });
            }
        } catch (e) {
            this.toast.error(e);
            this.toast.closeModalDialog();
            this.toast.showConfigurableDialog({
                title: 'ADMIN.FRONTPAGE.CONFIG_BROKEN',
                message: 'ADMIN.FRONTPAGE.CONFIG_BROKEN_INFO',
                buttons: [
                    new DialogButton('CANCEL', { color: 'standard' }, () =>
                        this.toast.closeModalDialog(),
                    ),
                    new DialogButton('ADMIN.FRONTPAGE.RESET', { color: 'danger' }, () => {
                        this.toast.showProgressDialog();
                        this.adminService.updateRepositoryConfig(null).subscribe(() => {
                            this.update();
                        });
                    }),
                ],
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
    openNode(node: any) {
        this.onOpenNode.emit(node.node);
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
