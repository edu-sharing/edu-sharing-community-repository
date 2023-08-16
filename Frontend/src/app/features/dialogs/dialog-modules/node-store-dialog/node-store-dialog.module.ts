import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { SearchNodeStoreDialogComponent } from './node-store-dialog.component';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';

export { SearchNodeStoreDialogComponent as SearchNodeStoreComponent };

@NgModule({
    declarations: [SearchNodeStoreDialogComponent],
    imports: [SharedModule, EduSharingUiModule],
})
export class NodeStoreDialogModule {}
