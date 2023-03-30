import { A11yModule } from '@angular/cdk/a11y';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { OverlayModule } from '@angular/cdk/overlay';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatOptionModule, MatRippleModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTreeModule } from '@angular/material/tree';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ActionbarComponent } from '../../../projects/edu-sharing-ui/src/lib/actionbar/actionbar.component';
import { AuthorityRowComponent } from './components/authority-search-input/authority-row/authority-row.component';
import { AuthoritySearchInputComponent } from './components/authority-search-input/authority-search-input.component';
import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { CalendarComponent } from './components/calendar/calendar.component';
import { CardComponent } from './components/card/card.component';
import { DropdownComponent } from '../../../projects/edu-sharing-ui/src/lib/dropdown/dropdown.component';
import { GlobalProgressComponent } from './components/global-progress/global-progress.component';
import { InfoMessageComponent } from './components/info-message/info-message.component';
import { InputPasswordComponent } from './components/input-password/input-password.component';
import { LicenseSourceComponent } from './components/license-source/license-source.component';
import { LinkComponent } from './components/link/link.component';
import { ModalDialogComponent } from './components/modal-dialog/modal-dialog.component';
import { MultiLineLabelComponent } from './components/multi-line-label/multi-line-label.component';
import { NodeUrlComponent } from '../../../projects/edu-sharing-ui/src/lib/node-url/node-url.component';
import { SpinnerSmallComponent } from './components/spinner-small/spinner-small.component';
import { SpinnerComponent } from '../../../projects/edu-sharing-ui/src/lib/spinner/spinner.component';
import { TutorialComponent } from './components/tutorial/tutorial.component';
import { UserAvatarComponent } from './components/user-avatar/user-avatar.component';
import { WorkspaceCreateConnector } from './dialogs/create-connector/create-connector.component';
import { BorderBoxObserverDirective } from './directives/border-box-observer.directive';
import { ElementRefDirective } from './directives/element-ref.directive';
import { EscapeHtmlPipe } from './directives/escape-html.pipe';
import { FileDropDirective } from './directives/file-drop';
import { IconDirective } from './directives/icon.directive';
import { InfiniteScrollDirective } from './directives/infinite-scroll.directive';
import { NodesDragDirective } from '../../../projects/edu-sharing-ui/src/lib/directives/drag-nodes/nodes-drag.directive';
import { NodesDropTargetDirective } from '../../../projects/edu-sharing-ui/src/lib/directives/drag-nodes/nodes-drop-target.directive';
import { OnAttributeChangeDirective } from './directives/on-attribute-change.directive';
import { RegisterCustomPropertyDirective } from './directives/register-custom-property.directive';
import { SkipTargetDirective } from './directives/skip-target.directive';
import { AuthorityAffiliationPipe } from './pipes/authority-affiliation.pipe';
import { AuthorityColorPipe } from './pipes/authority-color.pipe';
import { AuthorityNamePipe } from './pipes/authority-name.pipe';
import { BitwisePipe } from './pipes/bitwise.pipe';
import { FormatSizePipe } from './pipes/file-size.pipe';
import { FormatDatePipe } from './pipes/format-date.pipe';
import { NodeImageSizePipe } from './pipes/node-image-size.pipe';
import { NodeImagePipe } from './pipes/node-image.pipe';
import { NodePersonNamePipe } from './pipes/node-person-name.pipe';
import { OptionTooltipPipe } from '../../../projects/edu-sharing-ui/src/lib/pipes/option-tooltip.pipe';
import { PermissionNamePipe } from './pipes/permission-name.pipe';
import { ReplaceCharsPipe } from '../../../projects/edu-sharing-ui/src/lib/pipes/replace-chars.pipe';
import { SafeHtmlPipe } from './pipes/safe-html.pipe';
import { VersionLabelPipe } from './pipes/version-label.pipe';
import { CreateLtitoolComponent } from './dialogs/create-ltitool/create-ltitool.component';
import { EduSharingUiModule } from 'edu-sharing-ui';
import { environment } from '../../environments/environment';

