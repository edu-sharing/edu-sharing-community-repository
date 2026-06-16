import { AfterViewInit, Component, ElementRef, Input, signal, ViewChild } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIcon } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { AssetStateItem, RenderData } from '../../dto/RenderData';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { TrackingService } from '../../../tracking.service';

@Component({
    selector: 'rs-module-video',
    imports: [
        RenderingModule,
        MatIcon,
        MatIconButton,
        FormsModule,
        MatMenu,
        MatMenuTrigger,
        MatMenuItem,
        EduSharingUiModule,
    ],
    templateUrl: './video.component.html',
    styleUrl: './video.component.scss',
})
export class VideoComponent implements RenderModule, AfterViewInit {
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    @ViewChild('video') videoRef: ElementRef<HTMLVideoElement> | undefined;
    activeObject = signal<AssetStateItem | undefined>(undefined);
    hasMultipleResolutions: Boolean = true;
    private hasBeenPlayed: Boolean = false;

    constructor(private trackingService: TrackingService) {}

    get filteredItems(): AssetStateItem[] {
        return this.data?.items?.filter((item) => item.status !== 'FAILED') || [];
    }

    ngAfterViewInit(): void {
        this.hasMultipleResolutions = (this.data?.items?.length ?? 1) > 1;
        const downlink: number = (navigator as any).connection?.downlink;
        let optimalResolution = 720;
        if (downlink < 2) {
            optimalResolution = 240;
        }
        if (downlink > 6) {
            optimalResolution = 1080;
        }
        const allFinishedItems = this.data?.items?.filter((item) => item.link !== '');
        this.activeObject.set(
            allFinishedItems?.sort((a, b) => {
                // sort by the image size closest to the viewport
                return Math.abs(a.width - optimalResolution) > Math.abs(b.width - optimalResolution)
                    ? 1
                    : -1;
            })[0],
        );
    }

    switchResolution(item: AssetStateItem) {
        this.activeObject.set(item);
        const currentTime = this.videoRef?.nativeElement.currentTime;
        this.videoRef?.nativeElement.load();
        if (currentTime !== undefined) {
            this.videoRef!.nativeElement.currentTime = currentTime;
        }
    }

    onVideoPlay(): void {
        if (!this.hasBeenPlayed) {
            this.trackingService.trackPlayed(this.node?.ref.id!!, this.node?.ref.repo!!);
        }
        this.hasBeenPlayed = true;
    }
}
