import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { CommentsListComponent } from './comments-list/comments-list.component';
import { MdsNodeRelationsWidgetComponent } from './node-relations/node-relations-widget.component';
import { RenderPageRoutingModule } from './render-page-routing.module';
import { RenderPageComponent } from './render-page.component';
import { TouchEventDirective } from './touchevents.directive';

@NgModule({
    declarations: [
        CommentsListComponent,
        MdsNodeRelationsWidgetComponent,
        RenderPageComponent,
        TouchEventDirective,
    ],
    imports: [SharedModule, RenderPageRoutingModule],
})
export class RenderPageModule {}
