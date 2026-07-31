import {
    Component,
    computed,
    EventEmitter,
    OnInit,
    Output,
    signal,
    TemplateRef,
    ViewChild,
    WritableSignal,
    inject,
} from '@angular/core';
import { DEFAULT, SessionStorageService } from 'ngx-edu-sharing-api';
import { Helper, MdsExtendedValues, OptionItem } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import { SharedModule } from '../../shared/shared.module';
import {
    DELETE_OR_CANCEL,
    SAVE_OR_CANCEL,
} from '../dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../dialogs/dialogs.service';
import { MdsEditorWrapperComponent } from '../mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MdsModule } from '../mds/mds.module';

interface Template {
    id: string;
    name: string;
    values: MdsExtendedValues;
}

@Component({
    selector: 'es-metadata-template-management',
    providers: [],
    templateUrl: './metadata-template-management.component.html',
    styleUrls: ['./metadata-template-management.component.scss'],
    imports: [SharedModule, MdsModule],
})
export class MetadataTemplateManagementComponent implements OnInit {
    private dialogs = inject(DialogsService);
    private storage = inject(SessionStorageService);

    protected readonly i18nPrefix: string = 'EDITORIAL.OPTIONS.NODES_SELECTOR.';
    private readonly emptyTemplate: Template = {
        id: 'empty-template',
        name: this.i18nPrefix + 'METHODOLOGY.NO_TEMPLATE',
        values: {},
    };
    customTemplates: WritableSignal<Template[]> = signal([]);
    /** the value state as it was when the user last hit "save", restored on re-open */
    lastUsedValues: WritableSignal<MdsExtendedValues> = signal(null);
    selectedTemplateIndex: WritableSignal<number> = signal(-1);
    /** the empty template, followed by the "last used" entry as soon as there is a saved state */
    initialTemplates = computed<Template[]>(() => [
        this.emptyTemplate,
        ...(this.lastUsedValues()
            ? [
                  {
                      id: 'last-used-template',
                      name: this.i18nPrefix + 'METHODOLOGY.LAST_USED',
                      values: this.lastUsedValues(),
                  },
              ]
            : []),
    ]);
    templates = computed(() => [...this.initialTemplates(), ...this.customTemplates()]);
    currentTemplate = computed(() =>
        this.templates()?.[this.selectedTemplateIndex()]
            ? this.templates()[this.selectedTemplateIndex()]
            : this.templates()[0],
    );
    templateOptions = computed(() => {
        // call the signal to recompute the options value
        this.selectedTemplateIndex();
        return this.templates().map((template, index) => {
            const optionItem = new OptionItem(template.name, null, () => {
                void this.selectTemplate(index);
            });
            optionItem.isEnabled = index !== this.selectedTemplateIndex();
            // TODO: this sets .mat-menu-item-selected, but the visible selection is based on .cdk-focused
            // optionItem.isSelected = index === this.selectedTemplateIndex();
            return optionItem;
        });
    });
    templateSelection: WritableSignal<boolean> = signal(false);
    readonly metadataTemplatesKey: string = 'metadataTemplates';
    readonly metadataTemplateslastUsedKey: string = 'metadataTemplatesLastUsed';
    readonly metadataTemplateGroup: string = 'io_bulk_sidebar';
    selectedValues: WritableSignal<MdsExtendedValues> = signal(null);
    templateName: string = '';
    @Output() extendedValuesChange = new EventEmitter<MdsExtendedValues>();
    @ViewChild('mdsEditor') mdsEditor: MdsEditorWrapperComponent;
    @ViewChild('templateTitleDialog') templateTitleDialogRef: TemplateRef<undefined>;

    async ngOnInit() {
        await this.updateCustomTemplates();
        const lastUsed = await this.storage.get<MdsExtendedValues>(
            this.metadataTemplateslastUsedKey,
        );
        if (this.hasValues(lastUsed)) {
            this.lastUsedValues.set(lastUsed);
            // the "last used" entry is always inserted right after the empty template
            void this.selectTemplate(1);
        }
    }

    /**
     * Stores the current value state as the "last used" state. To be called by the host whenever the
     * user saves the metadata, so the selection is restored the next time the view is opened.
     */
    async persistLastUsedValues(): Promise<void> {
        const values = this.selectedValues();
        await this.storage.set(this.metadataTemplateslastUsedKey, values ?? {});
        const hadLastUsed = !!this.lastUsedValues();
        this.lastUsedValues.set(this.hasValues(values) ? values : null);
        // adding/removing the entry shifts all following templates, so keep the selection in place
        if (hadLastUsed !== !!this.lastUsedValues() && this.selectedTemplateIndex() >= 1) {
            this.selectedTemplateIndex.update((index) =>
                this.lastUsedValues() ? index + 1 : index - 1,
            );
        }
    }

