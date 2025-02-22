import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {RenderComponent, RenderingServiceLibModule} from 'ngx-rendering-service-lib';
import {RenderWrapperComponent} from './render-wrapper.component';
import {CommonModule} from '@angular/common';
import {EduSharingUiModule, TranslationsModule} from 'ngx-edu-sharing-ui';
import {MatButtonModule} from '@angular/material/button';

/**
 * new module for (kotlin based) rendering backend
 */
@NgModule({
    declarations: [RenderWrapperComponent],
    imports: [
        CommonModule,
        EduSharingUiModule,
        MatButtonModule,
        RenderComponent,
        TranslationsModule,
        RenderingServiceLibModule,
        // this module is loaded optional cause of deps
        // MdsModule,
    ],
    // required for optional mds module
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    exports: [RenderWrapperComponent],
})
export class RenderWrapperModule {}
