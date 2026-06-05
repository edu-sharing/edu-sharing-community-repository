import { Component, inject } from '@angular/core';
import { EduSharingUiCommonModule, UIConstants } from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';

import { RouterLink } from '@angular/router';
import { EditorialBreadcrumbService } from './editorial-breadcrumb.service';

@Component({
    selector: 'es-editorial-breadcrumb',
    templateUrl: 'editorial-breadcrumb.component.html',
    styleUrls: ['editorial-breadcrumb.component.scss'],
    imports: [EduSharingUiCommonModule, TranslateModule, RouterLink],
})
export class EditorialBreadcrumbComponent {
    editorialBreadcrumbService = inject(EditorialBreadcrumbService);

    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
}
