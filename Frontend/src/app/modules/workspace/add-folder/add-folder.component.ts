import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestMdsService} from "../../../common/rest/services/rest-mds.service";
import {MdsMetadatasets, Node, MdsInfo} from "../../../common/rest/data-object";
import {TranslateService} from "@ngx-translate/core";
import {RestHelper} from "../../../common/rest/rest-helper";

@Component({
  selector: 'workspace-add-folder',
  templateUrl: 'add-folder.component.html',
  styleUrls: ['add-folder.component.scss']
})
export class WorkspaceAddFolder  {
  @ViewChild('input') input : ElementRef;
  public disabled=true;
  public _folder="";
  public mdsSetsIds: MdsInfo[];
  public mdsSets: MdsInfo[];
  public mdsSet: string;
  private _parent: Node;
  @Input() set folder(folder : string){
    this._folder=folder;
    this.input.nativeElement.focus();
  }
  @Input() set parent(parent : Node){
    this.mds.getSets().subscribe((data:MdsMetadatasets)=>{
      this.mdsSets=data.metadatasets;
      if(this.mdsSets) {
        RestHelper.prepareMetadatasets(this.translate,this.mdsSets);
        this.mdsSet = this.mdsSets[0].id;
      }
      this._parent=parent;
      if(this._parent && this._parent.metadataset && this._parent.metadataset!="default")
        this.mdsSet=this._parent.metadataset;
      this.input.nativeElement.focus();
    })


  }
  @Output() onCancel=new EventEmitter();
  @Output() onFolderAdded=new EventEmitter();
  constructor(private mds:RestMdsService,private translate : TranslateService){

  }
  public cancel(){
    this.onCancel.emit();
  }
  public addFolder(){
    if(this.disabled)
      return;
    this.onFolderAdded.emit({name:this._folder,metadataset:this.mdsSets ? this.mdsSet : null});
  }
  public setState(event : any){
    this.disabled=!this._folder.trim();
  }
}
