import { AfterContentInit, Component, Input } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData } from '../../dto/RenderData';
import { EduSharingUiCommonModule, EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { MatCard, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';

@Component({
    selector: 'rs-module-error',
    imports: [
        RenderingModule,
        MatButtonModule,
        MatIconModule,
        EduSharingUiModule,
        MatCard,
        EduSharingUiCommonModule,
        MatCardSubtitle,
        MatCardHeader,
        MatCardTitle,
    ],
    templateUrl: './error.component.html',
    styleUrl: './error.component.scss',
})
export class ErrorComponent implements RenderModule, AfterContentInit {
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    errorMessage: string = '';

    ngAfterContentInit() {
        this.errorMessage = this.data?.publicErrorMessage ?? '';
    }
}
