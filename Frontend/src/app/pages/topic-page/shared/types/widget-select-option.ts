import { WIDGET_TYPE } from './custom-definitions';

export interface WidgetSelectOption {
    icon?: string;
    value: WIDGET_TYPE;
    viewValue: string;
}
