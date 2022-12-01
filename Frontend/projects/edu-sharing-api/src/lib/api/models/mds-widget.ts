/* tslint:disable */
/* eslint-disable */
import { MdsSubwidget } from './mds-subwidget';
import { MdsValue } from './mds-value';
import { MdsWidgetCondition } from './mds-widget-condition';
export interface MdsWidget {
    allowValuespaceSuggestions?: boolean;
    allowempty?: boolean;
    bottomCaption?: string;
    caption?: string;
    condition?: MdsWidgetCondition;
    configuration?: string;
    defaultMax?: number;
    defaultMin?: number;
    defaultvalue?: string;
    filterMode?: 'disabled' | 'auto' | 'always';
    format?: string;
    hasValues?: boolean;
    hideIfEmpty?: boolean;
    icon?: string;
    id?: string;
    ids?: {
        [key: string]: string;
    };
    interactionType?: 'Input' | 'None';
    isExtended?: boolean;
    isRequired?: 'mandatory' | 'mandatoryForPublish' | 'recommended' | 'optional' | 'ignore';
    isSearchable?: boolean;
    link?: string;
    max?: number;
    maxlength?: number;
    min?: number;
    placeholder?: string;
    required?: 'mandatory' | 'mandatoryForPublish' | 'recommended' | 'optional' | 'ignore';
    step?: number;
    subwidgets?: Array<MdsSubwidget>;
    template?: string;
    type?: string;
    unit?: string;
    values?: Array<MdsValue>;
}
