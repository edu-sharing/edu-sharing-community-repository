import { storiesOf } from '@storybook/angular';

import { action } from '@storybook/addon-actions';
import { boolean, select, text, withKnobs } from '@storybook/addon-knobs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { createTranslateLoaderDummy } from '../app/core-ui-module/translation';
import { SpinnerComponent } from '../app/core-ui-module/components/spinner/spinner.component';
import { SpinnerSmallComponent } from '../app/core-ui-module/components/spinner-small/spinner-small.component';
import { InfobarComponent } from '../app/common/ui/infobar/infobar.component';
import { UserAvatarComponent } from '../app/core-ui-module/components/user-avatar/user-avatar.component';
import { DialogButton } from '../app/core-module/ui/dialog-button';
import { Router } from '@angular/router';
import { ButtonsTestComponent } from '../app/common/test/buttons/buttons-test.component';
import { InputsTestComponent } from '../app/common/test/inputs/inputs-test.component';
import { UserAvatarTestComponent } from '../app/common/test/user-avatar/user-avatar-test.component';
import { ModalTestComponent } from '../app/common/test/modal/modal-test.component';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { IconComponent } from '../app/core-ui-module/components/icon/icon.component';
import { AuthorityNamePipe } from '../app/core-ui-module/pipes/authority-name.pipe';
import { AuthorityColorPipe } from '../app/core-ui-module/pipes/authority-color.pipe';
import { MatRipple } from '@angular/material/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatHint, MatLabel } from '@angular/material/form-field';
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
