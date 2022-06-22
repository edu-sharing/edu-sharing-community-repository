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
import { ToastyConfig, ToastyService } from 'ngx-toasty';
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
import { TranslatePipe, TranslateModule, TranslateLoader } from '@ngx-translate/core';
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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

const allImports = [
    CoreModule,
    CoreUiModule,
    MatCardModule,
    MatButtonModule,
    MatMenuModule,
    MatInputModule,
    MatToolbarModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    MatSelectModule,
    MatSliderModule,
    MatSlideToggleModule,
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    MatFormFieldModule,
    TranslateModule.forRoot({
        loader: {
            provide: TranslateLoader,
            useFactory: createTranslateLoaderDummy,
        },
    }),
];
class RouterStub {
    navigateByUrl(url) {
        return url;
    }
}
let allProviders = [
    { provide: Router, useClass: RouterStub },
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
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
    { provide: MAT_TOOLTIP_DEFAULT_OPTIONS, useValue: { showDelay: 500 } },
];

storiesOf('Elements')
    .addDecorator(withKnobs)
    .add('Buttons', () => ({
        component: ButtonsTestComponent,
        moduleMetadata: {
            imports: allImports,
            providers: allProviders,
        },
    }))
    .add('Inputs', () => ({
        component: InputsTestComponent,
        moduleMetadata: {
            imports: allImports,
            providers: allProviders,
        },
    }))
    .add('Modals & Cards', () => ({
        component: ModalTestComponent,
        moduleMetadata: {
            imports: allImports,
            providers: allProviders,
        },
    }))
    .add('User Avatars', () => ({
        component: UserAvatarTestComponent,
        moduleMetadata: {
            imports: allImports,
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
            imports: allImports,
        },
    }))
    /*
    .add('Actionbar', () => ({
        component: ActionbarComponent,
        props: {
            numberOfAlwaysVisibleOptions:number('numberOfAlwaysVisibleOptions', 2),
            numberOfAlwaysVisibleOptionsMobile:number('numberOfAlwaysVisibleOptionsMobile', 1),
            dropdownPosition:select('dropdownPosition', ['left','right']),
            appearance:select('appearance', ['button','round']),
            backgroundType:select('backgroundType',['bright','dark','primary']),
            options:[
                new OptionItem('Option 1','help_outline'),
                new OptionItem('Option 2','help_outline'),
            ]
        },
        moduleMetadata: {
            imports: allImports,
            providers: allProviders
        }
    }))
    */
    .add('Powered By', () => ({
        component: PoweredByComponent,
        props: {
            mode: select('mode', ['white', 'color']),
        },
        moduleMetadata: {
            imports: allImports,
            providers: allProviders,
        },
    }))
    .add('Breadcrumbs', () => ({
        component: BreadcrumbsComponent,
        props: {
            home: text('home', 'My Files'),
            clickable: boolean('clickable', true),
            searchQuery: text('searchQuery', ''),
            breadcrumbsAsNode: select('breadcrumbsAsNode', {
                Empty: [],
                '1 Node': [{ name: 'Test 1' }],
                '2 Nodes': [{ name: 'Test 1' }, { name: 'Test 2' }],
                '3 Nodes': [{ name: 'Test 1' }, { name: 'Test 2' }, { name: 'Test 3' }],
            }),
            onClick: action('onClick'),
        },
        moduleMetadata: {
            imports: allImports,
            providers: allProviders,
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
            imports: allImports,
            providers: allProviders,
        },
    }));
