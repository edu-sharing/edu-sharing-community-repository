import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { DialogButton, SessionStorageService } from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import {TranslateService} from '@ngx-translate/core';

interface LuceneTemplate {
    query: string;
    properties: string;
    isDefault?: boolean;
}

type LuceneTemplates = { [key: string]: LuceneTemplate };

@Component({
    selector: 'es-lucene-template-memory',
    templateUrl: './lucene-template-memory.component.html',
    styleUrls: ['./lucene-template-memory.component.scss'],
})
export class LuceneTemplateMemoryComponent implements OnInit {
    private static readonly STORAGE_KEY = 'admin_lucene_templates';
    private static readonly DEFAULT_TEMPLATES: LuceneTemplates = {
        GROUPS: {
            query: 'TYPE:"cm:authorityContainer"',
            properties: ['cm:authorityDisplayName', 'cm:authorityName', 'ccm:groupType', 'ccm:groupEmail', 'ccm:groupScope', 'ccm:groupSource'].join('\n'),
            isDefault: true
        },
        ORGS: {
            query: 'TYPE:"cm:authorityContainer"',
            properties: ['cm:authorityDisplayName', 'cm:authorityName','ccm:edu_homedir' ,'ccm:groupType', 'ccm:groupEmail', 'ccm:groupScope', 'ccm:groupSource'].join('\n'),
            isDefault: true
        },
        PERSONS: {
            query: 'TYPE:"cm:person"',
            properties: ['cm:userName', 'cm:firstName', 'cm:lastName', 'cm:email', 'cm:esuid', 'cm:homeFolder', 'cm:esLastLogin'].join('\n'),
            isDefault: true
        },
        CREATED_CONTENTS_BY_PERSON: {
            query: '@cm\\:creator:"user" OR @cm\\:modifier:"user"',
            properties: ['sys:node-uuid', 'cm:name', 'cclom:title', 'cm:creator', 'cm:created', 'cm:modifier', 'cm:modified', 'cclom:general_keyword', 'ccm:comment_content'].join('\n'),
            isDefault: true
        },
        BROKEN_LINKS: {
            query: 'ISNOTNULL:"ccm:location_status" AND NOT @ccm\\:location_status:"200"',
            properties: ['sys:node-uuid', 'cm:name', 'cclom:title', 'cm:created', 'cm:modified', 'cclom:general_keyword', 'cclom:location', 'ccm:replicationsource', 'ccm:replicationsourceid'].join('\n'),
            isDefault: true
        }
    }

    @Input() query: string;
    @Input() properties: string;

    @Output() queryChange = new EventEmitter<string>();
    @Output() propertiesChange = new EventEmitter<string>();

    templates: LuceneTemplates;
    selectedTemplate: string;
    isNewTemplateDialogVisible = false;
    newTemplateName: string;

    readonly newTemplateDialogButtons = [
        new DialogButton('CLOSE', DialogButton.TYPE_CANCEL, () => {
            this.closeNewTemplateDialog();
        }),
        new DialogButton(
            'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CREATE_BUTTON',
            DialogButton.TYPE_PRIMARY,
            () => this.createNewTemplate(),
        ),
    ];

    constructor(private storage: SessionStorageService,
                private translate: TranslateService,
                private toast: Toast) {
        this.storage
            .get(LuceneTemplateMemoryComponent.STORAGE_KEY)
            .subscribe((templates: LuceneTemplates) => {
                this.templates = templates ?? {};
                for(const key of Object.keys(LuceneTemplateMemoryComponent.DEFAULT_TEMPLATES)) {
                    this.templates[this.translate.instant('ADMIN.BROWSER.LUCENE_DEFAULT_TEMPLATES.' + key)] =
                        LuceneTemplateMemoryComponent.DEFAULT_TEMPLATES[key];
                }
            });
    }

    ngOnInit(): void {}

    createNewTemplate(): void {
        if (!this.newTemplateName) {
            // Do nothing
        } else if (this.newTemplateName in this.templates) {
            if(this.templates[this.newTemplateName].isDefault) {
                this.toast.error(null, 'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.DEFAULT_TEMPLATE_OVERRIDE');
                return;
            }
            this.confirmUpdateTemplate(this.newTemplateName).then((isUpdated) => {
                if (isUpdated) {
                    this.closeNewTemplateDialog();
                }
            });
        } else {
            this.templates[this.newTemplateName] = {
                query: this.query,
                properties: this.properties,
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
    }

    confirmUpdateTemplate(template: string): Promise<boolean> {
        return new Promise((resolve) => {
            this.toast.showModalDialog(
                'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_TITLE',
                'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_MESSAGE',
                [
                    new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => {
                        this.toast.closeModalDialog();
                        resolve(false);
                    }),
                    new DialogButton(
                        'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_UPDATE_BUTTON',
                        DialogButton.TYPE_PRIMARY,
                        () => {
                            this.updateTemplate(template);
                            this.toast.closeModalDialog();
                            resolve(true);
                        },
                    ),
                ],
                true,
                () => {
                    resolve(false);
                },
                { template },
            );
        });
    }

    confirmDeleteTemplate(template: string): void {
        this.toast.showModalDialog(
            'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_TITLE',
            'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_MESSAGE',
            [
                new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => {
                    this.toast.closeModalDialog();
                }),
                new DialogButton(
                    'ADMIN.BROWSER.LUCENE_TEMPLATE_MEMORY.CONFIRM_DELETE_BUTTON',
                    DialogButton.TYPE_DANGER,
                    () => {
                        this.deleteTemplate(template);
                        this.toast.closeModalDialog();
                    },
                ),
            ],
            true,
            undefined,
            { template },
        );
    }

    private updateTemplate(template: string): void {
        this.templates[template] = {
            query: this.query,
            properties: this.properties,
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
        Object.keys(this.templates).filter((t) =>
            !this.templates[t].isDefault
        ).forEach((t) =>
            storeTemplates[t] = this.templates[t]
        );
        this.storage.set(LuceneTemplateMemoryComponent.STORAGE_KEY, storeTemplates);
    }
}
