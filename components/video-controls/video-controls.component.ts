import {Component, Input, OnInit} from '@angular/core';
import {RestNodeService} from "../../../core-module/rest/services/rest-node.service";
import {Node} from "../../../core-module/rest/data-object"
import {RestConstants} from "../../../core-module/rest/rest-constants";

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
    this._video = video;
    this.track = video.addTextTrack("chapters", "English", "en");
    this.track.mode = "showing"
    this.vtt.forEach((item)=> {
      let parts = item.split(",");
      this.track.addCue(new VTTCue(parseFloat(parts[0]), parseFloat(parts[1]), parts[2]));
    });
    this.render();
  }
  @Input() node:Node;

  private track:TextTrack;
  private b = document.querySelector("#bar");
  markers:any[]=[];

  private vtt = ['0, 40.7, Start',
    '40.7, 147.6, Part Two',
    '147.6, 370.7, Somwhere in the middle',
    '370.7, 735, Beginning of the end',]

  constructor(private nodeService : RestNodeService) {

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
  }

  getProgress() {
    return this._video.currentTime/this._video.duration*100;
  }

  setStartTime() {
    this._startTime = this.toHHMMSS(this._video.currentTime);
  }

  setEndTime() {
    this._endTime = this.toHHMMSS(this._video.currentTime);
  }
}
