import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { EduSharingUiCommonModule, UIConstants } from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';
import { ScopesService } from 'ngx-edu-sharing-api';

import { RouterLink } from '@angular/router';
import { EditorialBreadcrumbService } from './editorial-breadcrumb.service';

@Component({
    selector: 'es-editorial-breadcrumb',
    templateUrl: 'editorial-breadcrumb.component.html',
    styleUrls: ['editorial-breadcrumb.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [EduSharingUiCommonModule, TranslateModule, RouterLink],
})
export class EditorialBreadcrumbComponent {
    editorialBreadcrumbService = inject(EditorialBreadcrumbService);
    /** the scope mode works inside the active scope: its name is shown next to the mode */
    protected readonly activeScope = inject(ScopesService).activeScope;

    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
}
