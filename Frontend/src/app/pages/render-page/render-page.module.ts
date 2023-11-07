import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { MdsNodeRelationsWidgetComponent } from './node-relations/node-relations-widget.component';
import { RenderPageRoutingModule } from './render-page-routing.module';
import { RenderPageComponent } from './render-page.component';

@NgModule({
    declarations: [RenderPageComponent, MdsNodeRelationsWidgetComponent],
    imports: [SharedModule, RenderPageRoutingModule],
})
export class RenderPageModule {}
