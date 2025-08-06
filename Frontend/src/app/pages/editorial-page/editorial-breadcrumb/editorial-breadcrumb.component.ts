import { Component, input } from '@angular/core';
import { EduSharingUiCommonModule, UIConstants } from 'ngx-edu-sharing-ui';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'es-editorial-breadcrumb',
    templateUrl: 'editorial-breadcrumb.component.html',
    styleUrls: ['editorial-breadcrumb.component.scss'],
    imports: [EduSharingUiCommonModule, CommonModule, TranslateModule, RouterLink],
})
export class EditorialBreadcrumbComponent {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    mode = input.required<string>();
}
