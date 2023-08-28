import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { ROUTES } from './router/router.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { MatDatepickerModule } from '@angular/material/datepicker';
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
import { CustomModule } from './custom-module/custom.module';
import { RestLocatorService, CoreModule } from './core-module/core.module';
import { CoreBridgeModule } from './core-bridge-module/core.bridge.module';
import { CoreUiModule } from './core-ui-module/core-ui.module';
import { MonacoEditorModule } from 'ngx-monaco-editor';
import { A11yModule } from '@angular/cdk/a11y';
import { MatTreeModule } from '@angular/material/tree';
import { OverlayModule } from '@angular/cdk/overlay';

export const IMPORTS = [
    A11yModule,
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    LazyLoadImageModule,
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
    MonacoEditorModule.forRoot({ baseUrl: './assets' }),
    RouterModule.forRoot(ROUTES, {
        // scrollPositionRestoration: 'enabled' emulated via ScrollPositionRestorationService.
        relativeLinkResolution: 'legacy',
        // This prevents the browser history from getting messed up when navigation attempts are
        // cancelled by guards.
        canceledNavigationResolution: 'computed',
    }),
];
