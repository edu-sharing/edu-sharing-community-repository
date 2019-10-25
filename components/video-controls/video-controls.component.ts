import {Component, Input, OnInit} from '@angular/core';
import {RestNodeService} from "../../../core-module/rest/services/rest-node.service";
import {Node} from "../../../core-module/rest/data-object"
import {RestConstants} from "../../../core-module/rest/rest-constants";
import {Toast} from "../../toast";

@Component({
  selector: 'video-controls',
  templateUrl: 'video-controls.component.html',
  styleUrls: ['video-controls.component.scss']
})
export class VideoControlsComponent{
  _video: HTMLVideoElement;
  _startTime:any = "00:00:00";
  _endTime:any = "00:00:00";
  _chapterName:any;

  @Input() set video(video:HTMLVideoElement){
    // timeout to make sure node is already bound
    setTimeout(()=> {
      this._video = video;
      this.track = video.addTextTrack("chapters", "English", "en");
      this.track.mode = "showing";
      let vtt = this.node.properties[RestConstants.CCM_PROP_IO_REF_VIDEO_VTT];
      if (vtt && vtt.length == 1) {
        vtt = JSON.parse(vtt[0]);
        this.objectToCues(vtt, this.track);
      }
      this.render();
    });
  }
  @Input() node:Node;

  private track:TextTrack;
  private b = document.querySelector("#bar");
  markers:any[]=[];

  private vtt = ['0, 40.7, Start',
    '40.7, 147.6, Part Two',
    '147.6, 370.7, Somwhere in the middle',
    '370.7, 735, Beginning of the end',]

  constructor(private nodeService : RestNodeService,private toast:Toast) {

  }

  isReadOnly(){
    return this.node.access.indexOf(RestConstants.ACCESS_WRITE)==-1;
  }

  render() {
    this.markers = [];
    for(let t of (this._video.textTracks as any) ) {
      if (t.kind == 'chapters'){
        for(let c of t.cues ) {
          this.markers = this.markers.concat(c);
        }
      }
    }
    console.log(this.markers);
  }

  seek(startTime:number) {
    this._video.currentTime = startTime;
    if(this._video.paused) { this._video.play(); }
  }

  toHHMMSS( sec:any) {
    let sec_num:any = parseInt(sec, 10); // don't forget the second param
    let hours:any   = Math.floor(sec_num / 3600);
    let minutes:any = Math.floor((sec_num - (hours * 3600)) / 60);
    let seconds:any = sec_num - (hours * 3600) - (minutes * 60);
    if (hours   < 10) {hours   = "0"+hours;}
    if (minutes < 10) {minutes = "0"+minutes;}
    if (seconds < 10) {seconds = "0"+seconds;}
    return hours+':'+minutes+':'+seconds;
  }

  secs(hhmmss:any) {
    let a = hhmmss.split(':');
    let seconds = (+a[0]) * 60 * 60 + (+a[1]) * 60 + (+a[2]);
    return seconds;
  }

  updateChapters() {
    this.track.addCue(new VTTCue(this.secs(this._startTime), this.secs(this._endTime), this._chapterName));
    this.render();
    let props:any={};
    console.log(this.track.cues);
    props[RestConstants.CCM_PROP_IO_REF_VIDEO_VTT]=[JSON.stringify(this.cuesToObject((this.track.cues)))];
    this.nodeService.editNodeMetadata(this.node.ref.id,props).subscribe(()=>{
      // no feedback at the moment
    },(error)=>{
      this.toast.error(error);
    });
  }

  getProgress() {
    return this._video.currentTime/this._video.duration*100;
  }

  private objectToCues(obj:any[],track:TextTrack){
    obj.forEach((o)=>{
      track.addCue(new VTTCue(o.startTime,o.endTime,o.text));
    });
  }
  private cuesToObject(cues : TextTrackCueList) {
    let result=[];
    for(let i=0;i<cues.length;i++){
      let c=cues[i];
      result.push({
        startTime:c.startTime,endTime:c.endTime,text:c.text
      });
    }
    return result;
  }
}
