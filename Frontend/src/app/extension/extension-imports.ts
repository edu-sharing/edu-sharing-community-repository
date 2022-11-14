// Overwrite this file in extensions to define additional imports.

import { ModuleWithProviders, Type } from '@angular/core';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSortModule } from '@angular/material/sort';

export const extensionImports: (any[] | Type<any> | ModuleWithProviders<{}>)[] = [
    MatPaginatorModule,
    MatSortModule,
    MatSidenavModule,
];
