
import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import { HttpModule } from '@angular/http';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {RestSearchService} from '../../common/rest/services/rest-search.service';
import {RestMetadataService} from '../../common/rest/services/rest-metadata.service';
import {RestNodeService} from '../../common/rest/services/rest-node.service';
import {RestConstants} from '../../common/rest/rest-constants';
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {Node, NodeList, LoginResult} from "../../common/rest/data-object";
import {OptionItem} from "../../common/ui/actionbar/option-item";
import {TemporaryStorageService} from "../../common/services/temporary-storage.service";
import {UIHelper} from "../../common/ui/ui-helper";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../common/services/configuration.service";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {UIConstants} from "../../common/ui/ui-constants";
import {RestMdsService} from "../../common/rest/services/rest-mds.service";
import {RestHelper} from "../../common/rest/rest-helper";
import {ListItem} from "../../common/ui/list-item";
import {MdsHelper} from "../../common/rest/mds-helper";
import {Observable} from 'rxjs/Rx';
import {RestStreamService} from "../../common/rest/services/rest-stream.service";



@Component({
  selector: 'app-stream',
  templateUrl: 'stream.component.html',
  styleUrls: ['stream.component.scss'],
  })



export class StreamComponent {
  test = 'sdsd';
  streams: any;
  actionOptions:OptionItem[]=[];
  // TODO: Store and use current search query
  searchQuery:string;
  doSearch(query:string){
    this.searchQuery=query;
    console.log(query);
    // TODO: Search for the given query
  }
  constructor(
    private router : Router,
    private route : ActivatedRoute,
    private connector:RestConnectorService,
    private nodeService: RestNodeService,
    private searchService: RestSearchService,
    private metadataService:RestMetadataService,
    private streamService:RestStreamService,
    private storage : TemporaryStorageService,
    private session : SessionStorageService,
    private title : Title,
    private config : ConfigurationService,
    private http: Http,
    private translate : TranslateService) {
      Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=>{
        UIHelper.setTitle('STREAM.TITLE',title,translate,config);
      });
      // please refer to http://appserver7.metaventis.com/ngdocs/4.1/classes/optionitem.html
      this.actionOptions.push(new OptionItem('EXAMPLE 1','cloud',()=>{
        alert('callback 1');
      }));
      this.actionOptions.push(new OptionItem('EXAMPLE 2','adb',()=>{
          alert('callback 2');
      }));
      this.getJSON().subscribe(data => this.streams = data['stream'], error => console.log(error));

  }

  onScroll() {
    console.log("scrolled!!");
    this.getJSON().subscribe(data => this.streams = this.streams.concat(data['stream']), error => console.log(error));
  }

  sortieren() {
    // here is going to be the sorting functionality: 
    console.log(this.streams);
   // let temp = this.other['stream'].shift();
    //this.other['stream'].push(temp);
  }

  // the way of doing the post request will be changed:
  public getJSON(): Observable<any> {
    return this.streamService.getStream();
  }


}
