import {Component, Input, Output, EventEmitter} from '@angular/core';

@Component({
  selector: 'file',
  templateUrl: './file.component.html',
})
/**
 * Provides a file picker dialog for uploading files
 */
export class FileComponent {
  @Input() selectedFile : File;
  @Input() accept="";
  @Input() maxSize=-1;
  @Output() OnFileSelected = new EventEmitter();
  constructor() {
  }
  fileDataChanged(event:any) : void {
    // get files and check if available
    var files = event.srcElement.files;
    if (typeof files == "undefined" || files.length<=0) {
      console.log("files = undefined -> ignoring");
      return;
    }
    if (files.length<=0) {
      console.log("files.length = 0 -> ignoring");
      return;
    }

    // get first file
    var file:File = files[0];
    console.log(file);
    // check if file type is correct


    // check max file size
    if (this.maxSize!=-1 && file.size>this.maxSize) {
      console.log("file too big");
      return;
    }

    // remember file for upload
    this.selectedFile = file;
    this.OnFileSelected.emit(file);
  }

}
