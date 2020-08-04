import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { DialogButton, SessionStorageService } from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';

interface LuceneTemplate {
    query: string;
    properties: string;
}

type LuceneTemplates = { [key: string]: LuceneTemplate };

@Component({
    selector: 'app-lucene-template-memory',
    templateUrl: './lucene-template-memory.component.html',
    styleUrls: ['./lucene-template-memory.component.scss'],
})
export class LuceneTemplateMemoryComponent implements OnInit {
    private static readonly STORAGE_KEY = 'admin_lucene_templates';

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

    constructor(private storage: SessionStorageService, private toast: Toast) {
        this.storage
            .get(LuceneTemplateMemoryComponent.STORAGE_KEY)
            .subscribe((templates: LuceneTemplates) => {
                this.templates = templates ?? {};
            });
    }

    ngOnInit(): void {}

    createNewTemplate(): void {
        if (!this.newTemplateName) {
            // Do nothing
        } else if (this.newTemplateName in this.templates) {
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
            this.storage.set(LuceneTemplateMemoryComponent.STORAGE_KEY, this.templates);
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
        this.storage.set(LuceneTemplateMemoryComponent.STORAGE_KEY, this.templates);
    }

    private deleteTemplate(template: string): void {
        this.selectedTemplate = null;
        delete this.templates[template];
        this.storage.set(LuceneTemplateMemoryComponent.STORAGE_KEY, this.templates);
    }
}
