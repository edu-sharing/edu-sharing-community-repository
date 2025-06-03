import { DatePipe } from '@angular/common';
import {
    Component,
    ElementRef,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MatRipple } from '@angular/material/core';
import { filter, first, map } from 'rxjs/operators';
import { MdsValue, MdsWidget, Node, RestConstants, Suggestion } from 'ngx-edu-sharing-api';
import { UIConstants } from '../../util/ui-constants';
import { DateHelper } from '../../util/DateHelper';
import { UIService } from '../../services/ui.service';
import { ViewInstanceService } from '../view-instance.service';
import { RestHelper } from '../../util/rest-helper';
import { FormatSizePipe } from '../../pipes/file-size.pipe';
import { BehaviorSubject, of } from 'rxjs';
import { MdsViewerService } from '../mds-viewer.service';
import { Values } from '../../services/search-helper.service';
import { NodeHelperService } from '../../services/node-helper.service';

export enum MdsType {
    Io = 'io',
    IoBulk = 'io_bulk',
    Map = 'map',
    MapRef = 'map_ref',
    IoChildObject = 'io_childobject',
    Collection = 'collection',
    ToolDefinition = 'tool_definition',
    ToolInstance = 'tool_instance',
    SavedSearch = 'saved_search',
}

export interface MdsValueList {
    values: Suggestion[];
}
export interface MdsViewerWidget {
    definition: MdsWidget;
    getInitalValuesAsync(): Promise<InitialValues>;
    getInitialDisplayValues(): BehaviorSubject<MdsValueList>;
}
export enum MdsWidgetType {
    Text = 'text',
    Number = 'number',
    Email = 'email',
    Date = 'date',
    Month = 'month',
    Color = 'color',
    Textarea = 'textarea',
    TinyMCE = 'tinyMCE',
    VCard = 'vcard',
    Checkbox = 'checkbox',
    RadioHorizontal = 'radioHorizontal',
    RadioVertical = 'radioVertical',
    CheckboxHorizontal = 'checkboxHorizontal',
    CheckboxVertical = 'checkboxVertical',
    MultiValueBadges = 'multivalueBadges',
    MultiValueFixedBadges = 'multivalueFixedBadges',
    MultiValueSuggestBadges = 'multivalueSuggestBadges',
    MultiValueAuthorityBadges = 'multivalueAuthorityBadges',
    Singleoption = 'singleoption',
    Slider = 'slider',
    Range = 'range',
    Duration = 'duration',
    SingleValueTree = 'singlevalueTree',
    SingleValueSuggestBadges = 'singlevalueSuggestBadges',
    MultiValueTree = 'multivalueTree',
    DefaultValue = 'defaultvalue',
    FacetList = 'facetList',
}

export interface InitialValues {
    /** Values that are initially present in all nodes. */
    readonly jointValues: string[];
    /**
     * Values that are initially present in some but not all nodes.
     *
     * Can be null but will never be set to an empty array.
     */
    readonly individualValues?: string[];
}
export enum ValueType {
    String,
    MultiValue,
    Range,
}

@Component({
    selector: 'es-mds-widget',
    templateUrl: 'mds-widget.component.html',
    styleUrls: ['mds-widget.component.scss'],
    // required for external editor injection
})
export class MdsWidgetComponent implements OnInit, OnChanges {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    private static readonly inlineEditing: MdsWidgetType[] = [
        MdsWidgetType.Text,
        MdsWidgetType.Number,
        MdsWidgetType.Date,
        MdsWidgetType.Email,
        MdsWidgetType.Textarea,
        MdsWidgetType.Singleoption,
        MdsWidgetType.SingleValueTree,
        MdsWidgetType.SingleValueSuggestBadges,
        MdsWidgetType.MultiValueBadges,
        MdsWidgetType.MultiValueFixedBadges,
        MdsWidgetType.MultiValueSuggestBadges,
        MdsWidgetType.MultiValueTree,
    ];

    readonly valueType = ValueType.String;

    // use any instead of `Widget` cause of external type
    @Input() widget: MdsViewerWidget;
    @Input() showCaption = true;
    /**
     * allow inline editing
     */
    @Input() inlineEditing: 'auto' | 'always' = 'auto';
    @Input() definition: MdsWidget;
    // use any instead of MdsEditorViewComponent cause of external type
    @Input() view: any;

