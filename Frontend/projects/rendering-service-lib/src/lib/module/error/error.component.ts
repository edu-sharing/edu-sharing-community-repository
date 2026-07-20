import { AfterContentInit, Component, computed, Input, signal } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData } from '../../dto/RenderData';
import { EduSharingUiCommonModule, EduSharingUiModule } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'rs-module-error',
    imports: [
        RenderingModule,
        MatButtonModule,
        MatIconModule,
        EduSharingUiModule,
        EduSharingUiCommonModule,
    ],
    templateUrl: './error.component.html',
    styleUrl: './error.component.scss',
})
export class ErrorComponent implements RenderModule, AfterContentInit {
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    readonly errorMessage = signal('');
    readonly mode = computed<'warning' | 'error'>(() =>
        this.errorMessage() === 'RENDERING.ERROR.GENERIC_ACCESS_DENIED' ? 'warning' : 'error',
    );

    ngAfterContentInit() {
        this.errorMessage.set(this.data?.publicErrorMessage ?? '');
    }
}
