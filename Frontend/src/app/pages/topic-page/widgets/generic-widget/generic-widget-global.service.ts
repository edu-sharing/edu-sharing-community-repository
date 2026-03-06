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
 * This service is intended to add custom behavior to components of the generic widget.
 */
@Injectable({
    providedIn: 'root',
})
export class GenericWidgetGlobalService {
    private defaultMds: string = DEFAULT;
    private customWidgets: CustomWidgetInfo[] = [];
    private customDisplayType: CustomDisplayType[] = [];
    private customCards: { role: CustomCardRole; templateRef: TemplateRef<unknown> }[] = [];

    /**
     * Registers a custom widget component.
     */
    registerCustomWidget(customWidgetInfo: CustomWidgetInfo) {
        this.customWidgets.push(customWidgetInfo);
    }
    /**
     * Registers a custom widget display type (for generic node entries).
     */
    registerCustomDisplayType(customDisplayType: CustomDisplayType) {
        this.customDisplayType.push(customDisplayType);
    }

    /**
     * Registers a custom card template with a given role.
     */
    registerCustomCard(role: CustomCardRole, templateRef: TemplateRef<unknown>) {
        this.customCards.push({ role, templateRef });
    }

    /**
     * Sets the default metadata set to a given value.
     *
     * @param mds
     */
    setDefaultMds(mds: string) {
        this.defaultMds = mds;
    }

    /**
     * Retrieves a custom widget component for a given widget ID.
     *
     * @param widgetId
     */
    async getCustomWidgetComponent(widgetId: string) {
        const info = this.customWidgets.find((w) => w.id === widgetId);
        if (info != null) {
            return info.component();
        }
        return null;
    }

    /**
     * Retrieves the matching widget type of a custom widget component for a given widget ID.
     *
     * @param widgetId
     */
    getCustomWidgetMatchingWidgetType(widgetId: string) {
        return this.customWidgets.find((w) => w.id === widgetId)?.matchingWidgetType;
    }

    /**
     * Retrieves a custom display type component for a given display type.
     *
     * @param displayType
     */
    async getCustomDisplayTypeComponent(displayType: GenericNodeEntriesDisplayType) {
        const info = this.customDisplayType.find((w) => w.displayType === displayType);
        if (info != null) {
            return info.component();
        }
        return null;
    }

    /**
     * Retrieves a list of custom cards with a given role.
     *
     * @param role
     */
    getCustomCards(role: CustomCardRole) {
        return this.customCards.filter((c) => c.role === role);
    }

    /**
     * Retrieves the specified default metadata set.
     */
    getDefaultMds() {
        return this.defaultMds;
    }

    /**
     * Checks if a given widget ID is registered as a custom widget.
     *
     * @param widgetId
     */
    hasCustomWidget(widgetId: string) {
        return !!this.customWidgets.find((w) => w.id === widgetId);
    }

    /**
     * Checks if a given display type is registered as a custom display type.
     *
     * @param displayType
     */
    hasCustomDisplayType(displayType?: GenericNodeEntriesDisplayType) {
        return !!this.customDisplayType.find((w) => w.displayType === displayType);
    }
}
