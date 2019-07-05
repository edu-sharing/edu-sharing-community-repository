import { storiesOf } from '@storybook/angular';

import { action } from '@storybook/addon-actions';
import { withKnobs, text, number, boolean, array, select, radios, color, date, button } from '@storybook/addon-knobs';
import {MatButtonModule, MatCardModule, MatFormFieldModule, MatMenuModule, MatToolbarModule} from "@angular/material";
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {Location} from '@angular/common';
import {FormsModule} from "@angular/forms";
import {HttpClient, HttpHandler} from "@angular/common/http";
import {Toast} from "../src/app/core-ui-module/toast";
import {ToastyConfig, ToastyService} from "ngx-toasty";
import {ConfigurationService} from "../src/app/core-module/rest/services/configuration.service";
import {TemporaryStorageService} from "../src/app/core-module/rest/services/temporary-storage.service";
import {CordovaService} from "../src/app/common/services/cordova.service";
import {RestConnectorService} from "../src/app/core-module/rest/services/rest-connector.service";
import {RestIamService} from "../src/app/core-module/rest/services/rest-iam.service";
import {UIService} from "../src/app/core-module/rest/services/ui.service";
import {RestLocatorService} from "../src/app/core-module/rest/services/rest-locator.service";
import {RestNodeService} from "../src/app/core-module/rest/services/rest-node.service";
import {FrameEventsService} from "../src/app/core-module/rest/services/frame-events.service";
import {CoreUiModule} from "../src/app/core-ui-module/core-ui.module";
import {TranslatePipe, TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {createTranslateLoader, createTranslateLoaderDummy} from "../src/app/core-ui-module/translation";
import {APP_BASE_HREF, LocationStrategy, PathLocationStrategy} from "@angular/common";
import {SpinnerComponent} from "../src/app/core-ui-module/components/spinner/spinner.component";
import {SpinnerSmallComponent} from "../src/app/core-ui-module/components/spinner-small/spinner-small.component";
import {InfobarComponent} from "../src/app/common/ui/infobar/infobar.component";
import {CardComponent} from "../src/app/core-ui-module/components/card/card.component";
import {UserAvatarComponent} from "../src/app/common/ui/user-avatar/user-avatar.component";
import {BreadcrumbsComponent} from "../src/app/core-ui-module/components/breadcrumbs/breadcrumbs.component";
import {PoweredByComponent} from "../src/app/common/ui/powered-by/powered-by.component";
import {DialogButton} from "../src/app/core-module/ui/dialog-button";
import {Router} from "@angular/router";
import {CoreBridgeModule} from "../src/app/core-bridge-module/core.bridge.module";

let allImports=[
    CoreUiModule,
    CoreBridgeModule,
    MatCardModule,
    MatButtonModule,
    MatMenuModule,
    MatToolbarModule,
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    MatFormFieldModule,
    TranslateModule.forRoot({
        loader:{
            provide: TranslateLoader,
            useFactory: createTranslateLoaderDummy,
        }
    }),
];
class RouterStub {
    navigateByUrl(url) {
        return url;
    }
}
let allProviders=[
    {provide: Router, useClass: RouterStub},
    HttpClient,
    HttpHandler,
    Toast,
    ToastyService,
    ToastyConfig,
    ConfigurationService,
    TemporaryStorageService,
    CordovaService,
    Location,
    { provide: APP_BASE_HREF, useValue: '/' },
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    RestConnectorService,
    RestIamService,
    UIService,
    RestLocatorService,
    RestNodeService,
    FrameEventsService

];

storiesOf('Spinners & Progress', module)
    .add('Edu-Sharing', () => ({
    component: SpinnerComponent,
    props: {},
}))
    .add('Material Small', () => ({
        component: SpinnerSmallComponent,
        props: {},
    }));
storiesOf('Elements')
    .addDecorator(withKnobs)
    .add('Infobar', () => ({
        component: InfobarComponent,
        props: {
            title:text('title','Bar Title'),
            message:text('message','Bar Message'),
            isCancelable:boolean('isCancelable',false),
            buttons:select('buttons',{'':null,'OK':DialogButton.getOk(),'CANCEL':DialogButton.getCancel()}),
            onCancel:action('onCancel')
        },
        moduleMetadata: {
            imports: allImports
        }
    }))
    .add('Card', () => ({
        component: CardComponent,
        props: {
            title:text('title', 'Title'),
            subtitle:text('subtitle', 'Subtitle'),
            isCancelable:boolean('isCancelable', true),
            width:select('width',['auto','xsmall','small','normal','large','xlarge','xxlarge']),
            height:select('height',['auto','small','normal','large','xlarge','xxlarge']),
            buttons:select('buttons',{
                '':null,
                'OK':DialogButton.getOk(),
                'CANCEL':DialogButton.getCancel(),
                'OK CANCEL':DialogButton.getOkCancel(),
                'YES NO':DialogButton.getYesNo()
            }),
            onCancel:action('onCancel')
        },
        //template:'<card>Lorem ipsum</card>',
        moduleMetadata: {
            imports: allImports,
            providers: allProviders
        }
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
            mode:select('mode',['white','color'])
        },
        moduleMetadata: {
            imports: allImports,
            providers: allProviders
        }
    }))
    .add('Breadcrumbs', () => ({
    component: BreadcrumbsComponent,
    props: {
        home:text('home','My Files'),
        clickable:boolean('clickable',true),
        searchQuery:text('searchQuery',''),
        breadcrumbsAsNode:select('breadcrumbsAsNode',{
            'Empty':[],
            '1 Node':[{name:'Test 1'}],
            '2 Nodes':[{name:'Test 1'},{name:'Test 2'}],
            '3 Nodes':[{name:'Test 1'},{name:'Test 2'},{name:'Test 3'}]
        }),
        onClick:action('onClick')
    },
    moduleMetadata: {
        imports: allImports,
        providers: allProviders
    }
}))
    .add('User Avatar', () => ({
    component: UserAvatarComponent,
    props: {
        user:select('user', {
            'Color Test 1': {authorityName:'Test 1'},
            'Color Test 2': {authorityName:'Test 2'},
            'Color Test 3': {authorityName:'Test 3'},
            'Color Test 4': {authorityName:'Test 4'},
            'Color Test 5': {authorityName:'Test 5'},
        }),
        size:select('size',['xsmall','small','medium','large'])
    },
    moduleMetadata: {
        imports: allImports,
        providers: allProviders
    }
}));
