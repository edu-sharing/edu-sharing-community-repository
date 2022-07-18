import { Injectable, TemplateRef } from '@angular/core';
import {ListItem, ListItemType} from "../../core-module/ui/list-item";

export type CustomFieldInfo = {
    type: ListItemType,
        name: string,
        templateRef: TemplateRef<any>
};
export enum PaginationStrategy {
    InfiniteScroll,
    Paginator
}

/**
 * this service is intented to add custom behaviour to the global tables & grid views
 */
@Injectable()
export class NodeEntriesGlobalService {
    private customFields: CustomFieldInfo[] = [];
    public paginationStrategy = PaginationStrategy.InfiniteScroll;
    public paginatorSizeOptions = [25, 50, 75, 100];

    constructor() {}

    public getCustomFieldTemplate(item: ListItem) {
        return this.customFields.filter(c => c.type === item.type && c.name === item.name)?.[0]?.templateRef;
    }

    /**
     * register a custom (node) attribute you want to render via the given template
     * You may also override existing attributes if you want to provide a custom view
     */
    registerCustomFieldRendering(customFieldInfo: CustomFieldInfo) {
        this.customFields.push(customFieldInfo);
    }
}
