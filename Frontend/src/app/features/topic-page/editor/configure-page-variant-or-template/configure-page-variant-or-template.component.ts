import {
    AfterViewInit,
    Component,
    computed,
    EventEmitter,
    inject,
    input,
    Input,
    InputSignal,
    OnInit,
    Output,
    Signal,
    signal,
    WritableSignal,
    ChangeDetectionStrategy,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DEFAULT, HOME_REPOSITORY, MdsWidget, Node, RestConstants } from 'ngx-edu-sharing-api';
import { Values } from 'ngx-edu-sharing-ui';
import { MdsModule } from '../../../mds/mds.module';
import { SharedModule } from '../../../../shared/shared.module';
import { SidebarStickyFooterComponent } from '../../../editorial-sidebar/sidebar-sticky-footer/sidebar-sticky-footer.component';
import { retrieveNodeId } from '../../shared/utils/template-util';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';

@Component({
    selector: 'es-configure-page-variant-or-template',
    imports: [SharedModule, MdsModule, SidebarStickyFooterComponent],
    templateUrl: 'configure-page-variant-or-template.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['configure-page-variant-or-template.component.scss'],
})
export class ConfigurePageVariantOrTemplateComponent implements AfterViewInit, OnInit {
    private genericWidgetGlobalService = inject(GenericWidgetGlobalService);

    readonly i18nPrefix: string = 'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_VARIANT.';
    readonly templateI18nPrefix: string = 'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_TEMPLATE.';
    readonly globalTemplateI18nPrefix: string = 'TOPIC_PAGE.SIDE_MENU.CONFIG_GLOBAL_TEMPLATE.';

    addToGlobalTemplatesEnabled: InputSignal<boolean> = input(false);
    deleteGlobalTemplateEnabled: InputSignal<boolean> = input(false);
    // the configured node is a global template itself, not a variant or template of a collection
    globalTemplateMode: InputSignal<boolean> = input(false);
    pageVariantConfigNodes: InputSignal<Node[]> = input([]);
    @Input() pageVariantNode: Node;
    @Input() pageVariantTitle: string;
    selectDimensions: InputSignal<Map<string, MdsWidget>> = input(new Map<string, MdsWidget>());
    templateMode: InputSignal<boolean> = input(false);
    @Input() viewIcons: string[] = [];
    @Input() viewLabels: string[] = [];
    @Input() viewModes: string[] = ['checkbox'];

    @Output() addToGlobalTemplatesClicked: EventEmitter<Map<string, string | string[]>> =
        new EventEmitter<Map<string, string | string[]>>();
    @Output() applyChangesClicked: EventEmitter<Map<string, string | string[]>> = new EventEmitter<
        Map<string, string | string[]>
    >();
    @Output() deleteGlobalTemplateClicked: EventEmitter<void> = new EventEmitter<void>();

    private translateService = inject(TranslateService);

    currentValues: Values = {};
    dynamicI18nPrefix = computed(() => {
        if (this.globalTemplateMode()) {
            return this.globalTemplateI18nPrefix;
        }
        return this.templateMode() ? this.templateI18nPrefix : this.i18nPrefix;
    });
    // a collection's templates are told apart like its variants, by their parameters
    duplicateI18nPrefix: Signal<string> = computed(() =>
        this.templateMode() && !this.globalTemplateMode()
            ? this.templateI18nPrefix
            : this.i18nPrefix,
    );
    // pages and templates of a collection are deleted from the list of its pages
    deleteEnabled: Signal<boolean> = computed(
        () => this.globalTemplateMode() && this.deleteGlobalTemplateEnabled(),
    );
    variableInputValid: WritableSignal<boolean> = signal(true);
    invalidParametersTooltip: Signal<string> = computed(() =>
        this.variableInputValid()
            ? null
            : this.translateService.instant(
                  this.isDuplicate()
                      ? this.duplicateI18nPrefix() + 'DUPLICATE_PARAMETERS'
                      : this.i18nPrefix + 'DEFINE_PARAMETERS',
              ),
    );
    furtherExistingPageVariants: Signal<Node[]> = computed(() =>
        this.pageVariantConfigNodes().filter(
            (n) => retrieveNodeId(n) !== retrieveNodeId(this.pageVariantNode),
        ),
    );
    isDuplicate: WritableSignal<boolean> = signal(false);
    initialized: WritableSignal<boolean> = signal(false);
    mdsParams: { repository: string; setId: string } = {
        repository: HOME_REPOSITORY,
        setId: DEFAULT,
    };
    titleInput: string;

