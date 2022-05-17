import { A11yModule } from '@angular/cdk/a11y';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { OverlayModule } from '@angular/cdk/overlay';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatOptionModule, MatRippleModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTreeModule } from '@angular/material/tree';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MdsDurationPipe } from '../features/mds/mds-editor/shared/mds-duration.pipe';
import { AuthorityRowComponent } from './components/authority-search-input/authority-row/authority-row.component';
import { AuthoritySearchInputComponent } from './components/authority-search-input/authority-search-input.component';
import { CardComponent } from './components/card/card.component';
import { DropdownComponent } from './components/dropdown/dropdown.component';
import { GlobalProgressComponent } from './components/global-progress/global-progress.component';
import { InfoMessageComponent } from './components/info-message/info-message.component';
import { LinkComponent } from './components/link/link.component';
import { ModalDialogComponent } from './components/modal-dialog/modal-dialog.component';
import { MultiLineLabelComponent } from './components/multi-line-label/multi-line-label.component';
import { SpinnerSmallComponent } from './components/spinner-small/spinner-small.component';
import { SpinnerComponent } from './components/spinner/spinner.component';
import { TutorialComponent } from './components/tutorial/tutorial.component';
import { UserAvatarComponent } from './components/user-avatar/user-avatar.component';
import { WorkspaceCreateConnector } from './dialogs/create-connector/create-connector.component';
import { BorderBoxObserverDirective } from './directives/border-box-observer.directive';
import { CheckTextOverflowDirective } from './directives/check-text-overflow.directive';
import { ElementRefDirective } from './directives/element-ref.directive';
import { FileDropDirective } from './directives/file-drop';
import { IconDirective } from './directives/icon.directive';
import { InfiniteScrollDirective } from './directives/infinite-scroll.directive';
import { OnAttributeChangeDirective } from './directives/on-attribute-change.directive';
import { RegisterCustomPropertyDirective } from './directives/register-custom-property.directive';
import { EscapeHtmlPipe } from './directives/escape-html.pipe';
import { SkipTargetDirective } from './directives/skip-target.directive';
import { AuthorityAffiliationPipe } from './pipes/authority-affiliation.pipe';
import { AuthorityColorPipe } from './pipes/authority-color.pipe';
import { AuthorityNamePipe } from './pipes/authority-name.pipe';
import { BitwisePipe } from './pipes/bitwise.pipe';
import { OptionTooltipPipe } from './pipes/option-tooltip.pipe';
import { ReplaceCharsPipe } from './pipes/replace-chars.pipe';
import { SafeHtmlPipe } from './pipes/safe-html.pipe';
import { VCardNamePipe } from './pipes/vcard-name.pipe';

@NgModule({
    declarations: [
        AuthorityAffiliationPipe,
        AuthorityColorPipe,
        AuthorityNamePipe,
        AuthorityRowComponent,
        AuthoritySearchInputComponent,
        BitwisePipe,
        BorderBoxObserverDirective,
        CardComponent,
        CheckTextOverflowDirective,
        DropdownComponent,
        ElementRefDirective,
        FileDropDirective,
        GlobalProgressComponent,
        IconDirective,
        InfiniteScrollDirective,
        InfoMessageComponent,
        LinkComponent,
        MdsDurationPipe,
        ModalDialogComponent,
        MultiLineLabelComponent,
        OnAttributeChangeDirective,
        OptionTooltipPipe,
        RegisterCustomPropertyDirective,
        ReplaceCharsPipe,
        EscapeHtmlPipe,
        SkipTargetDirective,
        SpinnerComponent,
        SpinnerSmallComponent,
        TutorialComponent,
        UserAvatarComponent,
        VCardNamePipe,
        WorkspaceCreateConnector,
        SafeHtmlPipe,
    ],
    imports: [
        A11yModule,
        CommonModule,
        FormsModule,
        MatAutocompleteModule,
        MatButtonModule,
        MatCardModule,
        MatInputModule,
        MatMenuModule,
        MatOptionModule,
        MatProgressSpinnerModule,
        MatSelectModule,
        MatTooltipModule,
        ReactiveFormsModule,
        TranslateModule,
    ],
    exports: [
        A11yModule,
        AuthorityAffiliationPipe,
        AuthorityColorPipe,
        AuthorityNamePipe,
        AuthorityRowComponent,
        AuthoritySearchInputComponent,
        BitwisePipe,
        BorderBoxObserverDirective,
        CardComponent,
        CheckTextOverflowDirective,
        CommonModule,
        DragDropModule,
        DropdownComponent,
        ElementRefDirective,
        FileDropDirective,
        FormsModule,
        GlobalProgressComponent,
        IconDirective,
        InfiniteScrollDirective,
        InfoMessageComponent,
        LinkComponent,
        MatAutocompleteModule,
        MatButtonModule,
        MatCardModule,
        MatCheckboxModule,
        MatChipsModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatMenuModule,
        MatOptionModule,
        MatRadioModule,
        MatRippleModule,
        MatSelectModule,
        MatSlideToggleModule,
        MatTabsModule,
        MatTooltipModule,
        MatTreeModule,
        MdsDurationPipe,
        ModalDialogComponent,
        MultiLineLabelComponent,
        OnAttributeChangeDirective,
        OptionTooltipPipe,
        OverlayModule,
        ReactiveFormsModule,
        RegisterCustomPropertyDirective,
        ReplaceCharsPipe,
        RouterModule,
        EscapeHtmlPipe,
        SkipTargetDirective,
        SpinnerComponent,
        SpinnerSmallComponent,
        TranslateModule,
        TutorialComponent,
        UserAvatarComponent,
        VCardNamePipe,
        WorkspaceCreateConnector,
        SafeHtmlPipe,
    ],
})
export class SharedModule {}
