import {RouterModule} from "@angular/router";
import {ToastyModule} from "ng2-toasty";
import {TranslateModule, TranslateLoader} from "@ngx-translate/core";
import {HttpModule, Http} from "@angular/http";
import {FormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {createTranslateLoader} from "./common/translation";
import {ROUTES} from "./router/router.component";
import {InfiniteScrollModule} from "ngx-infinite-scroll";
import {NgDatepickerModule} from "ng2-datepicker";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {RestLocatorService} from "./common/rest/services/rest-locator.service";


export const IMPORTS=[
  BrowserModule,
  BrowserAnimationsModule,
  FormsModule,
  HttpModule,
  InfiniteScrollModule,
  NgDatepickerModule,
  TranslateModule.forRoot({
    loader:{
      provide: TranslateLoader,
      useFactory: createTranslateLoader,
      deps: [Http,RestLocatorService]
    }
  }),
  ToastyModule.forRoot(),
  RouterModule.forRoot(ROUTES),
];