    @ViewChild('editWrapper') editWrapper: ElementRef;
    @ViewChild(MatRipple) matRipple: MatRipple;
    basicType: string;
    rawValue: { path: MdsValue[]; id: string }[];

    private mdsEditorInstance: any;
    license$ = new BehaviorSubject<{ name: string; icon: string }>(null);

    get headingLevel() {
        return this.viewInstance.headingLevel;
    }

    value: string[] = undefined;
    private temporaryValue: string[] = undefined;

    constructor(
        // public mdsEditorInstance: MdsEditorInstanceService,
        public translate: TranslateService,
        private ui: UIService,
        private viewInstance: ViewInstanceService,
        private mdsViewerService: MdsViewerService,
        private nodeHelper: NodeHelperService,
    ) {
        // super(toast, null, translate);
    }

    async ngOnChanges(changes: SimpleChanges) {
        this.value = await this.getNodeValue();
    }

    async ngOnInit() {
        this.value = await this.getNodeValue();
        this.widget
            .getInitialDisplayValues()
            .pipe(filter((v: MdsValueList) => !!v))
            .subscribe(async (value: MdsValueList) => {
                this.value = value.values.map((v) => v.displayString);
            });
        this.basicType = this.getBasicType();
        this.rawValue = await this.getRawValue().toPromise();
    }

    getDefinition(): MdsWidget {
        return this.widget?.definition || this.definition;
    }

    getBasicType() {
        switch (this.getDefinition().id) {
            case 'license':
                return 'license';
        }
        switch (this.getDefinition().type) {
            case 'text':
            case 'email':
            case 'month':
            case 'color':
            case 'textarea':
            case 'singleoption':
                return 'text';
            case 'number':
                return 'number';
            case 'date':
                return 'date';
            case 'vcard':
                return 'vcard';
            case 'multivalueFixedBadges':
            case 'multivalueSuggestBadges':
            case 'singlevalueSuggestBadges':
            case 'multivalueBadges':
            case 'singlevalueTree':
            case 'multivalueTree':
                return this.viewInstance.treeDisplay === 'flat' ? 'array' : 'tree';
            case 'slider':
                return 'slider';
            case 'duration':
                return 'duration';
            case 'range':
                return 'range';
        }
        return 'unknown';
    }

    supportsInlineEditing() {
        return MdsWidgetComponent.inlineEditing.includes(
            this.widget?.definition.type as MdsWidgetType,
        );
    }
    private getNodeValues() {
        console.log(this.mdsViewerService);
        if (this.mdsEditorInstance) {
            return (
                (this.mdsEditorInstance.values$.value as Values) ||
                this.mdsEditorInstance.nodes$.value.map((n: Node) => n.properties)
            );
        } else {
            return this.mdsViewerService.values$.value;
        }
    }
    private async getNodeValue() {
        if (this.temporaryValue !== undefined) {
            return this.getValue(this.temporaryValue);
        }
        const id = this.getDefinition().id;
        const values = this.getNodeValues();
        if (this.getBasicType() === 'license') {
            this.license$.next({
                icon: await this.nodeHelper.getLicenseIcon({
                    properties: this.getNodeValues(),
                } as Node),
                name: this.nodeHelper.getLicenseName({ properties: this.getNodeValues() } as Node),
            });
        }
        if (this.getDefinition().type === 'range') {
            if (values) {
                return [values[id + '_from']?.[0], values[id + '_to']?.[0]];
            }
            return null;
        } else if (values?.[id]) {
            // support on the fly changes+updates of the values
            return this.getValue(values[id]);
        } else if ((await this.widget.getInitalValuesAsync())?.jointValues) {
            return (await this.widget.getInitalValuesAsync()).jointValues;
        } else {
            return null;
        }
    }

    getValue(data: string[]) {
        let value = data;
        if (!value || value.every((v) => !v)) {
            return null;
        }

        if (this.getDefinition().values) {
            const mapping = this.widget.definition.values
                .filter((v: any) => data.filter((d) => d === v.id).length > 0)
                .map((v: any) => v.caption);
            if (mapping) {
                return mapping;
            }
        }

        return data;
    }

