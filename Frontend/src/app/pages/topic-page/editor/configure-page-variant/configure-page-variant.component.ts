import {
    Component,
    computed,
    EventEmitter,
    input,
    Input,
    InputSignal,
    OnInit,
    Output,
    signal,
    WritableSignal,
} from '@angular/core';
import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { MdsWidget, Node, RestConstants } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../../shared/shared.module';
import { VarDirective } from '../../shared/directives/ng-var.directive';
import { DEFAULT_PAGE_VARIANT_CONFIG_PROP } from '../../shared/types/custom-definitions';
import { PageVariantConfig } from '../../shared/types/page-variant-config';
import { retrievePageVariantConfig } from '../../shared/utils/template-util';

@Component({
    selector: 'es-configure-page-variant-dialog',
    imports: [SharedModule, VarDirective],
    templateUrl: 'configure-page-variant.component.html',
    styleUrls: ['configure-page-variant.component.scss'],
})
export class ConfigurePageVariantComponent implements OnInit {
    readonly i18nPrefix: string = 'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_VARIANT.';
    readonly templateI18nPrefix: string = 'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_TEMPLATE.';

    deleteVariantEnabled: InputSignal<boolean> = input(false);
    @Input() pageVariantNode: Node;
    @Input() pageVariantTitle: string;
    selectDimensions: InputSignal<Map<string, MdsWidget>> = input(new Map<string, MdsWidget>());
    templateMode: InputSignal<boolean> = input(false);
    @Input() viewIcons: string[] = [];
    @Input() viewLabels: string[] = [];
    @Input() viewModes: string[] = ['checkbox'];

    @Output() applyChangesClicked: EventEmitter<Map<string, string>> = new EventEmitter<
        Map<string, string>
    >();
    @Output() deletePageVariantClicked: EventEmitter<void> = new EventEmitter<void>();
    @Output() settingsValidityChanged: EventEmitter<boolean> = new EventEmitter<boolean>();

    availableSelectDimensionKeys = computed(() => Array.from(this.selectDimensions().keys()));
    form: FormGroup;
    formInitialized: WritableSignal<boolean> = signal(false);
    formInputValid: WritableSignal<boolean> = signal(true);

    /**
     * Initializes the component by retrieving the page variant config and initializing the form controls.
     */
    ngOnInit(): void {
        const pageVariantConfig: PageVariantConfig = retrievePageVariantConfig(
            this.pageVariantNode,
        );
        const parameters = pageVariantConfig?.variables ?? {};

        const formControls: { [key: string]: FormControl | FormArray } = {
            title: new FormControl(this.pageVariantTitle),
        };

        if (this.selectDimensions().size) {
            let index: number = 0;
            // TODO: replace by mds usage
            this.selectDimensions().forEach((widget: MdsWidget, key: string) => {
                if (widget.values && widget.values.length > 0) {
                    let matchingParameter = parameters[key];
                    // fix old format not being recognized as array
                    if (!Array.isArray(matchingParameter)) {
                        matchingParameter = [matchingParameter];
                    }
                    const isCheckbox: boolean = this.viewModes[index] === 'checkbox';
                    if (isCheckbox) {
                        // handle checkboxes - create FormArray
                        const checkboxControls = widget.values.map((value) => {
                            const isSelected = matchingParameter.includes(value.id);
                            return new FormGroup({
                                id: new FormControl(value.id),
                                checked: new FormControl(isSelected),
                            });
                        });
                        formControls[key] = new FormArray(checkboxControls);
                    } else {
                        // handle radio buttons - single FormControl
                        let defaultValue = '';
                        if (matchingParameter.length) {
                            defaultValue = matchingParameter[0];
                        }
                        formControls[key] = new FormControl(defaultValue);
                    }
                }
                index++;
            });
        }

        this.form = new FormGroup(formControls);

        // initial form validity check
        this.checkValidity();

        // subscribe to form changes to check the validity of the form input
        this.form.valueChanges.subscribe((currentValue) => {
            this.checkValidity(currentValue);
        });

        this.formInitialized.set(true);
    }

    /**
     * Emits the page variant delete event.
     */
    deletePageVariant(): void {
        this.deletePageVariantClicked.emit();
    }

    /**
     * Processes the form changes and emits the apply changes event.
     */
    applyChanges(): void {
        const pageVariantConfig: PageVariantConfig = retrievePageVariantConfig(
            this.pageVariantNode,
        );
        const parameters = pageVariantConfig?.variables ?? {};
        const value = this.form.value;
        if (!value) {
            return;
        }
        const outputMap = new Map<string, string>();
        let parametersChanged: boolean = false;

        // process form values
        this.selectDimensions().forEach((widget: MdsWidget, key: string) => {
            if (value[key] !== undefined) {
                let processedValue: string[];

                if (Array.isArray(value[key])) {
                    // handle checkbox arrays - extract selected IDs
                    const checkboxValues = value[key] as Array<{ id: string; checked: boolean }>;
                    const selectedValues = checkboxValues
                        .filter((item) => item.checked)
                        .map((item) => item.id);
                    processedValue = selectedValues;
                } else {
                    // handle single values (radio buttons)
                    processedValue = [value[key] || ''];
                }

                // check if the value has been changed
                if (JSON.stringify(processedValue) !== JSON.stringify(parameters[key] || [])) {
                    pageVariantConfig.variables[key] = processedValue;
                    parametersChanged = true;
                }
            }
        });

        if (parametersChanged) {
            outputMap.set(DEFAULT_PAGE_VARIANT_CONFIG_PROP, JSON.stringify(pageVariantConfig));
        }
        const titleChanged: boolean = value.title && value.title !== this.pageVariantNode.title;
        if (titleChanged) {
            outputMap.set(RestConstants.CM_PROP_TITLE, value.title);
        }
        this.applyChangesClicked.emit(outputMap);
    }

    /**
     * Helper function to check the validity of the form input.
     *
     * @param currentValue
     */
    checkValidity(currentValue: any = null): void {
        if (!currentValue) {
            currentValue = this.form.value;
        }
        let validInput: boolean = true;
        Object.keys(currentValue).forEach((key: string) => {
            const value: Array<{ id: string; checked: boolean }> | string = currentValue[key];
            const noCheckboxValue: boolean =
                Array.isArray(value) && !value.find((item) => item.checked);
            const noRadioOrTitleValue: boolean = !value;
            if (noCheckboxValue || noRadioOrTitleValue) {
                validInput = false;
            }
        });
        this.settingsValidityChanged.emit(validInput);
        this.formInputValid.set(validInput);
    }
}
