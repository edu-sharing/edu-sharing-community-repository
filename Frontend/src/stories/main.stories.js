import { storiesOf } from '@storybook/angular';

import { action } from '@storybook/addon-actions';
import {
    withKnobs,
    text,
    number,
    boolean,
    array,
    select,
    radios,
    color,
    date,
    button,
} from '@storybook/addon-knobs';
import {
    MAT_FORM_FIELD_DEFAULT_OPTIONS,
    MAT_TOOLTIP_DEFAULT_OPTIONS,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatRadioModule,
    MatSelectModule,
    MatSliderModule,
    MatSlideToggleModule,
    MatToolbarModule,
} from '@angular/material';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHandler } from '@angular/common/http';
import { Toast } from '../app/core-ui-module/toast';
import { ToastyConfig, ToastyModule, ToastyService } from 'ngx-toasty';
import { ConfigurationService } from '../app/core-module/rest/services/configuration.service';
import { TemporaryStorageService } from '../app/core-module/rest/services/temporary-storage.service';
import { CordovaService } from '../app/common/services/cordova.service';
import { RestConnectorService } from '../app/core-module/rest/services/rest-connector.service';
import { RestIamService } from '../app/core-module/rest/services/rest-iam.service';
import { UIService } from '../app/core-module/rest/services/ui.service';
import { RestLocatorService } from '../app/core-module/rest/services/rest-locator.service';
import { RestNodeService } from '../app/core-module/rest/services/rest-node.service';
import { FrameEventsService } from '../app/core-module/rest/services/frame-events.service';
import { CoreUiModule } from '../app/core-ui-module/core-ui.module';
import {
    TranslatePipe,
    TranslateModule,
    TranslateLoader,
    TranslateService,
} from '@ngx-translate/core';
import {
    createTranslateLoader,
    createTranslateLoaderDummy,
} from '../app/core-ui-module/translation';
import { APP_BASE_HREF, LocationStrategy, PathLocationStrategy } from '@angular/common';
import { SpinnerComponent } from '../app/core-ui-module/components/spinner/spinner.component';
import { SpinnerSmallComponent } from '../app/core-ui-module/components/spinner-small/spinner-small.component';
import { InfobarComponent } from '../app/common/ui/infobar/infobar.component';
import { UserAvatarComponent } from '../app/core-ui-module/components/user-avatar/user-avatar.component';
import { BreadcrumbsComponent } from '../app/core-ui-module/components/breadcrumbs/breadcrumbs.component';
import { PoweredByComponent } from '../app/common/ui/powered-by/powered-by.component';
import { DialogButton } from '../app/core-module/ui/dialog-button';
import { Router, RouterModule } from '@angular/router';
import { CoreBridgeModule } from '../app/core-bridge-module/core.bridge.module';
import { ButtonsTestComponent } from '../app/common/test/buttons/buttons-test.component';
import { InputsTestComponent } from '../app/common/test/inputs/inputs-test.component';
import { UserAvatarTestComponent } from '../app/common/test/user-avatar/user-avatar-test.component';
import { CoreModule } from '../app/core-module/core.module';
import { ModalTestComponent } from '../app/common/test/modal/modal-test.component';
import { MatProgressSpinner, MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AppModule } from '../app';
import { OptionsHelperService } from '../app/core-ui-module/options-helper.service';
import { NgModule } from '@angular/core';
import { CollectionChooserComponent } from '../app/core-ui-module/components/collection-chooser/collection-chooser.component';
import { ListTableComponent } from '../app/core-ui-module/components/list-table/list-table.component';
import { DropdownComponent } from '../app/core-ui-module/components/dropdown/dropdown.component';
import { SortDropdownComponent } from '../app/core-ui-module/components/sort-dropdown/sort-dropdown.component';
import { IconComponent } from '../app/core-ui-module/components/icon/icon.component';
import { CardComponent } from '../app/core-ui-module/components/card/card.component';
import { UserTileComponent } from '../app/core-ui-module/components/user-tile/user-tile.component';
import { LinkComponent } from '../app/core-ui-module/components/link/link.component';
import { GlobalProgressComponent } from '../app/core-ui-module/components/global-progress/global-progress.component';
import { VideoControlsComponent } from '../app/core-ui-module/components/video-controls/video-controls.component';
import { InfoMessageComponent } from '../app/core-ui-module/components/info-message/info-message.component';
import { InputPasswordComponent } from '../app/core-ui-module/components/input-password/input-password.component';
import { InfiniteScrollDirective } from '../app/core-ui-module/directives/infinite-scroll.directive';
import { AuthorityNamePipe } from '../app/core-ui-module/pipes/authority-name.pipe';
import { AuthorityColorPipe } from '../app/core-ui-module/pipes/authority-color.pipe';
import { NodeDatePipe } from '../app/core-ui-module/pipes/date.pipe';
import { FormatSizePipe } from '../app/core-ui-module/pipes/file-size.pipe';
import { KeysPipe } from '../app/core-ui-module/pipes/keys.pipe';
import { ReplaceCharsPipe } from '../app/core-ui-module/pipes/replace-chars.pipe';
import { PermissionNamePipe } from '../app/core-ui-module/pipes/permission-name.pipe';
import { UrlPipe } from '../app/core-ui-module/pipes/url.pipe';
import { AuthorityAffiliationPipe } from '../app/core-ui-module/pipes/authority-affiliation.pipe';
import { NodesDragSourceDirective } from '../../projects/edu-sharing-ui/src/lib/directives/drag-nodes/nodes-drag-source.directive';
import { NodesDropTargetLegacyDirective } from '../../projects/edu-sharing-ui/src/lib/directives/nodes-drop-target-legacy.directive';
import { SafeHtmlPipe } from '../app/core-ui-module/pipes/safe-html.pipe';
import { ListOptionItemComponent } from '../app/core-ui-module/components/list-option-item/list-option-item.component';
import { A11yModule } from '@angular/cdk/a11y';
import { MatTabsModule } from '@angular/material/tabs';
import { MatRipple, MatRippleModule } from '@angular/material/core';
import { MatProgressBar, MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltip, MatTooltipModule } from '@angular/material/tooltip';
import { StorybookModule } from './storybook.module';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatFormField, MatFormFieldControl, MatHint, MatLabel } from '@angular/material/form-field';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatRadioButton } from '@angular/material/radio';
import { ObserversModule } from '@angular/cdk/observers';
import { OverlayModule } from '@angular/cdk/overlay';

