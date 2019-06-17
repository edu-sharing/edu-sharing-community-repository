import {RouterModule} from "@angular/router";
import {ToastyModule} from "ngx-toasty";
import {TranslateModule, TranslateLoader} from "@ngx-translate/core";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {createTranslateLoader} from "./core-ui-module/translation";
import {ROUTES} from "./router/router.component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {MatDatepickerModule} from '@angular/material/datepicker';
import {
    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatNativeDateModule,
    MatOptionModule,
    MatRadioModule,
    MatSelectModule,
    MatSidenavModule,
    MatSlideToggleModule,
    MatTabsModule,
    MatToolbarModule
} from '@angular/material';
import { LazyLoadImageModule } from 'ng-lazyload-image';
import {CustomModule} from './custom-module/custom.module';
import {RestLocatorService, CoreModule} from "./core-module/core.module";
import {CoreBridgeModule} from "./core-bridge-module/core.bridge.module";
import {CoreUiModule} from "./core-ui-module/core-ui.module";


export const IMPORTS=[
  BrowserModule,
  FormsModule,
  ReactiveFormsModule,
  HttpClientModule,
  LazyLoadImageModule,
  MatButtonModule,
  MatTabsModule,
  MatSidenavModule,
  MatCheckboxModule,
  MatSlideToggleModule,
  MatMenuModule,
  MatOptionModule,
  MatSelectModule,
  MatAutocompleteModule,
  MatToolbarModule,
  MatRadioModule,
  MatCardModule,
  MatDatepickerModule,
  MatNativeDateModule,
  MatAutocompleteModule,
  MatOptionModule,
  MatFormFieldModule,
  ReactiveFormsModule,
  MatInputModule,
  CoreModule,
  CoreBridgeModule,
  CoreUiModule,
  CustomModule,
  RouterModule.forRoot(ROUTES),
];
