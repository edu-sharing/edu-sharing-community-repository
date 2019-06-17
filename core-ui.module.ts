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
import {ReplaceCharsPipe} from "./replace-chars.pipe";
import {SpinnerComponent} from "./components/spinner/spinner.component";
import {SpinnerSmallComponent} from "./components/spinner-small/spinner-small.component";
import {GlobalProgressComponent} from "./components/global-progress/global-progress.component";
import {Toast} from "./toast";
import {ToastyModule} from "ngx-toasty";
import {DropdownComponent} from "./components/dropdown/dropdown.component";
import {MatButtonModule, MatCheckboxModule, MatInputModule, MatMenuModule} from "@angular/material";

@NgModule({
    declarations: [
        CollectionChooserComponent,
        ListTableComponent,
        DropdownComponent,
        SortDropdownComponent,
        IconComponent,
        SpinnerComponent,
        SpinnerSmallComponent,
        GlobalProgressComponent,
        ReplaceCharsPipe,
        InfiniteScrollDirective,
    ],
    imports: [
        BrowserModule,
        FormsModule,
        MatButtonModule,
        MatMenuModule,
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
        SpinnerSmallComponent,
        GlobalProgressComponent,
        IconComponent,
        CollectionChooserComponent,
        ReplaceCharsPipe,
        DropdownComponent,
        SortDropdownComponent,
        InfiniteScrollDirective,
        ToastyModule,
    ]
})
export class CoreUiModule { }

