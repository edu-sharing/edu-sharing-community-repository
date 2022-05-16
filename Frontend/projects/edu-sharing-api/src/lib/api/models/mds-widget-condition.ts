/* tslint:disable */
/* eslint-disable */
export interface MdsWidgetCondition {
    dynamic: boolean;
    negate: boolean;
    pattern?: string;
    type: 'PROPERTY' | 'TOOLPERMISSION';
    value: string;
}
