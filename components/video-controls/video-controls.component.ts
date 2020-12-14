import {trigger} from '@angular/animations';
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Router} from '@angular/router';
import {Options} from 'ng5-slider';
import {of} from 'rxjs';
import {catchError, map, tap} from 'rxjs/operators';
import {BridgeService} from '../../../core-bridge-module/bridge.service';
import {
    NodesRightMode,
    RestCollectionService,
    RestConnectorService,
    RestHelper,
    TemporaryStorageService,
    UIConstants,
} from '../../../core-module/core.module';
import {Node} from '../../../core-module/rest/data-object';
import {RestConstants} from '../../../core-module/rest/rest-constants';
import {RestNodeService} from '../../../core-module/rest/services/rest-node.service';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {Toast} from '../../toast';
import {UIHelper} from '../../ui-helper';
import {DurationPipe} from './duration.pipe';
import {NodeHelper} from "../../node-helper";

interface VideoControlsValues {
    startTime: number;
    endTime: number;
    title: string;
}

@Component({
    selector: 'video-controls',
    templateUrl: 'video-controls.component.html',
    styleUrls: ['video-controls.component.scss'],
    animations: [trigger('fromRight', UIAnimation.fromRight())],
})
export class VideoControlsComponent implements OnInit {
    @Input() node: Node;
    @Input() video: HTMLVideoElement;
    @Input() size: 'small' | 'large' = 'large';
    @Output() updateCurrentNode = new EventEmitter<Node>();

    /** Data chosen by the user and saved with the video */
    values: VideoControlsValues;
    /** Whether the user has all required permissions to use this tool on the given node */
    hasRequiredPermissions: boolean;
    isLoading = false;
    isCollectionChooserVisible = false;
    sliderOptions: Options;
    private playbackStartedTime: Date;
    private previousValues: VideoControlsValues;

    constructor(
        private bridge: BridgeService,
        private collectionService: RestCollectionService,
        private connector: RestConnectorService,
        private nodeService: RestNodeService,
        private router: Router,
        private temporaryStorage: TemporaryStorageService,
        private toast: Toast,
    ) {}

    ngOnInit(): void {
        // This component is injected programmatically without calling ngOnChanges. Therefore, we
        // require that critical inputs are available after init and don't support updates on these
        // values.
        if (!this.node) {
            throw new Error('Missing required input `node`');
        }
        if (!this.video) {
            throw new Error('Missing required input `video`');
        }
        this.hasRequiredPermissions = this.getHasRequiredPermissions(this.node);
        if (this.video.duration) {
            this.onVideoLoaded();
        } else {
            this.video.onloadedmetadata = () => {
                this.onVideoLoaded();
            };
        }
    }

    async save(): Promise<void> {
        if (this.isCollectionRef(this.node) && this.isOwner(this.node)) {
            const node = await this.writeVideoControlsValues(this.node, this.values);
            if (node) {
                this.updateCurrentNode.emit(node);
            }
        } else {
            // Not an individual object, choose new location first.
            this.isCollectionChooserVisible = true;
        }
    }

    addToCollection(collection: Node) {
        this.isCollectionChooserVisible = false;
        this.isLoading = true;
        UIHelper.addToCollection(
            this.collectionService,
            this.router,
            this.bridge,
            collection,
            [this.node],
            (elements: Node[]) => {
                if (elements.length) {
                    const node = elements[0];
                    this.writeVideoControlsValues(node, this.values);
                } else {
                    this.isLoading = false;
                }
            },
        );
    }

    createCollectionAndAdd(root?: Node) {
        this.temporaryStorage.set(TemporaryStorageService.COLLECTION_ADD_NODES, [this.node]);
        this.router.navigate([
            UIConstants.ROUTER_PREFIX,
            'collections',
            'collection',
            'new',
            root ? root.ref.id : RestConstants.ROOT,
        ]);
    }

    onValueChange() {
        if (this.values.startTime !== this.previousValues?.startTime) {
            this.video.currentTime = this.values.startTime;
        } else if (this.values.endTime !== this.previousValues?.endTime) {
            this.video.currentTime = this.values.endTime;
        }
        this.previousValues = { ...this.values };
    }

    private onVideoLoaded(): void {
        this.sliderOptions = this.getSliderOptions(this.video);
        this.values = this.readVideoControlsValues(this.node, this.video);
        this.registerVideoHooks(this.video);
        this.video.currentTime = this.values.startTime;
    }

