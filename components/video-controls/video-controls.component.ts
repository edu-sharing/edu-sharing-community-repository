import {Component, EventEmitter, Input, Output} from '@angular/core';
import {RestNodeService} from '../../../core-module/rest/services/rest-node.service';
import {Collection, Node} from '../../../core-module/rest/data-object';
import {RestConstants} from '../../../core-module/rest/rest-constants';
import {Toast} from '../../toast';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {UIHelper} from '../../ui-helper';
import {RestCollectionService, RestConnectorService, TemporaryStorageService, UIConstants} from '../../../core-module/core.module';
import {Router} from '@angular/router';
import {BridgeService} from '../../../core-bridge-module/bridge.service';
import { Options, ChangeContext } from 'ng5-slider';

@Component({
    selector: 'video-controls',
    templateUrl: 'video-controls.component.html',
    styleUrls: ['video-controls.component.scss'],
    animations: [trigger('fromRight', UIAnimation.fromRight())],
})
export class VideoControlsComponent {
    _video: HTMLVideoElement;
    _startTime: number = this.secs('00:00:00');
    _endTime: number = this.secs('00:00:00');
    _title = 'Title';
    loading = false;
    chooseCollection = false;
    isGuest: boolean;
    hasPermission: boolean;

    display_startTime = this.toHHMMSS(this._startTime);
    display_endTime = this.toHHMMSS(this._endTime);

    options: Options = {
        floor: 0,
        ceil: 100,
    };

    onUserChangeEnd(changeContext: ChangeContext): void {
        this.display_startTime = this.toHHMMSS(this._startTime);
        this.display_endTime = this.toHHMMSS(this._endTime);
    }

    display_startTime = this.toHHMMSS(this._startTime);
    display_endTime = this.toHHMMSS(this._endTime);

    options: Options = {
        floor: 0,
        ceil: 100,
    };

    onUserChangeEnd(changeContext: ChangeContext): void {
        this.display_startTime = this.toHHMMSS(this._startTime);
        this.display_endTime = this.toHHMMSS(this._endTime);
    }

    @Input() set video(video: HTMLVideoElement) {
        // timeout to make sure node is already bound
        setTimeout(() => {
            this._video = video;
            this.track = video.addTextTrack('chapters', 'English', 'en');
            this.track.mode = 'showing';
            this._title = this.node.properties[RestConstants.LOM_PROP_TITLE]
                ? this.node.properties[RestConstants.LOM_PROP_TITLE][0]
                : '';
            let vtt = this.node.properties[
                RestConstants.CCM_PROP_IO_REF_VIDEO_VTT
            ];
            if (vtt && vtt.length === 1) {
                vtt = JSON.parse(vtt[0]);
                this.objectToCues(vtt, this.track);
                const c = vtt[vtt.length - 1];
                if (c) {
                    this._startTime = c.startTime;
                    this._endTime = c.endTime;
                }
            }
            this.render();
        });
    }
    @Input() node: Node;
    @Output() save = new EventEmitter<VideoData>();

    markers: any[] = [];

    private track: TextTrack;
    private b = document.querySelector('#bar');

