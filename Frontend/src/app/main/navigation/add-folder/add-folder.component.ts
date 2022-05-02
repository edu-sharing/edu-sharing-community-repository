import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {DialogButton, RestMdsService} from '../../../core-module/core.module';
import {MdsMetadatasets, Node, MdsInfo} from '../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {ConfigurationService} from '../../../core-module/core.module';
import {RestConstants} from '../../../core-module/core.module';
import {ConfigurationHelper} from '../../../core-module/core.module';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../../core-module/ui/ui-animation';

@Component({
  selector: 'es-workspace-add-folder',
  templateUrl: 'add-folder.component.html',
  styleUrls: ['add-folder.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class WorkspaceAddFolder  {
  public _folder= '';
  public mdsSetsIds: MdsInfo[];
  public mdsSets: MdsInfo[];
  public mdsSet: string;
  _parent: Node;
  buttons: DialogButton[];
  @Input() set folder(folder: string){
    this._folder = folder;
    this.updateButtons();
  }
  get folder(){
    return this._folder;
  }
  @Input() set parent(parent: Node){
    this.mds.getSets().subscribe((data: MdsMetadatasets) => {
      this.mdsSets = ConfigurationHelper.filterValidMds(RestConstants.HOME_REPOSITORY, data.metadatasets, this.config);
      if (this.mdsSets) {
        UIHelper.prepareMetadatasets(this.translate, this.mdsSets);
        if (this.mdsSets.length) {
            this.mdsSet = this.mdsSets[0].id;
        }
        else{
          console.error('Filtering valid mds failed, no mds was available after filtering. Will use default mds');
          console.error('Check availableMds in config');
        }
      }
      this._parent = parent;
      if (this._parent && this._parent.metadataset && this._parent.metadataset !== 'default') {
        this.mdsSet = this._parent.metadataset;
      }
    });


  }
  @Output() onCancel= new EventEmitter();
  @Output() onFolderAdded= new EventEmitter();
  constructor(private mds: RestMdsService,
              private translate: TranslateService,
              private config: ConfigurationService){
    this.updateButtons();
  }
  public cancel(){
    this.onCancel.emit();
  }
  public addFolder(){
    if (!this._folder.trim()) {
      return;
    }
    this.onFolderAdded.emit({name: this._folder, metadataset: this.mdsSets ? this.mdsSet : null});
  }

  private updateButtons() {
    this.buttons = [
      new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.cancel()),
      new DialogButton('SAVE', DialogButton.TYPE_PRIMARY, () => this.addFolder())
    ];
    this.buttons[1].disabled = !this._folder.trim();
  }
}
