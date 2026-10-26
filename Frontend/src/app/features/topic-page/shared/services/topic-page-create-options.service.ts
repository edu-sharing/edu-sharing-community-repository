import { inject, Injectable } from '@angular/core';
import { Constrain, DefaultGroups, ElementType, OptionItem } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { MainNavService } from '../../../../main/navigation/main-nav.service';

/**
 * Options the topic page adds to the "new" button of the main nav, identified by their i18n name.
 */
@Injectable()
export class TopicPageCreateOptionsService {
    private mainNavService = inject(MainNavService);

    /**
     * Adds an option to the "new" button, unless one of the same name is already offered.
     */
    async add(
        name: string,
        icon: string,
        callback: () => void,
        priority: number,
        constrain: Constrain,
    ): Promise<void> {
        const option = new OptionItem(name, icon, callback);
        option.elementType = [ElementType.NoneOrUnknown];
        option.group = DefaultGroups.Toggles;
        option.priority = priority;
        option.constrains = [constrain];
        const currentConfig = (await firstValueFrom(this.mainNavService.observeMainNavConfig()))
            .customCreateOptions;
        const existingCreateOptions = currentConfig?.addOptions || [];
        if (existingCreateOptions.some((o) => o.name === name)) {
            return;
        }
        this.mainNavService.patchMainNavConfig({
            customCreateOptions: {
                ...currentConfig,
                addOptions: existingCreateOptions.concat([option]),
            },
        });
    }

    /**
     * Removes the options of the given names from the "new" button.
     */
    async remove(...names: string[]): Promise<void> {
        const currentConfig = (await firstValueFrom(this.mainNavService.observeMainNavConfig()))
            .customCreateOptions;
        const existingCreateOptions = currentConfig?.addOptions || [];
        if (!existingCreateOptions.some((o) => names.includes(o.name))) {
            return;
        }
        this.mainNavService.patchMainNavConfig({
            customCreateOptions: {
                ...currentConfig,
                addOptions: existingCreateOptions.filter((o) => !names.includes(o.name)),
            },
        });
    }
}
