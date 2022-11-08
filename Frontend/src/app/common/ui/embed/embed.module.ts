import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MdsModule } from '../../../features/mds/mds.module';
import { SharedModule } from '../../../shared/shared.module';
import { EmbedComponent } from './embed.component';

@NgModule({
    declarations: [EmbedComponent],
    imports: [
        RouterModule.forChild([
            {
                path: '',
                component: EmbedComponent,
            },
        ]),
        SharedModule,
        MdsModule,
    ],
})
export class EmbedModule {}
