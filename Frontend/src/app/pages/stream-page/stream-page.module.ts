import { NgModule } from '@angular/core';
import { LazyLoadImageModule } from 'ng-lazyload-image';
import { SharedModule } from '../../shared/shared.module';
import { StreamPageRoutingModule } from './stream-page-routing.module';
import { StreamPageComponent } from './stream-page.component';

@NgModule({
    declarations: [StreamPageComponent],
    imports: [SharedModule, StreamPageRoutingModule, LazyLoadImageModule],
})
export class StreamPageModule {}
