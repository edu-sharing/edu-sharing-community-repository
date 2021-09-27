import {Component, Input, OnInit} from '@angular/core';
import {MdsEditorWidgetBase, ValueType} from '../../mds-editor/widgets/mds-editor-widget-base';
import {Widget} from '../../mds-editor/mds-editor-instance.service';

@Component({
    selector: 'mds-widget',
    templateUrl: 'mds-widget.component.html',
    styleUrls: ['mds-widget.component.scss'],
})
export class MdsWidgetComponent extends MdsEditorWidgetBase implements OnInit{
    readonly valueType = ValueType.String;
    value:string[];
    @Input() widget: Widget;
    @Input() set data(data:string[]){
        this.value=this.getValue(data);
    }

    getBasicType() {
        switch(this.widget.definition.type) {
            case 'text':
            case 'number':
            case 'email':
            case 'month':
            case 'color':
            case 'textarea':
            case 'singleoption':
                return 'text';
            case 'multivalueFixedBadges':
            case 'multivalueSuggestBadges':
            case 'multivalueBadges':
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
    ngOnInit(): void {
        if(this.widget.getInitialValues()?.jointValues) {
            console.log(this.widget.getInitialValues());
            this.value = this.getValue(this.widget.getInitialValues().jointValues);
        }
    }
    getValue(data: string[]) {
        let value = data;
        if(!value || !value[0]) {
            return null;
        }
        if(this.widget.definition.values) {
            const mapping=this.widget.definition.values.filter((v:any) => data.filter((d) => d === v.id).length > 0).map((v) => v.caption);
            if(mapping){
                return mapping;
            }
        }
        return data;
    }

    click() {
        if(this.widget.definition.link === '_BLANK') {
            window.open(this.value[0]);
        } else {
            console.warn('Unsupported link type ' + this.widget.definition.link);
        }
    }

    isEmpty() {
        return this.value?.every((v) => !v) || this.value?.length === 0 || !this.value;
    }
}
