import { WidgetCondition as WidgetCondition_ } from '../api/models';
import { Narrow } from './utility-types';

export interface WidgetCondition extends WidgetCondition_ {
    type: Narrow<WidgetCondition_['type'], 'PROPERTY' | 'TOOLPERMISSION'>;
}
