import { NgModule } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { XmlAppPropertiesDialogComponent } from './xml-app-properties-dialog.component';

export { XmlAppPropertiesDialogComponent };

@NgModule({
    declarations: [XmlAppPropertiesDialogComponent],
    imports: [SharedModule],
})
export class XmlAppPropertiesDialogModule {}
