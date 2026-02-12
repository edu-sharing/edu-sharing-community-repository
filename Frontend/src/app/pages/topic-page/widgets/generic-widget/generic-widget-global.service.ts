import { Injectable, TemplateRef, Type } from '@angular/core';
import { WidgetComponentInterface } from './generic-widget.component';
import { DEFAULT } from 'ngx-edu-sharing-api';
import { GenericNodeEntriesDisplayType } from '../../shared/types/generic-node-entries-display-type';
import {
    CustomCardRole,
    DisplayTypeComponentInterface,
} from '../content-teaser/generic-node-entries/generic-node-entries.component';

export type CustomDisplayType = {
    displayType: GenericNodeEntriesDisplayType;
    component: () => Promise<Type<DisplayTypeComponentInterface>>;
};
export type CustomWidgetInfo = {
    id: string;
    matchingWidgetType?: string;
    component: () => Promise<Type<WidgetComponentInterface>>;
};
/**
 * this service is intended to add custom behaviour to the global tables & grid views
 */
@Injectable({
    providedIn: 'root',
})
export class GenericWidgetGlobalService {
    private defaultMds: string = DEFAULT;
    private customWidgets: CustomWidgetInfo[] = [];
    private customDisplayType: CustomDisplayType[] = [];
    private customCards: { role: CustomCardRole; templateRef: TemplateRef<unknown> }[] = [];

    async getCustomWidget(widgetId: string) {
        const info = this.customWidgets.find((w) => w.id === widgetId);
        if (info != null) {
            return info.component();
        }
        return null;
    }
    getCustomWidgetMatchingWidgetType(widgetId: string) {
        return this.customWidgets.find((w) => w.id === widgetId)?.matchingWidgetType;
    }
    async getCustomDisplayType(displayType: GenericNodeEntriesDisplayType) {
        const info = this.customDisplayType.find((w) => w.displayType === displayType);
        if (info != null) {
            return info.component();
        }
        return null;
    }

    /**
     * Registers a custom widget component
     */
    registerCustomWidget(customWidgetInfo: CustomWidgetInfo) {
        this.customWidgets.push(customWidgetInfo);
    }
    /**
     * Registers a custom widget display type (for generic node entries)
     */
    registerCustomDisplayType(customDisplayType: CustomDisplayType) {
        this.customDisplayType.push(customDisplayType);
    }
    getCustomCards(role: CustomCardRole) {
        return this.customCards.filter((c) => c.role === role);
    }
    registerCustomCard(role: CustomCardRole, templateRef: TemplateRef<unknown>) {
        this.customCards.push({ role, templateRef });
    }

    setDefaultMds(mds: string) {
        this.defaultMds = mds;
    }

    getDefaultMds() {
        return this.defaultMds;
    }

    hasCustomWidget(widgetId: string) {
        return !!this.customWidgets.find((w) => w.id === widgetId);
    }

    hasCustomDisplayType(displayType?: GenericNodeEntriesDisplayType) {
        return !!this.customDisplayType.find((w) => w.displayType === displayType);
    }
}
