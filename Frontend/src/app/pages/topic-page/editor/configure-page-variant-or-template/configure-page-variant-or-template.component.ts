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
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DEFAULT, HOME_REPOSITORY, MdsWidget, Node, RestConstants } from 'ngx-edu-sharing-api';
import { Values } from 'ngx-edu-sharing-ui';
import { MdsModule } from '../../../../features/mds/mds.module';
import { SharedModule } from '../../../../shared/shared.module';
import { retrieveNodeId } from '../../shared/utils/template-util';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';

@Component({
    selector: 'es-configure-page-variant-or-template',
    imports: [SharedModule, MdsModule],
    templateUrl: 'configure-page-variant-or-template.component.html',
    styleUrls: ['configure-page-variant-or-template.component.scss'],
})
export class ConfigurePageVariantOrTemplateComponent implements AfterViewInit, OnInit {
    private genericWidgetGlobalService = inject(GenericWidgetGlobalService);

    readonly i18nPrefix: string = 'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_VARIANT.';
    readonly templateI18nPrefix: string = 'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_TEMPLATE.';

    addToGlobalTemplatesEnabled: InputSignal<boolean> = input(false);
    deleteVariantEnabled: InputSignal<boolean> = input(false);
    pageVariantConfigNodes: InputSignal<Node[]> = input([]);
    @Input() pageVariantNode: Node;
    @Input() pageVariantTitle: string;
    selectDimensions: InputSignal<Map<string, MdsWidget>> = input(new Map<string, MdsWidget>());
    templateMode: InputSignal<boolean> = input(false);
    templateNode: InputSignal<Node> = input<Node>(null);
    templateUpdateAvailable: InputSignal<boolean> = input(false);
    @Input() viewIcons: string[] = [];
    @Input() viewLabels: string[] = [];
    @Input() viewModes: string[] = ['checkbox'];

    @Output() addToGlobalTemplatesClicked: EventEmitter<Map<string, string | string[]>> =
        new EventEmitter<Map<string, string | string[]>>();
    @Output() applyChangesClicked: EventEmitter<Map<string, string | string[]>> = new EventEmitter<
        Map<string, string | string[]>
    >();
    @Output() deletePageVariantClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() regenerateClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() settingsValidityChanged: EventEmitter<boolean> = new EventEmitter<boolean>();

    private translateService = inject(TranslateService);

    currentValues: Values = {};
    dynamicI18nPrefix = computed(() =>
        this.templateMode() ? this.templateI18nPrefix : this.i18nPrefix,
    );
    templateModifiedDate: Signal<string> = computed(() => {
        const timestamp = this.templateNode()?.properties?.[RestConstants.CM_MODIFIED_DATE]?.[0];
        if (!timestamp) return '';
        return new Date(Number(timestamp)).toLocaleDateString('de-DE', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
        });
    });
    regenerateTooltip: Signal<string> = computed(() => {
        if (this.templateUpdateAvailable()) {
            return this.translateService.instant(
                `${this.i18nPrefix}TEMPLATE_UPDATE.TOOLTIP_AVAILABLE`,
                { date: this.templateModifiedDate() },
            );
        }
        return this.translateService.instant(
            `${this.i18nPrefix}TEMPLATE_UPDATE.TOOLTIP_UP_TO_DATE`,
        );
    });
    variableInputValid: WritableSignal<boolean> = signal(true);
    invalidParametersTooltip: Signal<string> = computed(() =>
        this.variableInputValid()
            ? null
            : this.translateService.instant(
                  this.i18nPrefix +
                      (this.isDuplicate() ? 'DUPLICATE_PARAMETERS' : 'DEFINE_PARAMETERS'),
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
     *
     * @param currentValues
     */
    onCurrentValuesChange(currentValues: Values) {
        this.currentValues = currentValues;
        this.checkValidity();
    }

    /**
     * Emits the page variant delete event.
     */
    deletePageVariant(): void {
        this.deletePageVariantClicked.emit();
    }

    /**
     * Emits the regenerate event to replace this variant with a fresh copy of its template.
     */
    triggerRegenerate(): void {
        this.regenerateClicked.emit();
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

        if (!this.currentValues || Object.keys(!this.currentValues)?.length) {
            this.variableInputValid.set(false);
            return;
        }

        let validInput: boolean = true;
        Array.from(this.selectDimensions().keys()).forEach((key: string) => {
            if (!this.currentValues[key]?.length) {
                validInput = false;
            }
        });

        if (!validInput) {
            this.settingsValidityChanged.emit(validInput);
        } else {
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
