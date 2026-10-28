import { NgTemplateOutlet } from '@angular/common';
import { Component, inject, input, output, viewChild } from '@angular/core';
import { MatSlideToggleChange, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { ActionbarComponent, EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { CollectionNavigationService } from '../../../features/collection-info/collection-navigation.service';
import { SharedModule } from '../../../shared/shared.module';
import { CollectionScopeLabelComponent } from '../../../features/collection-info/collection-scope-label/collection-scope-label.component';
import { CollectionThumbnailComponent } from '../../../features/collection-info/collection-thumbnail/collection-thumbnail.component';

/**
 * Slim bar with the title of a collection, its visibility and the actions that apply to the
 * collection. The actions are not defined here: the host fills the exposed {@link actionbar}.
 */
@Component({
    selector: 'es-collection-header-bar',
    templateUrl: 'collection-header-bar.component.html',
    styleUrls: ['collection-header-bar.component.scss'],
    imports: [
        NgTemplateOutlet,
        TranslateModule,
        EduSharingUiCommonModule,
        SharedModule,
        MatSlideToggleModule,
        CollectionThumbnailComponent,
        CollectionScopeLabelComponent,
    ],
})
export class CollectionHeaderBarComponent {
    private readonly navigation = inject(CollectionNavigationService);

    readonly collection = input.required<Node>();
    /** whether the content of the collection can be arranged by hand, which shows the toggle */
    readonly orderModeAvailable = input(false);
    readonly orderModeActive = input(false);
    readonly orderModeToggle = output<MatSlideToggleChange>();

    protected canShowInfo(): boolean {
        return this.navigation.canShowInfo();
    }

    /** the actionbar that shows the actions of the collection */
    readonly actionbar = viewChild(ActionbarComponent);

    protected showInfo(): void {
        this.navigation.showInfo();
    }
}
