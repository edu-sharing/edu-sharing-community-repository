import { AfterViewInit, Component, ElementRef, Input, signal, ViewChild } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { FormsModule } from '@angular/forms';
import { RenderData, AssetStateItem } from '../../dto/RenderData';
import { TrackingService } from '../../../tracking.service';

@Component({
    selector: 'rs-module-audio',
    imports: [RenderingModule, FormsModule],
    templateUrl: './audio.component.html',
    styleUrl: './audio.component.scss',
})
export class AudioComponent implements RenderModule, AfterViewInit {
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    previewUrl: String = '';
    private hasBeenPlayed: Boolean = false;
    activeObject = signal<AssetStateItem | undefined>(undefined);

    constructor(private trackingService: TrackingService) {}

    ngAfterViewInit(): void {
        this.previewUrl = this.node?.preview?.url ?? '';
        const allFinishedItems = this.data?.items?.filter((item) => item.link !== '');
        this.activeObject.set(allFinishedItems?.[0]);
    }

    onVideoPlay(): void {
        if (!this.hasBeenPlayed) {
            this.trackingService.trackPlayed(this.node?.ref.id!!, this.node?.ref.repo!!);
        }
        this.hasBeenPlayed = true;
    }
}
