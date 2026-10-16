import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DialogButton } from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { SessionStorageService } from 'ngx-edu-sharing-api';

interface LuceneTemplate {
    query: string;
    properties: string;
    /** elastic index (name, pattern or one of the constants -workspace- / -authorities-) */
    index?: string;
    isDefault?: boolean;
}

const dsl = (query: object) => JSON.stringify({ query }, null, 2);
// the workspace index also contains archived nodes, restrict to the workspace store
const workspaceDsl = (query: object) =>
    dsl({
        bool: {
            filter: [{ match: { 'nodeRef.storeRef.protocol': 'workspace' } }],
            must: [query],
        },
    });

type LuceneTemplates = { [key: string]: LuceneTemplate };

@Component({
    selector: 'es-lucene-template-memory',
    templateUrl: './lucene-template-memory.component.html',
    styleUrls: ['./lucene-template-memory.component.scss'],
    standalone: false,
})
export class LuceneTemplateMemoryComponent {
    private dialogs = inject(DialogsService);
    private storage = inject(SessionStorageService);
    private toast = inject(Toast);
    private translate = inject(TranslateService);

    private static readonly STORAGE_KEY = 'admin_lucene_templates';
    private static readonly DEFAULT_TEMPLATES: LuceneTemplates = {
        GROUPS: {
            index: '-authorities-',
            query: dsl({ term: { type: 'cm:authorityContainer' } }),
            properties: [
                'cm:authorityDisplayName',
                'cm:authorityName',
                'ccm:groupType',
                'ccm:groupEmail',
                'ccm:groupScope',
                'ccm:groupSource',
            ].join('\n'),
            isDefault: true,
        },
        ORGS: {
            index: '-authorities-',
            query: dsl({
                bool: {
                    must: [
                        { term: { type: 'cm:authorityContainer' } },
                        { prefix: { 'properties.cm:authorityName.keyword': 'GROUP_ORG_' } },
                    ],
                },
            }),
            properties: [
                'cm:authorityDisplayName',
                'cm:authorityName',
                'ccm:edu_homedir',
                'ccm:groupType',
                'ccm:groupEmail',
                'ccm:groupScope',
                'ccm:groupSource',
            ].join('\n'),
            isDefault: true,
        },
        PERSONS: {
            index: '-authorities-',
            query: dsl({ term: { type: 'cm:person' } }),
            properties: [
                'cm:userName',
                'cm:firstName',
                'cm:lastName',
                'cm:email',
                'cm:esuid',
                'cm:homeFolder',
                'cm:esLastLogin',
            ].join('\n'),
            isDefault: true,
        },
        CREATED_CONTENTS_BY_PERSON: {
            index: '-workspace-',
            query: workspaceDsl({
                bool: {
                    should: [
                        { term: { 'properties.cm:creator.keyword': 'user' } },
                        { term: { 'properties.cm:modifier.keyword': 'user' } },
                    ],
                    minimum_should_match: 1,
                },
            }),
            properties: [
                'cm:name',
                'cclom:title',
                'cm:creator',
                'cm:created',
                'cm:modifier',
                'cm:modified',
                'cclom:general_keyword',
                'ccm:comment_content',
            ].join('\n'),
            isDefault: true,
        },
        BROKEN_LINKS: {
            index: '-workspace-',
            query: workspaceDsl({
                bool: {
                    must: [{ exists: { field: 'properties.ccm:location_status' } }],
                    must_not: [{ term: { 'properties.ccm:location_status': '200' } }],
                },
            }),
            properties: [
                'cm:name',
                'cclom:title',
                'cm:created',
                'cm:modified',
                'cclom:general_keyword',
                'cclom:location',
                'ccm:replicationsource',
                'ccm:replicationsourceid',
            ].join('\n'),
            isDefault: true,
        },
        MISSING_LICENSE: {
            index: '-workspace-',
            query: workspaceDsl({
                bool: {
                    must: [{ term: { type: 'ccm:io' } }],
                    must_not: [
                        { exists: { field: 'properties.ccm:commonlicense_key' } },
                        { term: { aspects: 'ccm:io_childobject' } },
                        { term: { aspects: 'ccm:collection_io_reference' } },
                    ],
                },
            }),
            properties: [
                'cm:name',
                'cclom:title',
                'cm:creator',
                'cm:created',
                'cm:modified',
                'ccm:replicationsource',
            ].join('\n'),
            isDefault: true,
        },
        PUBLISHED_COPIES: {
            index: '-workspace-',
            query: workspaceDsl({ exists: { field: 'properties.ccm:published_original' } }),
            properties: [
                'cm:name',
                'cclom:title',
                'ccm:published_original',
                'ccm:published_date',
                'ccm:published_handle_id',
                'ccm:published_doi_id',
                'cm:creator',
            ].join('\n'),
            isDefault: true,
        },
        COLLECTIONS: {
            index: '-workspace-',
            query: workspaceDsl({
                bool: {
                    must: [{ term: { type: 'ccm:map' } }, { term: { aspects: 'ccm:collection' } }],
                },
            }),
            properties: [
                'cm:name',
                'cclom:title',
                'ccm:collectiontype',
                'ccm:collectionscope',
                'ccm:collection_pinned_status',
                'cm:creator',
                'cm:created',
                'cm:modified',
                'parent::cm:name',
            ].join('\n'),
            isDefault: true,
        },
        IMPORTED_BY_SOURCE: {
            index: '-workspace-',
            query: workspaceDsl({ term: { 'properties.ccm:replicationsource.keyword': 'source' } }),
            properties: [
                'cm:name',
                'cclom:title',
                'ccm:replicationsource',
                'ccm:replicationsourceid',
                'ccm:replicationsourcetimestamp',
                'ccm:importblocked',
                'cm:created',
                'cm:modified',
            ].join('\n'),
            isDefault: true,
        },
        INACTIVE_PERSONS: {
            index: '-authorities-',
            query: dsl({
                bool: {
                    must: [{ term: { type: 'cm:person' } }],
                    should: [
                        { range: { 'properties.cm:esLastLogin': { lt: 'now-1y' } } },
                        {
                            bool: {
                                must_not: [{ exists: { field: 'properties.cm:esLastLogin' } }],
                            },
                        },
                    ],
                    minimum_should_match: 1,
                },
            }),
            properties: [
                'cm:userName',
                'cm:firstName',
                'cm:lastName',
                'cm:email',
                'cm:esLastLogin',
                'cm:created',
            ].join('\n'),
            isDefault: true,
        },
    };

