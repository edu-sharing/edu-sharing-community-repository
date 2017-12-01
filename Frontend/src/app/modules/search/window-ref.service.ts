import { Injectable } from '@angular/core';

function _window() : any {
   // return the global native browser window object
   return window;
}


@Injectable()
export class WindowRefService {

  constructor() { }

   getNativeWindow() : any {
      return _window();
   }

}
