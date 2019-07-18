import { NgModule } from '@angular/core';
import {CollectionChooserComponent} from "./components/collection-chooser/collection-chooser.component";
import {TranslateLoader, TranslateModule, TranslatePipe, TranslateService} from "@ngx-translate/core";
import {createTranslateLoader} from "./translation";
import {HttpClient} from "@angular/common/http";
import {RestLocatorService} from "../core-module/rest/services/rest-locator.service";
import {InfiniteScrollDirective} from "./infinite-scroll.directive";
import {ListTableComponent} from "./components/list-table/list-table.component";
import {FormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {SortDropdownComponent} from "./components/sort-dropdown/sort-dropdown.component";
import {IconComponent} from "./components/icon/icon.component";
import {ReplaceCharsPipe} from "./pipes/replace-chars.pipe";
import {SpinnerComponent} from "./components/spinner/spinner.component";
import {SpinnerSmallComponent} from "./components/spinner-small/spinner-small.component";
import {GlobalProgressComponent} from "./components/global-progress/global-progress.component";
import {Toast} from "./toast";
import {ToastyModule} from "ngx-toasty";
import {DropdownComponent} from "./components/dropdown/dropdown.component";
import {MatButtonModule, MatCardModule, MatCheckboxModule, MatInputModule, MatMenuModule, MatRadioModule, MatRippleModule, MatTabsModule} from "@angular/material";
import {AuthorityNamePipe} from "./pipes/authority-name.pipe";
import {AuthorityColorPipe} from "./pipes/authority-color.pipe";
import {FormatSizePipe} from "./pipes/file-size.pipe";
import {KeysPipe} from "./pipes/keys.pipe";
import {UrlPipe} from "./pipes/url.pipe";
import {NodeDatePipe} from "./pipes/date.pipe";
import {CardComponent} from "./components/card/card.component";
import {PermissionNamePipe} from "./pipes/permission-name.pipe";
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {BreadcrumbsComponent} from "./components/breadcrumbs/breadcrumbs.component";

@NgModule({
    declarations: [
        CollectionChooserComponent,
        ListTableComponent,
        DropdownComponent,
        SortDropdownComponent,
        IconComponent,
        CardComponent,
        SpinnerComponent,
        BreadcrumbsComponent,
        SpinnerSmallComponent,
        GlobalProgressComponent,
        InfiniteScrollDirective,
        AuthorityNamePipe,
        AuthorityColorPipe,
        NodeDatePipe,
        FormatSizePipe,
        KeysPipe,
        ReplaceCharsPipe,
        PermissionNamePipe,
        UrlPipe
    ],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        MatCardModule,
        MatButtonModule,
        MatTabsModule,
        MatRadioModule,
        MatMenuModule,
        MatRippleModule,
        MatInputModule,
        MatCheckboxModule,
        ToastyModule.forRoot(),
        TranslateModule.forRoot({
            loader:{
                provide: TranslateLoader,
                useFactory: createTranslateLoader,
                deps: [HttpClient,RestLocatorService]
            }
        }),
    ],
    providers: [
        Toast
    ],
    entryComponents: [
        SpinnerComponent,
        ListTableComponent,
    ],
    exports: [
        TranslateModule,
        ListTableComponent,
        SpinnerComponent,
        BreadcrumbsComponent,
        SpinnerSmallComponent,
        GlobalProgressComponent,
        IconComponent,
        CardComponent,
        CollectionChooserComponent,
        DropdownComponent,
        SortDropdownComponent,
        InfiniteScrollDirective,
        ToastyModule,
        AuthorityNamePipe,
        AuthorityColorPipe,
        NodeDatePipe,
        FormatSizePipe,
        KeysPipe,
        ReplaceCharsPipe,
        PermissionNamePipe,
        UrlPipe
    ]
})
export class CoreUiModule { }