    @Input() query: string;
    @Input() properties: string;
    @Input() index: string;

    @Output() queryChange = new EventEmitter<string>();
    @Output() propertiesChange = new EventEmitter<string>();
    @Output() indexChange = new EventEmitter<string>();

    templates: LuceneTemplates;
    selectedTemplate: string;
    isNewTemplateDialogVisible = false;
    newTemplateName: string;

    readonly newTemplateDialogButtons = [
        new DialogButton('CLOSE', { color: 'standard' }, () => {
            this.closeNewTemplateDialog();
        }),
        new DialogButton(
            'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CREATE_BUTTON',
            { color: 'primary' },
            () => this.createNewTemplate(),
        ),
    ];

    constructor() {
        void this.storage
            .get<LuceneTemplates>(LuceneTemplateMemoryComponent.STORAGE_KEY)
            .then((templates) => {
                this.templates = templates ?? {};
                for (const key of Object.keys(LuceneTemplateMemoryComponent.DEFAULT_TEMPLATES)) {
                    this.templates[
                        this.translate.instant('ADMIN.BROWSER.LUCENE_DEFAULT_TEMPLATES.' + key)
                    ] = LuceneTemplateMemoryComponent.DEFAULT_TEMPLATES[key];
                }
            });
    }

    createNewTemplate(): void {
        if (!this.newTemplateName) {
            // Do nothing
        } else if (this.newTemplateName in this.templates) {
            if (this.templates[this.newTemplateName].isDefault) {
                this.toast.error(
                    null,
                    'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.DEFAULT_TEMPLATE_OVERRIDE',
                );
                return;
            }
            void this.confirmUpdateTemplate(this.newTemplateName).then((isUpdated) => {
                if (isUpdated) {
                    this.closeNewTemplateDialog();
                }
            });
        } else {
            this.templates[this.newTemplateName] = {
                query: this.query,
                properties: this.properties,
                index: this.index,
            };
            this.updateStorage();
            this.selectedTemplate = this.newTemplateName;
            this.closeNewTemplateDialog();
        }
    }

    closeNewTemplateDialog(): void {
        this.isNewTemplateDialogVisible = false;
        this.newTemplateName = '';
    }

    loadTemplate(key: string) {
        this.selectedTemplate = key;
        this.query = this.templates[key].query;
        this.queryChange.emit(this.query);
        this.properties = this.templates[key].properties;
        this.propertiesChange.emit(this.properties);
        this.index = this.templates[key].index;
        this.indexChange.emit(this.index);
    }

    async confirmUpdateTemplate(template: string): Promise<boolean> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_TITLE',
            message: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_MESSAGE',
            messageParameters: { template },
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                {
                    label: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_BUTTON',
                    config: { color: 'primary' },
                },
            ],
        });
        const response = await dialogRef.afterClosed().toPromise();
        if (response === 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_BUTTON') {
            this.updateTemplate(template);
            return true;
        } else {
            return false;
        }
    }

    async confirmDeleteTemplate(template: string): Promise<void> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_TITLE',
            message: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_MESSAGE',
            messageParameters: { template },
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                {
                    label: 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_BUTTON',
                    config: { color: 'danger' },
                },
            ],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_BUTTON') {
                this.deleteTemplate(template);
            }
        });
    }

    private updateTemplate(template: string): void {
        this.templates[template] = {
            query: this.query,
            properties: this.properties,
            index: this.index,
        };
        this.updateStorage();
    }

    private deleteTemplate(template: string): void {
        this.selectedTemplate = null;
        delete this.templates[template];
        this.updateStorage();
    }

    private updateStorage() {
        const storeTemplates: LuceneTemplates = {};
        Object.keys(this.templates)
            .filter((t) => !this.templates[t].isDefault)
            .forEach((t) => (storeTemplates[t] = this.templates[t]));
        void this.storage.set(LuceneTemplateMemoryComponent.STORAGE_KEY, storeTemplates);
    }
}