    private getHasRequiredPermissions(node: Node): boolean {
        if (this.connector.getCurrentLogin().isGuest) {
            return false;
        } else if (
            !this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_VIDEO_AUDIO_CUT)
        ) {
            return false;
        } else {
            if (this.isCollectionRef(node)) {
                if(this.isOwner(node)) {
                    return NodeHelper.getNodesRight([node], RestConstants.ACCESS_WRITE);
                } else{
                    return NodeHelper.getNodesRight([node], RestConstants.ACCESS_CC_PUBLISH, NodesRightMode.Original);
                }
            } else {
                return NodeHelper.getNodesRight([node], RestConstants.ACCESS_CC_PUBLISH);
            }
        }
    }

    /**
     * Reads video-control values from `node`.
     *
     * Uses `video` to set meaningful fallback values in case node doesn't define video-control
     * values. `video` has to have loaded metadata.
     */
    private readVideoControlsValues(node: Node, video: HTMLVideoElement): VideoControlsValues {
        const values: VideoControlsValues = {
            title: node.properties[RestConstants.LOM_PROP_TITLE]?.[0] ?? '',
            startTime: 0,
            endTime: video.duration,
        };
        const vttCuesJson: string[] = node.properties[RestConstants.CCM_PROP_IO_REF_VIDEO_VTT];
        if (vttCuesJson && vttCuesJson.length === 1) {
            const vttCues = JSON.parse(vttCuesJson[0]);
            const vttCue = vttCues[vttCues.length - 1];
            if (vttCue) {
                values.startTime = vttCue.startTime;
                values.endTime = vttCue.endTime;
            }
        }
        return values;
    }
    convertStringToTime(str: string){
        const splitted = str.split(':');
        if(splitted.length === 2){
            return parseInt(splitted[0], 10)*60 +
                parseInt(splitted[1], 10);
        } else if(splitted.length === 3){
            return parseInt(splitted[0], 10)*3600 +
                parseInt(splitted[1], 10)*60 +
                parseInt(splitted[2], 10);
        }
        return 0;
    }

    private writeVideoControlsValues(
        node: Node,
        values: VideoControlsValues,
    ): Promise<Node | null> {
        this.isLoading = true;
        const props = {
            [RestConstants.CCM_PROP_IO_REF_VIDEO_VTT]: [
                JSON.stringify([
                    {
                        startTime: values.startTime,
                        endTime: values.endTime,
                        text: '',
                    },
                ]),
            ],
            [RestConstants.LOM_PROP_TITLE]: [values.title],
        };
        return this.nodeService
            .editNodeMetadata(node.ref.id, props)
            .pipe(
                tap({
                    next: () => {
                        this.toast.toast('VIDEO_CONTROLS.SAVED');
                    },
                    error: (error) => {
                        this.toast.error(error);
                    },
                    complete: () => {
                        this.isLoading = false;
                    },
                }),
                map((nodeWrapper) => nodeWrapper.node),
                // Don't propagate errors any further.
                catchError((error) => of(null)),
            )
            .toPromise();
    }

    private getSliderOptions(video: HTMLVideoElement): Options {
        const durationPipe = new DurationPipe();
        return {
            floor: 0,
            ceil: video.duration,
            animate: true,
            draggableRange: true,
            minRange: 1,
            translate: (value: number): string => durationPipe.transform(value, video.duration),
        };
    }

    private registerVideoHooks(video: HTMLVideoElement): void {
        const videoControlsComponent = this;
        video.addEventListener('timeupdate', function pauseOnEndTime() {
            // Pause each time endTime is reached, but allow to resume playback.
            if (
                this.currentTime >= videoControlsComponent.values.endTime &&
                // Pause only up to one second after exceeding endTime.
                Math.abs(this.currentTime - videoControlsComponent.values.endTime) < 1 &&
                // Do not pause if playback was started less than 2 seconds ago.
                new Date().getTime() - videoControlsComponent.playbackStartedTime?.getTime() > 2000
            ) {
                this.pause();
            }
        });
        video.addEventListener('play', function onPlay() {
            videoControlsComponent.playbackStartedTime = new Date();
        });
    }

    private isCollectionRef(node: Node) {
        return node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1;
    }

    private isOwner(node: Node) {
        return node.properties[RestConstants.CM_CREATOR][0] === this.connector.getCurrentLogin().authorityName;
    }
}
