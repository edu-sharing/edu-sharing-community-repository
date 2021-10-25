import {RouterModule} from "@angular/router";
import {TranslateModule, TranslateLoader} from "@ngx-translate/core";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {BrowserModule} from "@angular/platform-browser";
import {ROUTES} from "./router/router.component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {MatDatepickerModule} from '@angular/material/datepicker';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatNativeDateModule, MatOptionModule, MatRippleModule } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LazyLoadImageModule } from 'ng-lazyload-image';
import {CustomModule} from './custom-module/custom.module';
import {RestLocatorService, CoreModule} from "./core-module/core.module";
import {CoreBridgeModule} from "./core-bridge-module/core.bridge.module";
import {CoreUiModule} from "./core-ui-module/core-ui.module";
import {MonacoEditorModule} from "ngx-monaco-editor";
import {QRCodeModule} from 'angularx-qrcode';
import { A11yModule } from "@angular/cdk/a11y";
import {MatTreeModule} from '@angular/material/tree';
import {OverlayModule} from '@angular/cdk/overlay';


export const IMPORTS=[
  A11yModule,
  BrowserModule,
  FormsModule,
  ReactiveFormsModule,
  HttpClientModule,
  LazyLoadImageModule,
  QRCodeModule,
  OverlayModule,
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
  MatButtonToggleModule,
  ReactiveFormsModule,
  MatInputModule,
  MatIconModule,
  MatTreeModule,
  CoreModule,
  CoreBridgeModule,
  CoreUiModule,
  CustomModule,
  MonacoEditorModule.forRoot({baseUrl: "./assets"}),
  RouterModule.forRoot(ROUTES, {
    // Scroll position restoration used behave sensibly out of the box because browsers tried to set
    // the scroll position before the content was loaded and ended up scrolling to the top of the
    // page each time. While this is still true depending on browser and loading times, browsers
    // sometimes respect the `scrollPositionRestoration` setting now. This might lead to keeping the
    // scroll position on navigation (even forward) with the default `disabled` setting. We could
    // emulate the old behavior by setting this to `top` but `enabled` is actually useful when it
    // works.
    //
    // To consistently get the `enabled` behavior across browsers and independently of loading
    // times, we could fetch the content via a [resolver](https://angular.io/api/router/Resolve) on
    // navigation. In case this doesn't work everywhere, a small modification to the default
    // behavior might be needed as done here:
    // https://github.com/openeduhub/oeh-search-frontend/blob/a74047b57007fc6c9561feab02d4d3664051e5c9/src/app/app.component.ts#L37-L52
    scrollPositionRestoration: 'enabled',
    relativeLinkResolution: 'legacy'
}),
];
