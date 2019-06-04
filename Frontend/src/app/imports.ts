import {RouterModule} from "@angular/router";
import {ToastyModule} from "ngx-toasty";
import {TranslateModule, TranslateLoader} from "@ngx-translate/core";
import {FormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {createTranslateLoader} from "./common/translation";
import {ROUTES} from "./router/router.component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatFormFieldModule, MatInputModule, MatNativeDateModule} from "@angular/material";
import { LazyLoadImageModule } from 'ng-lazyload-image';
import {CustomModule} from './custom-module/custom.module';
import {RestLocatorService, CoreModule} from "./core-module/core.module";
import {CoreBridgeModule} from "./core-bridge-module/core.bridge.module";


export const IMPORTS=[
  BrowserModule,
  BrowserAnimationsModule,
  FormsModule,
  HttpClientModule,
  LazyLoadImageModule,
  MatDatepickerModule,
  MatNativeDateModule,
  MatFormFieldModule,
  MatInputModule,
  CoreModule,
  CoreBridgeModule,
  CustomModule,
    TranslateModule.forRoot({
    loader:{
      provide: TranslateLoader,
      useFactory: createTranslateLoader,
      deps: [HttpClient,RestLocatorService]
    }
  }),
  ToastyModule.forRoot(),
  RouterModule.forRoot(ROUTES),
];
