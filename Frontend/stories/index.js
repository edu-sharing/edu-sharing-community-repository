import { storiesOf } from '@storybook/angular';

import { action } from '@storybook/addon-actions';
import { withKnobs, text, number, boolean, array, select, radios, color, date, button } from '@storybook/addon-knobs';

import {Pipe} from "@angular/core"
import {GlobalProgressComponent} from "../src/app/common/ui/global-progress/global-progress.component";
import {SpinnerSmallComponent} from "../src/app/common/ui/spinner-small/spinner-small.component";
import {SpinnerComponent} from "../src/app/common/ui/spinner/spinner.component";
import {IconComponent} from "../src/app/common/ui/icon/icon.component";
import {InfobarComponent} from "../src/app/common/ui/infobar/infobar.component";
import {TranslatePipe, TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {createTranslateLoader, createTranslateLoaderDummy} from "../src/app/common/translation";
import {HttpClient, HttpHandler} from "@angular/common/http";
import {RestLocatorService} from "../src/app/common/rest/services/rest-locator.service";
import {DialogButton} from "../src/app/common/ui/modal-dialog/modal-dialog.component";
import {BreadcrumbsComponent} from "../src/app/common/ui/breadcrumbs/breadcrumbs.component";
import {RestNodeService} from "../src/app/common/rest/services/rest-node.service";
import {RestConnectorService} from "../src/app/common/rest/services/rest-connector.service";
import {Router} from "@angular/router";
import {ConfigurationService} from "../src/app/common/services/configuration.service";
import {Toast} from "../src/app/common/ui/toast";
import {ToastyService,ToastyConfig} from "ngx-toasty";
import {TemporaryStorageService} from "../src/app/common/services/temporary-storage.service";
import {CordovaService} from "../src/app/common/services/cordova.service";
import {Location, LocationStrategy, PathLocationStrategy} from '@angular/common';
import {APP_BASE_HREF} from '@angular/common';
import {FrameEventsService} from "../src/app/common/services/frame-events.service";
import {RestIamService} from "../src/app/common/rest/services/rest-iam.service";
import {UIService} from "../src/app/common/services/ui.service";
import {UserAvatarComponent} from "../src/app/common/ui/user-avatar/user-avatar.component";
import {PoweredByComponent} from "../src/app/common/ui/powered-by/powered-by.component";
import {CardComponent} from "../src/app/common/ui/card/card.component";
import {MatCardModule, MatButtonModule, MatMenuModule, MatToolbarModule, MatFormFieldModule} from "@angular/material";
import {AppExportsModule} from "../src/app/app.exports.module";
import {FormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";

let allImports=[
    AppExportsModule,
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