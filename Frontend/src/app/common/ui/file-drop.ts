import {
  Directive,
  EventEmitter,
  ElementRef,
  HostListener,
  Input,
  Output,
} from '@angular/core';

export interface Options {
  readAs?: string;
}

@Directive({ selector: '[fileDrop]' })
export class FileDropDirective {
  @Output() public fileOver: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() public onFileDrop: EventEmitter<File> = new EventEmitter<File>();
  @Input() public options: Options;

  private element: ElementRef;

  public constructor(
    element: ElementRef
  ) {
    this.element = element;
  }

  private isFileOver=false;

  @HostListener('dragover', [
    '$event',
  ])
  public onDragOver(event: any): void {
    let transfer = this.getDataTransfer(event);
    if (!this.haveFiles(transfer.types)) {
      return;
    }
    transfer.dropEffect = 'copy';
    this.preventAndStop(event);
    this.isFileOver=true;
    this.emitFileOver(true);
  }


  @HostListener('dragleave', [
    '$event',
  ])
  public onDragLeave(event: any): void {
    this.isFileOver=false;
    // super hacky, but the only reliable way it seems. May someone will fix this later?
    setTimeout(()=>{
      if( !this.isFileOver || true ){ this.emitFileOver(false); }
    },2000);
  }
  @HostListener('dragenter', [
    '$event',
  ])
  public onDragEnter(event: any): void {
    let transfer = this.getDataTransfer(event);
    if (!this.haveFiles(transfer.types)) {
      return;
    }

    this.preventAndStop(event);
    this.isFileOver=true;
    this.emitFileOver(true);
  }

  checkLeave(event:any){


    if (event.currentTarget == this.element.nativeElement) {
      return;
    }



    let parent=event.target.parentNode;
    while(parent){
      if(parent==this.element.nativeElement){
        return;
      }

      parent=parent.parentNode;
    }

    this.preventAndStop(event);
    this.emitFileOver(false);
  }

  @HostListener('drop', [
    '$event',
  ])
  public onDrop(event: any): void {
    console.log("onDrop");
    const transfer = this.getDataTransfer(event);

    if (!transfer.files.length) {
      return;
    }

    this.preventAndStop(event);
    this.emitFileOver(false);
    this.readFile(transfer.files);
  }

  private readFile(file: FileList): void {
    const strategy = this.pickStrategy();

    if (!strategy) {
      this.emitFileDrop(file);
    } else {
      /*
      // XXX Waiting for angular/zone.js#358
      const method = `readAs${strategy}`;

      FileAPI[method](file, (event) => {
        if (event.type === 'load') {
          this.emitFileDrop(event.result);
        } else if (event.type === 'error') {
          throw new Error(`Couldn't read file '${file.name}'`);
        }
      });
      */
    }

  }

  private emitFileOver(isOver: boolean): void {
    this.fileOver.emit(isOver);
  }

  private emitFileDrop(file: any): void {
    this.onFileDrop.emit(file);
  }

  private pickStrategy(): string | void {
    if (!this.options) {
      return;
    }

    if (this.hasStrategy(this.options.readAs)) {
      return this.options.readAs;
    }
  }

  private hasStrategy(type: string): boolean {
    return [
      'DataURL',
      'BinaryString',
      'ArrayBuffer',
      'Text',
    ].indexOf(type) !== -1;
  }

  private getDataTransfer(event: any | any): DataTransfer {

    return event.dataTransfer ? event.dataTransfer : event.originalEvent.dataTransfer;
  }

  private preventAndStop(event: any): void {
    event.preventDefault();
    event.stopPropagation();
  }

  private haveFiles(types: any): boolean {
    if (!types) {
      return false;
    }

    if (types.indexOf) {
      return types.indexOf('Files') !== -1;
    }

    if (types.contains) {
      return types.contains('Files');
    }

    return false;
  }
}
