import { Injectable } from '@angular/core';
import {FrameEventsService} from "./frame-events.service";

export interface GwtEventListener {
  onGwtEvent(command:string, message:any) : void;
}

@Injectable()
export class GwtInterfaceService {

  // keep a list of all listeners
  gwtEventListeners: Array<GwtEventListener>;

  constructor(private frame : FrameEventsService) {

    this.gwtEventListeners = new Array<GwtEventListener>();

    // TODO: do scope better with new typescript possibilities
    var that = this;
    window.addEventListener('message', function (m:any) {
      if ((typeof m.data !== 'undefined') && (typeof m.data.command !== 'undefined')) {

        // alert('Got event from GWT: command('+m.data.command+') message('+m.data.message+')');
        // alert('Length Listeners: '+that.gwtEventListeners.length);

        // send to all GWT event listeners
        that.gwtEventListeners.forEach(function(listener:GwtEventListener){
          listener.onGwtEvent(m.data.command, m.data.message);
        });

      }
    }, false);

  }

  /**
   * add a listener on GWT events
   * @param listener
   */
  addListenerOfGwtEvents(listener:GwtEventListener) : void {
    this.gwtEventListeners.push(listener);
    console.log('OK Added GWT event listener');
  }

  /**
   * remove a listener on GWT events
   * @param listener
   */
  removeListenerOfGwtEvents(listener:GwtEventListener) : void {
    var index = this.gwtEventListeners.indexOf(listener, 0);
    if (index > -1) {
      this.gwtEventListeners.splice(index, 1);
    }
  }

  /*
   * PREPARED GWT EVENTS
   */

  sendEventTest() : void {
    this.sendEvent('test', 'Hello GWT from Angular.');
  }

  sendEventContentDetail(repo:String, nodeid:string) : void {
    this.sendEvent('content_detail', {"repo": repo, "nodeid": nodeid});
  }

  sendEventSwitchToSearchView(collectionId:String, breadcrumbsJSON:String) : void {
    var navigationData = {
      'fromCollection': collectionId,
    };
    // torsten: The Data does not come to gwt for some reason, so we just add the id to the command
    this.sendEvent('collectFromSearch:'+collectionId, navigationData);
  }

  /**
   * sends a message event to GWT running in parent frame or in same window
   * @param command
   * @param message
   */
  sendEvent(command:string, message:any) {
    if (this.frame.isRunningInFrame()) {
      console.log("posting "+command+" "+message);
      window.parent.postMessage({command: command, message: message}, '*');
    } else {
      window.postMessage({command: command, message: message}, '*');
    }
  }


}
