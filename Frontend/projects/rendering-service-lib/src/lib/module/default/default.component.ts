import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
    selector: 'rs-module-default',
    imports: [RenderingModule, MatButtonModule, MatIconModule],
    templateUrl: './default.component.html',
    styleUrl: './default.component.scss',
})
export class DefaultComponent implements RenderModule, OnChanges {
    @Input() data: undefined;
    @Input() node: Node | undefined;
    previewUrl: String = '';

    ngOnChanges(changes: SimpleChanges): void {
        if (this.node !== undefined) {
            this.previewUrl = this.node.preview?.url ?? '';
        }
    }
}
