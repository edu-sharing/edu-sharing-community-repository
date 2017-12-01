import {RouterModule} from "@angular/router";
import {ToastyModule} from "ng2-toasty";
import {TranslateModule, TranslateLoader} from "ng2-translate";
import {InfiniteScrollModule} from "angular2-infinite-scroll";
import {HttpModule, Http} from "@angular/http";
import {FormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {createTranslateLoader} from "./common/translation";
import {ROUTES} from "./router/router.component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {DatePickerModule} from "ng2-datepicker";


export const IMPORTS=[
  BrowserModule,
  BrowserAnimationsModule,
  FormsModule,
  HttpModule,
  InfiniteScrollModule,
  DatePickerModule,
  TranslateModule.forRoot({
    provide: TranslateLoader,
    useFactory: createTranslateLoader,
    deps: [Http]}),
  ToastyModule.forRoot(),
  RouterModule.forRoot(ROUTES),
];