  constructor(
              private nodeService: RestNodeService,
              private collectionService: RestCollectionService,
              private connector: RestConnectorService,
              private router: Router,
              private bridge: BridgeService,
              private toast: Toast,
              private temporaryStorage: TemporaryStorageService) {
    this.isGuest = this.connector.getCurrentLogin().isGuest;
    this.hasPermission = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_VIDEO_AUDIO_CUT);
  }

  render() {
        this.markers = [];
        this._video.onloadedmetadata = () => {
          this.options.ceil = this._video.duration;
          if( this._endTime == 0){
            this._endTime = this._video.duration;
          }
          this.options = {
                floor: 0,
                ceil: this._video.duration,
                animate: true,
                draggableRange: true,
                minRange: 1,
                translate: (value: number): string => {
                      return this.toHHMMSS(value);
                    }
              };

        };

        for (const t of this._video.textTracks as any) {
            if (t.kind === 'chapters') {
                for (const c of t.cues) {
                    this.markers = []; // only use the last cue
                    this.markers.push(c);
                    this._video.currentTime = c.startTime;
                    this._startTime = c.startTime;
                    this._endTime = c.endTime;
                }
            }
        }
        if (this.markers.length) {
            this._video.currentTime = this.markers[0].startTime;
        }
        const that = this;
        this._video.addEventListener('timeupdate', function pausing_function() {
            if (this.currentTime >= that.markers[0].endTime) {
                this.pause();

                if (this.paused) {
                    this.removeEventListener(
                        'timeupdate',
                        pausing_function,
                        false,
                    );
                }
                // remove the event listener after you paused the playback
            }
        });
    }

    seek(startTime: number) {
        this._video.currentTime = startTime;
        if (this._video.paused) {
            this._video.play();
        }
    }

    toHHMMSS(sec: any) {
        const sec_num: number = parseInt(sec, 10); // don't forget the second param
        let hours: number = Math.floor(sec_num / 3600);
        let minutes: number = Math.floor((sec_num - hours * 3600) / 60);
        let seconds: number = sec_num - hours * 3600 - minutes * 60;

        if (minutes < 10) {
            minutes = '0' + minutes;
        }
        if (seconds < 10) {
            seconds = '0' + seconds;
        }
        if (hours == 0) {
            return minutes + ':' + seconds;
        }else if (hours < 10) {
            hours = '0' + hours;
        }
        return hours + ':' + minutes + ':' + seconds;
    }

    secs(hhmmss: any) {
        const a = hhmmss.split(':');
        const seconds:number =  +a[0] * 60 * 60 + +a[1] * 60 + +a[2];
        return seconds;
    }

    onStartKey(){
        this._startTime = this.secs(this.display_startTime);
    }

    onEndKey(){
        this._endTime = this.secs(this.display_endTime);
    }

    updateChapters() {
        if (!this.isCollectionRef()) {
            // not an individual object, choose new location first
            this.chooseCollection = true;
            return;
        }
        this.loading = true;
        if (this.track.cues) {
            while (this.track.cues.length) {
                this.track.removeCue(this.track.cues[0]);
            }
        }
        this.track.addCue(
            new VTTCue(
                this._startTime,
                this._endTime,
                '',
            ),
        );
        this.render();
        const props: any = {};
        props[RestConstants.CCM_PROP_IO_REF_VIDEO_VTT] = [
            JSON.stringify(this.cuesToObject(this.track.cues)),
        ];
        props[RestConstants.LOM_PROP_TITLE] = [this._title];
        this.nodeService.editNodeMetadata(this.node.ref.id, props).subscribe(
            node => {
                // no feedback at the moment
                this.save.emit({
                    node: node.node,
                    startTime: this._startTime,
                    endTime: this._endTime,
                });
                this.loading = false;
                this.toast.toast('VIDEO_CONTROLS.SAVED');
            },
            error => {
                this.toast.error(error);
                this.loading = false;
            },
        );
    }

    getProgress() {
        return (this._video.currentTime / this._video.duration) * 100;
    }

    addToCollection(collection: Node) {
        this.chooseCollection = false;
        this.loading = true;
        UIHelper.addToCollection(
            this.collectionService,
            this.router,
            this.bridge,
            collection,
            [this.node],
            (elements: Node[]) => {
                if (elements.length) {
                    this.node = elements[0];
                    this.updateChapters();
                } else {
                    this.loading = false;
                }
            },
        );
    }

    createCollectionAndAdd(root?: Node) {
        this.temporaryStorage.set(
            TemporaryStorageService.COLLECTION_ADD_NODES,
            [this.node],
        );
        this.router.navigate([
            UIConstants.ROUTER_PREFIX,
            'collections',
            'collection',
            'new',
            root ? root.ref.id : RestConstants.ROOT,
        ]);
    }

    private isCollectionRef() {
        return (
            this.node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !==
            -1
        );
    }

    private objectToCues(obj: any[], track: TextTrack) {
        obj.forEach(o => {
            track.addCue(new VTTCue(o.startTime, o.endTime, o.text));
        });
    }

    private cuesToObject(cues: TextTrackCueList) {
        const result = [];
        for (let i = 0; i < cues.length; i++) {
            const c = cues[i];
            result.push({
                startTime: c.startTime,
                endTime: c.endTime,
                text: c.text,
            });
        }
        return result;
    }
}

export class VideoData {
    node: Node;
    startTime: number;
    endTime: number;
}
