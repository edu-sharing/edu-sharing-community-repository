import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { MdsModule } from '../../../mds/mds.module';
import { NodeRelationsDialogComponent } from './node-relations-dialog.component';
import { NodeSearchSelectorComponent } from './node-search-selector/node-search-selector.component';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';

export { NodeRelationsDialogComponent };

@NgModule({
    declarations: [NodeRelationsDialogComponent, NodeSearchSelectorComponent],
    imports: [SharedModule, EduSharingUiModule, MdsModule],
})
export class NodeRelationsDialogModule {}
