import { Injectable, TemplateRef, Type } from '@angular/core';
import { NavigationExtras } from '@angular/router';
import { Node } from 'ngx-edu-sharing-api';
import { Observable, Subject } from 'rxjs';
import { BreadcrumbExtensionInterface } from '../../widgets/breadcrumb/breadcrumb.component';

export type CustomBreadcrumbExtension = {
    id: string;
    component: () => Promise<Type<BreadcrumbExtensionInterface>>;
};
export type CustomSideMenuItem = {
    id: string;
    heading: string;
    icon: string;
    position: 'before' | 'after';
    templateRef: TemplateRef<unknown>;
};

/**
 * This service is intended to add custom behavior to components of the topic page.
 */
@Injectable({
    providedIn: 'root',
})
export class TopicPageGlobalService {
    private customBreadcrumbExtension: CustomBreadcrumbExtension = null;
    private customBreadcrumbRootLink: string = '';
    private customReurlComponent: string = '';
    private customReurlExtras: NavigationExtras;
    private customApplyFilterComponent: string = '';
    private customApplyFilterExtras: NavigationExtras;
    private customSideMenuItems: CustomSideMenuItem[] = [];
    private customUrlFunction: (node: Node) => string;
    private visibleNodesMap: Map<string, Node[]> = new Map<string, Node[]>();
    private visibleNodesUpdated = new Subject<void>();

    /**
     * Registers a custom breadcrumb extension.
     *
     * @param customBreadcrumbExtension
     */
    registerCustomBreadcrumbExtension(customBreadcrumbExtension: CustomBreadcrumbExtension) {
        this.customBreadcrumbExtension = customBreadcrumbExtension;
    }

    /**
     * Sets a custom root link for the breadcrumb component.
     *
     * @param link
     */
    setCustomBreadcrumbRootLink(link: string) {
        this.customBreadcrumbRootLink = link;
    }

    /**
     * Sets a custom component for the reurl link to be used.
     *
     * @param component
     */
    setCustomReurlComponent(component: string) {
        this.customReurlComponent = component;
    }

    /**
     * Sets custom navigation extras for the reurl link to be used.
     *
     * @param extras
     */
    setCustomReurlExtras(extras: NavigationExtras) {
        this.customReurlExtras = extras;
    }

    /**
     * Sets a custom component for the apply filter link to be used.
     *
     * @param component
     */
    setCustomApplyFilterComponent(component: string) {
        this.customApplyFilterComponent = component;
    }

    /**
     * Sets custom navigation extras for the applyFilter link to be used.
     *
     * @param extras
     */
    setCustomApplyFilterExtras(extras: NavigationExtras) {
        this.customApplyFilterExtras = extras;
    }

    /**
     * Registers a custom breadcrumb extension.
     *
     * @param id
     * @param heading
     * @param templateRef
     * @param icon
     * @param position
     */
    registerCustomSideMenuItem(
        id: string,
        heading: string = '',
        templateRef: TemplateRef<unknown>,
        icon: string = null,
        position: 'before' | 'after' = 'after',
    ) {
        this.customSideMenuItems.push({ id, heading: heading || id, icon, position, templateRef });
    }

    /**
     * Sets a custom URL function to be used for links.
     *
     * @param urlFunction
     */
    setCustomUrlFunction(urlFunction: (node: Node) => string): void {
        this.customUrlFunction = urlFunction;
    }

    /**
     * Retrieves the custom reurl component, if available.
     */
    getCustomReurlComponent(): string {
        return this.customReurlComponent;
    }

    /**
     * Retrieves the custom navigation extras for the reurl link, if available.
     */
    getCustomReurlExtras() {
        return this.customReurlExtras;
    }

    /**
     * Retrieves the custom apply filter component, if available.
     */
    getCustomApplyFilterComponent(): string {
        return this.customApplyFilterComponent;
    }

    /**
     * Retrieves the custom navigation extras for the applyFilter link, if available.
     */
    getCustomApplyFilterExtras() {
        return this.customApplyFilterExtras;
    }

    /**
     * Retrieves the custom URL function, if available.
     */
    getCustomUrlFunction(): ((node: Node) => string) | null {
        return this.customUrlFunction;
    }

    /**
     * Retrieves the custom breadcrumb extension, if available.
     */
    async getCustomBreadcrumbExtension() {
        if (this.customBreadcrumbExtension != null) {
            return this.customBreadcrumbExtension.component();
        }
        return null;
    }

    /**
     * Retrieves the list of registered custom side menu items.
     */
    getCustomSideMenuItems(): CustomSideMenuItem[] {
        return this.customSideMenuItems;
    }

    /**
     * Retrieves the custom root link for the breadcrumb component.
     */
    getCustomBreadcrumbRootLink() {
        return this.customBreadcrumbRootLink;
    }

    /**
     * Checks if a custom breadcrumb extension is registered.
     */
    hasCustomBreadcrumbExtension() {
        return !!this.customBreadcrumbExtension;
    }

    /**
     * Updates the visible nodes for a given swimlane and grid index.
     *
     * @param swimlaneIndex
     * @param gridIndex
     * @param nodes
     */
    updateVisibleNodes(swimlaneIndex: number, gridIndex: number, nodes: Node[]): void {
        const key = this.getKey(swimlaneIndex, gridIndex);
        this.visibleNodesMap.set(key, nodes);
        this.visibleNodesUpdated.next();
    }

    /**
     * Retrieves the visible nodes map.
     */
    getVisibleNodesMap(): Map<string, Node[]> {
        return this.visibleNodesMap;
    }

    /**
     * Retrieves an observable that emits whenever the visible nodes map has been updated.
     */
    visibleNodesUpdated$(): Observable<void> {
        return this.visibleNodesUpdated.asObservable();
    }

    /**
     * Deletes all visible nodes of a given swimlane index.
     *
     * @param swimlaneIndex
     */
    deleteVisibleNodesBySwimlane(swimlaneIndex: number): void {
        Array.from(this.visibleNodesMap.keys())
            .filter((key) => {
                const [swimlane] = key.split('-').map(Number);
                return swimlane === swimlaneIndex;
            })
            .forEach((key) => this.visibleNodesMap.delete(key));
    }

    /**
     * Deletes all visible nodes from the map.
     */
    deleteVisibleNodesMap(): void {
        this.visibleNodesMap.clear();
    }

    /**
     * Retrieves a custom key for a given swimlane and grid index.
     *
     * @param swimlaneIndex
     * @param gridIndex
     */
    getKey(swimlaneIndex: number, gridIndex: number): string {
        return `${swimlaneIndex}-${gridIndex}`;
    }
}
