import {
    Component,
    computed,
    OnInit,
    signal,
    TemplateRef,
    ViewChild,
    WritableSignal,
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
    protected readonly i18nPrefix: string = 'EDITORIAL.OPTIONS.NODES_SELECTOR.';
    initialTemplates: Template[] = [
        {
            id: uuidv4(),
            name: 'keine Vorlage',
            values: {},
        },
    ];
    customTemplates: WritableSignal<Template[]> = signal([]);
    selectedTemplateIndex: WritableSignal<number> = signal(-1);
    templates = computed(() => [...this.initialTemplates, ...this.customTemplates()]);
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
                this.selectTemplate(index);
            });
            optionItem.isEnabled = index !== this.selectedTemplateIndex();
            // TODO: this sets .mat-menu-item-selected, but the visible selection is based on .cdk-focused
            // optionItem.isSelected = index === this.selectedTemplateIndex();
            return optionItem;
        });
    });
    templateSelection: WritableSignal<boolean> = signal(false);
    readonly metadataTemplatesKey: string = 'metadataTemplates';
    readonly metadataTemplateGroup: string = 'io_bulk';
    selectedValues: WritableSignal<MdsExtendedValues> = signal(null);
    templateName: string = '';
    @ViewChild('mdsEditor') mdsEditor: MdsEditorWrapperComponent;
    @ViewChild('templateTitleDialog') templateTitleDialogRef: TemplateRef<undefined>;

    constructor(private dialogs: DialogsService, private storage: SessionStorageService) {}

    async ngOnInit() {
        await this.updateCustomTemplates();
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
            this.selectTemplate(this.templates().length - 1);
        }
        // reset template name
        this.templateName = '';
    }

    // HELPERS
    /**
     * Helper function to retrieve the custom templates.
     */
    private async updateCustomTemplates(): Promise<void> {
        const customTemplates = await firstValueFrom(this.storage.get(this.metadataTemplatesKey));
        if (customTemplates) {
            this.customTemplates.set(customTemplates as Template[]);
        }
    }

    /**
     * Helper function to select a template at a given index.
     *
     * @param index
     */
    private selectTemplate(index: number) {
        this.selectedTemplateIndex.set(index);
        // wait for the view being rendered
        setTimeout(() => {
            void this.mdsEditor.reInit();
        });
    }

    protected readonly DEFAULT = DEFAULT;
    protected readonly Helper = Helper;
}