    /**
     * Handles the toggling of the template selection.
     */
    toggleTemplateSelection(): void {
        this.templateSelection.set(!this.templateSelection());
    }

    /**
     * Reacts to the (currentValuesExtendedChange) output by holding the currently selected values.
     *
     * @param event
     */
    currentValuesExtendedChange(event: MdsExtendedValues) {
        this.extendedValuesChange.emit(event);
        this.selectedValues.set(event);
    }

    /**
     * Handles the renaming of a given template.
     *
     * @param template
     */
    async renameTemplate(template: Template): Promise<void> {
        const defaultName: string = 'Template ' + (this.customTemplates().length + 1);
        this.templateName = template.name;
        const editTemplateDialogRef = await this.dialogs.openGenericDialog({
            title: this.i18nPrefix + 'METHODOLOGY.EDIT_TEMPLATE.DIALOG_TITLE',
            subtitle: template.name,
            avatar: {
                kind: 'icon',
                icon: 'edit',
            },
            contentTemplate: this.templateTitleDialogRef,
            buttons: SAVE_OR_CANCEL,
        });
        const result = await firstValueFrom(editTemplateDialogRef.afterClosed());
        if (result === 'SAVE') {
            template.name = this.templateName || defaultName;
            const currentTemplates: Template[] = [...this.customTemplates()];
            const index = currentTemplates.findIndex((t) => t.id === template.id);
            currentTemplates.splice(index, 1, template);
            await this.storage.set(this.metadataTemplatesKey, currentTemplates);
            await this.updateCustomTemplates();
        }
        // reset template name
        this.templateName = '';
    }

    /**
     * Handles the deletion of a given template.
     *
     * @param template
     */
    async deleteTemplate(template: Template): Promise<void> {
        const deleteTemplateDialogRef = await this.dialogs.openGenericDialog({
            title: this.i18nPrefix + 'METHODOLOGY.DELETE_TEMPLATE.DIALOG_TITLE',
            subtitle: template.name,
            avatar: {
                kind: 'icon',
                icon: 'delete',
            },
            message: this.i18nPrefix + 'METHODOLOGY.DELETE_TEMPLATE.CONFIRMATION_MESSAGE',
            buttons: DELETE_OR_CANCEL,
        });
        const result = await firstValueFrom(deleteTemplateDialogRef.afterClosed());
        if (result === 'YES_DELETE') {
            const currentTemplates: Template[] = [...this.customTemplates()];
            const index = currentTemplates.findIndex((t) => t.id === template.id);
            currentTemplates.splice(index, 1);
            await this.storage.set(this.metadataTemplatesKey, currentTemplates);
            await this.updateCustomTemplates();
            if (!this.customTemplates().length) {
                this.templateSelection.set(false);
            }
        }
    }

    /**
     * Handles the saving of the current selection as a template.
     */
    async createTemplate(): Promise<void> {
        const defaultName: string = 'Template ' + (this.customTemplates().length + 1);
        this.templateName = defaultName;
        const createTemplateDialogRef = await this.dialogs.openGenericDialog({
            title: this.i18nPrefix + 'METHODOLOGY.CREATE_TEMPLATE.DIALOG_TITLE',
            avatar: {
                kind: 'icon',
                icon: 'save',
            },
            contentTemplate: this.templateTitleDialogRef,
            buttons: SAVE_OR_CANCEL,
        });
        const result = await firstValueFrom(createTemplateDialogRef.afterClosed());
        if (result === 'SAVE') {
            const currentTemplates: Template[] = [...this.customTemplates()];
            currentTemplates.push({
                id: uuidv4(),
                name: this.templateName || defaultName,
                values: this.selectedValues(),
            });
            await this.storage.set(this.metadataTemplatesKey, currentTemplates);
            await this.updateCustomTemplates();
            void this.selectTemplate(this.templates().length - 1);
        }
        // reset template name
        this.templateName = '';
    }

    // HELPERS
    /**
     * Helper function to check whether a value state holds any metadata at all.
     */
    private hasValues(values: MdsExtendedValues): boolean {
        return !!values && Object.keys(values).length > 0;
    }

    /**
     * Helper function to retrieve the custom templates.
     */
    private async updateCustomTemplates(): Promise<void> {
        const customTemplates = await this.storage.get<Template[]>(this.metadataTemplatesKey);
        if (customTemplates) {
            this.customTemplates.set(customTemplates as Template[]);
        }
    }

    /**
     * Helper function to select a template at a given index.
     *
     * @param index
     */
    private async selectTemplate(index: number) {
        this.selectedTemplateIndex.set(index);
        // wait for the view being rendered
        setTimeout(() => {
            void this.mdsEditor.reInit();
        });
    }

    protected readonly DEFAULT = DEFAULT;
    protected readonly Helper = Helper;
}
