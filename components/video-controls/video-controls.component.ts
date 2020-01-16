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
  _startTime:string = "00:00:00";
  _endTime:string = "00:00:00";
  _title:string = 'Title';

  @Input() set video(video:HTMLVideoElement){
    // timeout to make sure node is already bound
    setTimeout(()=> {
      this._video = video;
      this.track = video.addTextTrack("chapters", "English", "en");
      this.track.mode = "showing";
      this._title = this.node.properties[RestConstants.LOM_PROP_TITLE] ? this.node.properties[RestConstants.LOM_PROP_TITLE][0] : '';
      let vtt = this.node.properties[RestConstants.CCM_PROP_IO_REF_VIDEO_VTT];
      if (vtt && vtt.length===1) {
        vtt = JSON.parse(vtt[0]);
        console.log(vtt);
        this.objectToCues(vtt, this.track);
        const c = vtt[vtt.length-1];
        this._startTime=this.toHHMMSS(c.startTime);
        this._endTime=this.toHHMMSS(c.endTime);
      }
      this.render();
    });
  }
  @Input() node:Node;

  private track:TextTrack;
  private b = document.querySelector("#bar");
  markers:any[]=[];

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
          this.markers = []; //only use the last cue
          this.markers.push(c);
          this._video.currentTime = c.startTime;
        }
      }
    }

    this._video.currentTime = this.markers[0].startTime;
    let that = this;
    this._video.addEventListener("timeupdate", function pausing_function(){
        if(this.currentTime >= that.markers[0].endTime) {
            this.pause();

            if(this.paused){
                this.removeEventListener("timeupdate",pausing_function, false);
            }
            // remove the event listener after you paused the playback
        }
    });
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
    if(this.track.cues){
      while(this.track.cues.length){
        this.track.removeCue(this.track.cues[0]);
      }
  }
    this.track.addCue(new VTTCue(this.secs(this._startTime), this.secs(this._endTime), ''));
    this.render();
    let props:any={};
    console.log(this.track.cues);
    props[RestConstants.CCM_PROP_IO_REF_VIDEO_VTT]=[JSON.stringify(this.cuesToObject((this.track.cues)))];
    props[RestConstants.LOM_PROP_TITLE]=[this._title];
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
