import { Injectable } from '@angular/core';
import { MdsEditorWidgetComponent, NativeWidgetClass } from '../types/types';

export type CustomWidget = {
    type: string;
    component: MdsEditorWidgetComponent;
};
export type CustomNativeWidget = {
    id: string;
    component: NativeWidgetClass;
};
/**
 * this service is intended to add custom widget rendering
 */
@Injectable()
export class MdsEditorGlobalService {
    private customWidgets: CustomWidget[] = [];
    private customNativeWidgets: CustomNativeWidget[] = [];

    getCustomNativeWidgets() {
        return this.customNativeWidgets;
    }

    getCustomWidgetComponent(type: string) {
        return this.customWidgets.find((f) => f.type === type)?.component;
    }
    /**
     * register a custom widget component for a given type
     * The type is the one as configured in the mds widget definition
     */
    registerCustomWidget(customWidget: CustomWidget) {
        this.customWidgets.push(customWidget);
    }
    registerCustomNativeWidget(customWidget: CustomNativeWidget) {
        this.customNativeWidgets.push(customWidget);
    }
}
