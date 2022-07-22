import { Injectable, TemplateRef } from '@angular/core';
import {ListItem, ListItemType} from "../../core-module/ui/list-item";
import {Scope} from "../../core-ui-module/option-item";

export type CustomFieldInfo = {
    type: ListItemType,
        name: string,
        templateRef: TemplateRef<unknown>
};
export enum PaginationStrategy {
    InfiniteScroll,
    Paginator
}

type PaginationScope = Scope | 'DEFAULT';
/**
 * this service is intented to add custom behaviour to the global tables & grid views
 */
@Injectable()
export class NodeEntriesGlobalService {
    private customFields: CustomFieldInfo[] = [];
    private paginationStrategy: {[key in PaginationScope]?: PaginationStrategy} = {
        [Scope.WorkspaceList]: PaginationStrategy.InfiniteScroll,
        [Scope.Search]: PaginationStrategy.InfiniteScroll,
        DEFAULT: PaginationStrategy.InfiniteScroll,
    };
    private paginatorSizeOptions: {[key in PaginationScope]?: number[]} = {
        DEFAULT: [25, 50, 75, 100],
    };

    constructor() {}

    public setPaginationStrategy(scope: PaginationScope, strategy: PaginationStrategy) {
        this.paginationStrategy[scope] = strategy;
    }
    public getPaginationStrategy(scope: Scope) {
        return this.paginationStrategy[scope] ?? this.paginationStrategy['DEFAULT'];
    }
    public setPaginatorSizeOptions(scope: PaginationScope, size: number[]) {
        this.paginatorSizeOptions[scope] = size;
    }
    public getPaginatorSizeOptions(scope: Scope) {
        return this.paginatorSizeOptions[scope] ?? this.paginatorSizeOptions['DEFAULT'];
    }
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
