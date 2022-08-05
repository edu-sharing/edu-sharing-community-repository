// Overwrite this file in extensions to define additional declarables in modules.

import { Type } from '@angular/core';
import { CustomMainMenuComponent } from './custom-main-menu/custom-main-menu.component';

export const extensionDeclarationsMap: { [module: string]: Type<any>[] } = {
    MainModule: [CustomMainMenuComponent],
};
