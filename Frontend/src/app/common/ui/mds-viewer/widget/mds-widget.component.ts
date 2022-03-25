import { DatePipe } from '@angular/common';
import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DateHelper } from '../../../../core-ui-module/DateHelper';
import { FormatSizePipe } from '../../../../core-ui-module/pipes/file-size.pipe';
import { MdsEditorInstanceService, Widget } from '../../mds-editor/mds-editor-instance.service';
import { MdsEditorViewComponent } from '../../mds-editor/mds-editor-view/mds-editor-view.component';
import { ViewInstanceService } from '../../mds-editor/mds-editor-view/view-instance.service';
import { MdsWidgetType } from '../../mds-editor/types';
import { MdsEditorWidgetBase, ValueType } from '../../mds-editor/widgets/mds-editor-widget-base';
import {NodeHelperService} from '../../../../core-ui-module/node-helper.service';
import {RestHelper} from '../../../../core-module/rest/rest-helper';
import {RestConstants} from '../../../../core-module/rest/rest-constants';

@Component({
    selector: 'es-mds-widget',
    templateUrl: 'mds-widget.component.html',
    styleUrls: ['mds-widget.component.scss'],
})
export class MdsWidgetComponent extends MdsEditorWidgetBase implements OnInit {
    private static readonly inlineEditing: MdsWidgetType[] = [
        MdsWidgetType.Text,
        MdsWidgetType.Number,
        MdsWidgetType.Email,
        MdsWidgetType.Textarea,
        MdsWidgetType.Singleoption,
        MdsWidgetType.SingleValueTree,
        MdsWidgetType.MultiValueBadges,
        MdsWidgetType.MultiValueFixedBadges,
        MdsWidgetType.MultiValueSuggestBadges,
        MdsWidgetType.MultiValueTree,
    ];

    readonly valueType = ValueType.String;

    @Input() widget: Widget;
    @Input() view: MdsEditorViewComponent;

    @ViewChild('editWrapper') editWrapper: ElementRef;

    get headingLevel() {
        return this.viewInstance.headingLevel;
    }

    private temporaryValue: string[] = undefined;

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private viewInstance: ViewInstanceService,
    ) {
        super(mdsEditorInstance, translate);
    }

    ngOnInit(): void {}

    getBasicType() {
        switch (this.widget.definition.type) {
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
            case 'multivalueBadges':
            case 'singlevalueTree':
            case 'multivalueTree':
                return 'array';
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
            this.widget.definition.type as MdsWidgetType,
        );
    }

    getNodeValue() {
        if (this.temporaryValue !== undefined) {
            return this.getValue(this.temporaryValue);
        }
        const id = this.widget.definition.id;
        if (this.widget.definition.type === 'range') {
            const values = this.mdsEditorInstance.values$.value;
            if (values) {
                return [values[id + '_from']?.[0], values[id + '_to']?.[0]];
            }
            return null;
        } else if (this.mdsEditorInstance.values$.value?.[id]) {
            // support on the fly changes+updates of the values
            return this.getValue(this.mdsEditorInstance.values$.value[id]);
        } else if (this.widget.getInitialValues()?.jointValues) {
            return this.getValue(this.widget.getInitialValues().jointValues);
        } else {
            return null;
        }
    }

    getValue(data: string[]) {
        let value = data;
        if (!value || value.every((v) => !v)) {
            return null;
        }

        if (this.widget.definition.values) {
            const mapping = this.widget.definition.values
                .filter((v: any) => data.filter((d) => d === v.id).length > 0)
                .map((v) => v.caption);
            if (mapping) {
                return mapping;
            }
        }

        return data;
    }

    click() {
        if (this.widget.definition.link === '_BLANK') {
            window.open(this.formatText()[0]);
        } else if (this.widget.definition.link === '_SELF') {
            window.location.href = this.formatText()[0];
        } else {
            console.warn('Unsupported link type ' + this.widget.definition.link);
        }
    }

    isEmpty() {
        return (
            this.getNodeValue()?.every((v) => !v) ||
            this.getNodeValue()?.length === 0 ||
            !this.getNodeValue()
        );
    }

    formatDate() {
        return this.getNodeValue().map((v) => {
            if (this.widget.definition.format) {
                return new DatePipe(null).transform(v, this.widget.definition.format);
            } else {
                return DateHelper.formatDate(this.translate, v, {
                    showAlwaysTime: true,
                });
            }
        });
    }

    formatNumber() {
        return this.getNodeValue().map((v) => {
            if (this.widget.definition.format === 'bytes') {
                return new FormatSizePipe().transform(v);
            }
            return v;
        });
    }

    formatText() {
        return this.getNodeValue().map((v) => {
            if (this.widget.definition.format) {
                return this.widget.definition.format.replace('${value}', v);
            }
            return v;
        });
    }

    async finishEdit(instance: MdsEditorWidgetBase) {
        await this.mdsEditorInstance.saveWidgetValue(instance.widget);
        this.temporaryValue = instance.widget.getValue();
        this.editWrapper.nativeElement.children[0].innerHTML = null;
    }

    isEditable() {
        const nodes = this.mdsEditorInstance.nodes$.value;
        return (
            this.mdsEditorInstance.editorMode === 'inline' &&
            this.widget.definition.interactionType === 'Input' &&
            nodes?.length === 1 && RestHelper.hasAccessPermission(nodes[0], RestConstants.ACCESS_WRITE) &&
            this.supportsInlineEditing()
        );
    }
}
