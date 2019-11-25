import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  NgZone,
  HostListener,
  ViewChild,
  ElementRef,
  Sanitizer, ViewContainerRef, ComponentFactoryResolver, QueryList, ViewChildren
} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {RestMdsService} from '../../rest/services/rest-mds.service';
import {MdsMetadataset, View, Type, Node, NodeList, NodeWrapper, MdsValueList, Mds} from '../../rest/data-object';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';
import {RestNodeService} from '../../rest/services/rest-node.service';
import {RestConstants} from '../../rest/rest-constants';
import {Translation} from '../../translation';
import {HtmlParser} from '@angular/compiler';
import {ActivatedRoute} from '@angular/router';
import {Toast} from '../toast';
import {VCard} from '../../VCard';
import {Helper} from '../../helper';
import {ConfigurationService} from '../../services/configuration.service';
import {SessionStorageService} from '../../services/session-storage.service';
import {RestConnectorService} from '../../rest/services/rest-connector.service';
import {RestToolService} from '../../rest/services/rest-tool.service';
import {UIHelper} from '../ui-helper';
import {RestHelper} from '../../rest/rest-helper';
import {NodeHelper} from '../node-helper';
import {RestLocatorService} from '../../rest/services/rest-locator.service';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../ui-animation';
import {DialogButton} from '../modal-dialog/modal-dialog.component';
import {UIService} from '../../services/ui.service';
import {ConfigurationHelper} from "../../rest/configuration-helper";
import {RestSearchService} from '../../rest/services/rest-search.service';
import {RestUtilitiesService} from "../../rest/services/rest-utilities.service";
import {MdsHelper} from "../../rest/mds-helper";
import {withIdentifier} from "codelyzer/util/astQuery";
import {MdsWidgetComponent} from "./widget/mds-widget.component";

@Component({
  selector: 'mds-viewer',
  templateUrl: 'mds-viewer.component.html',
  styleUrls: ['mds-viewer.component.scss'],
})
export class MdsViewerComponent{
  @ViewChildren('container') container : QueryList<ElementRef>;
  _groupId:string;
  _data: string;
  private mds: any;
  private templates: any[];
  @Input() set groupId(groupId:string){
    this._groupId=groupId;
    this.inflate();
  }
  @Input() set data(data:string){
    this._data=data;
    this.inflate();
  }
  /**
   * show group headings (+ icons) for the individual templates
   */
  @Input() showGroupHeadings=true;
  getGroup(){
    return this.mds.groups.find((g:any)=>g.id==this._groupId);
  }
  getView(id:string){
    return this.mds.views.find((v:any)=>v.id==id);
  }
  private inflate() {
    if(!this.mds) {
      setTimeout(() => this.inflate(), 1000 / 60);
      return;
    }
    this.templates=[];
    for(let view of this.getGroup().views){
        let v=this.getView(view);
        this.templates.push({view:v,html:this.sanitizer.bypassSecurityTrustHtml(this.prepareHTML(v.html))});
    }
    // wait for angular to inflate the new binding
    setTimeout(()=>{
      for(let w of this.mds.widgets) {
        // @TODO: it would be better to filter by widgets based on template and condition, should be implemented in 5.1
        this.container.toArray().forEach((c) => {
          let element = c.nativeElement.getElementsByTagName(w.id);
          if (element && element[0]) {
            UIHelper.injectAngularComponent(this.factoryResolver, this.containerRef, MdsWidgetComponent, element[0], {
              widget: w,
              data: this._data[w.id]
            });
          }
        });
      }
    });
  }
  constructor(private mdsService : RestMdsService,
              private factoryResolver:ComponentFactoryResolver,
              private containerRef:ViewContainerRef,
              private sanitizer:DomSanitizer){
    this.mdsService.getSet().subscribe((mds)=>{
      this.mds=mds;
    });
  }

  /**
   * close all custom tags inside the html which are not closed
   * e.g. <cm:name>
   *     -> <cm:name></cm:name>
   * @param html
   */
  private prepareHTML(html:string) {
    for(let w of this.mds.widgets){
      let start=html.indexOf('<'+w.id);
      if(start==-1) continue;
      let end=html.indexOf('>',start)+1;
      html=html.substring(0,end)+'</'+w.id+'>'+html.substring(end);
    }
    return html;
  }
}
