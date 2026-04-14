import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { TemplateComponent } from './editor/template.component';
import { TopicPageComponent } from './topic-page.component';
import { TopicPageRoutingModule } from './topic-page-routing.module';

@NgModule({
    declarations: [TopicPageComponent],
    imports: [SharedModule, TemplateComponent, TopicPageRoutingModule],
})
export class TopicPageModule {}
