import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssetLinkPipe } from './asset-link.pipe';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { MatButtonModule } from '@angular/material/button';

@NgModule({
    imports: [CommonModule, EduSharingApiModule.forRoot(), MatButtonModule],
    declarations: [AssetLinkPipe],
    exports: [CommonModule, AssetLinkPipe],
})
export class RenderingModule {}