    constructor() {
        this.mdsParams.setId = this.genericWidgetGlobalService.getDefaultMds();
    }

    /**
     * Initializes the component by setting up the input variables.
     */
    ngOnInit(): void {
        this.titleInput = this.pageVariantTitle;
        if (this.selectDimensions().size) {
            Array.from(this.selectDimensions().keys()).forEach((key: string) => {
                if (this.pageVariantNode.properties?.[key]?.length) {
                    this.currentValues[key] = this.pageVariantNode.properties[key];
                }
            });
        }
        this.checkValidity();
    }

    /**
     * After the view was initialized, set the initialized signal to true.
     */
    ngAfterViewInit(): void {
        this.initialized.set(true);
    }

    /**
     * Handles the change of the current values input.
     */
    onCurrentValuesChange(currentValues: Values) {
        this.currentValues = currentValues;
        this.checkValidity();
    }

    /**
     * Emits the delete event of the configured global template.
     */
    deleteGlobalTemplate(): void {
        this.deleteGlobalTemplateClicked.emit();
    }

    /**
     * Emits the event to publish the current variant/template into the global templates folder.
     * The still unsaved settings changes are passed along so the host can persist them first —
     * otherwise the published template would carry the previously saved values.
     */
    addToGlobalTemplates(): void {
        this.addToGlobalTemplatesClicked.emit(this.collectChanges());
    }

    /**
     * Processes the variables input and emits the apply changes event.
     */
    applyChanges(): void {
        this.applyChangesClicked.emit(this.collectChanges());
    }

    /**
     * Collects the settings changes (title + variable dimensions) that differ from the values
     * currently persisted on the page variant node.
     */
    private collectChanges(): Map<string, string | string[]> {
        const outputMap = new Map<string, string | string[]>();

        // process variable selections
        Array.from(this.selectDimensions().keys()).forEach((key: string) => {
            // check if the value does exist
            if (this.currentValues[key]?.length) {
                // check if the value has been changed and if so, add them to the output map
                if (
                    JSON.stringify(this.currentValues[key]) !==
                    JSON.stringify(this.pageVariantNode.properties?.[key] || [])
                ) {
                    outputMap.set(key, this.currentValues[key]);
                }
            }
        });
        if (this.titleInput !== this.pageVariantNode.title) {
            outputMap.set(RestConstants.LOM_PROP_TITLE, this.titleInput);
        }
        return outputMap;
    }

    /**
     * Helper function to check the validity of the variables input.
     */
    checkValidity(): void {
        this.isDuplicate.set(false);

        if (!this.currentValues) {
            this.variableInputValid.set(false);
            return;
        }

        let validInput: boolean = true;
        Array.from(this.selectDimensions().keys()).forEach((key: string) => {
            if (!this.currentValues[key]?.length) {
                validInput = false;
            }
        });

        if (validInput) {
            // duplicate check (does another variant already exist with the same values?)
            this.furtherExistingPageVariants()?.forEach((pageVariantNode: Node): void => {
                let isDuplicate: boolean = true;
                Array.from(this.selectDimensions().keys()).forEach((key: string): void => {
                    const currentValue: string[] = this.currentValues?.[key] || [];
                    if (
                        currentValue.length !== pageVariantNode.properties?.[key]?.length ||
                        !currentValue.every((v: string): boolean =>
                            pageVariantNode.properties?.[key]?.includes(v),
                        )
                    ) {
                        isDuplicate = false;
                    }
                });
                if (isDuplicate) {
                    this.isDuplicate.set(true);
                    validInput = false;
                }
            });
        }
        this.variableInputValid.set(validInput);
    }
}