    click() {
        if (this.getDefinition().link === '_BLANK') {
            window.open(this.formatText()[0]);
        } else if (this.getDefinition().link === '_SELF') {
            window.location.href = this.formatText()[0];
        } else {
            console.warn('Unsupported link type ' + this.getDefinition().link);
        }
    }

    isEmpty() {
        if (this.basicType === 'license') {
            return false;
        }
        return this.value?.every((v) => !v) || this.value?.length === 0 || !this.value;
    }

    formatDate() {
        return this.value.map((v) => {
            if (this.getDefinition().format) {
                try {
                    return new DatePipe('en').transform(v, this.getDefinition().format);
                } catch (e) {
                    console.warn('Could not format date', e, this.getDefinition());
                    return DateHelper.formatDate(this.translate, v, {
                        showAlwaysTime: true,
                    });
                }
            } else {
                return DateHelper.formatDate(this.translate, v, {
                    showAlwaysTime: true,
                });
            }
        });
    }

    formatNumber() {
        return this.value.map((v) => {
            if (this.widget.definition.format === 'bytes') {
                return new FormatSizePipe(this.translate).transform(v);
            }
            return v;
        });
    }

    formatText() {
        return this.value.map((v) => {
            if (this.widget.definition.format) {
                return this.widget.definition.format.replace('${value}', v);
            }
            return v;
        });
    }
    // instance: MdsEditorWidgetBase
    async finishEdit(instance: any, store = false) {
        if (store) {
            await this.mdsEditorInstance.saveWidgetValue(instance.widget);
        }
        this.temporaryValue = instance.widget.getValue();
        this.value = await this.getNodeValue();
        this.editWrapper.nativeElement.children[0].innerHTML = null;
        await this.mdsEditorInstance.fetchDisplayValues(this.widget);
    }

    isEditable() {
        if (!this.mdsEditorInstance) {
            return false;
        }
        if (this.inlineEditing === 'always') {
            return this.supportsInlineEditing();
        }
        const nodes = this.mdsEditorInstance.nodes$.value;
        return (
            this.mdsEditorInstance.editorMode === 'inline' &&
            this.widget.definition.interactionType === 'Input' &&
            nodes?.length === 1 &&
            RestHelper.hasAccessPermission(nodes[0], RestConstants.ACCESS_WRITE) &&
            this.supportsInlineEditing()
        );
    }

    async focus() {
        // this.matRipple.launch({});
        await this.ui.scrollSmoothElementToChild(this.editWrapper.nativeElement);
        this.matRipple.launch({});
        //const result = await this.view.injectEditField(this, this.editWrapper.nativeElement.children[0]);
        //await this.ui.scrollSmoothElementToChild(result.htmlElement);
    }

    /**
     * return the path for a given value in a tree
     */
    private getPath(v: string) {
        if (!this.getDefinition().values) {
            return [
                {
                    id: v,
                    caption: v,
                },
            ];
        }
        const path: MdsValue[] = [];
        let pointer: string = v;
        for (let i = 0; i < 100; i++) {
            const mapped = this.getDefinition().values.find((w) => w.id === pointer);
            if (mapped) {
                path.push(mapped);
                pointer = mapped.parent;
            } else {
                break;
            }
        }
        return path.reverse();
    }

    /**
     * fetch the raw node value
     * Note: Will not work in a bulk state!
     */
    private getRawValue() {
        return (
            this.mdsEditorInstance?.nodes$.pipe(
                first(),
                map((v: Node[]) =>
                    (v?.[0]?.properties[this.widget.definition.id] as string[])?.map((id) => {
                        return {
                            id,
                            path: this.getPath(id),
                        };
                    }),
                ),
            ) || of(null)
        );
    }

    getSearchParams(key: MdsValue) {
        const params: any = {};
        const mds: { [key: string]: string[] } = {};
        mds[this.widget.definition.id] = [key.id];
        params.mds = this.mdsEditorInstance.mdsId;
        params.sidenav = true;
        params.repo = this.mdsEditorInstance.nodes$.value?.[0].ref.repo;
        params.filters = JSON.stringify(mds);
        return params;
    }

    startEdit(event: MouseEvent) {
        event.stopPropagation();
        void this.view.injectEditField(this, this.editWrapper.nativeElement.children[0]);
    }
}