/*
let allProviders=[
    HttpClient,
    HttpHandler,
    Toast,
    ToastyService,
    ToastyConfig,
    ConfigurationService,
    TemporaryStorageService,
    CordovaService,
    Location,
    RestConnectorService,
    RestIamService,
    UIService,
    RestLocatorService,
    RestNodeService,
    FrameEventsService,
    { provide: APP_BASE_HREF, useValue: '/' },
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: {appearance: 'outline'} },
    { provide: MAT_TOOLTIP_DEFAULT_OPTIONS, useValue: {showDelay: 500} },
    [ OptionsHelperService, {provide: OPTIONS_HELPER_CONFIG, useValue: {
            subscribeEvents: false
    }}]
];
*/
let allImports = [
    ObserversModule,
    OverlayModule,
    MatTooltipModule,
    // MatInputModule,
    TranslateModule.forRoot({
        loader: {
            provide: TranslateLoader,
            useFactory: createTranslateLoaderDummy,
        },
    }),
];
let allDeclarations = [
    MatProgressSpinner,
    MatIcon,
    MatLabel,
    MatButton,
    MatRipple,
    MatHint,
    MatSlideToggle,
    MatCheckbox,
    MatRadioButton,
    IconComponent,
    UserAvatarComponent,
    AuthorityNamePipe,
    AuthorityColorPipe,
];
class RouterStub {
    navigateByUrl(url) {
        return url;
    }
}
let allProviders = [{ provide: Router, useClass: RouterStub }];
storiesOf('Elements')
    .addDecorator(withKnobs)
    .add('Buttons', () => ({
        component: ButtonsTestComponent,
        moduleMetadata: {
            declarations: allDeclarations,
        },
    }))
    .add('Inputs', () => ({
        component: InputsTestComponent,
        moduleMetadata: {
            declarations: allDeclarations,
            imports: allImports,
            providers: allProviders,
        },
    }))
    .add('Modals & Cards', () => ({
        component: ModalTestComponent,
        moduleMetadata: {
            declarations: allDeclarations,
        },
    }))
    .add('User Avatars', () => ({
        component: UserAvatarTestComponent,
        moduleMetadata: {
            imports: allImports,
            declarations: allDeclarations,
            providers: allProviders,
        },
    }))
    .add('Infobar', () => ({
        component: InfobarComponent,
        props: {
            title: text('title', 'Bar Title'),
            message: text('message', 'Bar Message'),
            isCancelable: boolean('isCancelable', false),
            buttons: select('buttons', {
                '': null,
                OK: DialogButton.getOk(),
                CANCEL: DialogButton.getCancel(),
            }),
            onCancel: action('onCancel'),
        },
        moduleMetadata: {
            declarations: allDeclarations,
        },
    }));
storiesOf('Spinners & Progress', module)
    .add('Edu-Sharing', () => ({
        component: SpinnerComponent,
        props: {},
    }))
    .add('Material Small', () => ({
        component: SpinnerSmallComponent,
        props: {},
        moduleMetadata: {
            declarations: allDeclarations,
        },
    }));
