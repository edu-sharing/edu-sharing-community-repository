import { A11yModule } from '@angular/cdk/a11y';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CardComponent } from './components/card/card.component';
import { DropdownComponent } from './components/dropdown/dropdown.component';
import { GlobalProgressComponent } from './components/global-progress/global-progress.component';
import { SpinnerComponent } from './components/spinner/spinner.component';
import { TutorialComponent } from './components/tutorial/tutorial.component';
import { UserAvatarComponent } from './components/user-avatar/user-avatar.component';
import { WorkspaceCreateConnector } from './dialogs/create-connector/create-connector.component';
import { BorderBoxObserverDirective } from './directives/border-box-observer.directive';
import { ElementRefDirective } from './directives/element-ref.directive';
import { FileDropDirective } from './directives/file-drop';
import { IconDirective } from './directives/icon.directive';
import { InfiniteScrollDirective } from './directives/infinite-scroll.directive';
import { SkipTargetDirective } from './directives/skip-target.directive';
import { AuthorityColorPipe } from './pipes/authority-color.pipe';
import { AuthorityNamePipe } from './pipes/authority-name.pipe';
import { BitwisePipe } from './pipes/bitwise.pipe';
import { OptionTooltipPipe } from './pipes/option-tooltip.pipe';
import { ReplaceCharsPipe } from './pipes/replace-chars.pipe';

@NgModule({
    declarations: [
        AuthorityColorPipe,
        AuthorityNamePipe,
        BitwisePipe,
        BorderBoxObserverDirective,
        CardComponent,
        DropdownComponent,
        ElementRefDirective,
        FileDropDirective,
        GlobalProgressComponent,
        IconDirective,
        InfiniteScrollDirective,
        OptionTooltipPipe,
        ReplaceCharsPipe,
        SkipTargetDirective,
        SpinnerComponent,
        TutorialComponent,
        UserAvatarComponent,
        WorkspaceCreateConnector,
    ],
    imports: [
        A11yModule,
        CommonModule,
        FormsModule,
        MatButtonModule,
        MatCardModule,
        MatMenuModule,
        MatOptionModule,
        MatSelectModule,
        MatTooltipModule,
        TranslateModule,
    ],
    exports: [
        A11yModule,
        AuthorityColorPipe,
        AuthorityNamePipe,
        BitwisePipe,
        BorderBoxObserverDirective,
        CardComponent,
        CommonModule,
        DropdownComponent,
        ElementRefDirective,
        FileDropDirective,
        FormsModule,
        GlobalProgressComponent,
        IconDirective,
        InfiniteScrollDirective,
        MatButtonModule,
        MatFormFieldModule,
        MatMenuModule,
        MatOptionModule,
        MatTooltipModule,
        OptionTooltipPipe,
        ReactiveFormsModule,
        ReplaceCharsPipe,
        RouterModule,
        SkipTargetDirective,
        SpinnerComponent,
        TranslateModule,
        TutorialComponent,
        UserAvatarComponent,
        WorkspaceCreateConnector,
    ],
})
export class SharedModule {}