@NgModule({
    declarations: [
        ActionbarComponent,
        AuthorityAffiliationPipe,
        AuthorityColorPipe,
        AuthorityNamePipe,
        AuthorityRowComponent,
        AuthoritySearchInputComponent,
        BitwisePipe,
        BorderBoxObserverDirective,
        BreadcrumbsComponent,
        CalendarComponent,
        CardComponent,
        DropdownComponent,
        ElementRefDirective,
        EscapeHtmlPipe,
        FileDropDirective,
        FormatDatePipe,
        FormatSizePipe,
        GlobalProgressComponent,
        IconDirective,
        InfiniteScrollDirective,
        InfoMessageComponent,
        InputPasswordComponent,
        LicenseSourceComponent,
        LinkComponent,
        ModalDialogComponent,
        MultiLineLabelComponent,
        NodeImageSizePipe,
        NodePersonNamePipe,
        NodeUrlComponent,
        OnAttributeChangeDirective,
        OptionTooltipPipe,
        PermissionNamePipe,
        RegisterCustomPropertyDirective,
        ReplaceCharsPipe,
        SafeHtmlPipe,
        SkipTargetDirective,
        SpinnerComponent,
        SpinnerSmallComponent,
        TutorialComponent,
        UserAvatarComponent,
        VersionLabelPipe,
        WorkspaceCreateConnector,
        CreateLtitoolComponent,
        NodeImagePipe,
    ],
    imports: [
        A11yModule,
        CommonModule,
        DragDropModule,
        FormsModule,
        MatAutocompleteModule,
        MatButtonModule,
        MatCardModule,
        MatDatepickerModule,
        MatInputModule,
        MatInputModule,
        MatMenuModule,
        MatOptionModule,
        MatProgressSpinnerModule,
        MatRippleModule,
        MatSelectModule,
        MatTooltipModule,
        ReactiveFormsModule,
        RouterModule,
        TranslateModule,
        EduSharingUiModule.forRoot({ production: environment.production }),
    ],
    exports: [
        A11yModule,
        ActionbarComponent,
        AuthorityAffiliationPipe,
        AuthorityColorPipe,
        AuthorityNamePipe,
        AuthorityRowComponent,
        AuthoritySearchInputComponent,
        BitwisePipe,
        BorderBoxObserverDirective,
        BreadcrumbsComponent,
        CalendarComponent,
        CardComponent,
        CommonModule,
        DragDropModule,
        DropdownComponent,
        ElementRefDirective,
        EscapeHtmlPipe,
        FileDropDirective,
        FormatDatePipe,
        FormatSizePipe,
        FormsModule,
        GlobalProgressComponent,
        IconDirective,
        InfiniteScrollDirective,
        InfoMessageComponent,
        InputPasswordComponent,
        LicenseSourceComponent,
        LinkComponent,
        MatAutocompleteModule,
        MatButtonModule,
        MatCardModule,
        MatCheckboxModule,
        MatPaginatorModule,
        MatProgressBarModule,
        MatChipsModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatMenuModule,
        MatOptionModule,
        MatRadioModule,
        MatRippleModule,
        MatSelectModule,
        MatSidenavModule,
        MatSlideToggleModule,
        MatSortModule,
        MatTableModule,
        MatTabsModule,
        MatTooltipModule,
        MatTreeModule,
        ModalDialogComponent,
        MultiLineLabelComponent,
        NodeImageSizePipe,
        NodePersonNamePipe,
        NodeUrlComponent,
        OnAttributeChangeDirective,
        OptionTooltipPipe,
        OverlayModule,
        PermissionNamePipe,
        ReactiveFormsModule,
        RegisterCustomPropertyDirective,
        ReplaceCharsPipe,
        RouterModule,
        SafeHtmlPipe,
        SkipTargetDirective,
        SpinnerComponent,
        SpinnerSmallComponent,
        TranslateModule,
        TutorialComponent,
        UserAvatarComponent,
        VersionLabelPipe,
        WorkspaceCreateConnector,
        NodesDragDirective,
        NodesDropTargetDirective,
        MatBadgeModule,
        CreateLtitoolComponent,
        NodeImagePipe,
        EduSharingUiModule,
    ],
})
export class SharedModule {}
