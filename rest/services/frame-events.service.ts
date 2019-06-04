import { Injectable } from '@angular/core';
export interface EventListener {
  onEvent(event:string, data:any) : void;
}
@Injectable()
export class FrameEventsService {

  public static EVENT_UPDATE_LOGIN_STATE="UPDATE_LOGIN_STATE";
  public static EVENT_USER_LOGGED_IN="USER_LOGGED_IN";
  public static EVENT_USER_LOGGED_OUT="USER_LOGGED_OUT";
  public static EVENT_VIEW_SWITCHED="VIEW_SWITCHED";
  public static EVENT_VIEW_OPENED="VIEW_OPENED";
  public static EVENT_SHARED="SHARED";
  public static EVENT_NODE_FOLDER_OPENED="NODE_FOLDER_OPENED";
  public static EVENT_GLOBAL_SEARCH="GLOBAL_SEARCH";
  public static EVENT_CONTENT_HEIGHT="CONTENT_HEIGHT";
  public static EVENT_INVALIDATE_HEIGHT="INVALIDATE_HEIGHT";
  public static EVENT_SESSION_TIMEOUT="SESSION_TIMEOUT";
  public static EVENT_APPLY_NODE="APPLY_NODE";
  public static EVENT_NODE_SAVED="NODE_SAVED";
    /**
     * Hint to ng that the content has changed (e.g. via a connector) and should be reloaded
     * @type {string}
     */
  public static EVENT_REFRESH="REFRESH";
  public static EVENT_CLOSE="CLOSE";
  public static EVENT_CORDOVA_CAMERA="EVENT_CORDOVA_CAMERA";
  public static EVENT_CORDOVA_CAMERA_RESPONSE="EVENT_CORDOVA_CAMERA_RESPONSE";
  public static EVENT_REST_RESPONSE="PARENT_REST_RESPONSE";

  public static INVALIDATE_HEIGHT_EVENTS=[
    FrameEventsService.EVENT_VIEW_SWITCHED,
    FrameEventsService.EVENT_VIEW_OPENED,
    FrameEventsService.EVENT_NODE_FOLDER_OPENED
    ];

  // incomming events
  public static EVENT_PARENT_SCROLL="PARENT_SCROLL";
  public static EVENT_PARENT_SEARCH="PARENT_SEARCH";
  public static EVENT_PARENT_ADD_NODE_URL="PARENT_ADD_NODE_URL";
  public static EVENT_PARENT_REST_REQUEST="PARENT_REST_REQUEST";
  public static EVENT_UPDATE_SESSION_TIMEOUT="UPDATE_SESSION_TIMEOUT";

  private eventListeners :EventListener[]=[];
  private windows: Window[]=[];

  constructor() {
    let t=this;
    window.addEventListener('message', (event:any)=>this.onEvent(event), false);
      setInterval(()=>{
        this.broadcastEvent(FrameEventsService.EVENT_CONTENT_HEIGHT,document.body.scrollHeight);
      },250);
  }

  /**
   * Add a window which should be notified (a handle returned by window.open)
   * @param window
   */
  public addWindow(window:Window){
    this.windows.push(window);
  }
  public onEvent(event:any){
      if (event.source!==window.self && event.data){
          if(event.data.event==FrameEventsService.EVENT_CLOSE){
              event.source.close();
              let pos=this.windows.indexOf(event.source);
              if(pos!=-1)
                this.windows.splice(pos,1);
              return;
          }
          this.eventListeners.forEach(function(listener:EventListener){
              listener.onEvent(event.data.event,event.data.data);
          });
      }
  }
  public addListener(listener:EventListener) : void {
    this.eventListeners.push(listener);
  }
  /**
   * sends a message to a parent view
   * @param event use one constant from the event list
   * @param message can be an object with more information
   */
  public broadcastEvent(event:string, data:any=null) {
    if(FrameEventsService.INVALIDATE_HEIGHT_EVENTS.indexOf(event)!=-1){
      this.broadcastEvent(FrameEventsService.EVENT_INVALIDATE_HEIGHT);
    }
    if (this.isRunningInFrame()) {
      window.parent.postMessage({event: event, data: data}, '*');
    } else if(window.opener) {
      window.opener.postMessage({event: event, data: data}, '*');
    } else{
      window.postMessage({event: event, data: data}, '*');
    }
    for(let w of this.windows){
      try {
        w.postMessage({event: event, data: data}, '*');
      }catch(e){
        // The window has may be closed
      }
    }
  }

  /**
   * checks if the actual script is running in a frame
   * @returns {boolean}
   */
  public isRunningInFrame() : boolean {
    try {
      return window.self !== window.top;
    } catch (e) {
      return true;
    }
  }

}
