import { PortalModule } from '@angular/cdk/portal';
import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { CardActionsComponent } from './card-dialog/card-dialog-container/card-actions/card-actions.component';
import { CardDialogContainerComponent } from './card-dialog/card-dialog-container/card-dialog-container.component';
import { CardHeaderComponent } from './card-dialog/card-dialog-container/card-header/card-header.component';
import { CardJumpMarksComponent } from './card-dialog/card-dialog-container/card-jump-marks/card-jump-marks.component';
import { JumpMarksHandlerDirective } from './card-dialog/card-dialog-container/jump-marks-handler.directive';
import {} from './dialog-modules/node-embed-dialog/node-embed-dialog.component';

@NgModule({
    declarations: [
        CardDialogContainerComponent,
        CardActionsComponent,
        CardHeaderComponent,
        CardJumpMarksComponent,
        JumpMarksHandlerDirective,
    ],
    imports: [SharedModule, PortalModule],
})
export class DialogsModule {}
