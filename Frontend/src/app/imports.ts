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
    MatCheckboxModule, MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatNativeDateModule,
    MatTableModule,
    MatOptionModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    MatSelectModule,
    MatSidenavModule,
    MatSlideToggleModule,
    MatTabsModule,
    MatToolbarModule, MatTooltipModule, MatRippleModule, MatProgressBarModule
} from '@angular/material';
import { LazyLoadImageModule } from 'ng-lazyload-image';
import {CustomModule} from './custom-module/custom.module';
import {RestLocatorService, CoreModule} from "./core-module/core.module";
import {CoreBridgeModule} from "./core-bridge-module/core.bridge.module";
import {CoreUiModule} from "./core-ui-module/core-ui.module";
import {MonacoEditorModule} from "ngx-monaco-editor";


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
  MatChipsModule,
  MatProgressSpinnerModule,
  MatTableModule,
  MatToolbarModule,
  MatRippleModule,
  MatProgressBarModule,
  MatRadioModule,
  MatCardModule,
  MatDatepickerModule,
  MatNativeDateModule,
  MatTooltipModule,
  MatAutocompleteModule,
  MatOptionModule,
  MatFormFieldModule,
  ReactiveFormsModule,
  MatInputModule,
  CoreModule,
  CoreBridgeModule,
  CoreUiModule,
  CustomModule,
  MonacoEditorModule.forRoot(),
  RouterModule.forRoot(ROUTES),
];
