import { NgModule } from '@angular/core';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { SharedModule } from '../../shared/shared.module';
import { TemplateComponent } from './editor/template.component';
import { TopicPageComponent } from './topic-page.component';
import { TopicPageRoutingModule } from './topic-page-routing.module';
import { ResizableSidenavDirective } from '../editorial-page/resizable-sidenav.directive';

@NgModule({
    declarations: [TopicPageComponent],
    imports: [
        SharedModule,
        TemplateComponent,
        TopicPageRoutingModule,
        FooterComponent,
        ResizableSidenavDirective,
    ],
})
export class TopicPageModule {}
