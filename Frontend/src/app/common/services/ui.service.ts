
import {Injectable} from "@angular/core";
@Injectable()
export class UIService {
  /** Returns true if the current sessions seems to be running on a mobile device
   *
   */
  public isMobile(){
    // http://stackoverflow.com/questions/11381673/detecting-a-mobile-browser
    if( navigator.userAgent.match(/Android/i)
      || navigator.userAgent.match(/webOS/i)
      || navigator.userAgent.match(/iPhone/i)
      || navigator.userAgent.match(/iPad/i)
      || navigator.userAgent.match(/iPod/i)
      || navigator.userAgent.match(/BlackBerry/i)
      || navigator.userAgent.match(/Windows Phone/i)
    ){
      return true;
    }
    else {
      return false;
    }

  }
}
