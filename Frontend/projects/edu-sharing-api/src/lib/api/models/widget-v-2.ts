/* tslint:disable */
/* eslint-disable */
import { Subwidget } from './subwidget';
import { ValueV2 } from './value-v-2';
import { WidgetCondition } from './widget-condition';
export interface WidgetV2 {
    allowempty?: boolean;
    bottomCaption?: string;
    caption?: string;
    condition?: WidgetCondition;
    defaultMax?: number;
    defaultMin?: number;
    defaultvalue?: string;
    hasValues?: boolean;
    icon?: string;
    id?: string;
    interactionType?: 'Input' | 'None';
    isExtended?: boolean;
    isRequired?: 'mandatory' | 'mandatoryForPublish' | 'recommended' | 'optional' | 'ignore';
    isSearchable?: boolean;
    link?: string;
    max?: number;
    maxlength?: number;
    min?: number;
    placeholder?: string;
    step?: number;
    subwidgets?: Array<Subwidget>;
    template?: string;
    type?: string;
    unit?: string;
    values?: Array<ValueV2>;
}
