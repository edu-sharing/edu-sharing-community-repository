import { Injectable, OnDestroy } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import {
    OptionData,
    OptionItem,
    OptionsHelperComponents,
    OptionsHelperService as OptionsHelperServiceAbstract,
    Target,
} from 'ngx-edu-sharing-ui';

@Injectable()
export class OptionsHelperService extends OptionsHelperServiceAbstract implements OnDestroy {
    /**
     * Filter options by handling callbacks based on the target type.
     *
     * @param options
     * @param target
     * @param data
     * @param objects
     */
    async filterOptions(
        options: OptionItem[],
        target: Target,
        data: OptionData,
        objects: any,
    ): Promise<OptionItem[]> {
        if (target === Target.List) {
            // attach the show callbacks
            this.handleCallbacks(options, target, data);
        } else {
            options = await this.handleCallbackStates(options, target, data, objects);
        }
        return Promise.resolve(options);
    }

    /**
     * Handle callbacks by iterating options and defining showCallback functions for those.
     * reduced version of Frontend/src/app/services/options-helper.service.ts
     *
     * @param options
     * @param objects
     * @param data
     */
    private handleCallbacks(options: OptionItem[], objects: Node[] | any, data: OptionData): void {
        options?.forEach((o: OptionItem): void => {
            o.showCallback = async (object): Promise<boolean> => {
                const list = object
                    ? Array.isArray(object)
                        ? object
                        : [object]
                    : objects && objects.length
                    ? objects
                    : null;
                return await this.isOptionAvailable(o, list);
            };
            o.enabledCallback = async (object): Promise<boolean> => {
                return Promise.resolve(true);
            };
        });
    }

    /**
     * Checks, whether a given option is available.
     *
     * @param option
     * @param objects
     */
    private async isOptionAvailable(option: OptionItem, objects: Node[] | any[]): Promise<boolean> {
        if (option.customShowCallback) {
            if ((await option.customShowCallback(objects)) === false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handle the callback states by filtering options by their availability.
     *
     * @param options
     * @param target
     * @param data
     * @param objects
     */
    private async handleCallbackStates(
        options: OptionItem[],
        target: Target,
        data: OptionData,
        objects: Node[] | any[] = null,
    ): Promise<OptionItem[]> {
        this.handleCallbacks(options, objects, data);
        if (options?.length > 0) {
            const showState: boolean[] = await Promise.all(
                options.map((o: OptionItem) =>
                    o.showCallback(
                        target === Target.List && objects && objects[0] ? objects[0] : null,
                    ),
                ),
            );
            options = options.filter((o: OptionItem, i: number) => showState[i]);
        }
        return options;
    }

    /**
     * Retrieves available options by filtering those.
     *
     * @param target
     * @param objects
     * @param components
     * @param data
     */
    async getAvailableOptions(
        target: Target,
        objects: Node[],
        components: OptionsHelperComponents,
        data: OptionData,
    ): Promise<OptionItem[]> {
        if (target === Target.List) {
            if (objects == null) {
                // fetch ALL options of ALL items inside list
                // the callback handlers will later decide for the individual node
                objects = null;
            }
        } else if (target === Target.ListDropdown) {
            if (data.activeObjects) {
                objects = data.activeObjects;
            } else {
                return null;
            }
        }
        return this.filterOptions(data.customOptions?.addOptions, target, data, objects);
    }

    ngOnDestroy(): void {
        console.info('OptionsHelperService ngOnDestroy');
    }

    pasteNode(
        components: OptionsHelperComponents,
        data: OptionData,
        addVirutalNodes: boolean,
        nodes: Node[],
    ): void {}

    async refreshComponents(components: OptionsHelperComponents, data: OptionData): Promise<void> {
        if (data == null) {
            return;
        }
        if (components?.list) {
            components.list.setOptions({
                [Target.List]: await this.getAvailableOptions(Target.List, [], components, data),
                [Target.ListDropdown]: await this.getAvailableOptions(
                    Target.ListDropdown,
                    [],
                    components,
                    data,
                ),
            });
        }
        return Promise.resolve(undefined);
    }

    wrapOptionCallbacks(data: OptionData): OptionData {
        return data;
    }
}
